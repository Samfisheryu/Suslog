package app.suslog.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp

/**
 * Bordered telemetry surface with a [SectionHeader]. Shared across all screens so the
 * card chrome stays identical app-wide.
 */
@Composable
fun TelemetryCard(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(title = title, meta = meta)
            content()
        }
    }
}

/**
 * Telemetry section header: lime tick · uppercase mono title · dashed rule · trailing meta.
 * [trailing] is an optional slot (e.g. an expand/collapse control) placed after the meta.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 13.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DashedRule(modifier = Modifier.weight(1f))
        meta?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun DashedRule(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
        )
    }
}
