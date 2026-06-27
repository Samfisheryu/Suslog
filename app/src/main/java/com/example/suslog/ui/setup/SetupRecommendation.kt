package com.example.suslog.ui.setup

import com.example.suslog.domain.setup.SetupValues

data class SetupRecommendation(
    val carId: String,
    val configId: String,
    val configName: String,
    val stateLabel: String,
    val setup: SetupValues,
)
