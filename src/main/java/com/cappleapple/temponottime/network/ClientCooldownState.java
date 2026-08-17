package com.cappleapple.temponottime.network;

import java.util.Map;

public final class ClientCooldownState {
    private static volatile SyncCooldownStatePayload snapshot = SyncCooldownStatePayload.empty();

    private ClientCooldownState() {
    }

    public static void accept(SyncCooldownStatePayload payload) {
        snapshot = payload;
    }

    public static SyncCooldownStatePayload snapshot() {
        return snapshot;
    }

    public static boolean enabled() {
        return snapshot.enabled();
    }

    public static boolean manaDisabled() {
        return snapshot.enabled() && snapshot.manaDisabled();
    }

    public static Map<String, SyncCooldownStatePayload.SpellState> spells() {
        return snapshot.spells();
    }
}
