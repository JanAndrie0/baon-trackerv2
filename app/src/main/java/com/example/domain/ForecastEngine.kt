package com.example.domain

import kotlin.math.max
import kotlin.math.roundToLong

enum class ForecastStatus {
    ON_TRACK,
    TIGHT,
    OVER
}

data class ForecastResult(
    val baonAmountCentavos: Long,
    val spentThisCycleCentavos: Long,
    val spentTodayCentavos: Long,
    val remainingCentavos: Long,
    val daysLeft: Int,
    val dailyTargetCentavos: Long,
    val safeToSpendTodayCentavos: Long,
    val status: ForecastStatus,
    val progressRemainingRatio: Float
)

object ForecastEngine {

    /**
     * Pure deterministic forecast math tied to the active baon cycle.
     * All money values are integer centavos (100 centavos = 1 PHP).
     */
    fun calculate(
        baonAmountCentavos: Long,
        spentBeforeTodayCentavos: Long,
        spentTodayCentavos: Long,
        todayEpochDay: Long,
        nextBaonEpochDay: Long
    ): ForecastResult {
        val safeBaon = max(0L, baonAmountCentavos)
        val safeBeforeToday = max(0L, spentBeforeTodayCentavos)
        val safeToday = max(0L, spentTodayCentavos)

        val spentThisCycle = safeBeforeToday + safeToday
        val remaining = safeBaon - spentThisCycle
        val daysLeft = max(1, (nextBaonEpochDay - todayEpochDay).toInt())

        val remainingBeforeToday = max(0L, safeBaon - safeBeforeToday)
        val dailyTarget = remainingBeforeToday / daysLeft
        val safeLeftForToday = if (remaining <= 0L) {
            0L
        } else {
            max(0L, dailyTarget - safeToday)
        }

        val tightThreshold = (dailyTarget * 1.20).roundToLong()
        val status = when {
            remaining < 0L -> ForecastStatus.OVER
            dailyTarget == 0L && safeToday > 0L -> ForecastStatus.OVER
            safeToday <= dailyTarget -> ForecastStatus.ON_TRACK
            safeToday <= tightThreshold -> ForecastStatus.TIGHT
            else -> ForecastStatus.OVER
        }

        val progressRatio = if (safeBaon <= 0L) {
            0f
        } else {
            (remaining.toFloat() / safeBaon.toFloat()).coerceIn(0f, 1f)
        }

        return ForecastResult(
            baonAmountCentavos = safeBaon,
            spentThisCycleCentavos = spentThisCycle,
            spentTodayCentavos = safeToday,
            remainingCentavos = remaining,
            daysLeft = daysLeft,
            dailyTargetCentavos = dailyTarget,
            safeToSpendTodayCentavos = safeLeftForToday,
            status = status,
            progressRemainingRatio = progressRatio
        )
    }

    /**
     * Evaluates a single historical or calendar day against a daily budget cap.
     */
    fun statusForDay(spentCentavos: Long, dailyCapCentavos: Long): ForecastStatus {
        val safeSpent = max(0L, spentCentavos)
        val safeCap = max(0L, dailyCapCentavos)
        val tightThreshold = (safeCap * 1.20).roundToLong()
        return when {
            safeSpent == 0L -> ForecastStatus.ON_TRACK
            safeCap == 0L -> ForecastStatus.OVER
            safeSpent <= safeCap -> ForecastStatus.ON_TRACK
            safeSpent <= tightThreshold -> ForecastStatus.TIGHT
            else -> ForecastStatus.OVER
        }
    }

    fun formatPeso(centavos: Long, showExactCentavos: Boolean = false): String {
        val isNegative = centavos < 0L
        val abs = kotlin.math.abs(centavos)
        val pesos = abs / 100L
        val rem = abs % 100L
        val formattedPesos = "%,d".format(pesos)
        val prefix = if (isNegative) "-₱" else "₱"
        return if (showExactCentavos || rem != 0L) {
            "$prefix$formattedPesos.${"%02d".format(rem)}"
        } else {
            "$prefix$formattedPesos"
        }
    }

    fun parsePesoInputToCentavos(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("₱", "")
        if (cleaned.isEmpty() || cleaned == ".") return null
        val parts = cleaned.split(".")
        if (parts.size > 2) return null
        val wholePart = if (parts[0].isEmpty()) 0L else parts[0].toLongOrNull() ?: return null
        val fracPart = if (parts.size == 1) {
            0L
        } else {
            val rawFrac = parts[1]
            if (rawFrac.length > 2) return null
            val padded = rawFrac.padEnd(2, '0')
            padded.toLongOrNull() ?: return null
        }
        return wholePart * 100L + fracPart
    }
}
