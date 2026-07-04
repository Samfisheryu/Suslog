package app.suslog.data.ai

import app.suslog.data.local.SuslogDao
import java.util.Calendar

data class AiUsageByModel(
    val modelId: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val requestCount: Long,
)

data class AiUsageSummary(
    val inputTokens: Long,
    val outputTokens: Long,
    val requestCount: Long,
    val estimatedCostUsd: Double?,
    val unpricedModelCount: Int,
    val periodStartMillis: Long,
    val periodEndMillis: Long,
) {
    val totalTokens: Long
        get() = inputTokens + outputTokens
}

class AiUsageRepository(
    private val dao: SuslogDao,
) {
    suspend fun loadCurrentMonthOpenAiUsage(
        localUserId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): AiUsageSummary {
        val startMillis = currentMonthStartMillis(nowMillis)
        val buckets = dao.getAiUsageByModel(
            localUserId = localUserId,
            provider = AiConversationRepository.PROVIDER_OPENAI,
            startMillis = startMillis,
            endMillis = nowMillis
        )

        return buckets.toSummary(
            periodStartMillis = startMillis,
            periodEndMillis = nowMillis
        )
    }

    private fun List<AiUsageByModel>.toSummary(
        periodStartMillis: Long,
        periodEndMillis: Long,
    ): AiUsageSummary {
        val pricedCosts = mapNotNull { bucket ->
            OpenAiModelPricing.costUsd(
                modelId = bucket.modelId,
                inputTokens = bucket.inputTokens,
                outputTokens = bucket.outputTokens
            )
        }
        val unpricedModelCount = count { bucket ->
            bucket.inputTokens + bucket.outputTokens > 0 &&
                OpenAiModelPricing.priceFor(bucket.modelId) == null
        }
        val estimatedCost = when {
            isEmpty() -> 0.0
            pricedCosts.isEmpty() -> null
            unpricedModelCount > 0 -> null
            else -> pricedCosts.sum()
        }

        return AiUsageSummary(
            inputTokens = sumOf { it.inputTokens },
            outputTokens = sumOf { it.outputTokens },
            requestCount = sumOf { it.requestCount },
            estimatedCostUsd = estimatedCost,
            unpricedModelCount = unpricedModelCount,
            periodStartMillis = periodStartMillis,
            periodEndMillis = periodEndMillis
        )
    }

    private fun currentMonthStartMillis(nowMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

object OpenAiModelPricing {
    private val prices = listOf(
        ModelPrice(prefix = "gpt-5.5", inputUsdPerMillion = 5.00, outputUsdPerMillion = 30.00),
        ModelPrice(prefix = "gpt-5.4-mini", inputUsdPerMillion = 0.75, outputUsdPerMillion = 4.50),
        ModelPrice(prefix = "gpt-5.4", inputUsdPerMillion = 2.50, outputUsdPerMillion = 15.00),
        ModelPrice(prefix = "gpt-4.1-nano", inputUsdPerMillion = 0.10, outputUsdPerMillion = 0.40),
        ModelPrice(prefix = "gpt-4.1-mini", inputUsdPerMillion = 0.40, outputUsdPerMillion = 1.60),
        ModelPrice(prefix = "gpt-4.1", inputUsdPerMillion = 2.00, outputUsdPerMillion = 8.00),
        ModelPrice(prefix = "gpt-4o-mini", inputUsdPerMillion = 0.15, outputUsdPerMillion = 0.60),
        ModelPrice(prefix = "gpt-4o", inputUsdPerMillion = 2.50, outputUsdPerMillion = 10.00),
        ModelPrice(prefix = "chatgpt", inputUsdPerMillion = 5.00, outputUsdPerMillion = 30.00),
    )

    fun priceFor(modelId: String): ModelPrice? {
        val normalized = modelId.lowercase()
        return prices.firstOrNull { normalized.startsWith(it.prefix) }
    }

    fun costUsd(
        modelId: String,
        inputTokens: Long,
        outputTokens: Long,
    ): Double? =
        priceFor(modelId)?.let { price ->
            (inputTokens.toDouble() / TOKENS_PER_MILLION) * price.inputUsdPerMillion +
                (outputTokens.toDouble() / TOKENS_PER_MILLION) * price.outputUsdPerMillion
        }

    data class ModelPrice(
        val prefix: String,
        val inputUsdPerMillion: Double,
        val outputUsdPerMillion: Double,
    )

    private const val TOKENS_PER_MILLION = 1_000_000.0
}
