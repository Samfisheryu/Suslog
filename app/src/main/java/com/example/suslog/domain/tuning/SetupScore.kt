package com.example.suslog.domain.tuning

import com.example.suslog.domain.setup.SetupConfigState
import kotlin.math.abs

data class SubjectiveScoreBreakdown(
    val entry: Double,
    val mid: Double,
    val exit: Double,
    val grip: Double,
    val bodyControl: Double,
    val total: Double,
)

object SetupScoreV1 {
    fun compute(state: SetupConfigState): SubjectiveScoreBreakdown {
        val entry = balanceScore(state.cornerEntryBalance)
        val mid = balanceScore(state.cornerMidBalance)
        val exit = balanceScore(state.cornerExitBalance)
        val grip = gripScore(state.overallGrip)
        val bodyControl = balanceScore(state.bodyControlBalance)
        val balance = 0.40 * entry + 0.20 * mid + 0.40 * exit
        val total = 100.0 * (0.45 * grip + 0.40 * balance + 0.15 * bodyControl)

        return SubjectiveScoreBreakdown(
            entry = entry,
            mid = mid,
            exit = exit,
            grip = grip,
            bodyControl = bodyControl,
            total = total.coerceIn(0.0, 100.0)
        )
    }

    private fun balanceScore(value: Int): Double =
        (1.0 - abs(value.coerceIn(-2, 2)) / 2.0).coerceIn(0.0, 1.0)

    private fun gripScore(value: Int): Double =
        ((value.coerceIn(1, 5) - 1).toDouble() / 4.0).coerceIn(0.0, 1.0)
}
