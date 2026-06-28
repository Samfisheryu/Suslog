package app.suslog.domain.suspension

enum class StiffSide {
    LOW_VALUE,
    HIGH_VALUE;

    fun label(maxClicks: Int): String =
        when (this) {
            LOW_VALUE -> "1 stiff / $maxClicks soft"
            HIGH_VALUE -> "1 soft / $maxClicks stiff"
        }
}
