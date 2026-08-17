package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.data.CooldownInstance;
import com.cappleapple.temponottime.data.PlayerCooldownData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombinedMechanicsTest {
    @Test
    void multiBlastRecastsConsumeOnlyOneTempoUse() {
        boolean[] activeRecastAtCastEvent = {false, true, true, true, true, true, true};
        int tempoUses = 0;
        for (boolean activeRecast : activeRecastAtCastEvent) {
            if (RecastReservationPolicy.consumesTempoUse(activeRecast)) tempoUses++;
        }

        assertEquals(1, tempoUses, "Only the initial activation may consume a charge or reserve Casting Draw");
    }

    @Test
    void fiveChargesReserveCapacityIndependently() {
        PlayerCooldownData data = new PlayerCooldownData();
        int maximumCharges = ChargeCalculator.maxCharges(300, 60, 1, 10, 1);
        assertEquals(5, maximumCharges);

        for (int i = 0; i < 3; i++) {
            data.add("irons_spellbooks:fireball", 1, 60, 200, false, true, true);
        }
        assertEquals(2, maximumCharges - data.forSpell("irons_spellbooks:fireball").size());
        assertEquals(180, used(data));

        for (int i = 0; i < 2; i++) {
            data.add("irons_spellbooks:fireball", 1, 60, 200, false, true, true);
        }
        assertEquals(0, maximumCharges - data.forSpell("irons_spellbooks:fireball").size());
        assertEquals(300, used(data));
        assertFalse(CapacityCalculator.canReserve(300, used(data), 60, false));

        data.forSpell("irons_spellbooks:fireball").removeFirst();
        assertEquals(1, maximumCharges - data.forSpell("irons_spellbooks:fireball").size());
        assertEquals(240, used(data));
    }

    @Test
    void completingOneChargeUpdatesAllThreeMechanics() {
        PlayerCooldownData data = new PlayerCooldownData();
        for (int i = 0; i < 3; i++) {
            data.add("irons_spellbooks:fireball", 1, 60, 200, false, true, true);
        }
        data.add("irons_spellbooks:teleport", 1, 80, 240, false, true, true);

        assertEquals(4, data.allInstances().size());
        assertEquals(260, used(data));
        assertEquals(1.0 / 1.6, CooldownLoadCalculator.multiplier(4, 1, .20, .25), 1.0e-9);

        data.forSpell("irons_spellbooks:fireball").removeFirst();
        assertEquals(3, data.allInstances().size());
        assertEquals(200, used(data));
        assertEquals(1.0 / 1.4, CooldownLoadCalculator.multiplier(3, 1, .20, .25), 1.0e-9);
        assertTrue(CapacityCalculator.canReserve(300, used(data), 60, false));
    }

    @Test
    void serializedDebtRoundTripsWithoutResetting() {
        PlayerCooldownData original = new PlayerCooldownData();
        CooldownInstance instance = original.add("example:addon_spell", 3, 75, 300, false, true, true);
        instance.advance(42.5);

        PlayerCooldownData loaded = new PlayerCooldownData();
        loaded.load(original.save());

        assertEquals(1, loaded.allInstances().size());
        CooldownInstance restored = loaded.allInstances().iterator().next();
        assertEquals("example:addon_spell", restored.spellId());
        assertEquals(3, restored.spellLevel());
        assertEquals(75, restored.castingDraw());
        assertEquals(42.5, restored.progressTicks());
    }

    @Test
    void instantManaCreditRefillsAndOnlySingleDoseCanOvercharge() {
        PlayerCooldownData data = new PlayerCooldownData();
        data.addCastingReserveCredit(30.0, 80.0);
        assertEquals(30.0, data.castingReserveCredit());
        assertEquals(50.0, CapacityCalculator.free(100.0 + data.castingReserveCredit(), 80.0));

        for (int i = 0; i < 4; i++) data.addCastingReserveCredit(30.0, 80.0);
        assertEquals(110.0, data.castingReserveCredit());
        assertEquals(130.0, CapacityCalculator.free(100.0 + data.castingReserveCredit(), 80.0));

        data.addCastingReserveCredit(10.0, 80.0);
        assertEquals(90.0, data.castingReserveCredit());
        assertEquals(110.0, CapacityCalculator.free(100.0 + data.castingReserveCredit(), 80.0));
        assertEquals(0.0, data.consumeCastingReserveCredit(50.0));
        assertEquals(40.0, data.castingReserveCredit());
        assertEquals(20.0, data.consumeCastingReserveCredit(60.0));
        assertEquals(0.0, data.castingReserveCredit());
    }

    @Test
    void instantManaCreditPersistsWithRechargeDebt() {
        PlayerCooldownData original = new PlayerCooldownData();
        original.addCastingReserveCredit(45.0, 75.0);

        PlayerCooldownData loaded = new PlayerCooldownData();
        loaded.load(original.save());

        assertEquals(45.0, loaded.castingReserveCredit());
        assertEquals(45.0, loaded.castingReserveOverchargeLimit());
    }

    private static double used(PlayerCooldownData data) {
        List<Double> costs = data.allInstances().stream().filter(CooldownInstance::occupiesCastingReserve)
                .map(CooldownInstance::castingDraw).toList();
        return CapacityCalculator.used(costs);
    }
}
