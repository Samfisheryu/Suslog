package com.example.suslog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.suslog.domain.car.CarProfile
import com.example.suslog.domain.setup.defaultClick
import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.Corner
import com.example.suslog.domain.suspension.StiffSide

private val LockedAdjuster = AdjusterSpec("Damping", 30, StiffSide.HIGH_VALUE)

@Composable
fun SetupCornerCard(
    corner: Corner,
    car: CarProfile?,
    values: Map<String, Int>,
    targetValues: Map<String, Int>?,
    onAdjustClick: (Corner, AdjusterSpec, Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    recommendationLabel: String = "Recommend",
) {
    val adjusters = car?.adjusters ?: listOf(LockedAdjuster)

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = corner.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            adjusters.forEachIndexed { index, adjuster ->
                if (index > 0) {
                    HorizontalDivider(
                        color = DividerDefaults.color.copy(alpha = 0.45f)
                    )
                }
                AdjusterRow(
                    adjuster = adjuster,
                    value = if (enabled) values[adjuster.label] ?: adjuster.defaultClick() else null,
                    targetValue = targetValues?.get(adjuster.label),
                    enabled = enabled,
                    recommendationLabel = recommendationLabel,
                    onDecrease = { onAdjustClick(corner, adjuster, -1) },
                    onIncrease = { onAdjustClick(corner, adjuster, 1) }
                )
            }
        }
    }
}

@Composable
private fun AdjusterRow(
    adjuster: AdjusterSpec,
    value: Int?,
    targetValue: Int?,
    enabled: Boolean,
    recommendationLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FitText(
            text = adjuster.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDecrease,
                enabled = enabled && value != null && value > 1,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-")
            }
            Text(
                text = value?.toString() ?: "-",
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = onIncrease,
                enabled = enabled && value != null && value < adjuster.maxClicks,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+")
            }
        }
        Text(
            text = adjuster.stiffSide.label(adjuster.maxClicks),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (enabled && value != null && targetValue != null) {
            Text(
                text = "$recommendationLabel: ${targetDeltaLabel(current = value, target = targetValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun targetDeltaLabel(current: Int, target: Int): String {
    val delta = target - current
    return when {
        delta > 0 -> "+$delta"
        delta < 0 -> delta.toString()
        else -> "target"
    }
}
