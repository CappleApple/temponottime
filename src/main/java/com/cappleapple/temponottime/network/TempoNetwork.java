package com.cappleapple.temponottime.network;

import com.cappleapple.temponottime.TempoNotTime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TempoNetwork {
    private TempoNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TempoNotTime.MOD_ID).versioned("1.1.0");
        registrar.playToClient(SyncCooldownStatePayload.TYPE, SyncCooldownStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientCooldownState.accept(payload)));
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SyncCooldownStatePayload.from(player));
    }
}
