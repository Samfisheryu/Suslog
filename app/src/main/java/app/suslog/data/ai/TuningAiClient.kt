package app.suslog.data.ai

import app.suslog.domain.tuning.ai.TuningAiContext

/**
 * One assistant turn. [recommendation] is non-null ONLY when the model chose to call the
 * propose_next_test tool; otherwise the turn is plain chat ([assistantText] only).
 */
data class TuningAiTurn(
    val assistantText: String?,
    val recommendation: TuningAiStructuredRecommendation?,
    val providerResponseId: String? = null,
    val inputTokenCount: Int? = null,
    val outputTokenCount: Int? = null,
)

data class TuningAiChatTurn(
    val role: String, // "user" | "assistant"
    val text: String,
)

interface TuningAiClient {
    suspend fun send(
        context: TuningAiContext,
        history: List<TuningAiChatTurn>,
        userText: String,
        adjusterLabels: List<String>,
        apiKey: String,
        model: String,
        previousResponseId: String? = null,
    ): Result<TuningAiTurn>
}

data class TuningAiStructuredRecommendation(
    val diagnosis: String,
    val evidence: List<String>,
    val nextTest: TuningAiNextTest?,
    val confidence: TuningAiConfidence,
    val cautions: List<String>,
)

data class TuningAiNextTest(
    val axle: String, // "FRONT" | "REAR"
    val adjusterLabel: String,
    val targetClick: Int,
    val rationale: String,
)

enum class TuningAiConfidence {
    LOW,
    MEDIUM,
    HIGH,
}
