package com.cappleapple.temponottime.config;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.casting.CooldownManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/** Config watcher threads only enqueue work; player state and packets are handled on the server tick. */
public final class ServerConfigReloadHandler {
    private static final AtomicBoolean PENDING = new AtomicBoolean();
    private static boolean previouslyEnabled;

    private ServerConfigReloadHandler() { }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ServerConfig.SPEC) return;
        var loaded = event.getConfig().getLoadedConfig();
        // Remote clients receive non-file server configs. They use our authoritative gameplay snapshot.
        if (loaded == null) return;
        try {
            event.getConfig().getFullPath();
            PENDING.set(true);
        } catch (IllegalStateException remoteConfig) {
            // Synced remote configs have no backing file and cannot change a running server.
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        previouslyEnabled = ServerConfig.enabled();
        PENDING.set(false);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!PENDING.getAndSet(false)) return;
        boolean enabled = ServerConfig.enabled();
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            CooldownManager.INSTANCE.applyServerConfig(player, previouslyEnabled);
        }
        previouslyEnabled = enabled;
        TempoNotTime.LOGGER.info("Applied edited Tempo Not Time server config to {} online player(s)",
                event.getServer().getPlayerList().getPlayerCount());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.set(false);
        previouslyEnabled = false;
    }
}
