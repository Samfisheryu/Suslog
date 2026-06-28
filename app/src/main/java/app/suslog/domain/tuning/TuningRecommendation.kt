package app.suslog.domain.tuning

import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.setup.defaultClick
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.Corner
import app.suslog.domain.suspension.StiffSide
import kotlin.math.roundToInt

data class TuningVariableKey(
    val axle: Axle,
    val adjusterLabel: String,
)

data class TuningModelPoint(
    val stateIndex: Int,
    val state: SetupConfigState,
    val x: Double,
    val clickValue: Int,
    val score: Double,
)

data class QuadraticFit(
    val a: Double,
    val b: Double,
    val c: Double,
    val recommendedX: Double,
    val predictedScore: Double,
    val hasPeakInTestedRange: Boolean,
)

data class TuningRecommendation(
    val variable: TuningVariableKey?,
    val cleanPointCount: Int,
    val distinctPointCount: Int,
    val bestObserved: TuningModelPoint?,
    val fit: QuadraticFit?,
    val recommendedClick: Int?,
    val message: String,
)

fun buildTuningRecommendation(
    car: CarProfile,
    config: SetupConfig,
    selectedStateIndex: Int,
): TuningRecommendation {
    val cleanPoints = config.states.mapIndexedNotNull { index, state ->
        if (!state.hasFeedback) return@mapIndexedNotNull null

        val summary = summarizeSetupChange(
            previousState = config.states.getOrNull(index - 1),
            currentState = state
        )
        val axle = summary.axle ?: return@mapIndexedNotNull null
        val adjusterLabel = summary.adjusterLabel ?: return@mapIndexedNotNull null
        if (!summary.includedInModel) return@mapIndexedNotNull null

        val adjuster = car.adjusters.firstOrNull { it.label == adjusterLabel }
            ?: return@mapIndexedNotNull null
        val x = state.setup.normalizedAxleStiffness(
            axle = axle,
            adjuster = adjuster
        )

        TuningModelPoint(
            stateIndex = index,
            state = state,
            x = x,
            clickValue = state.setup.axleClickValue(
                axle = axle,
                adjuster = adjuster
            ),
            score = SetupScoreV1.compute(state).total
        ) to TuningVariableKey(axle = axle, adjusterLabel = adjusterLabel)
    }

    val preferredVariable = preferredVariable(
        cleanPoints = cleanPoints,
        config = config,
        selectedStateIndex = selectedStateIndex
    )
    if (preferredVariable == null) {
        return TuningRecommendation(
            variable = null,
            cleanPointCount = 0,
            distinctPointCount = 0,
            bestObserved = null,
            fit = null,
            recommendedClick = null,
            message = "No clean axle-adjuster points yet."
        )
    }

    val points = cleanPoints
        .filter { (_, key) -> key == preferredVariable }
        .map { (point, _) -> point }
    val bestObserved = points.maxByOrNull { it.score }
    val distinctPoints = points
        .groupBy { it.x.roundForGrouping() }
        .map { (_, grouped) ->
            val first = grouped.first()
            first.copy(
                x = grouped.map { it.x }.average(),
                clickValue = grouped.map { it.clickValue }.average().roundToInt(),
                score = grouped.map { it.score }.average()
            )
        }
        .sortedBy { it.x }

    val adjuster = car.adjusters.first { it.label == preferredVariable.adjusterLabel }
    val fit = if (distinctPoints.size >= 3) {
        quadraticFit(distinctPoints)
    } else {
        null
    }
    val recommendedX = if (fit?.hasPeakInTestedRange == true) {
        fit.recommendedX
    } else {
        nextObservedStep(
            points = distinctPoints,
            adjuster = adjuster
        )
    }
    val recommendedClick = recommendedX?.let {
        normalizedStiffnessToClick(
            normalized = it,
            adjuster = adjuster
        )
    }

    return TuningRecommendation(
        variable = preferredVariable,
        cleanPointCount = points.size,
        distinctPointCount = distinctPoints.size,
        bestObserved = bestObserved,
        fit = fit,
        recommendedClick = recommendedClick,
        message = recommendationMessage(
            distinctPointCount = distinctPoints.size,
            fit = fit
        )
    )
}

private fun preferredVariable(
    cleanPoints: List<Pair<TuningModelPoint, TuningVariableKey>>,
    config: SetupConfig,
    selectedStateIndex: Int,
): TuningVariableKey? {
    val selectedSummary = config.states.getOrNull(selectedStateIndex)?.let { selected ->
        summarizeSetupChange(
            previousState = config.states.getOrNull(selectedStateIndex - 1),
            currentState = selected
        )
    }
    if (selectedSummary?.includedInModel == true) {
        val axle = selectedSummary.axle
        val adjusterLabel = selectedSummary.adjusterLabel
        if (axle != null && adjusterLabel != null) {
            return TuningVariableKey(axle = axle, adjusterLabel = adjusterLabel)
        }
    }

    return cleanPoints
        .groupBy { it.second }
        .maxWithOrNull(
            compareBy<Map.Entry<TuningVariableKey, List<Pair<TuningModelPoint, TuningVariableKey>>>> {
                it.value.size
            }.thenBy {
                it.value.maxOf { (point, _) -> point.stateIndex }
            }
        )
        ?.key
}

private fun quadraticFit(points: List<TuningModelPoint>): QuadraticFit? {
    val n = points.size.toDouble()
    val sx = points.sumOf { it.x }
    val sx2 = points.sumOf { it.x * it.x }
    val sx3 = points.sumOf { it.x * it.x * it.x }
    val sx4 = points.sumOf { it.x * it.x * it.x * it.x }
    val sy = points.sumOf { it.score }
    val sxy = points.sumOf { it.x * it.score }
    val sx2y = points.sumOf { it.x * it.x * it.score }

    val solution = solve3x3(
        matrix = arrayOf(
            doubleArrayOf(n, sx, sx2),
            doubleArrayOf(sx, sx2, sx3),
            doubleArrayOf(sx2, sx3, sx4)
        ),
        values = doubleArrayOf(sy, sxy, sx2y)
    ) ?: return null
    val a = solution[0]
    val b = solution[1]
    val c = solution[2]
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val rawPeak = if (c < 0.0) -b / (2.0 * c) else null
    val hasPeakInRange = rawPeak != null && rawPeak in minX..maxX
    val recommendedX = when {
        rawPeak != null -> rawPeak.coerceIn(minX, maxX)
        else -> points.maxBy { it.score }.x
    }
    val predictedScore = a + b * recommendedX + c * recommendedX * recommendedX

    return QuadraticFit(
        a = a,
        b = b,
        c = c,
        recommendedX = recommendedX,
        predictedScore = predictedScore.coerceIn(0.0, 100.0),
        hasPeakInTestedRange = hasPeakInRange
    )
}

private fun solve3x3(
    matrix: Array<DoubleArray>,
    values: DoubleArray,
): DoubleArray? {
    val a = Array(3) { row -> matrix[row].copyOf() }
    val b = values.copyOf()

    for (pivot in 0 until 3) {
        val maxRow = (pivot until 3).maxBy { row -> kotlin.math.abs(a[row][pivot]) }
        if (kotlin.math.abs(a[maxRow][pivot]) < 1e-9) return null

        val tempRow = a[pivot]
        a[pivot] = a[maxRow]
        a[maxRow] = tempRow

        val tempValue = b[pivot]
        b[pivot] = b[maxRow]
        b[maxRow] = tempValue

        val divisor = a[pivot][pivot]
        for (column in pivot until 3) {
            a[pivot][column] /= divisor
        }
        b[pivot] /= divisor

        for (row in 0 until 3) {
            if (row == pivot) continue
            val factor = a[row][pivot]
            for (column in pivot until 3) {
                a[row][column] -= factor * a[pivot][column]
            }
            b[row] -= factor * b[pivot]
        }
    }

    return b
}

private fun nextObservedStep(
    points: List<TuningModelPoint>,
    adjuster: AdjusterSpec,
): Double? {
    val best = points.maxByOrNull { it.score } ?: return null
    val step = 1.0 / (adjuster.maxClicks - 1).coerceAtLeast(1)
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }

    return when (best.x) {
        maxX -> (best.x + step).coerceIn(0.0, 1.0)
        minX -> (best.x - step).coerceIn(0.0, 1.0)
        else -> best.x
    }
}

private fun recommendationMessage(
    distinctPointCount: Int,
    fit: QuadraticFit?,
): String =
    when {
        distinctPointCount < 3 -> "Need 3 distinct clean points before quadratic fitting."
        fit == null -> "Clean points found, but quadratic fit is unstable."
        fit.hasPeakInTestedRange -> "Quadratic fit predicts a local peak in the tested range."
        else -> "Quadratic fit has no confirmed peak yet; next test follows the best observed direction."
    }

private fun SetupValues.normalizedAxleStiffness(
    axle: Axle,
    adjuster: AdjusterSpec,
): Double =
    axle.corners().map { corner ->
        normalizedStiffness(
            click = this[corner]?.get(adjuster.label) ?: adjuster.defaultClick(),
            adjuster = adjuster
        )
    }.average()

private fun SetupValues.axleClickValue(
    axle: Axle,
    adjuster: AdjusterSpec,
): Int =
    axle.corners()
        .map { corner -> this[corner]?.get(adjuster.label) ?: adjuster.defaultClick() }
        .average()
        .roundToInt()

private fun normalizedStiffness(
    click: Int,
    adjuster: AdjusterSpec,
): Double {
    val maxClicks = adjuster.maxClicks.coerceAtLeast(2)
    val stiffClick = when (adjuster.stiffSide) {
        StiffSide.HIGH_VALUE -> click
        StiffSide.LOW_VALUE -> maxClicks + 1 - click
    }.coerceIn(1, maxClicks)

    return (stiffClick - 1).toDouble() / (maxClicks - 1).toDouble()
}

private fun normalizedStiffnessToClick(
    normalized: Double,
    adjuster: AdjusterSpec,
): Int {
    val maxClicks = adjuster.maxClicks.coerceAtLeast(2)
    val stiffClick = (1 + normalized.coerceIn(0.0, 1.0) * (maxClicks - 1))
        .roundToInt()
        .coerceIn(1, maxClicks)

    return when (adjuster.stiffSide) {
        StiffSide.HIGH_VALUE -> stiffClick
        StiffSide.LOW_VALUE -> maxClicks + 1 - stiffClick
    }.coerceIn(1, maxClicks)
}

private fun Axle.corners(): List<Corner> =
    when (this) {
        Axle.FRONT -> listOf(Corner.LEFT_FRONT, Corner.RIGHT_FRONT)
        Axle.REAR -> listOf(Corner.LEFT_REAR, Corner.RIGHT_REAR)
    }

private fun Double.roundForGrouping(): Double =
    (this * 1000.0).roundToInt() / 1000.0
