package com.cappleapple.temponottime.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConfigDefaultsTest {
    @Test
    void chargesRecoverSequentiallyByDefault() {
        assertEquals(ServerConfig.RecoveryMode.SEQUENTIAL, ServerConfig.RECOVERY_MODE.getDefault());
    }
}
