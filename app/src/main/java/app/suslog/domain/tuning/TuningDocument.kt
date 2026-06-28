package app.suslog.domain.tuning

data class TuningDocument(
    val id: String,
    val carId: String,
    val name: String,
    val createdAtMillis: Long,
)
