package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.data.CooldownInstance;
import com.cappleapple.temponottime.data.PlayerCooldownData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProratedReserveTest {
    @Test
    void onlyNewProgressIsReleasedAndOnlyOnSample() {
        var data = new PlayerCooldownData();
        var cast = data.add("irons_spellbooks:fireball", 1, 40, 200, false, true, true);
        cast.advance(100);
        assertEquals(40, cast.occupiedReserve(true));
        assertTrue(cast.updateProratedReserve());
        assertEquals(20, cast.occupiedReserve(true));
        assertFalse(cast.updateProratedReserve());
        assertEquals(20, cast.occupiedReserve(true));
        assertEquals(40, cast.occupiedReserve(false));
        cast.advance(50);
        cast.updateProratedReserve();
        assertEquals(10, cast.occupiedReserve(true));
        cast.advance(50);
        cast.updateProratedReserve();
        assertEquals(0, cast.occupiedReserve(true));
        assertEquals(0, data.castingReserveCredit());
    }

    @Test
    void waitingAndQueuedChargesDoNotRefundBeforeTheirOwnProgress() {
        var data = new PlayerCooldownData();
        var waiting = data.add("irons_spellbooks:fireball", 1, 40, 200, true, true, true);
        var queued = data.add("irons_spellbooks:fireball", 1, 40, 200, false, true, true);
        waiting.advance(100);
        waiting.updateProratedReserve();
        queued.updateProratedReserve();
        assertEquals(40, waiting.occupiedReserve(true));
        assertEquals(40, queued.occupiedReserve(true));
    }

    @Test
    void potionCreditIsNotRefundedAgainAndProgressSurvivesSaveAndClone() {
        var data = new PlayerCooldownData();
        data.addCastingReserveCredit(20, 0);
        var cast = data.add("irons_spellbooks:fireball", 1, data.consumeCastingReserveCredit(40), 200, false, true, true);
        cast.setCooldownPenaltyMultiplier(1.5);
        cast.advance(100);
        cast.updateProratedReserve();
        assertEquals(10, cast.occupiedReserve(true));
        var loaded = new PlayerCooldownData();
        loaded.load(data.save());
        var cloned = new PlayerCooldownData();
        cloned.copyFrom(loaded);
        var copy = cloned.forSpell("irons_spellbooks:fireball").getFirst();
        assertEquals(10, copy.occupiedReserve(true));
        assertEquals(1.5, copy.cooldownPenaltyMultiplier());
        assertEquals(0, cloned.castingReserveCredit());
        copy.activate(400);
        assertEquals(20, copy.occupiedReserve(true));
    }

    @Test
    void oldDebtLoadsWithNoPenaltyAndIsProratedOnNextSample() {
        var original = new CooldownInstance(1, "irons_spellbooks:fireball", 1, 40, 200, 100, false, true, true);
        var tag = original.save();
        tag.remove("cooldown_penalty_multiplier");
        tag.remove("reserve_recovered_fraction");
        var loaded = CooldownInstance.load(tag);
        assertEquals(1, loaded.cooldownPenaltyMultiplier());
        assertEquals(40, loaded.occupiedReserve(true));
        loaded.updateProratedReserve();
        assertEquals(20, loaded.occupiedReserve(true));
    }
}
