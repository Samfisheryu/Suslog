package app.suslog.domain.car

import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.SuspensionType

data class CarProfile(
    val id: String,
    val name: String,
    val suspensionType: SuspensionType,
    val adjusters: List<AdjusterSpec>,
)
