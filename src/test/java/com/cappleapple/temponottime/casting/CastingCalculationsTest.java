package com.cappleapple.temponottime.casting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CastingCalculationsTest {
    @Test
    void calculatesChargesAndClampsEdgeCases() {
        assertEquals(5, ChargeCalculator.maxCharges(300, 60, 1, 10, 1));
        assertEquals(3, ChargeCalculator.maxCharges(300, 100, 1, 10, 1));
        assertEquals(1, ChargeCalculator.maxCharges(300, 500, 1, 10, 1));
        assertEquals(10, ChargeCalculator.maxCharges(300, 0, 1, 10, 1));
        assertEquals(4, ChargeCalculator.maxCharges(10_000, 1, 1, 4, 1));
    }

    @Test
    void calculatesCapacityReservations() {
        double used = CapacityCalculator.used(List.of(60.0, 60.0, 60.0));
        assertEquals(180.0, used);
        assertEquals(120.0, CapacityCalculator.free(300, used));
        assertTrue(CapacityCalculator.canReserve(300, used, 100, false));
        assertFalse(CapacityCalculator.canReserve(300, used, 130, false));

        used = CapacityCalculator.used(List.of(60.0, 60.0));
        assertEquals(120.0, used);
        assertEquals(180.0, CapacityCalculator.free(300, used));
    }

    @Test
    void calculatesLoadFormulaAndFloor() {
        assertEquals(1.0, CooldownLoadCalculator.multiplier(1, 1, 0.20, 0.25), 1.0e-9);
        assertEquals(1.0 / 1.2, CooldownLoadCalculator.multiplier(2, 1, 0.20, 0.25), 1.0e-9);
        assertEquals(1.0 / 1.4, CooldownLoadCalculator.multiplier(3, 1, 0.20, 0.25), 1.0e-9);
        assertEquals(0.25, CooldownLoadCalculator.multiplier(100, 1, 0.20, 0.25), 1.0e-9);
    }

    @Test
    void combinesManaRegenAndLoadRecovery() {
        assertEquals(1.0, RecoveryCalculator.multiplier(1.0, 1.0, 4.0, 1.0), 1.0e-9);
        assertEquals(1.25, RecoveryCalculator.multiplier(1.25, 1.0, 4.0, 1.0), 1.0e-9);
        assertEquals(0.625, RecoveryCalculator.multiplier(1.25, 1.0, 4.0, 0.5), 1.0e-9);
        assertEquals(4.0, RecoveryCalculator.multiplier(100.0, 1.0, 4.0, 1.0), 1.0e-9);
    }

    @Test
    void smoothlyNormalizesRechargeExtremesAsymmetrically() {
        double one = normalized(1.0);
        double two = normalized(2.0);
        double five = normalized(5.0);
        double eight = normalized(8.0);
        double ten = normalized(10.0);
        double twenty = normalized(20.0);
        double forty = normalized(40.0);
        double sixty = normalized(60.0);
        double oneTwenty = normalized(120.0);

        assertTrue(one > 1.0 && one < two);
        assertTrue(two < five && five < eight && eight < ten);
        assertEquals(10.0, ten, 1.0e-9);
        assertTrue(ten < twenty && twenty < forty && forty < sixty && sixty < oneTwenty);
        assertTrue(twenty < 20.0 && forty < 40.0 && sixty < 60.0 && oneTwenty < 120.0);
        assertTrue(oneTwenty < 27.0);
    }

    @Test
    void rechargeNormalizationIsContinuousMonotonicAndSafe() {
        double previous = normalized(0.001);
        for (double seconds = 0.01; seconds <= 240.0; seconds += 0.01) {
            double current = normalized(seconds);
            assertTrue(Double.isFinite(current) && current > 0.0);
            assertTrue(current > previous, "Curve was not strictly increasing at " + seconds);
            previous = current;
        }

        assertEquals(3.0, RechargeNormalizer.normalizeSeconds(3.0, false, 10.0, 0.8, 0.5, 8.0));
        assertEquals(3.0, RechargeNormalizer.normalizeSeconds(3.0, true, 10.0, 0.0, 0.5, 8.0));
        assertTrue(Double.isFinite(RechargeNormalizer.normalizeSeconds(Double.NaN, true,
                Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0)));
        assertTrue(normalized(20_000.0) > normalized(10_000.0),
                "Extreme recharge durations must retain their relative ordering after tanh saturation");
    }

    @Test
    void normalizedEffectiveCooldownPreservesIronsReductionRatio() {
        double normalizedBase = RechargeNormalizer.normalizeEffectiveTicks(400.0, 400.0,
                true, 10.0, 0.8, 0.5, 8.0);
        double reduced = RechargeNormalizer.normalizeEffectiveTicks(400.0, 300.0,
                true, 10.0, 0.8, 0.5, 8.0);

        assertEquals(normalizedBase * 0.75, reduced, 1.0e-9);
        assertEquals(300.0, RechargeNormalizer.normalizeEffectiveTicks(400.0, 300.0,
                false, 10.0, 0.8, 0.5, 8.0), 1.0e-9);
    }

    private static double normalized(double seconds) {
        return RechargeNormalizer.normalizeSeconds(seconds, true, 10.0, 0.8, 0.5, 8.0);
    }
}
