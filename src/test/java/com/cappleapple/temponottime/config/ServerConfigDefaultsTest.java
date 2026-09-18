package com.cappleapple.temponottime.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConfigDefaultsTest {
    @Test
    void newBalancingControlsDefaultToOptInAndIndependentLoad() {
        assertEquals(false, ServerConfig.SHARED_COOLDOWN_LOAD.getDefault());
        assertEquals(false, ServerConfig.CAST_TIME_NORMALIZATION_ENABLED.getDefault());
        assertEquals(false, ServerConfig.AFFECT_CUSTOM_CAST_TIMES.getDefault());
        assertEquals(false, ServerConfig.AFFECT_NO_CAST_TIME_SPELLS.getDefault());
        assertEquals(0.0, ServerConfig.CAST_TIME_FLAT_MODIFIER.getDefault());
        assertEquals(0.0, ServerConfig.RECHARGE_FLAT_MODIFIER.getDefault());
    }

    @Test
    void chargeCastDelayDefaultsMatchTheRequestedFormula() {
        assertEquals(true, ServerConfig.CHARGE_CAST_DELAY_ENABLED.getDefault());
        assertEquals(0.1, ServerConfig.MINIMUM_CHARGE_CAST_DELAY.getDefault());
        assertEquals(10.0, ServerConfig.MAXIMUM_CHARGE_CAST_DELAY.getDefault());
        assertEquals(10.0, ServerConfig.CHARGE_CAST_DELAY_PERCENTAGE.getDefault());
        assertEquals(0.5, ServerConfig.CHARGE_CAST_DELAY_FLAT_BASE.getDefault());
    }

    @Test
    void proratedManaRegenDefaultsOn() {
        assertEquals(true, ServerConfig.PRORATED_MANA_REGEN.getDefault());
    }

    @Test
    void existingWorldsKeepReserveModeByDefault() {
        assertEquals(ServerConfig.CastingMode.CASTING_RESERVE, ServerConfig.CASTING_MODE.getDefault());
    }

    @Test
    void chargesRecoverSequentiallyByDefault() {
        assertEquals(ServerConfig.RecoveryMode.SEQUENTIAL, ServerConfig.RECOVERY_MODE.getDefault());
    }
}
