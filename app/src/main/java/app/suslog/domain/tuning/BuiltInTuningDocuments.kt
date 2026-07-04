package app.suslog.domain.tuning

data class BuiltInTuningDocument(
    val id: String,
    val title: String,
    val assetPath: String,
)

object BuiltInTuningDocuments {
    val all: List<BuiltInTuningDocument> = listOf(
        BuiltInTuningDocument(
            id = "coilover-complete-guide",
            title = "Coilover Track Tuning Complete Guide",
            assetPath = "tuning_docs/coilover_complete_guide.md"
        ),
        BuiltInTuningDocument(
            id = "two-way-coilover-guide",
            title = "Two-Way Coilover Tuning Guide",
            assetPath = "tuning_docs/two_way_coilover_guide.md"
        ),
        BuiltInTuningDocument(
            id = "damper-basics-guide",
            title = "Damper Basics Tuning Guide",
            assetPath = "tuning_docs/damper_basics_guide.md"
        )
    )
}
