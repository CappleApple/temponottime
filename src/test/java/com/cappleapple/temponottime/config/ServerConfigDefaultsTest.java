package com.cappleapple.temponottime.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConfigDefaultsTest {
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
