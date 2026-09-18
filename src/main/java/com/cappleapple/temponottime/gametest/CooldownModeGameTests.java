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
            verifyBalancing(helper, player);
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
    private static void verifyBalancing(GameTestHelper helper, ServerPlayer player) {
        var settings = java.util.List.<net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?>>of(
                ServerConfig.ENABLED, ServerConfig.CASTING_MODE, ServerConfig.LOAD_ENABLED,
                ServerConfig.SHARED_COOLDOWN_LOAD, ServerConfig.COUNT_PER_CHARGE, ServerConfig.FREE_COOLDOWNS,
                ServerConfig.PENALTY_PER_ADDITIONAL_COOLDOWN, ServerConfig.RECOVERY_MODE,
                ServerConfig.CONVERT_MANA_REGEN, ServerConfig.RECHARGE_NORMALIZATION_ENABLED,
                ServerConfig.RECHARGE_FLAT_MODIFIER, ServerConfig.CAST_TIME_NORMALIZATION_ENABLED,
                ServerConfig.CAST_TIME_FLAT_MODIFIER, ServerConfig.AFFECT_CUSTOM_CAST_TIMES,
                ServerConfig.AFFECT_NO_CAST_TIME_SPELLS, ServerConfig.CHARGE_CAST_DELAY_ENABLED,
                ServerConfig.CHARGE_CAST_DELAY_FLAT_BASE, ServerConfig.CHARGE_CAST_DELAY_PERCENTAGE,
                ServerConfig.MINIMUM_CHARGE_CAST_DELAY, ServerConfig.MAXIMUM_CHARGE_CAST_DELAY);
        var saved = settings.stream().map(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue::get).toList();
        var manager = CooldownManager.INSTANCE;
        var data = manager.data(player);
        var magic = MagicData.getPlayerMagicData(player);
        try {
            ServerConfig.ENABLED.set(true);
            ServerConfig.CHARGE_CAST_DELAY_ENABLED.set(true);
            ServerConfig.CHARGE_CAST_DELAY_FLAT_BASE.set(.5);
            ServerConfig.CHARGE_CAST_DELAY_PERCENTAGE.set(10.0);
            ServerConfig.MINIMUM_CHARGE_CAST_DELAY.set(.1);
            ServerConfig.MAXIMUM_CHARGE_CAST_DELAY.set(10.0);
            ServerConfig.CASTING_MODE.set(ServerConfig.CastingMode.SPELL_COOLDOWNS);
            ServerConfig.LOAD_ENABLED.set(true);
            ServerConfig.SHARED_COOLDOWN_LOAD.set(false);
            ServerConfig.COUNT_PER_CHARGE.set(false);
            ServerConfig.FREE_COOLDOWNS.set(1);
            ServerConfig.PENALTY_PER_ADDITIONAL_COOLDOWN.set(1.0);
            ServerConfig.CONVERT_MANA_REGEN.set(false);
            ServerConfig.RECOVERY_MODE.set(ServerConfig.RecoveryMode.SEQUENTIAL);
            var bolt = SpellRegistry.getSpell("irons_spellbooks:firebolt");
            var ball = SpellRegistry.getSpell("irons_spellbooks:fireball");
            data.clear();
            var a = data.add(bolt.getSpellId(), 1, 0, 100, false, false, true);
            var queued = data.add(bolt.getSpellId(), 1, 0, 100, false, false, true);
            var b = data.add(ball.getSpellId(), 1, 0, 100, false, false, true);
            helper.assertTrue(manager.loadMultiplier(player, bolt.getSpellId()) == .5
                    && manager.loadMultiplier(player, ball.getSpellId()) == 1,
                    "Per-spell load must count own charges even when count_per_charge is false");
            player.tickCount = 1;
            manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(a.remainingTicks() == 99.5 && queued.remainingTicks() == 100 && b.remainingTicks() == 99,
                    "Per-spell recovery speed or sequential queue is wrong");
            ServerConfig.SHARED_COOLDOWN_LOAD.set(true);
            helper.assertTrue(manager.loadMultiplier(player, ball.getSpellId()) == .5,
                    "Shared load did not count distinct spells");
            ServerConfig.COUNT_PER_CHARGE.set(true);
            helper.assertTrue(Math.abs(manager.loadMultiplier(player, ball.getSpellId()) - 1.0 / 3) < .00001,
                    "Shared load did not count all charges");
            data.clear();
            var finishing = data.add(bolt.getSpellId(), 1, 0, 1, false, false, true);
            finishing.advance(.75);
            var survivor = data.add(ball.getSpellId(), 1, 0, 100, false, false, true);
            manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(survivor.remainingTicks() == 99.5,
                    "Finishing another spell changed load during the same tick");
            data.clear();
            ServerConfig.LOAD_ENABLED.set(false);
            ServerConfig.RECHARGE_NORMALIZATION_ENABLED.set(false);
            ServerConfig.RECHARGE_FLAT_MODIFIER.set(2.0);
            player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION).setBaseValue(1.5);
            player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(800);
            int base = bolt.getSpellCooldown();
            double ratio = 2 - io.redspace.ironsspellbooks.api.util.Utils.softCapFormula(1.5);
            int expected = (int) ((base + 40) * ratio);
            int actual = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(bolt, player, CastSource.SPELLBOOK);
            helper.assertTrue(actual == expected, "Flat cooldown adjustment was not applied before native reduction");
            bolt.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(data.forSpell(bolt.getSpellId()).getFirst().durationTicks() == expected,
                    "Flat cooldown adjustment was lost or applied twice on commit");
            data.clear();
            ServerConfig.CAST_TIME_NORMALIZATION_ENABLED.set(false);
            ServerConfig.CAST_TIME_FLAT_MODIFIER.set(1.0);
            ServerConfig.AFFECT_CUSTOM_CAST_TIMES.set(false);
            ServerConfig.AFFECT_NO_CAST_TIME_SPELLS.set(false);
            player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).setBaseValue(1);
            var heal = SpellRegistry.getSpell("irons_spellbooks:greater_heal");
            helper.assertTrue(heal != SpellRegistry.none(), "Cast fixture missing");
            helper.assertTrue(heal.attemptInitiateCast(net.minecraft.world.item.ItemStack.EMPTY, 1, player.level(), player,
                    CastSource.SPELLBOOK, true, "mainhand"), "Timed cast initiation failed");
            helper.assertTrue(magic.getCastDuration() == heal.getCastTime(1) + 20,
                    "Actual player cast did not receive normalized duration");
            manager.commitCast(player, heal, 1, heal.getManaCost(1), CastSource.SPELLBOOK);
            helper.assertTrue(data.chargeCastDelayTicks(heal.getSpellId()) == 24,
                    "Charge delay did not use the actual adjusted seven-second cast time");
            manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(data.chargeCastDelayTicks(heal.getSpellId()) == 24,
                    "Charge delay elapsed before the current cast finished");
            magic.resetCastingState();
            helper.assertTrue(manager.canBeginCast(player, heal, 1, CastSource.SPELLBOOK).failure()
                            == com.cappleapple.temponottime.casting.CastDecision.Failure.CHARGE_CAST_DELAY,
                    "Another stored charge bypassed the inter-cast delay");
            helper.assertTrue(manager.canBeginCast(player, ball, 1, CastSource.SPELLBOOK).allowed(),
                    "Charge delay incorrectly blocked another spell");
            var savedDelay = data.save();
            data.load(savedDelay);
            var cloned = new com.cappleapple.temponottime.data.PlayerCooldownData();
            cloned.copyFrom(data);
            helper.assertTrue(cloned.chargeCastDelayTicks(heal.getSpellId()) == 24,
                    "Charge delay was lost across save/load or death-copy");
            manager.activateNext(player, heal, 100);
            manager.restoreInstantMana(player, 10000);
            helper.assertTrue(data.forSpell(heal.getSpellId()).isEmpty() && data.chargeCastDelayTicks(heal.getSpellId()) == 24,
                    "Instant mana must recover charges without removing the inter-cast delay");
            var delaySnapshot = SyncCooldownStatePayload.from(player).spells().get(heal.getSpellId());
            helper.assertTrue(delaySnapshot != null && delaySnapshot.chargeCastDelayTicks() == 24
                    && delaySnapshot.chargeCastDelayFraction() == 1,
                    "Delay-only spell state was not synchronized to the HUD");
            for (int tick = 0; tick < 24; tick++) {
                player.tickCount++;
                manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            }
            helper.assertTrue(manager.canBeginCast(player, heal, 1, CastSource.SPELLBOOK).allowed(),
                    "Delay did not expire after the configured number of ticks");
            data.startChargeCastDelay(heal.getSpellId(), 20);
            ServerConfig.CHARGE_CAST_DELAY_ENABLED.set(false);
            helper.assertTrue(manager.canBeginCast(player, heal, 1, CastSource.SPELLBOOK).allowed(),
                    "Disabled delay still blocked casting");
            manager.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(data.chargeCastDelays().isEmpty(), "Disabled delay retained stale timers");
            ServerConfig.CHARGE_CAST_DELAY_ENABLED.set(true);
            data.clear();
            helper.assertTrue(com.cappleapple.temponottime.casting.SpellTiming.effectiveCastTicks(bolt, 1, player, 0) == 0,
                    "Instant spell changed without opt-in");
            ServerConfig.AFFECT_NO_CAST_TIME_SPELLS.set(true);
            helper.assertTrue(bolt.attemptInitiateCast(net.minecraft.world.item.ItemStack.EMPTY, 1, player.level(), player,
                    CastSource.SPELLBOOK, true, "mainhand"), "Opted-in instant cast initiation failed");
            helper.assertTrue(magic.getCastDuration() == 20, "Opted-in instant spell did not receive a delay");
            magic.resetCastingState();
            data.clear();
            var custom = SpellRegistry.getSpell("irons_spellbooks:recall");
            helper.assertTrue(custom != SpellRegistry.none(), "Custom cast fixture missing");
            int original = custom.getEffectiveCastTime(1, player);
            helper.assertTrue(com.cappleapple.temponottime.casting.SpellTiming.effectiveCastTicks(custom, 1, player, original) == original,
                    "Custom timing changed without opt-in");
            ServerConfig.AFFECT_CUSTOM_CAST_TIMES.set(true);
            helper.assertTrue(com.cappleapple.temponottime.casting.SpellTiming.effectiveCastTicks(custom, 1, player, original) == original + 20,
                    "Custom cast-time opt-in did not apply");
            data.startChargeCastDelay(bolt.getSpellId(), 10);
            var snapshot = SyncCooldownStatePayload.from(player);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                SyncCooldownStatePayload.STREAM_CODEC.encode(buffer, snapshot);
                var decoded = SyncCooldownStatePayload.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.equals(snapshot) && decoded.rechargeFlatModifier() == 2
                        && decoded.castTimeNormalization().flatModifier() == 1
                        && decoded.castTimeNormalization().affectCustomCastTimes()
                        && decoded.castTimeNormalization().affectNoCastTimeSpells(), "Timing settings snapshot lost configuration");
            } finally { buffer.release(); }
        } finally {
            magic.resetCastingState();
            data.clear();
            for (int i = 0; i < settings.size(); i++) restoreSetting(settings.get(i), saved.get(i));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restoreSetting(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue setting, Object value) {
        setting.set(value);
    }

}
