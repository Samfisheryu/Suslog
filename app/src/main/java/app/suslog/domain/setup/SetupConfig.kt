package app.suslog.domain.setup

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

    val cornerMidBalance: Int
        get() = currentState?.cornerMidBalance ?: 0

    val cornerExitBalance: Int
        get() = currentState?.cornerExitBalance ?: 0

    val overallGrip: Int
        get() = currentState?.overallGrip ?: 3

    val bodyControlBalance: Int
        get() = currentState?.bodyControlBalance ?: 0

    val lapTimeMillis: Long?
        get() = currentState?.lapTimeMillis

    val hasFeedback: Boolean
        get() = currentState?.hasFeedback == true

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
            cornerMidBalance = requireNotNull(feedback.cornerMidBalance),
            cornerExitBalance = requireNotNull(feedback.cornerExitBalance),
            overallGrip = requireNotNull(feedback.overallGrip),
            bodyControlBalance = requireNotNull(feedback.bodyControlBalance),
            lapTimeMillis = feedback.lapTimeMillis,
            timestampMillis = timestampMillis,
            note = feedback.note,
            hasFeedback = true
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
    val cornerMidBalance: Int = 0,
    val cornerExitBalance: Int,
    val overallGrip: Int = 3,
    val bodyControlBalance: Int = 0,
    val lapTimeMillis: Long?,
    val timestampMillis: Long,
    val note: String? = null,
    val id: Long? = null,
    val hasFeedback: Boolean = true,
)

data class SetupConfigFeedback(
    val cornerEntryBalance: Int?,
    val cornerMidBalance: Int?,
    val cornerExitBalance: Int?,
    val overallGrip: Int?,
    val bodyControlBalance: Int?,
    val lapTimeMillis: Long?,
    val note: String?,
)
