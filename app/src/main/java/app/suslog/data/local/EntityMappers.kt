package app.suslog.data.local

import app.suslog.domain.car.CarProfile
import app.suslog.domain.setup.SetupConfig
import app.suslog.domain.setup.SetupConfigState
import app.suslog.domain.setup.SetupState
import app.suslog.domain.setup.SetupValues
import app.suslog.domain.suspension.AdjusterSpec
import app.suslog.domain.suspension.Corner
import app.suslog.domain.suspension.StiffSide
import app.suslog.domain.suspension.SuspensionType
import app.suslog.domain.tuning.TuningDocument

fun CarProfile.toEntity(localUserId: String): CarEntity =
    CarEntity(
        id = id,
        localUserId = localUserId,
        name = name,
        suspensionType = suspensionType.name
    )

fun CarProfile.toAdjusterEntities(): List<AdjusterEntity> =
    adjusters.mapIndexed { index, adjuster ->
        AdjusterEntity(
            carId = id,
            label = adjuster.label,
            maxClicks = adjuster.maxClicks,
            stiffSide = adjuster.stiffSide.name,
            sortOrder = index
        )
    }

fun CarWithAdjusters.toDomain(): CarProfile =
    CarProfile(
        id = car.id,
        name = car.name,
        suspensionType = SuspensionType.valueOf(car.suspensionType),
        adjusters = adjusters
            .sortedBy { it.sortOrder }
            .map { adjuster ->
                AdjusterSpec(
                    label = adjuster.label,
                    maxClicks = adjuster.maxClicks,
                    stiffSide = StiffSide.valueOf(adjuster.stiffSide)
                )
            }
    )

fun SetupState.toClickEntities(stateId: Long): List<SetupClickEntity> =
    setup.flatMap { (corner, values) ->
        values.map { (adjusterLabel, clickValue) ->
            SetupClickEntity(
                stateId = stateId,
                corner = corner.name,
                adjusterLabel = adjusterLabel,
                clickValue = clickValue
            )
        }
    }

fun setupStateFromEntities(
    state: SetupStateEntity,
    clicks: List<SetupClickEntity>,
): SetupState {
    val setup: SetupValues = clicks
        .groupBy { Corner.valueOf(it.corner) }
        .mapValues { (_, cornerClicks) ->
            cornerClicks.associate { click ->
                click.adjusterLabel to click.clickValue
            }
        }

    return SetupState(
        setup = setup,
        timestampMillis = state.timestampMillis
    )
}

fun SetupConfig.toEntity(): SetupConfigEntity =
    requireNotNull(currentState) { "SetupConfig must have at least one state before persistence." }.let { state ->
        SetupConfigEntity(
            id = id,
            carId = carId,
            name = name,
            cornerEntryBalance = state.cornerEntryBalance,
            cornerExitBalance = state.cornerExitBalance,
            lapTimeMillis = state.lapTimeMillis,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = state.timestampMillis
        )
    }

fun SetupConfig.currentStateEntity(): SetupConfigStateEntity {
    val state = requireNotNull(currentState) {
        "SetupConfig must have at least one state before persistence."
    }

    return SetupConfigStateEntity(
        id = state.id ?: 0,
        configId = id,
        cornerEntryBalance = state.cornerEntryBalance,
        cornerMidBalance = state.cornerMidBalance,
        cornerExitBalance = state.cornerExitBalance,
        overallGrip = state.overallGrip,
        bodyControlBalance = state.bodyControlBalance,
        lapTimeMillis = state.lapTimeMillis,
        timestampMillis = state.timestampMillis,
        note = state.note,
        hasFeedback = state.hasFeedback
    )
}

fun SetupConfigState.toClickEntities(stateId: Long): List<SetupConfigStateClickEntity> =
    setup.flatMap { (corner, values) ->
        values.map { (adjusterLabel, clickValue) ->
            SetupConfigStateClickEntity(
                stateId = stateId,
                corner = corner.name,
                adjusterLabel = adjusterLabel,
                clickValue = clickValue
            )
        }
    }

fun SetupConfigStateEntity.toDomain(
    clicks: List<SetupConfigStateClickEntity>,
): SetupConfigState {
    val setup: SetupValues = clicks
        .groupBy { Corner.valueOf(it.corner) }
        .mapValues { (_, cornerClicks) ->
            cornerClicks.associate { click ->
                click.adjusterLabel to click.clickValue
            }
        }

    return SetupConfigState(
        setup = setup,
        cornerEntryBalance = cornerEntryBalance,
        cornerMidBalance = cornerMidBalance,
        cornerExitBalance = cornerExitBalance,
        overallGrip = overallGrip,
        bodyControlBalance = bodyControlBalance,
        lapTimeMillis = lapTimeMillis,
        timestampMillis = timestampMillis,
        note = note,
        id = id,
        hasFeedback = hasFeedback
    )
}

fun setupConfigFromEntities(
    config: SetupConfigEntity,
    states: List<SetupConfigState>,
): SetupConfig =
    SetupConfig(
        id = config.id,
        carId = config.carId,
        name = config.name,
        states = states,
        createdAtMillis = config.createdAtMillis
    )

fun TuningDocument.toEntity(): TuningDocumentEntity =
    TuningDocumentEntity(
        id = id,
        carId = carId,
        name = name,
        createdAtMillis = createdAtMillis
    )

fun TuningDocument.toContentEntity(): TuningDocumentContentEntity =
    TuningDocumentContentEntity(
        documentId = id,
        content = content,
        updatedAtMillis = createdAtMillis
    )

fun TuningDocumentEntity.toDomain(
    content: TuningDocumentContentEntity?,
): TuningDocument =
    TuningDocument(
        id = id,
        carId = carId,
        name = name,
        content = content?.content.orEmpty(),
        createdAtMillis = createdAtMillis
    )
