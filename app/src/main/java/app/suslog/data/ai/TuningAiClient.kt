package app.suslog.data.ai

import app.suslog.domain.tuning.ai.TuningAiContext

interface TuningAiClient {
    suspend fun analyze(request: TuningAiRequest): Result<TuningAiResponse>
}

data class TuningAiRequest(
    val userIssue: String,
    val context: TuningAiContext,
    val mode: TuningAiContextMode,
    val providerThreadRef: String? = null,
    val lastTurnRef: String? = null,
)

data class TuningAiResponse(
    val recommendation: TuningAiStructuredRecommendation,
    val rawText: String,
    val providerThreadRef: String?,
    val lastTurnRef: String?,
    val inputTokenCount: Int?,
    val outputTokenCount: Int?,
)

enum class TuningAiContextMode {
    FULL,
    DELTA,
}

data class TuningAiStructuredRecommendation(
    val diagnosis: String,
    val evidence: List<String>,
    val relevantDocNotes: List<String>,
    val nextTest: TuningAiNextTest?,
    val confidence: TuningAiConfidence,
    val cautions: List<String>,
)

data class TuningAiNextTest(
    val axle: String,
    val adjusterLabel: String,
    val targetClick: Int,
    val rationale: String,
)

enum class TuningAiConfidence {
    LOW,
    MEDIUM,
    HIGH,
}
