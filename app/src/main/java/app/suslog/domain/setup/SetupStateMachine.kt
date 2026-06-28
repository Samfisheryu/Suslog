package app.suslog.domain.setup

import app.suslog.domain.car.CarProfile
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.Corner

data class SetupStateMachine(
    val states: List<SetupState>,
) {
    val currentState: SetupState?
        get() = states.lastOrNull()

    fun transition(
        corner: Corner,
        adjuster: AdjusterSpec,
        delta: Int,
        timestampMillis: Long = System.currentTimeMillis(),
    ): SetupStateMachine {
        val currentSetup = currentState?.setup.orEmpty()
        val nextSetup = currentSetup.withAdjustedClick(corner, adjuster, delta)

        return copy(
            states = states + SetupState(
                setup = nextSetup,
                timestampMillis = timestampMillis
            )
        )
    }

    fun recordSnapshot(
        setup: SetupValues,
        timestampMillis: Long = System.currentTimeMillis(),
    ): SetupStateMachine =
        copy(
            states = states + SetupState(
                setup = setup,
                timestampMillis = timestampMillis
            )
        )
}

fun CarProfile.defaultSetup(): SetupValues =
    Corner.entries.associateWith {
        adjusters.associate { adjuster ->
            adjuster.label to adjuster.defaultClick()
        }
    }

fun CarProfile.initialSetupStateMachine(
    timestampMillis: Long = System.currentTimeMillis(),
): SetupStateMachine =
    SetupStateMachine(
        states = listOf(
            SetupState(
                setup = defaultSetup(),
                timestampMillis = timestampMillis
            )
        )
    )

fun AdjusterSpec.defaultClick(): Int =
    ((maxClicks + 1) / 2).coerceIn(1, maxClicks)

fun SetupValues.withAdjustedClick(
    corner: Corner,
    adjuster: AdjusterSpec,
    delta: Int,
): SetupValues =
    mapValues { (setupCorner, values) ->
        if (setupCorner != corner) {
            values
        } else {
            values.toMutableMap().apply {
                val current = this[adjuster.label] ?: adjuster.defaultClick()
                this[adjuster.label] = (current + delta).coerceIn(1, adjuster.maxClicks)
            }
        }
    }

fun SetupValues.withAdjustedAxleMatchedClick(
    corner: Corner,
    adjuster: AdjusterSpec,
    delta: Int,
): SetupValues {
    val current = this[corner]?.get(adjuster.label) ?: adjuster.defaultClick()
    val target = (current + delta).coerceIn(1, adjuster.maxClicks)
    val axleCorners = corner.axleCorners()

    return mapValues { (setupCorner, values) ->
        if (setupCorner !in axleCorners) {
            values
        } else {
            values.toMutableMap().apply {
                this[adjuster.label] = target
            }
        }
    }
}

private fun Corner.axleCorners(): Set<Corner> =
    when (this) {
        Corner.LEFT_FRONT,
        Corner.RIGHT_FRONT -> setOf(Corner.LEFT_FRONT, Corner.RIGHT_FRONT)

        Corner.LEFT_REAR,
        Corner.RIGHT_REAR -> setOf(Corner.LEFT_REAR, Corner.RIGHT_REAR)
    }
