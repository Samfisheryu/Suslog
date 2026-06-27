package com.example.suslog.domain.setup

import com.example.suslog.domain.car.CarProfile
import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.Corner

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
