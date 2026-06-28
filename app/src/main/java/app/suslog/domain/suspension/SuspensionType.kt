package app.suslog.domain.suspension

enum class SuspensionType(
    val label: String,
    val adjusterLabels: List<String>,
) {
    ONE_WAY("1-way", listOf("Damping")),
    TWO_WAY("2-way", listOf("Rebound", "Compression")),
    THREE_WAY("3-way", listOf("Rebound", "Low Speed Compression", "High Speed Compression")),
}
