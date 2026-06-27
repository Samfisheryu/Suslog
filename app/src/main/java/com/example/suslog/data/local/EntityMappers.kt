package com.example.suslog.data.local

import com.example.suslog.domain.car.CarProfile
import com.example.suslog.domain.setup.SetupConfig
import com.example.suslog.domain.setup.SetupConfigState
import com.example.suslog.domain.setup.SetupState
import com.example.suslog.domain.setup.SetupValues
import com.example.suslog.domain.suspension.AdjusterSpec
import com.example.suslog.domain.suspension.Corner
import com.example.suslog.domain.suspension.StiffSide
import com.example.suslog.domain.suspension.SuspensionType
import com.example.suslog.domain.tuning.TuningDocument

fun CarProfile.toEntity(): CarEntity =
    CarEntity(
        id = id,
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
        note = state.note
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

fun SetupConfig.toLegacyClickEntities(): List<SetupConfigClickEntity> =
    setup.flatMap { (corner, values) ->
        values.map { (adjusterLabel, clickValue) ->
            SetupConfigClickEntity(
                configId = id,
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
        id = id
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

fun legacySetupConfigFromEntities(
    config: SetupConfigEntity,
    clicks: List<SetupConfigClickEntity>,
): SetupConfig {
    val setup: SetupValues = clicks
        .groupBy { Corner.valueOf(it.corner) }
        .mapValues { (_, cornerClicks) ->
            cornerClicks.associate { click ->
                click.adjusterLabel to click.clickValue
            }
        }

    return SetupConfig(
        id = config.id,
        carId = config.carId,
        name = config.name,
        states = listOf(
            SetupConfigState(
                setup = setup,
                cornerEntryBalance = config.cornerEntryBalance,
                cornerMidBalance = 0,
                cornerExitBalance = config.cornerExitBalance,
                overallGrip = 3,
                bodyControlBalance = 0,
                lapTimeMillis = config.lapTimeMillis,
                timestampMillis = config.updatedAtMillis,
                note = null
            )
        ),
        createdAtMillis = config.createdAtMillis
    )
}

fun TuningDocument.toEntity(): TuningDocumentEntity =
    TuningDocumentEntity(
        id = id,
        carId = carId,
        name = name,
        createdAtMillis = createdAtMillis
    )

fun TuningDocumentEntity.toDomain(): TuningDocument =
    TuningDocument(
        id = id,
        carId = carId,
        name = name,
        createdAtMillis = createdAtMillis
    )
