package app.suslog.ui.setup

import app.suslog.domain.setup.SetupValues

data class SetupRecommendation(
    val carId: String,
    val configId: String,
    val configName: String,
    val stateLabel: String,
    val setup: SetupValues,
)
