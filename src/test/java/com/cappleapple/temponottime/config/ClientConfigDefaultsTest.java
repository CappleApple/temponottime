package com.cappleapple.temponottime.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientConfigDefaultsTest {
    @Test
    void quickCastHudFilterIsDisabledByDefault() {
        assertFalse(ClientConfig.ONLY_SHOW_BOUND_QUICK_CAST_SLOTS.getDefault());
    }
}
