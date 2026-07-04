package app.suslog.data.ai

import app.suslog.domain.tuning.ai.TuningAiContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real OpenAI Responses API client with function ("tool") calling.
 * The model decides each turn whether to answer in prose or call propose_next_test,
 * which the app renders as an actionable recommendation card.
 */
class OpenAiTuningAiClient : TuningAiClient {

    override suspend fun send(
        context: TuningAiContext,
        history: List<TuningAiChatTurn>,
        userText: String,
        adjusterLabels: List<String>,
        apiKey: String,
        model: String,
        previousResponseId: String?,
    ): Result<TuningAiTurn> = withContext(Dispatchers.IO) {
        runCatching {
            val input = JSONArray()
            context.contextMessages(includeReferenceDocuments = previousResponseId == null)
                .forEach { message ->
                input.put(JSONObject().put("role", message.role).put("content", message.content))
            }
            history.forEach { turn ->
                input.put(JSONObject().put("role", turn.role).put("content", turn.text))
            }
            input.put(JSONObject().put("role", "user").put("content", userText.trim()))

            val body = JSONObject()
                .put("model", model)
                .put("instructions", context.systemPrompt)
                .put("input", input)
                .put("tools", JSONArray().put(proposeNextTestTool(adjusterLabels)))
                .put("tool_choice", "auto")
                .put("store", true)

            if (supportsTemperature(model)) {
                body.put("temperature", 0.4)
            }

            previousResponseId?.let {
                body.put("previous_response_id", it)
            }

            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            connection.disconnect()

            if (code !in 200..299) {
                val errorObject = runCatching {
                    JSONObject(responseText).getJSONObject("error")
                }.getOrNull()
                val apiMessage = errorObject?.optString("message")?.takeIf { it.isNotBlank() }
                val apiCode = errorObject?.optString("code")?.takeIf { it.isNotBlank() }
                throw OpenAiApiException(
                    statusCode = code,
                    errorCode = apiCode,
                    detail = apiMessage ?: "OpenAI request failed (HTTP $code)."
                )
            }

            parseTurn(JSONObject(responseText))
        }
    }

    /** Pulls the models this API key can actually use, filtered to chat/tool-capable ones. */
    suspend fun listChatModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(MODELS_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            connection.disconnect()
            if (code !in 200..299) {
                val apiMessage = runCatching {
                    JSONObject(responseText).getJSONObject("error").getString("message")
                }.getOrNull()
                error(apiMessage ?: "Could not load models (HTTP $code).")
            }
            val data = JSONObject(responseText).getJSONArray("data")
            (0 until data.length())
                .map { data.getJSONObject(it).getString("id") }
                .filter { isChatModel(it) }
                .sortedDescending()
        }
    }

    private fun isChatModel(id: String): Boolean {
        val lower = id.lowercase()
        val supported = lower.startsWith("gpt-") || lower.startsWith("chatgpt") ||
            lower.startsWith("o1") || lower.startsWith("o3") || lower.startsWith("o4")
        if (!supported) return false
        val excluded = listOf(
            "audio", "realtime", "transcribe", "tts", "image",
            "embedding", "moderation", "search", "dall", "instruct"
        )
        return excluded.none { lower.contains(it) }
    }

    private fun supportsTemperature(model: String): Boolean {
        val lower = model.lowercase()
        return lower.startsWith("gpt-") || lower.startsWith("chatgpt")
    }

    private fun parseTurn(root: JSONObject): TuningAiTurn {
        val output = root.optJSONArray("output") ?: JSONArray()
        val usage = root.optJSONObject("usage")
        var assistantText: String? = root.optString("output_text", "").ifBlank { null }
        var recommendation: TuningAiStructuredRecommendation? = null

        for (index in 0 until output.length()) {
            val item = output.optJSONObject(index) ?: continue
            when (item.optString("type")) {
                "message" -> {
                    assistantText = assistantText ?: item.outputText()
                }

                "function_call" -> {
                    if (item.optString("name") == TOOL_NAME) {
                        recommendation = runCatching {
                            parseRecommendation(JSONObject(item.getString("arguments")))
                        }.getOrElse {
                            error("OpenAI returned an invalid next-test payload: ${it.message}")
                        }
                    }
                }
            }
        }

        return TuningAiTurn(
            assistantText = assistantText,
            recommendation = recommendation,
            providerResponseId = root.optString("id").ifBlank { null },
            inputTokenCount = usage?.optInt("input_tokens")?.takeIf { it > 0 },
            outputTokenCount = usage?.optInt("output_tokens")?.takeIf { it > 0 },
        )
    }

    private fun JSONObject.outputText(): String? {
        val content = optJSONArray("content") ?: return null

        return (0 until content.length())
            .mapNotNull { index ->
                val part = content.optJSONObject(index) ?: return@mapNotNull null
                part.optString("text").ifBlank { null }
            }
            .joinToString(separator = "\n")
            .ifBlank { null }
    }

    private fun parseRecommendation(args: JSONObject): TuningAiStructuredRecommendation {
        val confidence = when (args.optString("confidence").uppercase()) {
            "HIGH" -> TuningAiConfidence.HIGH
            "MEDIUM" -> TuningAiConfidence.MEDIUM
            else -> TuningAiConfidence.LOW
        }
        val nextTest = if (args.has("axle") && args.has("adjusterLabel") && args.has("targetClick")) {
            TuningAiNextTest(
                axle = args.getString("axle").uppercase(),
                adjusterLabel = args.getString("adjusterLabel"),
                targetClick = args.getInt("targetClick"),
                rationale = args.optString("rationale"),
            )
        } else {
            null
        }
        return TuningAiStructuredRecommendation(
            diagnosis = args.optString("diagnosis"),
            evidence = args.optJSONArray("evidence").toStringList(),
            nextTest = nextTest,
            confidence = confidence,
            cautions = args.optJSONArray("cautions").toStringList(),
        )
    }

    private fun proposeNextTestTool(adjusterLabels: List<String>): JSONObject {
        val adjusterEnum = JSONArray().apply { adjusterLabels.forEach { put(it) } }
        val properties = JSONObject()
            .put(
                "diagnosis",
                JSONObject().put("type", "string")
                    .put("description", "Short read of what the car is doing.")
            )
            .put(
                "axle",
                JSONObject().put("type", "string")
                    .put("enum", JSONArray().put("FRONT").put("REAR"))
            )
            .put(
                "adjusterLabel",
                JSONObject().put("type", "string").put("enum", adjusterEnum)
            )
            .put(
                "targetClick",
                JSONObject().put("type", "integer")
                    .put("description", "Absolute target click for that axle and adjuster.")
            )
            .put("rationale", JSONObject().put("type", "string"))
            .put(
                "confidence",
                JSONObject().put("type", "string")
                    .put("enum", JSONArray().put("LOW").put("MEDIUM").put("HIGH"))
            )
            .put(
                "evidence",
                JSONObject().put("type", "array")
                    .put("items", JSONObject().put("type", "string"))
            )
            .put(
                "cautions",
                JSONObject().put("type", "array")
                    .put("items", JSONObject().put("type", "string"))
            )

        val parameters = JSONObject()
            .put("type", "object")
            .put("properties", properties)
            .put(
                "required",
                JSONArray()
                    .put("diagnosis").put("axle").put("adjusterLabel")
                    .put("targetClick").put("rationale").put("confidence")
            )

        return JSONObject()
            .put("type", "function")
            .put("name", TOOL_NAME)
            .put(
                "description",
                "Propose ONE next test: change one axle and one adjuster to a target click, " +
                    "near the current setup. Only call this when you have a concrete next test " +
                    "to recommend; otherwise answer in plain text."
            )
            .put("parameters", parameters)
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) {
            emptyList()
        } else {
            (0 until length()).mapNotNull { optString(it).ifBlank { null } }
        }

    companion object {
        private const val ENDPOINT = "https://api.openai.com/v1/responses"
        private const val MODELS_ENDPOINT = "https://api.openai.com/v1/models"
        private const val TOOL_NAME = "propose_next_test"
    }
}

class OpenAiApiException(
    val statusCode: Int,
    val errorCode: String?,
    detail: String,
) : Exception(detail)

fun Throwable.isRecoverableOpenAiResponseReferenceFailure(): Boolean {
    val apiError = this as? OpenAiApiException ?: return false
    val code = apiError.errorCode.orEmpty().lowercase()
    val message = apiError.message.orEmpty().lowercase()

    return apiError.statusCode in 400..404 &&
        (code.contains("previous_response") ||
            message.contains("previous_response") ||
            message.contains("response_not_found"))
}
