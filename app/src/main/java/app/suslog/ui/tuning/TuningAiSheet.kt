package app.suslog.ui.tuning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.suslog.data.ai.AiConversationRepository
import app.suslog.data.ai.AiCredentialStore
import app.suslog.data.ai.StoredAiMessage
import app.suslog.data.ai.TuningAiChatTurn
import app.suslog.data.ai.TuningAiClient
import app.suslog.data.ai.TuningAiConfidence
import app.suslog.data.ai.TuningAiStructuredRecommendation
import app.suslog.data.ai.isRecoverableOpenAiResponseReferenceFailure
import app.suslog.domain.tuning.ai.TuningAiContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val AiLime = Color(0xFFBDEE36)
private val AiTeal = Color(0xFF7FE0C0)
private val AiCyan = Color(0xFF4FD0E6)
private val AiSweep = listOf(AiLime, AiTeal, AiCyan, AiLime)
private val AiLine = Brush.linearGradient(listOf(AiLime, AiTeal, AiCyan))

data class AiChatMessage(
    val id: Long,
    val role: String, // "user" | "assistant"
    val text: String?,
    val recommendation: TuningAiStructuredRecommendation? = null,
)

class TuningAiChatController(
    private val client: TuningAiClient,
    private val credentials: AiCredentialStore,
    private val repository: AiConversationRepository,
    private val scope: CoroutineScope,
) {
    var messages by mutableStateOf<List<AiChatMessage>>(emptyList())
        private set
    var isThinking by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var nextId = 0L
    private var conversationId: String? = null
    private var lastTurnRef: String? = null
    private var lastSentStateCount: Int = 0
    private var nextSeq: Int = 0
    private var loadedKey: String? = null

    fun loadThread(
        localUserId: String,
        configId: String,
    ) {
        val key = "$localUserId:$configId"
        if (loadedKey == key) return
        loadedKey = key
        error = null
        isLoading = true

        scope.launch {
            runCatching {
                repository.loadLatestOpenAiThread(
                    localUserId = localUserId,
                    configId = configId
                )
            }.onSuccess { thread ->
                conversationId = thread.conversationId
                lastTurnRef = thread.lastTurnRef
                lastSentStateCount = thread.lastSentStateCount
                nextSeq = thread.nextSeq
                nextId = thread.nextSeq.toLong()
                messages = thread.messages.map { it.toUiMessage() }
            }.onFailure { throwable ->
                error = throwable.message ?: "Could not load AI chat."
            }
            isLoading = false
        }
    }

    fun clearThread() {
        loadedKey = null
        conversationId = null
        lastTurnRef = null
        lastSentStateCount = 0
        nextSeq = 0
        nextId = 0L
        messages = emptyList()
        error = null
        isThinking = false
        isLoading = false
    }

    fun send(
        userText: String,
        context: TuningAiContext,
        adjusterLabels: List<String>,
        localUserId: String?,
        carId: String,
        configId: String,
        configName: String,
    ) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || isThinking || isLoading) return
        if (localUserId == null) {
            error = "Login to a local account first."
            return
        }
        if (!credentials.isConnected) {
            error = "Connect OpenAI in Settings to use the assistant."
            return
        }

        val staleContext = lastSentStateCount > 0 && lastSentStateCount != context.totalStateCount
        val activeConversationIdBeforeSend = if (staleContext) null else conversationId
        val activeLastTurnRef = if (staleContext) null else lastTurnRef
        val activeHistory = if (staleContext) emptyList() else messages
        val history = activeHistory.mapNotNull { message ->
            historyText(message)?.let { TuningAiChatTurn(role = message.role, text = it) }
        }
        isThinking = true
        error = null

        scope.launch {
            runCatching {
                val apiKey = credentials.apiKey
                if (apiKey.isBlank()) {
                    throw IllegalStateException("OpenAI API key is missing. Reconnect it in Settings.")
                }
                val activeConversationId = repository.ensureOpenAiConversation(
                    existingConversationId = activeConversationIdBeforeSend,
                    localUserId = localUserId,
                    carId = carId,
                    configId = configId,
                    configName = configName,
                    modelId = credentials.model,
                    stateCount = context.totalStateCount
                )
                conversationId = activeConversationId

                if (staleContext) {
                    lastTurnRef = null
                    lastSentStateCount = context.totalStateCount
                    nextSeq = 0
                    nextId = 0L
                    messages = emptyList()
                }

                val previousResponseId = activeLastTurnRef
                val result = client.send(
                    context = context,
                    history = if (previousResponseId == null) history else emptyList(),
                    userText = trimmed,
                    adjusterLabels = adjusterLabels,
                    apiKey = apiKey,
                    model = credentials.model,
                    previousResponseId = previousResponseId,
                )
                    .recoverCatching { throwable ->
                        if (
                            previousResponseId == null ||
                            !throwable.isRecoverableOpenAiResponseReferenceFailure()
                        ) {
                            throw throwable
                        }
                        client.send(
                            context = context,
                            history = history,
                            userText = trimmed,
                            adjusterLabels = adjusterLabels,
                            apiKey = apiKey,
                            model = credentials.model,
                            previousResponseId = null,
                        ).getOrThrow()
                    }
                isThinking = false
                result
                    .onSuccess { turn ->
                        lastTurnRef = turn.providerResponseId
                        repository.updateLastTurnRef(
                            conversationId = activeConversationId,
                            lastTurnRef = turn.providerResponseId,
                            stateCount = context.totalStateCount
                        )
                        lastSentStateCount = context.totalStateCount
                        val userSeq = nextSeq
                        nextSeq = repository.appendMessage(
                            conversationId = activeConversationId,
                            seq = userSeq,
                            role = "user",
                            text = trimmed,
                            recommendation = null,
                            modelId = credentials.model
                        )
                        val assistantSeq = nextSeq
                        nextSeq = repository.appendMessage(
                            conversationId = activeConversationId,
                            seq = assistantSeq,
                            role = "assistant",
                            text = turn.assistantText,
                            recommendation = turn.recommendation,
                            modelId = credentials.model,
                            inputTokenCount = turn.inputTokenCount,
                            outputTokenCount = turn.outputTokenCount
                        )
                        messages = messages +
                            AiChatMessage(id = userSeq.toLong(), role = "user", text = trimmed) +
                            AiChatMessage(
                                id = assistantSeq.toLong(),
                                role = "assistant",
                                text = turn.assistantText,
                                recommendation = turn.recommendation,
                            )
                    }
                    .onFailure { throwable ->
                        error = throwable.message ?: "Something went wrong."
                    }
            }.onFailure { throwable ->
                isThinking = false
                error = throwable.message ?: "Something went wrong."
            }
        }
    }

    private fun historyText(message: AiChatMessage): String? {
        message.text?.takeIf { it.isNotBlank() }?.let { return it }
        val rec = message.recommendation?.nextTest ?: return null
        return "Recommended ${rec.axle} ${rec.adjusterLabel} to ${rec.targetClick} clicks. ${rec.rationale}"
    }

    private fun StoredAiMessage.toUiMessage(): AiChatMessage =
        AiChatMessage(
            id = seq.toLong(),
            role = role,
            text = text,
            recommendation = recommendation
        )
}

@Composable
fun rememberTuningAiChatController(
    client: TuningAiClient,
    credentials: AiCredentialStore,
    repository: AiConversationRepository,
): TuningAiChatController {
    val scope = rememberCoroutineScope()
    return remember(client, credentials, repository) {
        TuningAiChatController(client, credentials, repository, scope)
    }
}

@Composable
fun TuningAiSheet(
    visible: Boolean,
    contextSummary: String,
    connected: Boolean,
    messages: List<AiChatMessage>,
    isThinking: Boolean,
    isLoading: Boolean,
    error: String?,
    quickIssues: List<String>,
    normalizeRecommendation: (TuningAiStructuredRecommendation) -> TuningAiStructuredRecommendation,
    onSend: (String) -> Unit,
    onApply: (TuningAiStructuredRecommendation) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val dismissDragThresholdPx = with(density) { 120.dp.toPx() }
    var sheetDragOffsetPx by remember { mutableStateOf(0f) }

    LaunchedEffect(visible) {
        if (!visible) {
            sheetDragOffsetPx = 0f
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(160)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66121712))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.93f)
                    .animateEnterExit(
                        enter = slideInVertically(tween(240)) { it },
                        exit = slideOutVertically(tween(200)) { it }
                    )
                    .graphicsLayer { translationY = sheetDragOffsetPx }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(AiLine)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(dismissDragThresholdPx) {
                                detectVerticalDragGestures(
                                    onDragCancel = { sheetDragOffsetPx = 0f },
                                    onDragEnd = {
                                        if (sheetDragOffsetPx >= dismissDragThresholdPx) {
                                            onDismiss()
                                        } else {
                                            sheetDragOffsetPx = 0f
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        sheetDragOffsetPx =
                                            (sheetDragOffsetPx + dragAmount).coerceAtLeast(0f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 9.dp)
                                .size(width = 38.dp, height = 4.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }

                    AiSheetHeader(contextSummary = contextSummary)

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = rememberThreadState(messages.size, isThinking),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (messages.isEmpty() && !isThinking && !isLoading) {
                            item { AiGreeting(connected = connected) }
                        }
                        items(
                            count = messages.size,
                            key = { messages[it].id }
                        ) { index ->
                            AiMessageItem(
                                message = messages[index],
                                normalizeRecommendation = normalizeRecommendation,
                                onApply = onApply
                            )
                        }
                        if (isThinking) {
                            item { AiThinkingRow() }
                        } else if (isLoading) {
                            item { AiLoadingRow() }
                        }
                    }

                    error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    AiInputDock(
                        quickIssues = quickIssues,
                        enabled = !isThinking && !isLoading,
                        onSend = onSend
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberThreadState(messageCount: Int, isThinking: Boolean) =
    rememberLazyListState().also { state ->
        LaunchedEffect(messageCount, isThinking) {
            val target = (messageCount + if (isThinking) 1 else 0) - 1
            if (target >= 0) state.animateScrollToItem(target)
        }
    }

@Composable
private fun AiSheetHeader(contextSummary: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AiOrb(size = 30.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tuning Assistant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = contextSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiOrb(size: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "orb")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "orbAngle"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = angle }
            .clip(CircleShape)
            .background(Brush.sweepGradient(AiSweep))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = size * 0.18f, end = size * 0.22f)
                .size(size * 0.26f)
                .clip(CircleShape)
                .background(Color(0xE6FFFFFF))
        )
    }
}

@Composable
private fun AiGreeting(connected: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        AiOrb(size = 22.dp)
        AiBubble(fromUser = false) {
            Text(
                text = if (connected) {
                    "Tell me what the car's doing. I've got this config's runs, the local model, and your tuning guides loaded."
                } else {
                    "Connect OpenAI in Settings and I'll read your runs, the local model, and your guides to suggest a next test."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AiMessageItem(
    message: AiChatMessage,
    normalizeRecommendation: (TuningAiStructuredRecommendation) -> TuningAiStructuredRecommendation,
    onApply: (TuningAiStructuredRecommendation) -> Unit,
) {
    val fromUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!message.text.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (!fromUser) AiOrb(size = 22.dp)
                AiBubble(fromUser = fromUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        message.recommendation?.let { rec ->
            val normalized = normalizeRecommendation(rec)
            AiRecommendationCard(
                recommendation = normalized,
                onApply = { onApply(normalized) },
                modifier = Modifier.padding(start = 30.dp)
            )
        }
    }
}

@Composable
private fun AiBubble(fromUser: Boolean, content: @Composable () -> Unit) {
    val shape = if (fromUser) {
        RoundedCornerShape(14.dp, 14.dp, 5.dp, 14.dp)
    } else {
        RoundedCornerShape(14.dp, 14.dp, 14.dp, 5.dp)
    }
    Surface(
        shape = shape,
        color = if (fromUser) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (fromUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.widthIn(max = 264.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) { content() }
    }
}

@Composable
private fun AiThinkingRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AiOrb(size = 22.dp)
        Text(
            text = "Reading your runs and guides…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiLoadingRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AiOrb(size = 22.dp)
        Text(
            text = "Loading this config's AI chat…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiRecommendationCard(
    recommendation: TuningAiStructuredRecommendation,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextTest = recommendation.nextTest
    Surface(
        modifier = modifier.widthIn(max = 288.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AiLine)
                )
                Text(
                    text = "NEXT TEST",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                ConfidencePill(recommendation.confidence)
            }
            if (nextTest != null) {
                Text(
                    text = buildString {
                        append(nextTest.axle.lowercase().replaceFirstChar { it.uppercase() })
                        append(" ")
                        append(nextTest.adjusterLabel)
                        append("  →  ")
                        append(nextTest.targetClick)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            val body = nextTest?.rationale?.takeIf { it.isNotBlank() } ?: recommendation.diagnosis
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (recommendation.evidence.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recommendation.evidence.take(4).forEach { EvidenceChip(it) }
                }
            }
            if (nextTest != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(AiLine)
                        .clickable(onClick = onApply)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply to setup",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F1314)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidencePill(confidence: TuningAiConfidence) {
    val (label, color) = when (confidence) {
        TuningAiConfidence.HIGH -> "High" to MaterialTheme.colorScheme.primary
        TuningAiConfidence.MEDIUM -> "Med" to MaterialTheme.colorScheme.tertiary
        TuningAiConfidence.LOW -> "Low" to MaterialTheme.colorScheme.tertiary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label conf",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EvidenceChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun AiInputDock(
    quickIssues: List<String>,
    enabled: Boolean,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            quickIssues.forEach { issue ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .clickable(enabled = enabled) { text = issue }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = issue,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = "Describe what the car's doing…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = AiLine,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val canSend = enabled && text.isNotBlank()
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (canSend) AiLine else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)))
                    .clickable(enabled = canSend) {
                        onSend(text)
                        text = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↑",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1314),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
