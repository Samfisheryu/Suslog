package app.suslog.domain.suspension

data class AdjusterSpec(
    val label: String,
    val maxClicks: Int,
    val stiffSide: StiffSide,
)
