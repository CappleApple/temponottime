package com.cappleapple.temponottime.casting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalManaPolicyTest {
    @Test
    void simplySwordsManaCostUsesTheGlobalCastingDrawConversion() {
        assertEquals(30.0, ExternalManaPolicy.castingDraw(20.0, 1.5));
        assertEquals(0.0, ExternalManaPolicy.castingDraw(0.0, 1.5));
        assertEquals(0.0, ExternalManaPolicy.castingDraw(20.0, 0.0));
    }

    @Test
    void simplySwordsAffordabilityIncludesOccupiedReserveAndInstantManaCredit() {
        assertFalse(ExternalManaPolicy.canAfford(true, false, 100.0, 90.0, 0.0,
                20.0, false));
        assertTrue(ExternalManaPolicy.canAfford(true, false, 100.0, 90.0, 15.0,
                20.0, false));
    }

    @Test
    void disabledOrBypassedCapacityDoesNotBlockWeaponAbilities() {
        assertTrue(ExternalManaPolicy.canAfford(false, false, 0.0, 1_000.0, 0.0,
                1_000.0, false));
        assertTrue(ExternalManaPolicy.canAfford(true, true, 0.0, 1_000.0, 0.0,
                1_000.0, false));
    }

    @Test
    void weaponCooldownBecomesTempoRechargeDuration() {
        assertEquals(200.0, ExternalManaPolicy.rechargeDuration(200.0, false,
                10.0, 0.8, 0.5, 8.0));
        assertEquals(200.0, ExternalManaPolicy.rechargeDuration(200.0, true,
                10.0, 0.8, 0.5, 8.0));
        assertTrue(ExternalManaPolicy.rechargeDuration(20.0, true,
                10.0, 0.8, 0.5, 8.0) > 20.0);
    }
}
