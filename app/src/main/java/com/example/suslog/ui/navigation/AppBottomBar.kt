package com.example.suslog.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AppBottomBar(
    currentDestination: AppDestination,
    plusExpanded: Boolean,
    onDestinationClick: (AppDestination) -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plusScale by animateFloatAsState(
        targetValue = if (plusExpanded) 1.12f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "plusScale"
    )
    val glowSize by animateDpAsState(
        targetValue = if (plusExpanded) 70.dp else 52.dp,
        animationSpec = tween(durationMillis = 160),
        label = "plusGlowSize"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (plusExpanded) 0.22f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "plusGlowAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarTab(
                destination = AppDestination.SETUP,
                selected = currentDestination == AppDestination.SETUP,
                onClick = onDestinationClick,
                modifier = Modifier.weight(1f)
            )
            BottomBarTab(
                destination = AppDestination.CONFIG,
                selected = currentDestination == AppDestination.CONFIG,
                onClick = onDestinationClick,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(glowSize)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                            shape = CircleShape
                        )
                )
                FloatingActionButton(
                    onClick = onPlusClick,
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = plusScale
                            scaleY = plusScale
                        },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            BottomBarTab(
                destination = AppDestination.TUNING,
                selected = currentDestination == AppDestination.TUNING,
                onClick = onDestinationClick,
                modifier = Modifier.weight(1f)
            )
            BottomBarTab(
                destination = AppDestination.SETTINGS,
                selected = currentDestination == AppDestination.SETTINGS,
                onClick = onDestinationClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomBarTab(
    destination: AppDestination,
    selected: Boolean,
    onClick: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = { onClick(destination) },
        modifier = modifier
    ) {
        Text(
            text = destination.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
