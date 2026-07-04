package app.suslog.ui.tuning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.suslog.data.ai.AiConversationRepository
import app.suslog.data.ai.AssetBuiltInTuningDocumentRepository
import app.suslog.data.ai.TuningAiStructuredRecommendation
import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.setup.defaultClick
import app.suslog.domain.setup.defaultSetup
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.Corner
import app.suslog.domain.suspension.StiffSide
import app.suslog.domain.suspension.SuspensionType
import app.suslog.domain.tuning.Axle
import app.suslog.domain.tuning.SetupScoreV1
import app.suslog.domain.tuning.SetupChangeSummary
import app.suslog.domain.tuning.SetupChangeType
import app.suslog.domain.tuning.TuningDocument
import app.suslog.domain.tuning.TuningRecommendation
import app.suslog.domain.tuning.buildTuningRecommendation
import app.suslog.data.ai.AiCredentialStore
import app.suslog.data.ai.OpenAiTuningAiClient
import app.suslog.data.local.SuslogDatabase
import app.suslog.domain.tuning.ai.TuningAiApply
import app.suslog.domain.tuning.ai.TuningAiContextBuilder
import app.suslog.ui.setup.SetupRecommendation
import app.suslog.domain.tuning.ai.TuningAiReferenceDocument
import app.suslog.domain.tuning.summarizeSetupChange
import app.suslog.ui.common.DashedRule
import app.suslog.ui.common.FitText
import app.suslog.ui.common.TelemetryCard
import app.suslog.ui.common.formatLapTime
import app.suslog.ui.theme.SuslogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TuningHistoryScreen(
    car: CarProfile,
    config: SetupConfig,
    userTuningDocuments: List<TuningDocument>,
    localUserId: String?,
    isActiveConfig: Boolean,
    onBack: () -> Unit,
    onApplyState: (Int, SetupConfigState) -> Unit,
    onApplyAiRecommendation: (SetupRecommendation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val states = config.states
    var requestedStateIndex by rememberSaveable(config.id, states.size) {
        mutableIntStateOf(states.lastIndex.coerceAtLeast(0))
    }
    var referenceDocuments by remember {
        mutableStateOf<List<TuningAiReferenceDocument>>(emptyList())
    }
    val selectedStateIndex = requestedStateIndex.coerceIn(0, states.lastIndex.coerceAtLeast(0))
    val selectedState = states.getOrNull(selectedStateIndex)
    val previousState = states.getOrNull(selectedStateIndex - 1)
    val selectedChangeSummary = selectedState?.let { state ->
        summarizeSetupChange(
            previousState = previousState,
            currentState = state
        )
    }
    val recommendation = buildTuningRecommendation(
        car = car,
        config = config,
        selectedStateIndex = selectedStateIndex
    )
    val aiContext = TuningAiContextBuilder.build(
        car = car,
        config = config,
        selectedStateIndex = selectedStateIndex,
        modelRecommendation = recommendation,
        referenceDocuments = referenceDocuments,
        userDocuments = userTuningDocuments
    )
    val bestLap = states.mapNotNull { it.lapTimeMillis }.minOrNull()
    val hasAnyFeedback = states.any { it.hasFeedback }

    LaunchedEffect(context) {
        referenceDocuments = withContext(Dispatchers.IO) {
            AssetBuiltInTuningDocumentRepository(context).loadAll()
        }
    }

    val credentials = remember(localUserId) { AiCredentialStore(context, localUserId) }
    val aiClient = remember { OpenAiTuningAiClient() }
    val aiConversationRepository = remember {
        AiConversationRepository(SuslogDatabase.getInstance(context).suslogDao())
    }
    val aiChat = rememberTuningAiChatController(
        client = aiClient,
        credentials = credentials,
        repository = aiConversationRepository
    )
    var showAiSheet by remember(config.id) { mutableStateOf(false) }
    val aiQuickIssues = listOf(
        "Entry understeer", "Mid understeer", "Exit oversteer",
        "Too nervous", "Too lazy", "Poor bump compliance"
    )
    val aiContextSummary =
        "${aiContext.totalStateCount} runs · ${aiContext.referenceDocuments.size} docs · S${aiContext.selectedStateIndex + 1}"

    LaunchedEffect(localUserId, config.id) {
        if (localUserId == null) {
            aiChat.clearThread()
        } else {
            aiChat.loadThread(
                localUserId = localUserId,
                configId = config.id
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TuningHistoryHeader(
            car = car,
            config = config,
            isActiveConfig = isActiveConfig,
            onBack = onBack
        )

        TelemetryCard(title = "Tuning History", meta = "${states.size} STATES") {
            StateTimeline(
                stateCount = states.size,
                selectedStateIndex = selectedStateIndex,
                onSelectState = { requestedStateIndex = it }
            )
        }

        StateSummaryStrip(
            stateText = "${selectedStateIndex + 1}/${states.size.coerceAtLeast(1)}",
            bestLapText = bestLap?.formatLapTime() ?: "—",
            changeSummary = selectedChangeSummary
        )

        if (selectedState != null) {
            TelemetryCard(
                title = "State ${selectedStateIndex + 1} · Analysis",
                meta = "SUBJECTIVE · V1"
            ) {
                if (selectedState.hasFeedback) {
                    AnalysisModule(
                        state = selectedState,
                        previousState = previousState?.takeIf { it.hasFeedback },
                        recommendation = recommendation
                    )
                } else {
                    Text(
                        text = "No feedback shared for this state yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TelemetryCard(title = "Selected State Setup") {
                SelectedStateSetup(
                    car = car,
                    state = selectedState,
                    onApply = { onApplyState(selectedStateIndex, selectedState) }
                )
            }
        }

        TelemetryCard(
            title = "AI Tuning Assistant",
            meta = if (referenceDocuments.isEmpty()) "LOADING DOCS" else "READY"
        ) {
            Text(
                text = "Chat about this config. The assistant reads your runs, the local model, and your tuning guides, and can hand a next test straight to Setup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { showAiSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (credentials.isConnected) "Ask the tuning assistant" else "Set up AI, then ask")
            }
        }

        if (hasAnyFeedback) {
            TelemetryCard(title = "Balance Trend", meta = "NEUTRAL = 0") {
                TrendHeader(
                    left = "Entry / Mid / Exit",
                    right = "Neutral target"
                )
                BalanceTrendChart(
                    states = states,
                    selectedStateIndex = selectedStateIndex
                )
                BalanceLegend()
            }
        }

        if (selectedState != null) {
            TelemetryCard(title = "Note") {
                SelectedStateNote(state = selectedState)
            }
        }

        TelemetryCard(title = "Lap Time Trend", meta = "LOWER = FASTER") {
            TrendHeader(
                left = "Lower is faster",
                right = bestLap?.let { "Best ${it.formatLapTime()}" } ?: "No lap data"
            )
            LapTimeTrendChart(
                states = states,
                selectedStateIndex = selectedStateIndex
            )
        }
    }

        TuningAiSheet(
            visible = showAiSheet,
            contextSummary = aiContextSummary,
            connected = credentials.isConnected,
            messages = aiChat.messages,
            isThinking = aiChat.isThinking,
            isLoading = aiChat.isLoading,
            error = aiChat.error,
            quickIssues = aiQuickIssues,
            normalizeRecommendation = { recommendation ->
                normalizeAiRecommendation(car, recommendation)
            },
            onSend = { text ->
                aiChat.send(
                    userText = text,
                    context = aiContext,
                    adjusterLabels = car.adjusters.map { it.label },
                    localUserId = localUserId,
                    carId = car.id,
                    configId = config.id,
                    configName = config.name
                )
            },
            onApply = { recommendation ->
                val nextTest = recommendation.nextTest
                if (nextTest != null) {
                    val baseSetup = config.currentState?.setup ?: selectedState?.setup ?: emptyMap()
                    val target = TuningAiApply.buildTargetSetup(
                        car = car,
                        baseSetup = baseSetup,
                        axleName = nextTest.axle,
                        adjusterLabel = nextTest.adjusterLabel,
                        targetClick = nextTest.targetClick
                    )
                    if (target != null) {
                        showAiSheet = false
                        onApplyAiRecommendation(
                            SetupRecommendation(
                                carId = car.id,
                                configId = config.id,
                                configName = config.name,
                                stateLabel = "AI · ${nextTest.adjusterLabel} ${nextTest.targetClick}",
                                setup = target
                            )
                        )
                    }
                }
            },
            onDismiss = { showAiSheet = false }
        )
    }
}

private fun normalizeAiRecommendation(
    car: CarProfile,
    recommendation: TuningAiStructuredRecommendation,
): TuningAiStructuredRecommendation {
    val nextTest = recommendation.nextTest ?: return recommendation
    val clamped = TuningAiApply.clampedTargetClick(
        car = car,
        adjusterLabel = nextTest.adjusterLabel,
        targetClick = nextTest.targetClick
    ) ?: return recommendation.copy(
        nextTest = null,
        cautions = recommendation.cautions +
            "Recommendation ignored because the adjuster is not available on this car."
    )

    return recommendation.copy(
        nextTest = nextTest.copy(targetClick = clamped)
    )
}

@Composable
private fun TuningHistoryHeader(
    car: CarProfile,
    config: SetupConfig,
    isActiveConfig: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onBack,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Back")
        }
        Column(modifier = Modifier.weight(1f)) {
            FitText(
                text = config.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            FitText(
                text = car.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isActiveConfig) {
            ActiveConfigBadge()
        }
    }
}

// Section chrome (TelemetryCard / SectionHeader / DashedRule) lives in ui/common/TelemetryCard.kt

@Composable
private fun StatusPill(
    text: String,
    good: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val fill = if (good) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
    }
    Box(
        modifier = modifier
            .background(fill, RoundedCornerShape(5.dp))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
private fun ActiveConfigBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Text(
            text = "● Active".uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

// ───────────────────────── selected-state summary strip ─────────────────────────

@Composable
private fun StateSummaryStrip(
    stateText: String,
    bestLapText: String,
    changeSummary: SetupChangeSummary?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StripCell(label = "State", value = stateText, modifier = Modifier.weight(0.85f))
            StripDivider()
            StripCell(label = "Best Lap", value = bestLapText, modifier = Modifier.weight(1.1f))
            StripDivider()
            StripInputCell(
                changeSummary = changeSummary,
                modifier = Modifier.weight(1.7f)
            )
        }
    }
}

@Composable
private fun StripCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FitText(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun StripInputCell(
    changeSummary: SetupChangeSummary?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(
            text = "Input".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FitText(
                text = changeSummary?.changeLabel() ?: "—",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (changeSummary?.includedInModel == true) {
                StatusPill(text = "Incl", good = true)
            }
        }
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ───────────────────────── consolidated analysis module ─────────────────────────

@Composable
private fun AnalysisModule(
    state: SetupConfigState,
    previousState: SetupConfigState?,
    recommendation: TuningRecommendation,
    modifier: Modifier = Modifier,
) {
    val score = SetupScoreV1.compute(state)
    val primary = MaterialTheme.colorScheme.primary
    val warn = MaterialTheme.colorScheme.tertiary
    val bad = MaterialTheme.colorScheme.error

    fun markColor(sub: Double): Color = when {
        sub >= 0.75 -> primary
        sub >= 0.40 -> warn
        else -> bad
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRadar(
                state = state,
                previousState = previousState,
                modifier = Modifier.size(150.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                AxisTickRow("Entry", balanceFraction(state.cornerEntryBalance), signedBalance(state.cornerEntryBalance), markColor(score.entry))
                AxisTickRow("Mid", balanceFraction(state.cornerMidBalance), signedBalance(state.cornerMidBalance), markColor(score.mid))
                AxisTickRow("Exit", balanceFraction(state.cornerExitBalance), signedBalance(state.cornerExitBalance), markColor(score.exit))
                AxisTickRow("Grip", gripFraction(state.overallGrip), state.overallGrip.coerceIn(1, 5).toString(), markColor(score.grip))
                AxisTickRow("Body", balanceFraction(state.bodyControlBalance), signedBalance(state.bodyControlBalance), markColor(score.bodyControl))
            }
        }

        DashedRule(modifier = Modifier.fillMaxWidth())

        RecommendationRow(recommendation = recommendation)
    }
}

@Composable
private fun AxisTickRow(
    label: String,
    fraction: Float,
    valueText: String,
    markColor: Color,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.outline
    val mid = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(38.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        ) {
            val cy = size.height / 2f
            drawLine(
                color = track,
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { f ->
                val x = (f * size.width).coerceIn(0.5f, size.width - 0.5f)
                val half = if (f == 0.5f) 6.dp.toPx() else 4.dp.toPx()
                drawLine(
                    color = if (f == 0.5f) mid else track,
                    start = Offset(x, cy - half),
                    end = Offset(x, cy + half),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val mx = (fraction * size.width).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
            drawLine(
                color = markColor,
                start = Offset(mx, cy - 7.dp.toPx()),
                end = Offset(mx, cy + 7.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = markColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun RecommendationRow(
    recommendation: TuningRecommendation,
    modifier: Modifier = Modifier,
) {
    val variable = recommendation.variable
    val recClick = recommendation.recommendedClick
    val hasTarget = variable != null && recClick != null
    val targetText = if (hasTarget) {
        val from = recommendation.bestObserved?.clickValue
        val name = "${variable!!.axle.label()} ${variable.adjusterLabel}"
        if (from != null && from != recClick) "$name  $from → $recClick" else "$name → $recClick"
    } else {
        "No clean target yet"
    }
    val confGood = recommendation.fit?.hasPeakInTestedRange == true
    val confText = when {
        variable == null -> "No Data"
        confGood -> "Conf · Med"
        else -> "Conf · Low"
    }
    val note = buildString {
        append(recommendation.message)
        if (hasTarget) append(" Keep all other clicks unchanged.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FitText(
                text = targetText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            StatusPill(text = confText, good = confGood)
        }
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ───────────────────────── score radar ─────────────────────────

@Composable
private fun ScoreRadar(
    state: SetupConfigState,
    previousState: SetupConfigState?,
    modifier: Modifier = Modifier,
) {
    val score = SetupScoreV1.compute(state)
    val total = score.total.roundToInt()
    val delta = previousState?.let { total - SetupScoreV1.compute(it).total.roundToInt() }

    val stroke = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val values = listOf(score.entry, score.mid, score.exit, score.grip, score.bodyControl)
    val labels = listOf("Entry", "Mid", "Exit", "Grip", "Body")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.34f

            fun point(index: Int, scale: Double): Offset {
                val angle = -PI / 2.0 + index * 2.0 * PI / 5.0
                return Offset(
                    x = center.x + (cos(angle) * radius * scale).toFloat(),
                    y = center.y + (sin(angle) * radius * scale).toFloat()
                )
            }

            fun pentagonPath(scale: Double): Path =
                Path().apply {
                    repeat(5) { index ->
                        val p = point(index, scale)
                        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    close()
                }

            listOf(0.33, 0.66, 1.0).forEach { ring ->
                drawPath(
                    path = pentagonPath(ring),
                    color = grid,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            repeat(5) { index ->
                drawLine(
                    color = grid,
                    start = center,
                    end = point(index, 1.0),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val valuePath = Path().apply {
                values.forEachIndexed { index, value ->
                    val p = point(index, value.coerceIn(0.0, 1.0))
                    if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(path = valuePath, color = fill.copy(alpha = 0.30f))
            drawPath(
                path = valuePath,
                color = stroke,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            values.forEachIndexed { index, value ->
                drawCircle(
                    color = stroke,
                    radius = 3.dp.toPx(),
                    center = point(index, value.coerceIn(0.0, 1.0))
                )
            }

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = labelColor.toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 9.dp.toPx()
            }
            labels.forEachIndexed { index, label ->
                val p = point(index, 1.26)
                drawContext.canvas.nativeCanvas.drawText(label.uppercase(), p.x, p.y + 3.dp.toPx(), paint)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "SCORE / 100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (delta != null) {
                DeltaChip(delta = delta, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun DeltaChip(
    delta: Int,
    modifier: Modifier = Modifier,
) {
    val up = delta >= 0
    val accent = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (up) "+" else ""
    val arrow = if (up) "▲" else "▼"
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Δ $sign$delta $arrow",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

// ───────────────────────── kept functional sections ─────────────────────────

@Composable
private fun TrendHeader(
    left: String,
    right: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = right,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StateTimeline(
    stateCount: Int,
    selectedStateIndex: Int,
    onSelectState: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val firstVisibleIndex = (stateCount - 10).coerceAtLeast(0)
    val visibleIndices = (firstVisibleIndex until stateCount).toList()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .pointerInput(stateCount, firstVisibleIndex) {
                    detectTapGestures { tap ->
                        if (visibleIndices.isEmpty()) return@detectTapGestures

                        val startX = 18.dp.toPx()
                        val endX = size.width - 18.dp.toPx()
                        val step = if (visibleIndices.size == 1) {
                            0f
                        } else {
                            (endX - startX) / (visibleIndices.size - 1)
                        }
                        val nearestVisibleIndex = visibleIndices.indices.minBy { visibleIndex ->
                            val x = startX + step * visibleIndex
                            kotlin.math.abs(tap.x - x)
                        }

                        onSelectState(visibleIndices[nearestVisibleIndex])
                    }
                }
        ) {
            if (visibleIndices.isEmpty()) return@Canvas

            val startX = 18.dp.toPx()
            val endX = size.width - 18.dp.toPx()
            val centerY = size.height / 2f
            val step = if (visibleIndices.size == 1) 0f else (endX - startX) / (visibleIndices.size - 1)

            drawLine(
                color = outline,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            visibleIndices.forEachIndexed { visibleIndex, stateIndex ->
                val x = startX + step * visibleIndex
                val selected = stateIndex == selectedStateIndex
                drawCircle(
                    color = if (selected) primary else outline,
                    radius = if (selected) 8.dp.toPx() else 5.dp.toPx(),
                    center = Offset(x, centerY)
                )
                if (selected) {
                    drawCircle(
                        color = surface,
                        radius = 3.dp.toPx(),
                        center = Offset(x, centerY)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedStateSetup(
    car: CarProfile,
    state: SetupConfigState,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onApply,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text("Apply")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CornerSetupBlock(
                corner = Corner.LEFT_FRONT,
                car = car,
                setup = state.setup,
                modifier = Modifier.weight(1f)
            )
            CornerSetupBlock(
                corner = Corner.RIGHT_FRONT,
                car = car,
                setup = state.setup,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CornerSetupBlock(
                corner = Corner.LEFT_REAR,
                car = car,
                setup = state.setup,
                modifier = Modifier.weight(1f)
            )
            CornerSetupBlock(
                corner = Corner.RIGHT_REAR,
                car = car,
                setup = state.setup,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CornerSetupBlock(
    corner: Corner,
    car: CarProfile,
    setup: SetupValues,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = corner.shortLabel(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            car.adjusters.forEach { adjuster ->
                FitText(
                    text = "${adjuster.label}: ${
                        setup[corner]?.get(adjuster.label) ?: adjuster.defaultClick()
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LapTimeTrendChart(
    states: List<SetupConfigState>,
    selectedStateIndex: Int,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val points = states.mapIndexedNotNull { index, state ->
        state.lapTimeMillis?.let { index to it.toFloat() }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 16.dp.toPx()

        repeat(3) { row ->
            val y = top + (bottom - top) * row / 2f
            drawLine(
                color = grid,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (points.isEmpty()) return@Canvas

        val minValue = points.minOf { it.second }
        val maxValue = points.maxOf { it.second }
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
        val xRange = (states.size - 1).coerceAtLeast(1)

        fun xFor(index: Int): Float = left + (right - left) * index / xRange
        fun yFor(value: Float): Float = bottom - (bottom - top) * ((value - minValue) / range)

        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { pathIndex, point ->
                val x = xFor(point.first)
                val y = yFor(point.second)
                if (pathIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = primary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        points.forEach { point ->
            val center = Offset(xFor(point.first), yFor(point.second))
            val selected = point.first == selectedStateIndex
            if (selected) {
                drawCircle(
                    color = selectedColor.copy(alpha = 0.22f),
                    radius = 10.dp.toPx(),
                    center = center
                )
            }
            drawCircle(
                color = if (selected) selectedColor else primary,
                radius = if (selected) 5.dp.toPx() else 4.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun BalanceTrendChart(
    states: List<SetupConfigState>,
    selectedStateIndex: Int,
    modifier: Modifier = Modifier,
) {
    val entryColor = MaterialTheme.colorScheme.secondary
    val midColor = Color(0xFF7A52D0)
    val exitColor = Color(0xFFE0703A)
    val neutralColor = Color(0xFF2F8559)
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val centerY = top + (bottom - top) / 2f
        val xRange = (states.size - 1).coerceAtLeast(1)

        repeat(5) { row ->
            val y = top + (bottom - top) * row / 4f
            drawLine(
                color = if (row == 2) neutralColor.copy(alpha = 0.55f) else grid,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = if (row == 2) 2.dp.toPx() else 1.dp.toPx()
            )
        }

        fun xFor(index: Int): Float = left + (right - left) * index / xRange
        fun yFor(value: Int): Float = centerY - value.coerceIn(-2, 2) * ((bottom - top) / 4f)

        fun drawBalancePath(points: List<Pair<Int, Int>>, color: Color) {
            if (points.size >= 2) {
                val path = Path()
                points.forEachIndexed { pathIndex, (stateIndex, value) ->
                    val x = xFor(stateIndex)
                    val y = yFor(value)
                    if (pathIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            points.forEach { (index, value) ->
                drawCircle(
                    color = color,
                    radius = 3.5.dp.toPx(),
                    center = Offset(xFor(index), yFor(value))
                )
            }
        }

        fun feedbackPoints(valueForState: (SetupConfigState) -> Int): List<Pair<Int, Int>> =
            states.mapIndexedNotNull { index, state ->
                if (state.hasFeedback) index to valueForState(state) else null
            }

        drawBalancePath(feedbackPoints { it.cornerEntryBalance }, entryColor)
        drawBalancePath(feedbackPoints { it.cornerMidBalance }, midColor)
        drawBalancePath(feedbackPoints { it.cornerExitBalance }, exitColor)

        states.getOrNull(selectedStateIndex)?.takeIf { it.hasFeedback }?.let { selectedState ->
            val x = xFor(selectedStateIndex)
            listOf(
                selectedState.cornerEntryBalance,
                selectedState.cornerMidBalance,
                selectedState.cornerExitBalance
            ).forEach { value ->
                drawCircle(
                    color = selectedColor.copy(alpha = 0.22f),
                    radius = 12.dp.toPx(),
                    center = Offset(x, yFor(value))
                )
                drawCircle(
                    color = selectedColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, yFor(value))
                )
            }
        }
    }
}

@Composable
private fun BalanceLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem(label = "Entry", color = MaterialTheme.colorScheme.secondary)
        LegendItem(label = "Mid", color = Color(0xFF7A52D0))
        LegendItem(label = "Exit", color = Color(0xFFE0703A))
        LegendItem(label = "Selected", color = MaterialTheme.colorScheme.tertiary)
        LegendItem(label = "Neutral", color = Color(0xFF2F8559))
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectedStateNote(
    state: SetupConfigState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = state.note ?: "No note",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ───────────────────────── helpers ─────────────────────────

private fun balanceFraction(value: Int): Float = (value.coerceIn(-2, 2) + 2) / 4f

private fun gripFraction(value: Int): Float = (value.coerceIn(1, 5) - 1) / 4f

private fun signedBalance(value: Int): String = if (value > 0) "+$value" else value.toString()

private fun SetupChangeSummary.changeLabel(): String =
    when (type) {
        SetupChangeType.BASELINE -> "Baseline"
        SetupChangeType.AXLE_SINGLE_ADJUSTER -> listOfNotNull(
            axle?.label(),
            adjusterLabel,
            deltaClicks?.signedClicks()
        ).joinToString(" ")

        SetupChangeType.SINGLE_CORNER -> listOfNotNull(
            diffs.firstOrNull()?.corner?.shortLabel(),
            adjusterLabel,
            deltaClicks?.signedClicks()
        ).joinToString(" ")

        SetupChangeType.MIXED -> "Mixed changes"
        SetupChangeType.UNKNOWN -> "Unknown"
    }

private fun Axle.label(): String =
    when (this) {
        Axle.FRONT -> "Front"
        Axle.REAR -> "Rear"
    }

private fun Int.signedClicks(): String =
    when {
        this > 0 -> "+$this"
        else -> toString()
    }

private fun Corner.shortLabel(): String =
    when (this) {
        Corner.LEFT_FRONT -> "LF"
        Corner.RIGHT_FRONT -> "RF"
        Corner.LEFT_REAR -> "LR"
        Corner.RIGHT_REAR -> "RR"
    }

private fun sampleHistoryCar(): CarProfile =
    CarProfile(
        id = "preview",
        name = "E46 M3",
        suspensionType = SuspensionType.TWO_WAY,
        adjusters = listOf(
            AdjusterSpec("Rebound", 30, StiffSide.HIGH_VALUE),
            AdjusterSpec("Compression", 30, StiffSide.HIGH_VALUE)
        )
    )

private fun sampleHistoryConfig(car: CarProfile): SetupConfig {
    fun setup(
        reboundOffset: Int,
        compressionOffset: Int,
    ): SetupValues =
        Corner.entries.associateWith {
            mapOf(
                "Rebound" to (15 + reboundOffset),
                "Compression" to (15 + compressionOffset)
            )
        }

    return SetupConfig(
        id = "preview-config",
        carId = car.id,
        name = "Thunderbolt Dry",
        states = listOf(
            SetupConfigState(
                setup = car.defaultSetup(),
                cornerEntryBalance = -2,
                cornerMidBalance = -1,
                cornerExitBalance = -1,
                overallGrip = 2,
                bodyControlBalance = 1,
                lapTimeMillis = 94_820,
                timestampMillis = 1_767_222_000_000,
                note = "Baseline. Pushes on entry and is slow to rotate mid corner."
            ),
            SetupConfigState(
                setup = setup(-2, 0),
                cornerEntryBalance = -1,
                cornerMidBalance = -1,
                cornerExitBalance = -1,
                overallGrip = 3,
                bodyControlBalance = 1,
                lapTimeMillis = 93_960,
                timestampMillis = 1_767_222_600_000,
                note = "Entry improved. Exit still needs rotation."
            ),
            SetupConfigState(
                setup = setup(-2, 1),
                cornerEntryBalance = 0,
                cornerMidBalance = -1,
                cornerExitBalance = -1,
                overallGrip = 4,
                bodyControlBalance = 0,
                lapTimeMillis = 93_410,
                timestampMillis = 1_767_223_200_000,
                note = "Best rotation so far. Car feels more willing without snap."
            ),
            SetupConfigState(
                setup = setup(0, 2),
                cornerEntryBalance = 0,
                cornerMidBalance = 1,
                cornerExitBalance = 0,
                overallGrip = 4,
                bodyControlBalance = -1,
                lapTimeMillis = 93_780,
                timestampMillis = 1_767_223_800_000,
                note = "Went stiffer. Sharper response, traction a touch worse."
            ),
            SetupConfigState(
                setup = setup(-1, 1),
                cornerEntryBalance = 0,
                cornerMidBalance = 0,
                cornerExitBalance = 0,
                overallGrip = 5,
                bodyControlBalance = 0,
                lapTimeMillis = 93_360,
                timestampMillis = 1_767_224_400_000,
                note = "Returned one step. Balance is neutral and lap time recovered."
            )
        ),
        createdAtMillis = 1_767_222_000_000
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1300)
@Composable
private fun TuningHistoryScreenPreview() {
    val car = sampleHistoryCar()

    SuslogTheme {
        TuningHistoryScreen(
            car = car,
            config = sampleHistoryConfig(car),
            userTuningDocuments = emptyList(),
            localUserId = "preview-account",
            isActiveConfig = true,
            onBack = {},
            onApplyState = { _, _ -> },
            onApplyAiRecommendation = {}
        )
    }
}
