package app.suslog.domain.tuning.ai

import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.suspension.Corner
import app.suslog.domain.tuning.SetupScoreV1
import app.suslog.domain.tuning.TuningDocument
import app.suslog.domain.tuning.TuningRecommendation
import app.suslog.domain.tuning.summarizeSetupChange

private const val MAX_CONTEXT_STATE_COUNT = 12
private const val RECENT_STATE_COUNT = 9
private const val MAX_NOTE_CHARS = 500

object TuningAiContextBuilder {
    fun build(
        car: CarProfile,
        config: SetupConfig,
        selectedStateIndex: Int,
        modelRecommendation: TuningRecommendation?,
        referenceDocuments: List<TuningAiReferenceDocument>,
        userDocuments: List<TuningDocument>,
    ): TuningAiContext {
        val boundedSelectedIndex = selectedStateIndex.coerceIn(
            0,
            config.states.lastIndex.coerceAtLeast(0)
        )
        val currentStateIndex = config.currentState?.let { config.states.lastIndex }
        val contextStateIndexes = contextStateIndexes(
            stateCount = config.states.size,
            selectedStateIndex = boundedSelectedIndex
        )

        return TuningAiContext(
            systemPrompt = TuningAiSystemPrompt.text,
            car = car.toAiContext(),
            config = TuningAiConfigContext(
                id = config.id,
                name = config.name,
                createdAtMillis = config.createdAtMillis
            ),
            selectedStateIndex = boundedSelectedIndex,
            currentStateIndex = currentStateIndex,
            totalStateCount = config.states.size,
            omittedStateCount = config.states.size - contextStateIndexes.size,
            states = contextStateIndexes.map { index ->
                config.states[index].toAiContext(
                    index = index,
                    previousState = config.states.getOrNull(index - 1)
                )
            },
            modelRecommendation = modelRecommendation?.toAiContext(),
            referenceDocuments = referenceDocuments + userDocuments.toAiReferenceDocuments()
        )
    }

    private fun contextStateIndexes(
        stateCount: Int,
        selectedStateIndex: Int,
    ): List<Int> {
        if (stateCount <= MAX_CONTEXT_STATE_COUNT) {
            return (0 until stateCount).toList()
        }

        val lastIndex = stateCount - 1
        val recentStart = (lastIndex - RECENT_STATE_COUNT).coerceAtLeast(0)

        return (setOf(0, selectedStateIndex, lastIndex) + (recentStart..lastIndex))
            .filter { it in 0 until stateCount }
            .sorted()
    }

    private fun List<TuningDocument>.toAiReferenceDocuments(): List<TuningAiReferenceDocument> =
        filter { it.content.isNotBlank() }
            .map { document ->
                TuningAiReferenceDocument(
                    id = "user-${document.id}",
                    title = document.name,
                    content = document.content,
                    source = TuningAiReferenceDocumentSource.USER
                )
            }
}

private fun CarProfile.toAiContext(): TuningAiCarContext =
    TuningAiCarContext(
        id = id,
        name = name,
        suspensionType = suspensionType.label,
        adjusters = adjusters.map { adjuster ->
            TuningAiAdjusterContext(
                label = adjuster.label,
                maxClicks = adjuster.maxClicks,
                stiffSide = adjuster.stiffSide.name,
                stiffSideLabel = adjuster.stiffSide.label(adjuster.maxClicks)
            )
        }
    )

private fun SetupConfigState.toAiContext(
    index: Int,
    previousState: SetupConfigState?,
): TuningAiStateContext {
    val change = summarizeSetupChange(
        previousState = previousState,
        currentState = this
    )

    return TuningAiStateContext(
        index = index,
        timestampMillis = timestampMillis,
        hasFeedback = hasFeedback,
        setupByCorner = setup.toAiContext(),
        feedback = if (hasFeedback) {
            TuningAiFeedbackContext(
                cornerEntryBalance = cornerEntryBalance,
                cornerMidBalance = cornerMidBalance,
                cornerExitBalance = cornerExitBalance,
                overallGrip = overallGrip,
                bodyControlBalance = bodyControlBalance,
                lapTimeMillis = lapTimeMillis,
                note = note?.trim()?.take(MAX_NOTE_CHARS)
            )
        } else {
            null
        },
        score = if (hasFeedback) SetupScoreV1.compute(this).total else null,
        change = TuningAiChangeContext(
            type = change.type,
            axle = change.axle,
            adjusterLabel = change.adjusterLabel,
            deltaClicks = change.deltaClicks
        )
    )
}

private fun SetupValues.toAiContext(): Map<String, Map<String, Int>> =
    Corner.entries.associate { corner ->
        corner.name to get(corner).orEmpty()
    }

private fun TuningRecommendation.toAiContext(): TuningAiModelRecommendationContext =
    TuningAiModelRecommendationContext(
        variable = variable?.let { "${it.axle} ${it.adjusterLabel}" },
        cleanPointCount = cleanPointCount,
        distinctPointCount = distinctPointCount,
        bestObservedStateIndex = bestObserved?.stateIndex,
        bestObservedClick = bestObserved?.clickValue,
        bestObservedScore = bestObserved?.score,
        fittedRecommendedClick = recommendedClick,
        message = message
    )
