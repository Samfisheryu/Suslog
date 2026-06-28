package app.suslog.domain.tuning

import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.suspension.Corner

enum class Axle {
    FRONT,
    REAR,
}

enum class SetupChangeType {
    BASELINE,
    AXLE_SINGLE_ADJUSTER,
    SINGLE_CORNER,
    MIXED,
    UNKNOWN,
}

data class SetupDiff(
    val corner: Corner,
    val adjusterLabel: String,
    val before: Int?,
    val after: Int?,
) {
    val delta: Int?
        get() = if (before != null && after != null) after - before else null
}

data class SetupChangeSummary(
    val type: SetupChangeType,
    val diffs: List<SetupDiff>,
    val axle: Axle? = null,
    val adjusterLabel: String? = null,
    val deltaClicks: Int? = null,
) {
    val includedInModel: Boolean
        get() = type == SetupChangeType.AXLE_SINGLE_ADJUSTER
}

data class AxleSideMismatch(
    val axle: Axle,
    val adjusterLabel: String,
    val leftValue: Int,
    val rightValue: Int,
)

fun summarizeSetupChange(
    previousState: SetupConfigState?,
    currentState: SetupConfigState,
): SetupChangeSummary {
    if (previousState == null) {
        return SetupChangeSummary(
            type = SetupChangeType.BASELINE,
            diffs = emptyList()
        )
    }

    return summarizeSetupChange(
        previousSetup = previousState.setup,
        currentSetup = currentState.setup
    )
}

fun summarizeSetupChange(
    previousSetup: SetupValues,
    currentSetup: SetupValues,
): SetupChangeSummary {
    val diffs = setupDiffs(previousSetup, currentSetup)
    if (diffs.isEmpty()) {
        return SetupChangeSummary(
            type = SetupChangeType.BASELINE,
            diffs = emptyList()
        )
    }

    val labels = diffs.map { it.adjusterLabel }.distinct()
    if (labels.size != 1) {
        return SetupChangeSummary(
            type = SetupChangeType.MIXED,
            diffs = diffs
        )
    }

    val deltas = diffs.map { it.delta }
    if (deltas.any { it == null }) {
        return SetupChangeSummary(
            type = SetupChangeType.UNKNOWN,
            diffs = diffs
        )
    }

    val distinctDeltas = deltas.filterNotNull().distinct()
    if (distinctDeltas.size != 1) {
        return SetupChangeSummary(
            type = SetupChangeType.MIXED,
            diffs = diffs
        )
    }

    val corners = diffs.map { it.corner }.toSet()
    val adjusterLabel = labels.single()
    val delta = distinctDeltas.single()

    return when (corners) {
        frontCorners -> SetupChangeSummary(
            type = SetupChangeType.AXLE_SINGLE_ADJUSTER,
            diffs = diffs,
            axle = Axle.FRONT,
            adjusterLabel = adjusterLabel,
            deltaClicks = delta
        )

        rearCorners -> SetupChangeSummary(
            type = SetupChangeType.AXLE_SINGLE_ADJUSTER,
            diffs = diffs,
            axle = Axle.REAR,
            adjusterLabel = adjusterLabel,
            deltaClicks = delta
        )

        else -> if (corners.size == 1) {
            SetupChangeSummary(
                type = SetupChangeType.SINGLE_CORNER,
                diffs = diffs,
                adjusterLabel = adjusterLabel,
                deltaClicks = delta
            )
        } else {
            SetupChangeSummary(
                type = SetupChangeType.MIXED,
                diffs = diffs
            )
        }
    }
}

fun findAxleSideMismatches(
    car: CarProfile,
    setup: SetupValues,
): List<AxleSideMismatch> =
    buildList {
        car.adjusters.forEach { adjuster ->
            addMismatchIfPresent(
                axle = Axle.FRONT,
                leftCorner = Corner.LEFT_FRONT,
                rightCorner = Corner.RIGHT_FRONT,
                adjusterLabel = adjuster.label,
                setup = setup
            )
            addMismatchIfPresent(
                axle = Axle.REAR,
                leftCorner = Corner.LEFT_REAR,
                rightCorner = Corner.RIGHT_REAR,
                adjusterLabel = adjuster.label,
                setup = setup
            )
        }
    }

private fun MutableList<AxleSideMismatch>.addMismatchIfPresent(
    axle: Axle,
    leftCorner: Corner,
    rightCorner: Corner,
    adjusterLabel: String,
    setup: SetupValues,
) {
    val left = setup[leftCorner]?.get(adjusterLabel)
    val right = setup[rightCorner]?.get(adjusterLabel)
    if (left != null && right != null && left != right) {
        add(
            AxleSideMismatch(
                axle = axle,
                adjusterLabel = adjusterLabel,
                leftValue = left,
                rightValue = right
            )
        )
    }
}

private fun setupDiffs(
    previousSetup: SetupValues,
    currentSetup: SetupValues,
): List<SetupDiff> =
    Corner.entries.flatMap { corner ->
        val labels = previousSetup[corner].orEmpty().keys + currentSetup[corner].orEmpty().keys
        labels.distinct().mapNotNull { label ->
            val before = previousSetup[corner]?.get(label)
            val after = currentSetup[corner]?.get(label)

            if (before == after) {
                null
            } else {
                SetupDiff(
                    corner = corner,
                    adjusterLabel = label,
                    before = before,
                    after = after
                )
            }
        }
    }

private val frontCorners = setOf(Corner.LEFT_FRONT, Corner.RIGHT_FRONT)
private val rearCorners = setOf(Corner.LEFT_REAR, Corner.RIGHT_REAR)
