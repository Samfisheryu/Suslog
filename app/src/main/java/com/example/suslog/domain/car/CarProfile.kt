package com.example.suslog.domain.car

import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.SuspensionType

data class CarProfile(
    val id: String,
    val name: String,
    val suspensionType: SuspensionType,
    val adjusters: List<AdjusterSpec>,
)
