package app.suslog.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BalanceSelector(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    referenceValue: Int? = null,
) {
    val showingReference = value == null && referenceValue != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = value?.let(::balanceLabel)
                    ?: referenceValue?.let { "Prev ${balanceLabel(it)}" }
                    ?: "Not Shared",
                style = MaterialTheme.typography.bodySmall,
                color = if (showingReference) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Understeer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 76.dp)
            )
            BalanceOptionDots(
                value = value,
                referenceValue = referenceValue,
                onValueChange = onValueChange,
                enabled = enabled
            )
            Text(
                text = "Oversteer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 76.dp)
            )
        }
    }
}

@Composable
private fun BalanceOptionDots(
    value: Int?,
    referenceValue: Int?,
    onValueChange: (Int?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (-2..2).forEach { option ->
            val selected = option == value
            val referenced = value == null && option == referenceValue
            Surface(
                modifier = Modifier
                    .size(if (selected) 34.dp else 30.dp)
                    .clickable(
                        enabled = enabled,
                        onClick = {
                            onValueChange(
                                if (selected) null else option
                            )
                        }
                    ),
                shape = CircleShape,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.surfaceVariant
                    selected -> MaterialTheme.colorScheme.primary
                    referenced -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        selected && enabled -> MaterialTheme.colorScheme.primary
                        referenced && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {}
        }
    }
}

fun balanceLabel(value: Int): String =
    when (value) {
        -2 -> "Understeer"
        -1 -> "Slight Understeer"
        0 -> "Neutral"
        1 -> "Slight Oversteer"
        else -> "Oversteer"
    }

@Composable
fun LabeledScaleSelector(
    title: String,
    value: Int?,
    options: List<Int>,
    labelForValue: (Int) -> String,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    referenceValue: Int? = null,
    startLabel: String = labelForValue(options.first()),
    endLabel: String = labelForValue(options.last()),
) {
    val showingReference = value == null && referenceValue != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = value?.let(labelForValue)
                    ?: referenceValue?.let { "Prev ${labelForValue(it)}" }
                    ?: "Not Shared",
                style = MaterialTheme.typography.bodySmall,
                color = if (showingReference) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 76.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    val selected = option == value
                    val referenced = value == null && option == referenceValue
                    Surface(
                        modifier = Modifier
                            .size(if (selected) 34.dp else 30.dp)
                            .clickable(
                                enabled = enabled,
                                onClick = {
                                    onValueChange(
                                        if (selected) null else option
                                    )
                                }
                            ),
                        shape = CircleShape,
                        color = when {
                            !enabled -> MaterialTheme.colorScheme.surfaceVariant
                            selected -> MaterialTheme.colorScheme.primary
                            referenced -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                selected && enabled -> MaterialTheme.colorScheme.primary
                                referenced && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {}
                }
            }
            Text(
                text = endLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 76.dp)
            )
        }
    }
}

fun gripLabel(value: Int): String =
    when (value.coerceIn(1, 5)) {
        1 -> "Very Low"
        2 -> "Low"
        3 -> "Okay"
        4 -> "Good"
        else -> "Very Good"
    }

fun bodyControlLabel(value: Int): String =
    when (value.coerceIn(-2, 2)) {
        -2 -> "Too Stiff"
        -1 -> "Slight Stiff"
        0 -> "Controlled"
        1 -> "Slight Roll"
        else -> "Too Much Roll"
    }
