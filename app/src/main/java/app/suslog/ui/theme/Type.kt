package app.suslog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.suslog.R

// Telemetry typography — bundled static TTFs (res/font), no runtime download.
// Display = Chakra Petch (squared instrument look) for big readouts & headlines.
// Mono = JetBrains Mono for all data, labels, and body.
private val Display = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

val Mono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 46.sp, lineHeight = 46.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 1.0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.4.sp
    ),
)
