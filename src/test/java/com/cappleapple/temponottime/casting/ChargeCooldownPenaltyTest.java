package com.cappleapple.temponottime.casting;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChargeCooldownPenaltyTest {
    @Test
    void percentageUsesWholeCostAsDenominator() {
        assertEquals(1.25, ChargeCooldownPenalty.multiplier(75, 100));
        assertEquals(1.5, ChargeCooldownPenalty.multiplier(50, 100));
        assertEquals(1.75, ChargeCooldownPenalty.multiplier(25, 100));
        assertEquals(2, ChargeCooldownPenalty.multiplier(0, 100));
        assertEquals(1, ChargeCooldownPenalty.multiplier(100, 100));
        assertEquals(1, ChargeCooldownPenalty.multiplier(200, 100));
    }

    @Test
    void zeroAndInvalidCostsNeverProduceInfiniteCooldowns() {
        for (double cost : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(1, ChargeCooldownPenalty.multiplier(0, cost));
        }
        assertEquals(2, ChargeCooldownPenalty.multiplier(Double.NaN, 100));
        assertEquals(2, ChargeCooldownPenalty.multiplier(-50, 100));
    }
}
