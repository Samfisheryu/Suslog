package com.example.suslog.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Single committed scheme: telemetry light. No dynamic color, no dark variant yet.
private val TelemetryLightColors = lightColorScheme(
    primary = LimeInk,
    onPrimary = PanelWhite,
    primaryContainer = LimeBright,
    onPrimaryContainer = InkHigh,
    secondary = Cyan,
    onSecondary = PanelWhite,
    tertiary = Amber,
    onTertiary = PanelWhite,
    background = Paper,
    onBackground = InkHigh,
    surface = PanelWhite,
    onSurface = InkHigh,
    surfaceVariant = Inset,
    onSurfaceVariant = InkMute,
    outline = Line,
    outlineVariant = LineSoft,
    error = SignalRed,
    onError = PanelWhite,
)

@Composable
fun SuslogTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = TelemetryLightColors,
        typography = Typography,
        content = content
    )
}
