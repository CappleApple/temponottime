package com.cappleapple.temponottime.gametest;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.network.SyncCooldownStatePayload;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@GameTestHolder(TempoNotTime.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LiveConfigGameTests {
    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty", batch = "live_config", timeoutTicks = 2000)
    public static void savedConfigAppliesWithoutReload(GameTestHelper helper) throws Exception {
        new FileEditCheck(helper).start();
    }

    private static final class FileEditCheck {
        final GameTestHelper helper;
        final ServerPlayer player;
        final Path path;
        final java.util.List<ServerPlayer> onlinePlayers;
        final String original;
        final CooldownManager manager = CooldownManager.INSTANCE;
        SyncCooldownStatePayload snapshot;
        boolean wrongThread;
        boolean done;
        int stage;
        int ticks;

        FileEditCheck(GameTestHelper helper) throws Exception {
            this.helper = helper;
            var server = helper.getLevel().getServer();
            // getPlayers exposes an unmodifiable view; this development-only fixture joins its backing list.
            var playersField = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            playersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var players = (java.util.List<ServerPlayer>) playersField.get(server.getPlayerList());
            onlinePlayers = players;
            Path override = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/temponottime-server.toml");
            path = Files.exists(override) ? override : FMLPaths.CONFIGDIR.get().resolve("temponottime-server.toml");
            original = Files.readString(path);
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tempo-reload"), false);
            player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, cookie) {
                @Override public boolean hasChannel(net.minecraft.resources.ResourceLocation id) { return false; }
                @Override public void send(Packet<?> packet) {
                    wrongThread |= !server.isSameThread();
                    if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof SyncCooldownStatePayload state) {
                        snapshot = state;
                    }
                }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { send(packet); }
            };
            MagicData.getPlayerMagicData(player).setServerPlayer(player);
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(100);
            player.getAttribute(AttributeRegistry.MANA_REGEN).setBaseValue(1);
        }

        void start() throws Exception {
            onlinePlayers.add(player);
            try {
                write(0);
                helper.onEachTick(this::tick);
            } catch (Exception failure) {
                cleanup();
                throw failure;
            }
        }

        void write(int nextStage) throws Exception {
            stage = nextStage;
            snapshot = null;
            var config = new TomlParser().parse(new java.io.StringReader(original));
            config.set("general.enabled", nextStage != 4);
            config.set("general.casting_mode", nextStage >= 2 ? "CASTING_RESERVE" : "SPELL_COOLDOWNS");
            config.set("general.disable_mana_consumption", nextStage < 2);
            config.set("cooldown_load.enabled", true);
            config.set("cooldown_load.shared_cooldown_load", nextStage == 1);
            config.set("cooldown_load.free_cooldowns", 1);
            config.set("cooldown_load.penalty_per_additional_cooldown", 1.0);
            config.set("charges.enabled", nextStage != 3);
            config.set("casting_reserve.prorated_mana_regen", true);
            config.set("delay_between_charge_casts.enabled", nextStage < 2);
            config.set("cast_time_normalization.flat_modifer", .75 + nextStage);
            config.set("recharge_normalization.flat_modifer", 1.25 + nextStage);
            new TomlWriter().write(config, path, WritingMode.REPLACE_ATOMIC);
        }

        void tick() {
            if (done) return;
            try {
                // GameTestServer runs unpaced; give the debounced OS file watcher real time to run.
                Thread.sleep(10);
                if (++ticks > 1800) throw new AssertionError("Config watcher did not apply stage " + stage);
                if (snapshot == null || snapshot.rechargeFlatModifier() != 1.25 + stage) return;
                helper.assertTrue(!wrongThread, "Config reload sent gameplay packets off the server thread");
                var data = manager.data(player);
                var bolt = SpellRegistry.getSpell("irons_spellbooks:firebolt");
                var ball = SpellRegistry.getSpell("irons_spellbooks:fireball");
                switch (stage) {
                    case 0 -> {
                        helper.assertTrue(snapshot.enabled() && snapshot.spellCooldownsOnly() && snapshot.manaDisabled()
                                && snapshot.castTimeNormalization().flatModifier() == .75,
                                "Idle player did not receive edited mode and timing settings");
                        var debt = data.add(bolt.getSpellId(), 1, 40, 200, false, true, true);
                        debt.advance(100);
                        data.add(ball.getSpellId(), 1, 20, 200, false, true, true);
                        data.startChargeCastDelay(bolt.getSpellId(), 50);
                        helper.assertTrue(manager.loadMultiplier(player, bolt.getSpellId()) == 1, "Independent load fixture invalid");
                        write(1);
                    }
                    case 1 -> {
                        helper.assertTrue(manager.loadMultiplier(player, bolt.getSpellId()) == .5,
                                "Live shared-load edit did not affect existing debt");
                        helper.assertTrue(data.forSpell(bolt.getSpellId()).getFirst().progressTicks() == 100,
                                "Config edit reset recorded cooldown progress");
                        write(2);
                    }
                    case 2 -> {
                        helper.assertTrue(!snapshot.spellCooldownsOnly() && !snapshot.manaDisabled()
                                && snapshot.castingReserveEnabled() && snapshot.usedCastingReserve() == 40,
                                "Live mode/mana edit or immediate prorated reserve refresh failed");
                        helper.assertTrue(data.chargeCastDelays().isEmpty(), "Disabling delays did not clear the active timer");
                        data.add(bolt.getSpellId(), 1, 10, 300, false, true, true);
                        write(3);
                    }
                    case 3 -> {
                        helper.assertTrue(!snapshot.chargesEnabled() && data.forSpell(bolt.getSpellId()).size() == 1,
                                "Live charge-disable edit did not reconcile existing charges");
                        MagicData.getPlayerMagicData(player).getPlayerCooldowns().addCooldown(bolt, 100, 60);
                        write(4);
                    }
                    case 4 -> {
                        helper.assertTrue(!snapshot.enabled() && data.allInstances().isEmpty() && data.pendingCast() == null,
                                "Disabling Tempo left active custom state or stale client flags");
                        helper.assertTrue(MagicData.getPlayerMagicData(player).getPlayerCooldowns().isOnCooldown(bolt),
                                "Disabling Tempo deleted native cooldowns");
                        write(5);
                    }
                    case 5 -> {
                        helper.assertTrue(snapshot.enabled() && data.forSpell(bolt.getSpellId()).size() == 1,
                                "Re-enabling Tempo lost an existing native cooldown");
                        helper.assertTrue(data.forSpell(bolt.getSpellId()).getFirst().remainingTicks() == 60
                                && manager.usedCastingReserve(player) == 0,
                                "Imported cooldown lost progress or charged reserve twice");
                        cleanup();
                        helper.succeed();
                    }
                    default -> throw new AssertionError("Unexpected stage");
                }
            } catch (Throwable failure) {
                try { cleanup(); } catch (Exception cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                helper.fail("Live config stage " + stage + ": " + failure);
            }
        }

        void cleanup() throws Exception {
            done = true;
            onlinePlayers.remove(player);
            manager.data(player).clear();
            Files.writeString(path, original);
        }
    }
}
