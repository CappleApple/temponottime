package com.cappleapple.temponottime.gametest;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.network.SyncCooldownStatePayload;
import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.effect.InstantManaEffect;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(TempoNotTime.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CooldownModeGameTests {
    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty")
    public static void independentSpellsAndInstantMana(GameTestHelper helper) {
        var mode = ServerConfig.CASTING_MODE.get();
        boolean enabled = ServerConfig.ENABLED.get();
        boolean manaDisabled = ServerConfig.DISABLE_MANA_CONSUMPTION.get();
        boolean load = ServerConfig.LOAD_ENABLED.get();
        boolean capacity = ServerConfig.CAPACITY_ENABLED.get();
        boolean maxMana = ServerConfig.CONVERT_MAX_MANA.get();
        boolean regen = ServerConfig.CONVERT_MANA_REGEN.get();
        boolean prorated = ServerConfig.PRORATED_MANA_REGEN.get();
        var recovery = ServerConfig.RECOVERY_MODE.get();
        try {
            ServerConfig.ENABLED.set(true);
            ServerConfig.CASTING_MODE.set(ServerConfig.CastingMode.SPELL_COOLDOWNS);
            ServerConfig.DISABLE_MANA_CONSUMPTION.set(false);
            ServerConfig.LOAD_ENABLED.set(true);
            ServerConfig.CAPACITY_ENABLED.set(true);
            ServerConfig.CONVERT_MAX_MANA.set(true);
            ServerConfig.CONVERT_MANA_REGEN.set(true);
            ServerConfig.RECOVERY_MODE.set(ServerConfig.RecoveryMode.SEQUENTIAL);
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tempo-test"), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    cookie.gameProfile(), cookie.clientInformation());
            player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                    new Connection(PacketFlow.SERVERBOUND), player, cookie) {
                @Override public void send(Packet<?> packet) { }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { }
            };
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(100);
            player.getAttribute(AttributeRegistry.MANA_REGEN).setBaseValue(2);
            var magic = MagicData.getPlayerMagicData(player);
            magic.setServerPlayer(player);
            magic.setMana(0);
            var manager = CooldownManager.INSTANCE;
            var firebolt = SpellRegistry.getSpell("irons_spellbooks:firebolt");
            var fireball = SpellRegistry.getSpell("irons_spellbooks:fireball");
            helper.assertTrue(firebolt != SpellRegistry.none() && fireball != SpellRegistry.none(), "Spell fixture missing");
            var data = manager.data(player);
            var debt = data.add(firebolt.getSpellId(), 1, 10000, 200, false, true, true);
            debt.setRecoveryManaCost(60);
            helper.assertTrue(ServerConfig.manaDisabled(), "Mode must override mana spending toggle");
            helper.assertTrue(manager.usedCastingReserve(player) == 0, "Old reserve debt must not gate this mode");
            helper.assertTrue(manager.loadMultiplier(player) == 1, "Spells must recharge independently");
            helper.assertTrue(manager.recoveryMultiplier(player) == 2, "Mana Regen must speed recharge");
            helper.assertTrue(manager.canBeginCast(player, fireball, 1, CastSource.SPELLBOOK).allowed(), "Other spell blocked by shared reserve");
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(0);
            helper.assertTrue(fireball.canBeCastedBy(1, CastSource.SPELLBOOK, magic, player).isSuccess(), "Native mana check blocked zero-mana cast");
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(100);
            int initialCharges = manager.maxCharges(player, firebolt, 1);
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(800);
            helper.assertTrue(manager.maxCharges(player, firebolt, 1) > initialCharges, "Max Mana must increase charges");
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(100);
            helper.assertTrue(AttributeRegistry.MAX_MANA.value().getDescriptionId().equals("attribute.temponottime.charge_capacity"), "Cooldown mode did not localize Charge Capacity");
            helper.assertTrue(!AttributeRegistry.MANA_REGEN.value().getDescriptionId().contains("temponottime"), "Mana Regen was renamed");
            // Iron's level-I dose at 100 Max Mana is 30, even though compatibility mana reports full.
            var effect = new InstantManaEffect(MobEffectCategory.BENEFICIAL, 0);
            effect.applyInstantenousEffect(null, null, player, 0, 1);
            helper.assertTrue(Math.abs(debt.remainingTicks() - 100) < 0.001, "Instant Mana did not recover half a charge");
            effect.applyEffectTick(player, 0);
            helper.assertTrue(data.allInstances().isEmpty(), "Effect-tick application did not finish the charge");
            helper.assertTrue(data.castingReserveCredit() == 0, "Cooldown mode must not bank mana credit");
            var snapshot = SyncCooldownStatePayload.from(player);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                SyncCooldownStatePayload.STREAM_CODEC.encode(buffer, snapshot);
                var decoded = SyncCooldownStatePayload.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.equals(snapshot) && decoded.spellCooldownsOnly()
                        && decoded.manaDisabled() && !decoded.castingReserveEnabled(), "Mode snapshot round trip failed");
            } finally {
                buffer.release();
            }
            // Exercise the actual cast event, native spend redirect, and final cooldown activation.
            magic.setMana(17);
            firebolt.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(data.forSpell(firebolt.getSpellId()).size() == 1, "Cast did not commit exactly one charge");
            var cast = data.forSpell(firebolt.getSpellId()).getFirst();
            helper.assertTrue(!cast.waitingForIronCooldown(), "Native cooldown event did not activate recharge");
            helper.assertTrue(cast.recoveryManaCost() == firebolt.getManaCost(1), "Cast lost its raw mana cost");
            var savedMagic = new net.minecraft.nbt.CompoundTag();
            magic.saveNBTData(savedMagic, helper.getLevel().registryAccess());
            helper.assertTrue(savedMagic.getInt(MagicData.MANA) == 17, "Spell spent backing mana");
            double ordinaryDuration = cast.durationTicks();
            int chargeLimit = manager.maxCharges(player, firebolt, 1);
            for (int i = 1; i < chargeLimit; i++) {
                data.add(firebolt.getSpellId(), 1, 20, 200, false, false, true);
            }
            helper.assertTrue(!manager.canBeginCast(player, firebolt, 1, CastSource.SPELLBOOK).allowed(), "Exhausted spell was allowed");
            helper.assertTrue(manager.canBeginCast(player, fireball, 1, CastSource.SPELLBOOK).allowed(), "Exhausted spell blocked another spell");
            data.clear();
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(firebolt.getManaCost(1) * 0.5);
            firebolt.castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
            var penalized = data.forSpell(firebolt.getSpellId()).getFirst();
            helper.assertTrue(Math.abs(penalized.durationTicks() - ordinaryDuration * 1.5) < 0.001,
                    "Final native cooldown must receive the shortfall penalty exactly once");
            helper.assertTrue(penalized.cooldownPenaltyMultiplier() == 1.5, "Penalty was not captured on the cast");
            data.load(data.save());
            helper.assertTrue(data.forSpell(firebolt.getSpellId()).getFirst().cooldownPenaltyMultiplier() == 1.5,
                    "Penalty did not survive saved cooldown state");
            data.clear();
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(100);
            ServerConfig.CONVERT_MANA_REGEN.set(false);
            helper.assertTrue(manager.recoveryMultiplier(player) == 1, "Regen conversion toggle ignored");
            ServerConfig.CONVERT_MAX_MANA.set(false);
            helper.assertTrue(manager.maxCharges(player, firebolt, 1) == 1, "Max Mana conversion toggle ignored");
            ServerConfig.CONVERT_MAX_MANA.set(true);
            ServerConfig.CASTING_MODE.set(ServerConfig.CastingMode.CASTING_RESERVE);
            helper.assertTrue(!AttributeRegistry.MAX_MANA.value().getDescriptionId().contains("temponottime"),
                    "Reserve mode must keep native Max Mana localization");
            ServerConfig.PRORATED_MANA_REGEN.set(true);
            ServerConfig.LOAD_ENABLED.set(false);
            ServerConfig.DISABLE_MANA_CONSUMPTION.set(true);
            data.add(firebolt.getSpellId(), 1, 40, 200, false, true, true);
            data.add(firebolt.getSpellId(), 1, 40, 200, false, true, true);
            for (int tick = 1; tick <= 9; tick++) {
                player.tickCount = tick;
                manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            helper.assertTrue(manager.usedCastingReserve(player) == 80, "Reserve refunded before the ten-tick boundary");
            player.tickCount = 10;
            manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(Math.abs(manager.usedCastingReserve(player) - 78) < 0.001, "Ten-tick reserve refund incorrect");
            for (int tick = 11; tick <= 100; tick++) {
                player.tickCount = tick;
                manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            helper.assertTrue(manager.usedCastingReserve(player) == 60, "Only the recovering sequential charge should release reserve");
            helper.assertTrue(magic.getMana() == 40, "Iron-facing mana did not receive prorated reserve");
            helper.assertTrue(SyncCooldownStatePayload.from(player).usedCastingReserve() == 60, "HUD snapshot did not receive prorated reserve");
            ServerConfig.PRORATED_MANA_REGEN.set(false);
            helper.assertTrue(manager.usedCastingReserve(player) == 80, "Off toggle must restore full reserve occupancy");
            ServerConfig.PRORATED_MANA_REGEN.set(true);
            for (int tick = 101; tick <= 400; tick++) {
                player.tickCount = tick;
                manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            helper.assertTrue(data.allInstances().isEmpty() && manager.freeCastingReserve(player) == 100,
                    "Completing sequential charges must return exactly the original reserve");
            helper.assertTrue(data.castingReserveCredit() == 0, "Prorated recovery must not bank potion credit");
            ServerConfig.DISABLE_MANA_CONSUMPTION.set(false);
            data.add(firebolt.getSpellId(), 1, 10000, 200, false, true, true);
            helper.assertTrue(!manager.canBeginCast(player, fireball, 1, CastSource.SPELLBOOK).allowed(), "Legacy reserve gate was removed");
            effect.applyInstantenousEffect(null, null, player, 0, 1);
            helper.assertTrue(data.castingReserveCredit() == 30, "Legacy instant-mana reserve credit changed");
            data.clear();
            ServerConfig.ENABLED.set(false);
            magic.setMana(0);
            effect.applyInstantenousEffect(null, null, player, 0, 1);
            helper.assertTrue(magic.getMana() == 30 && data.castingReserveCredit() == 0, "Disabled mod must preserve native mana");
            helper.succeed();
        } finally {
            ServerConfig.ENABLED.set(enabled);
            ServerConfig.CASTING_MODE.set(mode);
            ServerConfig.DISABLE_MANA_CONSUMPTION.set(manaDisabled);
            ServerConfig.LOAD_ENABLED.set(load);
            ServerConfig.CAPACITY_ENABLED.set(capacity);
            ServerConfig.CONVERT_MAX_MANA.set(maxMana);
            ServerConfig.CONVERT_MANA_REGEN.set(regen);
            ServerConfig.PRORATED_MANA_REGEN.set(prorated);
            ServerConfig.RECOVERY_MODE.set(recovery);
        }
    }
}
