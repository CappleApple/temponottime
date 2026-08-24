package com.cappleapple.temponottime.casting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManaCompatibilityValuesTest {
    @Test
    void reportsFreeReserveAsCurrentMana() {
        var full = ManaCompatibilityValues.snapshot(300.0, 0.0, 0.0);
        var occupied = ManaCompatibilityValues.snapshot(300.0, 120.0, 0.0);

        assertEquals(300.0, full.maximum());
        assertEquals(300.0, full.current());
        assertEquals(300, full.currentAsInt());
        assertEquals(180.0, occupied.current());
    }

    @Test
    void reportsTemporaryCastingReserveOvercharge() {
        var snapshot = ManaCompatibilityValues.snapshot(100.0, 80.0, 110.0);

        assertEquals(100.0, snapshot.maximum());
        assertEquals(130.0, snapshot.current());
        assertEquals(130.0F, snapshot.currentAsFloat());
    }

    @Test
    void compatibilityValuesRemainFiniteAndNonNegative() {
        var invalid = ManaCompatibilityValues.snapshot(Double.NaN, Double.NaN, Double.POSITIVE_INFINITY);
        var enormous = ManaCompatibilityValues.snapshot(Double.MAX_VALUE, 0.0, Double.MAX_VALUE);

        assertEquals(0.0, invalid.maximum());
        assertEquals(0.0, invalid.current());
        assertTrue(Double.isFinite(enormous.current()));
        assertEquals(Integer.MAX_VALUE, enormous.currentAsInt());
        assertEquals(Float.MAX_VALUE, enormous.currentAsFloat());
    }
}
