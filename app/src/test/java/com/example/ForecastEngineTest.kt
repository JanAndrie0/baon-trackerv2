package com.example

import com.example.domain.ForecastEngine
import com.example.domain.ForecastStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastEngineTest {

    @Test
    fun calculate_midCycleOnTrack_returnsExpectedSafeToSpend() {
        val result = ForecastEngine.calculate(
            baonAmountCentavos = 1_000_00L, // ₱1,000
            spentBeforeTodayCentavos = 200_00L, // ₱200
            spentTodayCentavos = 50_00L, // ₱50
            todayEpochDay = 100L,
            nextBaonEpochDay = 104L // 4 days left
        )
        // Remaining before today = 800_00; dailyTarget = 800_00 / 4 = 200_00
        // Safe left today = 200_00 - 50_00 = 150_00
        assertEquals(750_00L, result.remainingCentavos)
        assertEquals(4, result.daysLeft)
        assertEquals(200_00L, result.dailyTargetCentavos)
        assertEquals(150_00L, result.safeToSpendTodayCentavos)
        assertEquals(ForecastStatus.ON_TRACK, result.status)
    }

    @Test
    fun calculate_within120Percent_returnsTightStatus() {
        val result = ForecastEngine.calculate(
            baonAmountCentavos = 1_000_00L,
            spentBeforeTodayCentavos = 0L,
            spentTodayCentavos = 230_00L, // daily target is 200, 120% is 240
            todayEpochDay = 10L,
            nextBaonEpochDay = 15L // 5 days left -> ₱200/day
        )
        assertEquals(0L, result.safeToSpendTodayCentavos)
        assertEquals(ForecastStatus.TIGHT, result.status)
    }

    @Test
    fun calculate_exceeding120PercentOrNegativeRemaining_returnsOverStatus() {
        val overDaily = ForecastEngine.calculate(
            baonAmountCentavos = 1_000_00L,
            spentBeforeTodayCentavos = 0L,
            spentTodayCentavos = 300_00L,
            todayEpochDay = 10L,
            nextBaonEpochDay = 15L
        )
        assertEquals(ForecastStatus.OVER, overDaily.status)

        val overCycle = ForecastEngine.calculate(
            baonAmountCentavos = 500_00L,
            spentBeforeTodayCentavos = 450_00L,
            spentTodayCentavos = 100_00L,
            todayEpochDay = 10L,
            nextBaonEpochDay = 10L // daysLeft clamps to 1
        )
        assertEquals(1, overCycle.daysLeft)
        assertEquals(-50_00L, overCycle.remainingCentavos)
        assertEquals(0L, overCycle.safeToSpendTodayCentavos)
        assertEquals(ForecastStatus.OVER, overCycle.status)
    }

    @Test
    fun parseAndFormatPeso_preservesIntegerCentavos() {
        assertEquals(8500L, ForecastEngine.parsePesoInputToCentavos("85"))
        assertEquals(1350L, ForecastEngine.parsePesoInputToCentavos("13.5"))
        assertEquals(123456L, ForecastEngine.parsePesoInputToCentavos("1,234.56"))
        assertNull(ForecastEngine.parsePesoInputToCentavos("12.345"))
        assertNull(ForecastEngine.parsePesoInputToCentavos(""))

        assertEquals("₱85", ForecastEngine.formatPeso(8500L))
        assertEquals("₱13.50", ForecastEngine.formatPeso(1350L))
        assertEquals("-₱50", ForecastEngine.formatPeso(-5000L))
    }
}
