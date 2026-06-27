package com.example.suslog.domain.setup

data class SetupConfig(
    val id: String,
    val carId: String,
    val name: String,
    val states: List<SetupConfigState>,
    val createdAtMillis: Long,
) {
    val currentState: SetupConfigState?
        get() = states.lastOrNull()

    val setup: SetupValues
        get() = currentState?.setup.orEmpty()

    val cornerEntryBalance: Int
        get() = currentState?.cornerEntryBalance ?: 0

    val cornerExitBalance: Int
        get() = currentState?.cornerExitBalance ?: 0

    val lapTimeMillis: Long?
        get() = currentState?.lapTimeMillis

    val updatedAtMillis: Long
        get() = currentState?.timestampMillis ?: createdAtMillis

    fun recordSnapshot(
        state: SetupConfigState,
        name: String = this.name,
    ): SetupConfig =
        copy(
            name = name,
            states = states + state
        )

    fun saveFeedback(
        setup: SetupValues,
        feedback: SetupConfigFeedback,
        timestampMillis: Long = System.currentTimeMillis(),
    ): SetupConfig {
        val nextState = SetupConfigState(
            setup = setup,
            cornerEntryBalance = requireNotNull(feedback.cornerEntryBalance),
            cornerExitBalance = requireNotNull(feedback.cornerExitBalance),
            lapTimeMillis = feedback.lapTimeMillis,
            timestampMillis = timestampMillis,
            note = feedback.note
        )
        val current = currentState

        return if (current != null && current.setup == setup) {
            copy(states = states.dropLast(1) + nextState.copy(id = current.id))
        } else {
            recordSnapshot(nextState)
        }
    }

    fun withCurrentStateId(stateId: Long): SetupConfig {
        val current = currentState ?: return this

        return copy(states = states.dropLast(1) + current.copy(id = stateId))
    }
}

data class SetupConfigState(
    val setup: SetupValues,
    val cornerEntryBalance: Int,
    val cornerExitBalance: Int,
    val lapTimeMillis: Long?,
    val timestampMillis: Long,
    val note: String? = null,
    val id: Long? = null,
)

data class SetupConfigFeedback(
    val cornerEntryBalance: Int?,
    val cornerExitBalance: Int?,
    val lapTimeMillis: Long?,
    val note: String?,
)
