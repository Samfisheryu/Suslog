package app.suslog.data.ai

import app.suslog.data.local.AiConversationEntity
import app.suslog.data.local.AiMessageEntity
import app.suslog.data.local.SuslogDao
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StoredAiThread(
    val conversationId: String?,
    val lastTurnRef: String?,
    val lastSentStateCount: Int,
    val messages: List<StoredAiMessage>,
    val nextSeq: Int,
)

data class StoredAiMessage(
    val seq: Int,
    val role: String,
    val text: String?,
    val recommendation: TuningAiStructuredRecommendation?,
)

class AiConversationRepository(
    private val dao: SuslogDao,
) {
    suspend fun loadLatestOpenAiThread(
        localUserId: String,
        configId: String,
    ): StoredAiThread {
        val conversation = dao.getLatestAiConversation(
            localUserId = localUserId,
            configId = configId,
            provider = PROVIDER_OPENAI
        ) ?: return StoredAiThread(
            conversationId = null,
            lastTurnRef = null,
            lastSentStateCount = 0,
            messages = emptyList(),
            nextSeq = 0
        )
        val messages = dao.getAiMessages(conversation.id)
            .filter { it.status == STATUS_COMPLETE }
            .map { it.toStoredAiMessage() }

        return StoredAiThread(
            conversationId = conversation.id,
            lastTurnRef = conversation.lastTurnRef,
            lastSentStateCount = conversation.lastSentStateCount,
            messages = messages,
            nextSeq = messages.maxOfOrNull { it.seq + 1 } ?: 0
        )
    }

    suspend fun ensureOpenAiConversation(
        existingConversationId: String?,
        localUserId: String,
        carId: String,
        configId: String,
        configName: String,
        modelId: String,
        stateCount: Int,
    ): String {
        if (existingConversationId != null) {
            return existingConversationId
        }

        val now = System.currentTimeMillis()
        val conversation = AiConversationEntity(
            id = UUID.randomUUID().toString(),
            localUserId = localUserId,
            carId = carId,
            configId = configId,
            provider = PROVIDER_OPENAI,
            modelId = modelId,
            providerThreadRef = null,
            lastTurnRef = null,
            systemPromptVersion = 1,
            builtInDocsHash = null,
            userDocsHash = null,
            lastSentStateCount = stateCount,
            currentSetupHash = null,
            summary = null,
            title = configName,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.insertAiConversation(conversation)

        return conversation.id
    }

    suspend fun appendMessage(
        conversationId: String,
        seq: Int,
        role: String,
        text: String?,
        recommendation: TuningAiStructuredRecommendation?,
        modelId: String?,
        inputTokenCount: Int? = null,
        outputTokenCount: Int? = null,
    ): Int {
        val now = System.currentTimeMillis()
        dao.insertAiMessage(
            AiMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                seq = seq,
                role = role,
                content = text.orEmpty(),
                status = STATUS_COMPLETE,
                errorMessage = null,
                modelId = modelId,
                providerResponseId = null,
                linkedConfigStateId = null,
                structuredRecommendationJson = recommendation?.toJsonString(),
                inputTokenCount = inputTokenCount,
                outputTokenCount = outputTokenCount,
                createdAtMillis = now
            )
        )
        dao.touchAiConversation(
            conversationId = conversationId,
            updatedAtMillis = now
        )

        return seq + 1
    }

    suspend fun updateLastTurnRef(
        conversationId: String,
        lastTurnRef: String?,
        stateCount: Int,
    ) {
        dao.updateAiConversationLastTurnRef(
            conversationId = conversationId,
            lastTurnRef = lastTurnRef,
            lastSentStateCount = stateCount,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun AiMessageEntity.toStoredAiMessage(): StoredAiMessage =
        StoredAiMessage(
            seq = seq,
            role = role,
            text = content.takeIf { it.isNotBlank() },
            recommendation = structuredRecommendationJson
                ?.takeIf { it.isNotBlank() }
                ?.let { json -> runCatching { JSONObject(json).toRecommendation() }.getOrNull() }
        )

    companion object {
        const val PROVIDER_OPENAI = "openai"
        private const val STATUS_COMPLETE = "complete"
    }
}

fun TuningAiStructuredRecommendation.toJsonString(): String =
    JSONObject()
        .put("diagnosis", diagnosis)
        .put("evidence", JSONArray(evidence))
        .put("nextTest", nextTest?.toJson() ?: JSONObject.NULL)
        .put("confidence", confidence.name)
        .put("cautions", JSONArray(cautions))
        .toString()

private fun TuningAiNextTest.toJson(): JSONObject =
    JSONObject()
        .put("axle", axle)
        .put("adjusterLabel", adjusterLabel)
        .put("targetClick", targetClick)
        .put("rationale", rationale)

private fun JSONObject.toRecommendation(): TuningAiStructuredRecommendation =
    TuningAiStructuredRecommendation(
        diagnosis = optString("diagnosis"),
        evidence = optJSONArray("evidence").toStringList(),
        nextTest = optJSONObject("nextTest")?.toNextTest(),
        confidence = when (optString("confidence").uppercase()) {
            "HIGH" -> TuningAiConfidence.HIGH
            "MEDIUM" -> TuningAiConfidence.MEDIUM
            else -> TuningAiConfidence.LOW
        },
        cautions = optJSONArray("cautions").toStringList()
    )

private fun JSONObject.toNextTest(): TuningAiNextTest =
    TuningAiNextTest(
        axle = optString("axle"),
        adjusterLabel = optString("adjusterLabel"),
        targetClick = optInt("targetClick"),
        rationale = optString("rationale")
    )

private fun JSONArray?.toStringList(): List<String> =
    if (this == null) {
        emptyList()
    } else {
        (0 until length()).mapNotNull { optString(it).ifBlank { null } }
    }
