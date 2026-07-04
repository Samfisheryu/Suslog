package app.suslog.domain.tuning.ai

import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.suspension.Corner
import app.suslog.domain.tuning.Axle

/**
 * Turns an AI "change one axle + one adjuster to a target click" into a concrete target setup.
 * The app is the guardrail: the adjuster must exist on the car and the click is clamped to range.
 */
object TuningAiApply {
    fun buildTargetSetup(
        car: CarProfile,
        baseSetup: SetupValues,
        axleName: String,
        adjusterLabel: String,
        targetClick: Int,
    ): SetupValues? {
        val adjuster = car.adjusters.firstOrNull { it.label == adjusterLabel } ?: return null
        val axle = parseAxle(axleName) ?: return null
        val clamped = targetClick.coerceIn(1, adjuster.maxClicks)
        val axleCorners = axle.corners()

        return Corner.entries.associateWith { corner ->
            val values = baseSetup[corner].orEmpty()
            if (corner in axleCorners) {
                values + (adjusterLabel to clamped)
            } else {
                values
            }
        }
    }

    fun clampedTargetClick(car: CarProfile, adjusterLabel: String, targetClick: Int): Int? {
        val adjuster = car.adjusters.firstOrNull { it.label == adjusterLabel } ?: return null
        return targetClick.coerceIn(1, adjuster.maxClicks)
    }

    private fun parseAxle(name: String): Axle? =
        when (name.uppercase()) {
            "FRONT" -> Axle.FRONT
            "REAR" -> Axle.REAR
            else -> null
        }

    private fun Axle.corners(): Set<Corner> =
        when (this) {
            Axle.FRONT -> setOf(Corner.LEFT_FRONT, Corner.RIGHT_FRONT)
            Axle.REAR -> setOf(Corner.LEFT_REAR, Corner.RIGHT_REAR)
        }
}
