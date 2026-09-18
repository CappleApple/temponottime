package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.config.ServerConfig.RecoveryMode;
import com.cappleapple.temponottime.data.CooldownInstance;
import com.cappleapple.temponottime.data.PlayerCooldownData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstantManaRecoveryTest {
    @Test
    void eachSpellGetsItsOwnDoseBasedOnManaCost() {
        var data = new PlayerCooldownData();
        var cheap = data.add("irons_spellbooks:firebolt", 1, 40, 200, false, false, true);
        var expensive = data.add("irons_spellbooks:fireball", 1, 80, 400, false, false, true);
        assertTrue(InstantManaRecovery.apply(data, 20, RecoveryMode.SEQUENTIAL));
        assertEquals(100, cheap.remainingTicks());
        assertEquals(300, expensive.remainingTicks());
        assertEquals(0, data.castingReserveCredit());
    }

    @Test
    void sequentialRecoveryCarriesUnusedManaIntoNextCharge() {
        var data = new PlayerCooldownData();
        var first = data.add("irons_spellbooks:fireball", 1, 40, 200, false, false, true);
        var next = data.add("irons_spellbooks:fireball", 2, 80, 400, false, false, true);
        first.advance(100);
        InstantManaRecovery.apply(data, 40, RecoveryMode.SEQUENTIAL);
        assertEquals(1, data.allInstances().size());
        assertEquals(300, next.remainingTicks());
        InstantManaRecovery.apply(data, 1000, RecoveryMode.SEQUENTIAL);
        assertTrue(data.allInstances().isEmpty());
        assertFalse(InstantManaRecovery.apply(data, 1000, RecoveryMode.SEQUENTIAL));
        assertEquals(0, data.castingReserveCredit(), "Excess potion recovery is not banked for future casts");
    }

    @Test
    void parallelRecoveryAdvancesEverySpentCharge() {
        var data = new PlayerCooldownData();
        var first = data.add("irons_spellbooks:fireball", 1, 40, 200, false, false, true);
        var next = data.add("irons_spellbooks:fireball", 1, 40, 200, false, false, true);
        InstantManaRecovery.apply(data, 20, RecoveryMode.PARALLEL);
        assertEquals(100, first.remainingTicks());
        assertEquals(100, next.remainingTicks());
    }

    @Test
    void activeRecastsPendingCastsAndNativeWeaponTimersAreNotShortened() {
        var data = new PlayerCooldownData();
        var waiting = data.add("irons_spellbooks:fireball", 1, 40, 200, true, false, true);
        var weapon = data.add("temponottime:simply_swords/test", 1, 40, 200, false, false, true);
        assertFalse(InstantManaRecovery.apply(data, 1000, RecoveryMode.SEQUENTIAL));
        assertEquals(200, waiting.remainingTicks());
        assertEquals(200, weapon.remainingTicks());
    }

    @Test
    void rawManaCostSurvivesReserveCreditAndPersistence() {
        var data = new PlayerCooldownData();
        var instance = data.add("irons_spellbooks:fireball", 1, 0, 200, false, true, true);
        instance.setRecoveryManaCost(40);
        var loaded = new PlayerCooldownData();
        loaded.load(data.save());
        InstantManaRecovery.apply(loaded, 20, RecoveryMode.SEQUENTIAL);
        assertEquals(100, loaded.allInstances().iterator().next().remainingTicks());
        var legacy = instance.save();
        legacy.remove("recovery_mana_cost");
        assertEquals(1, CooldownInstance.load(legacy).recoveryManaCost());
        legacy.putDouble("cost", 75);
        assertEquals(75, CooldownInstance.load(legacy).recoveryManaCost());
    }

    @Test
    void invalidDosesDoNotChangeProgressAndHugeFiniteDosesComplete() {
        var data = new PlayerCooldownData();
        var instance = data.add("irons_spellbooks:fireball", 1, 40, 200, false, false, true);
        for (double dose : new double[]{0, -10, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertFalse(InstantManaRecovery.apply(data, dose, RecoveryMode.SEQUENTIAL));
            assertEquals(200, instance.remainingTicks());
        }
        assertTrue(InstantManaRecovery.apply(data, Double.MAX_VALUE, RecoveryMode.SEQUENTIAL));
        assertTrue(data.allInstances().isEmpty());
    }
}
