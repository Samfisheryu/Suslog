package app.suslog.domain.tuning.ai

import app.suslog.domain.tuning.Axle
import app.suslog.domain.tuning.SetupChangeType

data class TuningAiContext(
    val systemPrompt: String,
    val car: TuningAiCarContext,
    val config: TuningAiConfigContext,
    val selectedStateIndex: Int,
    val currentStateIndex: Int?,
    val totalStateCount: Int,
    val omittedStateCount: Int,
    val states: List<TuningAiStateContext>,
    val modelRecommendation: TuningAiModelRecommendationContext?,
    val referenceDocuments: List<TuningAiReferenceDocument>,
) {
    fun toPromptMessages(userIssue: String): List<TuningAiPromptMessage> =
        listOf(
            TuningAiPromptMessage(
                role = "system",
                content = systemPrompt
            ),
            TuningAiPromptMessage(
                role = "user",
                content = referenceDocumentsToPrompt()
            ),
            TuningAiPromptMessage(
                role = "user",
                content = runtimeContextToPrompt()
            ),
            TuningAiPromptMessage(
                role = "user",
                content = "USER ISSUE\n${userIssue.trim()}"
            )
        )

    /** The stable context prefix (reference docs + runtime snapshot),
     *  without the user's message. Chat history and the new user turn are appended by the client. */
    fun contextMessages(
        includeReferenceDocuments: Boolean = true,
    ): List<TuningAiPromptMessage> =
        buildList {
            if (includeReferenceDocuments) {
                add(TuningAiPromptMessage(role = "user", content = referenceDocumentsToPrompt()))
            }
            add(TuningAiPromptMessage(role = "user", content = runtimeContextToPrompt()))
        }

    fun toUserContext(): String =
        buildString {
            appendLine(referenceDocumentsToPrompt())
            appendLine(runtimeContextToPrompt())
        }

    private fun referenceDocumentsToPrompt(): String =
        buildString {
            appendLine("REFERENCE DOCUMENTS")
            referenceDocuments.forEach { document ->
                appendLine("### ${document.title}")
                appendLine("source: ${document.source.label}")
                appendLine(document.content)
                appendLine()
            }
        }

    private fun runtimeContextToPrompt(): String =
        buildString {
            appendLine("RUNTIME CONTEXT")
            appendLine()

            appendLine("CAR")
            appendLine("name: ${car.name}")
            appendLine("suspensionType: ${car.suspensionType}")
            appendLine("adjusters:")
            car.adjusters.forEach { adjuster ->
                appendLine(
                    "- ${adjuster.label}: 1-${adjuster.maxClicks}; ${adjuster.stiffSideLabel}"
                )
            }
            appendLine()

            appendLine("CONFIG")
            appendLine("name: ${config.name}")
            appendLine("selectedState: ${selectedStateIndex + 1}")
            appendLine("currentState: ${currentStateIndex?.let { it + 1 } ?: "none"}")
            appendLine("totalStates: $totalStateCount")
            appendLine("omittedOlderStates: $omittedStateCount")
            appendLine()

            currentStateIndex
                ?.let { index -> states.firstOrNull { it.index == index } }
                ?.let { state ->
                    appendLine("CURRENT SETUP SNAPSHOT")
                    appendLine(state.setupByCorner.toPromptText())
                    appendLine()
                }

            appendLine("STATE HISTORY")
            states.forEachIndexed { localIndex, state ->
                appendLine("State ${state.index + 1}")
                appendLine("hasFeedback: ${state.hasFeedback}")
                appendLine("changeType: ${state.change.type}")
                state.change.axle?.let { appendLine("changedAxle: $it") }
                state.change.adjusterLabel?.let { appendLine("changedAdjuster: $it") }
                state.change.deltaClicks?.let { appendLine("deltaClicks: $it") }
                state.score?.let { appendLine("score: ${"%.1f".format(it)}") }
                state.feedback?.let { feedback ->
                    appendLine("entryBalance: ${feedback.cornerEntryBalance}")
                    appendLine("midBalance: ${feedback.cornerMidBalance}")
                    appendLine("exitBalance: ${feedback.cornerExitBalance}")
                    appendLine("overallGrip: ${feedback.overallGrip}")
                    appendLine("bodyControl: ${feedback.bodyControlBalance}")
                    appendLine("lapTimeMillis: ${feedback.lapTimeMillis ?: "none"}")
                    appendLine("note: ${feedback.note.orEmpty()}")
                }
                if (localIndex == 0 || state.index == selectedStateIndex) {
                    appendLine("setup:")
                    appendLine(state.setupByCorner.toPromptText(prefix = "  "))
                }
                appendLine()
            }

            appendLine("LOCAL MODEL RECOMMENDATION")
            appendLine(modelRecommendation?.toPromptText() ?: "none")
            appendLine()

        }
}

data class TuningAiPromptMessage(
    val role: String,
    val content: String,
)

data class TuningAiCarContext(
    val id: String,
    val name: String,
    val suspensionType: String,
    val adjusters: List<TuningAiAdjusterContext>,
)

data class TuningAiAdjusterContext(
    val label: String,
    val maxClicks: Int,
    val stiffSide: String,
    val stiffSideLabel: String,
)

data class TuningAiConfigContext(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
)

data class TuningAiStateContext(
    val index: Int,
    val timestampMillis: Long,
    val hasFeedback: Boolean,
    val setupByCorner: Map<String, Map<String, Int>>,
    val feedback: TuningAiFeedbackContext?,
    val score: Double?,
    val change: TuningAiChangeContext,
)

data class TuningAiFeedbackContext(
    val cornerEntryBalance: Int,
    val cornerMidBalance: Int,
    val cornerExitBalance: Int,
    val overallGrip: Int,
    val bodyControlBalance: Int,
    val lapTimeMillis: Long?,
    val note: String?,
)

data class TuningAiChangeContext(
    val type: SetupChangeType,
    val axle: Axle?,
    val adjusterLabel: String?,
    val deltaClicks: Int?,
)

data class TuningAiModelRecommendationContext(
    val variable: String?,
    val cleanPointCount: Int,
    val distinctPointCount: Int,
    val bestObservedStateIndex: Int?,
    val bestObservedClick: Int?,
    val bestObservedScore: Double?,
    val fittedRecommendedClick: Int?,
    val message: String,
) {
    fun toPromptText(): String =
        buildString {
            appendLine("variable: ${variable ?: "none"}")
            appendLine("cleanPointCount: $cleanPointCount")
            appendLine("distinctPointCount: $distinctPointCount")
            appendLine("bestObservedState: ${bestObservedStateIndex?.let { it + 1 } ?: "none"}")
            appendLine("bestObservedClick: ${bestObservedClick ?: "none"}")
            appendLine("bestObservedScore: ${bestObservedScore?.let { "%.1f".format(it) } ?: "none"}")
            appendLine("fittedRecommendedClick: ${fittedRecommendedClick ?: "none"}")
            appendLine("message: $message")
        }
}

data class TuningAiReferenceDocument(
    val id: String,
    val title: String,
    val content: String,
    val source: TuningAiReferenceDocumentSource,
)

enum class TuningAiReferenceDocumentSource(
    val label: String,
) {
    BUILT_IN("built-in tuning doc"),
    USER("user tuning doc"),
}

object TuningAiSystemPrompt {
    const val VERSION: Int = 1

    val text: String = """
        You are SusLog's suspension tuning assistant for coilover setup work.
        Use only the provided car setup, setup config history, driver feedback, lap times, local model output, and reference documents.
        Do not claim a global optimum. Recommend a next test point near the current setup.
        Prefer changing one axle and one adjuster at a time.
        Respect each adjuster's click range and stiff-side direction.
        Do not overwrite current setup or save a state. Return a recommendation only.
        If data is insufficient, explain what test point or feedback should be recorded next.
        Keep the answer practical and track-use focused.
    """.trimIndent()
}

private fun Map<String, Map<String, Int>>.toPromptText(
    prefix: String = "",
): String =
    entries.joinToString(separator = "\n") { (corner, values) ->
        "$prefix$corner: $values"
    }
