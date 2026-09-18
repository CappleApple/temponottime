package com.cappleapple.temponottime.network;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.casting.CooldownManager;
import com.cappleapple.temponottime.casting.TimingNormalization;
import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.compat.SimplySwordsManaCompatibility;
import com.cappleapple.temponottime.data.CooldownInstance;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SyncCooldownStatePayload(boolean enabled, boolean manaDisabled, boolean spellCooldownsOnly,
                                       boolean castingReserveEnabled, boolean chargesEnabled,
                                       boolean convertsMaxMana, boolean convertsManaRegen,
                                       double maximumCastingReserve, double usedCastingReserve,
                                       double castingReserveCredit, double castingRecoveryMultiplier,
                                       int activeRecharges, boolean rechargeNormalizationEnabled,
                                       double normalRechargeSeconds, double shortRechargeStrength,
                                       double longRechargeStrength, double normalizationSpread,
                                       double rechargeFlatModifier, TimingNormalization castTimeNormalization,
                                       Map<String, SpellState> spells) implements CustomPacketPayload {
    public record SpellState(int maximumCharges, int activeUses, int availableCharges, float nextRemainingFraction,
                             int chargeCastDelayTicks, float chargeCastDelayFraction) {
    }

    public static final Type<SyncCooldownStatePayload> TYPE = new Type<>(TempoNotTime.id("sync_cooldown_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCooldownStatePayload> STREAM_CODEC =
            CustomPacketPayload.codec(SyncCooldownStatePayload::write, SyncCooldownStatePayload::new);

    public SyncCooldownStatePayload(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                new TimingNormalization(buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(),
                        buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(), buffer.readBoolean()), readSpells(buffer));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(enabled);
        buffer.writeBoolean(manaDisabled);
        buffer.writeBoolean(spellCooldownsOnly);
        buffer.writeBoolean(castingReserveEnabled);
        buffer.writeBoolean(chargesEnabled);
        buffer.writeBoolean(convertsMaxMana);
        buffer.writeBoolean(convertsManaRegen);
        buffer.writeDouble(maximumCastingReserve);
        buffer.writeDouble(usedCastingReserve);
        buffer.writeDouble(castingReserveCredit);
        buffer.writeDouble(castingRecoveryMultiplier);
        buffer.writeVarInt(activeRecharges);
        buffer.writeBoolean(rechargeNormalizationEnabled);
        buffer.writeDouble(normalRechargeSeconds);
        buffer.writeDouble(shortRechargeStrength);
        buffer.writeDouble(longRechargeStrength);
        buffer.writeDouble(normalizationSpread);
        buffer.writeDouble(rechargeFlatModifier);
        buffer.writeBoolean(castTimeNormalization.enabled());
        buffer.writeDouble(castTimeNormalization.flatModifier());
        buffer.writeDouble(castTimeNormalization.normalSeconds());
        buffer.writeDouble(castTimeNormalization.shortStrength());
        buffer.writeDouble(castTimeNormalization.longStrength());
        buffer.writeDouble(castTimeNormalization.spreadSeconds());
        buffer.writeBoolean(castTimeNormalization.affectCustomCastTimes());
        buffer.writeBoolean(castTimeNormalization.affectNoCastTimeSpells());
        buffer.writeVarInt(spells.size());
        spells.forEach((id, state) -> {
            buffer.writeUtf(id);
            buffer.writeVarInt(state.maximumCharges());
            buffer.writeVarInt(state.activeUses());
            buffer.writeVarInt(state.availableCharges());
            buffer.writeFloat(state.nextRemainingFraction());
            buffer.writeVarInt(state.chargeCastDelayTicks());
            buffer.writeFloat(state.chargeCastDelayFraction());
        });
    }

    public static SyncCooldownStatePayload from(ServerPlayer player) {
        CooldownManager manager = CooldownManager.INSTANCE;
        Map<String, SpellState> spellStates = new LinkedHashMap<>();
        for (SpellData spellData : manager.selectedSpells(player)) {
            if (spellData == null || spellData.getSpell().getSpellId().isBlank()) continue;
            String id = spellData.getSpell().getSpellId();
            int level = Math.max(1, spellData.getLevel());
            spellStates.putIfAbsent(id, createSpellState(player, id, level));
        }
        manager.data(player).cooldowns().forEach((id, instances) -> {
            if (SimplySwordsManaCompatibility.isExternalCooldown(id)) return;
            int level = instances.isEmpty() ? 1 : instances.getFirst().spellLevel();
            spellStates.put(id, createSpellState(player, id, level));
        });
        manager.data(player).chargeCastDelays().keySet().forEach(id ->
                spellStates.computeIfAbsent(id, key -> createSpellState(player, key, 1)));
        return new SyncCooldownStatePayload(ServerConfig.enabled(), ServerConfig.manaDisabled(), ServerConfig.spellCooldownsOnly(),
                ServerConfig.capacityEnabled(), ServerConfig.CHARGES_ENABLED.get(),
                ServerConfig.CONVERT_MAX_MANA.get(), ServerConfig.CONVERT_MANA_REGEN.get(),
                manager.maximumCastingReserve(player), manager.usedCastingReserve(player),
                manager.data(player).castingReserveCredit(), manager.recoveryMultiplier(player),
                manager.activeCooldownCount(player), ServerConfig.RECHARGE_NORMALIZATION_ENABLED.get(),
                ServerConfig.NORMAL_RECHARGE_SECONDS.get(), ServerConfig.SHORT_RECHARGE_STRENGTH.get(),
                ServerConfig.LONG_RECHARGE_STRENGTH.get(), ServerConfig.NORMALIZATION_SPREAD.get(),
                ServerConfig.RECHARGE_FLAT_MODIFIER.get(), ServerConfig.castTimeNormalization(), Map.copyOf(spellStates));
    }

    public static SyncCooldownStatePayload empty() {
        return new SyncCooldownStatePayload(false, false, false, false, false, false, false,
                0.0, 0.0, 0.0, 1.0, 0, false, 10.0, 0.8, 0.5, 8.0, 0.0, TimingNormalization.UNCHANGED, Map.of());
    }

    public TimingNormalization rechargeNormalization() {
        return new TimingNormalization(rechargeNormalizationEnabled, rechargeFlatModifier, normalRechargeSeconds,
                shortRechargeStrength, longRechargeStrength, normalizationSpread);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static SpellState createSpellState(ServerPlayer player, String id, int level) {
        CooldownManager manager = CooldownManager.INSTANCE;
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(id);
        List<CooldownInstance> instances = manager.data(player).forSpell(id);
        int maximum = manager.maxCharges(player, spell, level);
        int active = instances.size();
        if (manager.data(player).pendingCast() != null && manager.data(player).pendingCast().spellId().equals(id)) active++;
        int available = Math.max(0, maximum - active);
        float next = (float) instances.stream().mapToDouble(CooldownInstance::remainingFraction).min().orElse(0.0);
        int delayTicks = manager.chargeCastDelayTicks(player, id);
        var delay = manager.data(player).chargeCastDelays().get(id);
        return new SpellState(maximum, active, available, next, delayTicks,
                delayTicks > 0 && delay != null ? delay.remainingFraction() : 0);
    }

    private static Map<String, SpellState> readSpells(FriendlyByteBuf buffer) {
        int size = Math.clamp(buffer.readVarInt(), 0, 10_000);
        Map<String, SpellState> result = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String id = buffer.readUtf();
            result.put(id, new SpellState(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readVarInt(), buffer.readFloat()));
        }
        return Map.copyOf(result);
    }
}
