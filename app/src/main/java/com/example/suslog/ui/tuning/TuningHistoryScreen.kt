package com.example.suslog.ui.tuning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.suslog.domain.car.CarProfile
import com.example.suslog.domain.setup.SetupConfig
import com.example.suslog.domain.setup.SetupConfigState
import com.example.suslog.domain.setup.SetupValues
import com.example.suslog.domain.setup.defaultClick
import com.example.suslog.domain.setup.defaultSetup
import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.Corner
import com.example.suslog.domain.suspension.StiffSide
import com.example.suslog.domain.suspension.SuspensionType
import com.example.suslog.ui.common.formatLapTime
import com.example.suslog.ui.theme.SuslogTheme

@Composable
fun TuningHistoryScreen(
    car: CarProfile,
    config: SetupConfig,
    isActiveConfig: Boolean,
    onBack: () -> Unit,
    onApplyState: (Int, SetupConfigState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val states = config.states
    var requestedStateIndex by rememberSaveable(config.id, states.size) {
        mutableIntStateOf(states.lastIndex.coerceAtLeast(0))
    }
    val selectedStateIndex = requestedStateIndex.coerceIn(0, states.lastIndex.coerceAtLeast(0))
    val selectedState = states.getOrNull(selectedStateIndex)
    val bestLap = states.mapNotNull { it.lapTimeMillis }.minOrNull()

    Column(
        modifier = modifier
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
//            TuningMetric(
//                label = "States",
//                value = states.size.toString(),
//                modifier = Modifier.weight(1f)
//            )
            TuningMetric(
                label = "Best Lap",
                value = bestLap?.formatLapTime() ?: "-",
                modifier = Modifier.weight(1f)
            )
//            TuningMetric(
//                label = "Selected",
//                value = selectedStateIndex.stateLabel(),
//                modifier = Modifier.weight(1f)
//            )
        }

        TuningCard(title = "State Timeline") {
            StateTimeline(
                stateCount = states.size,
                selectedStateIndex = selectedStateIndex,
                onSelectState = { requestedStateIndex = it }
            )
        }

        if (selectedState != null) {
            TuningCard(title = "Selected State Setup") {
                SelectedStateSetup(
                    car = car,
                    state = selectedState,
                    stateLabel = selectedStateIndex.stateLabel(),
                    onApply = { onApplyState(selectedStateIndex, selectedState) }
                )
            }
        }

        TuningCard(title = "Balance Trend") {
            TrendHeader(
                left = "Entry / Exit",
                right = "Neutral target"
            )
            BalanceTrendChart(
                states = states,
                selectedStateIndex = selectedStateIndex
            )
            BalanceLegend()
        }

        if (selectedState != null) {
            TuningCard(title = "Note") {
                SelectedStateNote(state = selectedState)
            }
        }

        TuningCard(title = "Lap Time Trend") {
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
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = car.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isActiveConfig) {
            ActiveConfigBadge()
        }
    }
}

@Composable
private fun TuningCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun TuningMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveConfigBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = "Active",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

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
    val visibleSelectedIndex = visibleIndices.indexOf(selectedStateIndex)

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

//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = visibleIndices.firstOrNull()?.stateLabel().orEmpty(),
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                text = if (visibleSelectedIndex >= 0) selectedStateIndex.stateLabel() else "",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.primary,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = visibleIndices.lastOrNull()?.stateLabel().orEmpty(),
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
    }
}

@Composable
private fun SelectedStateSetup(
    car: CarProfile,
    state: SetupConfigState,
    stateLabel: String,
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
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
                Text(
                    text = "${adjuster.label}: ${
                        setup[corner]?.get(adjuster.label) ?: adjuster.defaultClick()
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    val entryColor = Color(0xFF2D9CDB)
    val exitColor = Color(0xFFE36A4A)
    val neutralColor = Color(0xFF2F855A)
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

        fun drawBalancePath(values: List<Int>, color: Color) {
            if (values.size >= 2) {
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = xFor(index)
                    val y = yFor(value)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            values.forEachIndexed { index, value ->
                drawCircle(
                    color = color,
                    radius = 3.5.dp.toPx(),
                    center = Offset(xFor(index), yFor(value))
                )
            }
        }

        drawBalancePath(states.map { it.cornerEntryBalance }, entryColor)
        drawBalancePath(states.map { it.cornerExitBalance }, exitColor)

        states.getOrNull(selectedStateIndex)?.let { selectedState ->
            val x = xFor(selectedStateIndex)
            drawCircle(
                color = selectedColor.copy(alpha = 0.22f),
                radius = 12.dp.toPx(),
                center = Offset(x, yFor(selectedState.cornerEntryBalance))
            )
            drawCircle(
                color = selectedColor,
                radius = 5.dp.toPx(),
                center = Offset(x, yFor(selectedState.cornerEntryBalance))
            )
            drawCircle(
                color = selectedColor.copy(alpha = 0.22f),
                radius = 12.dp.toPx(),
                center = Offset(x, yFor(selectedState.cornerExitBalance))
            )
            drawCircle(
                color = selectedColor,
                radius = 5.dp.toPx(),
                center = Offset(x, yFor(selectedState.cornerExitBalance))
            )
        }
    }
}

@Composable
private fun BalanceLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem(label = "Entry", color = Color(0xFF2D9CDB))
        LegendItem(label = "Exit", color = Color(0xFFE36A4A))
        LegendItem(label = "Selected", color = MaterialTheme.colorScheme.tertiary)
        LegendItem(label = "Neutral", color = Color(0xFF2F855A))
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

private fun Int.stateLabel(): String = "S${this + 1}"

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
                cornerExitBalance = -1,
                lapTimeMillis = 94_820,
                timestampMillis = 1_767_222_000_000,
                note = "Baseline. Pushes on entry and is slow to rotate mid corner."
            ),
            SetupConfigState(
                setup = setup(-2, 0),
                cornerEntryBalance = -1,
                cornerExitBalance = -1,
                lapTimeMillis = 93_960,
                timestampMillis = 1_767_222_600_000,
                note = "Entry improved. Exit still needs rotation."
            ),
            SetupConfigState(
                setup = setup(-2, 1),
                cornerEntryBalance = 0,
                cornerExitBalance = -1,
                lapTimeMillis = 93_410,
                timestampMillis = 1_767_223_200_000,
                note = "Best rotation so far. Car feels more willing without snap."
            ),
            SetupConfigState(
                setup = setup(0, 2),
                cornerEntryBalance = 1,
                cornerExitBalance = 1,
                lapTimeMillis = 93_780,
                timestampMillis = 1_767_223_800_000,
                note = "Went too stiff. Better response, but traction is worse."
            ),
            SetupConfigState(
                setup = setup(-1, 1),
                cornerEntryBalance = 0,
                cornerExitBalance = 0,
                lapTimeMillis = 93_360,
                timestampMillis = 1_767_224_400_000,
                note = "Returned one step. Balance is neutral and lap time recovered."
            )
        ),
        createdAtMillis = 1_767_222_000_000
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun TuningHistoryScreenPreview() {
    val car = sampleHistoryCar()

    SuslogTheme {
        TuningHistoryScreen(
            car = car,
            config = sampleHistoryConfig(car),
            isActiveConfig = true,
            onBack = {},
            onApplyState = { _, _ -> }
        )
    }
}
