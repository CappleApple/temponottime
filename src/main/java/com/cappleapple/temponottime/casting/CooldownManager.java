package com.cappleapple.temponottime.casting;

import com.cappleapple.temponottime.TempoNotTime;
import com.cappleapple.temponottime.api.event.CastingDrawEvent;
import com.cappleapple.temponottime.api.event.CastingReserveEvent;
import com.cappleapple.temponottime.api.event.ChargeCalculationEvent;
import com.cappleapple.temponottime.api.event.SpellCastReservationEvent;
import com.cappleapple.temponottime.config.ServerConfig;
import com.cappleapple.temponottime.config.SpellOverride;
import com.cappleapple.temponottime.config.SpellOverrideManager;
import com.cappleapple.temponottime.data.CooldownInstance;
import com.cappleapple.temponottime.data.PendingCast;
import com.cappleapple.temponottime.data.PlayerCooldownData;
import com.cappleapple.temponottime.network.TempoNetwork;
import com.cappleapple.temponottime.registry.TempoRegistries;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CooldownManager {
    public static final CooldownManager INSTANCE = new CooldownManager();
    private static final int PROGRESS_SYNC_INTERVAL = 10;
    private static final int MANA_COMPATIBILITY_SYNC_INTERVAL = 10;

    private final Map<UUID, Long> lastFeedbackTicks = new HashMap<>();
    private final Map<UUID, ManaCompatibilityValues.Snapshot> lastManaCompatibilityValues = new HashMap<>();

    private CooldownManager() {
    }

    public PlayerCooldownData data(Player player) {
        return player.getData(TempoRegistries.COOLDOWN_DATA);
    }

    public double maximumCastingReserve(Player player) {
        double nativeCapacity = player.getAttributeValue(TempoRegistries.CASTING_RESERVE);
        double converted = ServerConfig.CONVERT_MAX_MANA.get()
                ? player.getAttributeValue(AttributeRegistry.MAX_MANA) * ServerConfig.MAX_MANA_TO_CAPACITY_MULTIPLIER.get()
                : 0.0;
        double value = safeNonNegative(nativeCapacity) + safeNonNegative(converted);
        double minimum = ServerConfig.MINIMUM_CAPACITY.get();
        double maximum = Math.max(minimum, ServerConfig.MAXIMUM_CAPACITY.get());
        value = Math.clamp(value, minimum, maximum);
        CastingReserveEvent event = NeoForge.EVENT_BUS.post(new CastingReserveEvent(player, value));
        return Math.clamp(safeNonNegative(event.getReserve()), minimum, maximum);
    }

    public double castingDraw(Player player, AbstractSpell spell, int spellLevel) {
        return castingDraw(player, spell, spellLevel, spell.getManaCost(spellLevel));
    }

    public double castingDraw(Player player, AbstractSpell spell, int spellLevel, double effectiveManaCost) {
        double fallback = ServerConfig.ZERO_MANA_SPELL_CAPACITY_COST.get();
        double value = Double.isFinite(effectiveManaCost) && effectiveManaCost > 0.0 ? effectiveManaCost : fallback;
        value *= ServerConfig.MANA_COST_TO_CAPACITY_COST_MULTIPLIER.get();
        SpellOverride override = SpellOverrideManager.get(spell.getSpellId());
        if (override.castingDraw() != null) {
            value = override.castingDraw();
        }
        CastingDrawEvent event = NeoForge.EVENT_BUS.post(new CastingDrawEvent(player, spell, spellLevel, value));
        value = event.getDraw();
        if (!Double.isFinite(value) || value < 0.0) {
            TempoNotTime.LOGGER.warn("Spell {} exposed invalid Casting Draw {}; using {}", spell.getSpellId(), value, fallback);
            value = fallback;
        }
        return Math.min(1_000_000.0, value);
    }

    public int maxCharges(Player player, AbstractSpell spell, int spellLevel) {
        SpellOverride override = SpellOverrideManager.get(spell.getSpellId());
        boolean enabled = ServerConfig.CHARGES_ENABLED.get() && override.chargesAllowed(true);
        int calculated = enabled
                ? ChargeCalculator.maxCharges(maximumCastingReserve(player), castingDraw(player, spell, spellLevel),
                ServerConfig.MINIMUM_CHARGES.get(), ServerConfig.MAXIMUM_CHARGES.get(),
                ServerConfig.ZERO_MANA_SPELL_CAPACITY_COST.get(), ServerConfig.CHARGE_REQUIREMENT_FORMULA.get())
                : 1;
        if (enabled && override.maxCharges() != null) {
            calculated = Math.clamp(override.maxCharges(), ServerConfig.MINIMUM_CHARGES.get(), ServerConfig.MAXIMUM_CHARGES.get());
        }
        ChargeCalculationEvent event = NeoForge.EVENT_BUS.post(new ChargeCalculationEvent(player, spell, spellLevel, calculated));
        return Math.max(1, event.getCharges());
    }

    public int availableCharges(Player player, AbstractSpell spell, int spellLevel) {
        return Math.max(0, maxCharges(player, spell, spellLevel) - committedUses(data(player), spell.getSpellId()));
    }

    public double usedCastingReserve(Player player) {
        if (!ServerConfig.CAPACITY_ENABLED.get()
                || (player.isCreative() && ServerConfig.CREATIVE_BYPASSES_CAPACITY.get())) return 0.0;
        PlayerCooldownData data = data(player);
        double used = data.allInstances().stream().filter(CooldownInstance::occupiesCastingReserve)
                .mapToDouble(CooldownInstance::castingDraw).sum();
        PendingCast pending = data.pendingCast();
        if (pending != null && pending.occupiesCastingReserve()) {
            used += pending.castingDraw();
        }
        return safeNonNegative(used);
    }

    public double freeCastingReserve(Player player) {
        return manaCompatibility(player).current();
    }

    public ManaCompatibilityValues.Snapshot manaCompatibility(Player player) {
        return ManaCompatibilityValues.snapshot(maximumCastingReserve(player), usedCastingReserve(player),
                data(player).castingReserveCredit());
    }

    public void rechargeCastingReserve(ServerPlayer player, double amount) {
        if (!ServerConfig.enabled() || !ServerConfig.CAPACITY_ENABLED.get()
                || !Double.isFinite(amount) || amount <= 0.0) return;
        PlayerCooldownData data = data(player);
        data.addCastingReserveCredit(amount, usedCastingReserve(player));
        sync(player);
    }

    public int activeCooldownCount(Player player) {
        PlayerCooldownData data = data(player);
        if (ServerConfig.COUNT_PER_CHARGE.get()) {
            int count = (int) data.allInstances().stream().filter(CooldownInstance::appliesLoad).count();
            if (data.pendingCast() != null && data.pendingCast().appliesLoad()) count++;
            return count;
        }
        Set<String> spells = new HashSet<>();
        data.allInstances().stream().filter(CooldownInstance::appliesLoad).map(CooldownInstance::spellId).forEach(spells::add);
        if (data.pendingCast() != null && data.pendingCast().appliesLoad()) spells.add(data.pendingCast().spellId());
        return spells.size();
    }

    public double loadMultiplier(Player player) {
        if (!ServerConfig.LOAD_ENABLED.get()) return 1.0;
        return CooldownLoadCalculator.multiplier(activeCooldownCount(player), ServerConfig.FREE_COOLDOWNS.get(),
                ServerConfig.PENALTY_PER_ADDITIONAL_COOLDOWN.get(), ServerConfig.MINIMUM_RECOVERY_MULTIPLIER.get());
    }

    public double recoveryMultiplier(Player player) {
        double manaRegen = ServerConfig.CONVERT_MANA_REGEN.get() ? player.getAttributeValue(AttributeRegistry.MANA_REGEN) : 1.0;
        return RecoveryCalculator.multiplier(manaRegen, ServerConfig.MANA_REGEN_TO_RECOVERY_MULTIPLIER.get(),
                ServerConfig.MAXIMUM_TOTAL_RECOVERY_MULTIPLIER.get(), loadMultiplier(player));
    }

    public CastDecision canBeginCast(ServerPlayer player, AbstractSpell spell, int spellLevel, CastSource castSource) {
        if (!ServerConfig.enabled() || !manages(castSource)) return CastDecision.allow();
        if (isFollowupRecast(player, spell)) return CastDecision.allow();
        SpellOverride override = SpellOverrideManager.get(spell.getSpellId());
        boolean chargeGate = ServerConfig.CHARGES_ENABLED.get() && override.chargesAllowed(true)
                && !(player.isCreative() && ServerConfig.CREATIVE_BYPASSES_CHARGES.get());
        if (chargeGate && availableCharges(player, spell, spellLevel) <= 0) {
            return CastDecision.deny(CastDecision.Failure.NO_CHARGES);
        }
        if (!chargeGate && committedUses(data(player), spell.getSpellId()) > 0
                && !(player.isCreative() && ServerConfig.CREATIVE_BYPASSES_CHARGES.get())) {
            return CastDecision.deny(CastDecision.Failure.NO_CHARGES);
        }

        double cost = castingDraw(player, spell, spellLevel);
        boolean reserveGate = ServerConfig.CAPACITY_ENABLED.get() && override.occupiesCastingReserve(true)
                && !(player.isCreative() && ServerConfig.CREATIVE_BYPASSES_CAPACITY.get());
        if (reserveGate && !CapacityCalculator.canReserve(maximumCastingReserve(player) + data(player).castingReserveCredit(),
                usedCastingReserve(player), cost,
                ServerConfig.ALLOW_OVERCAPACITY_SINGLE_CAST.get())) {
            return CastDecision.deny(CastDecision.Failure.NO_CAPACITY);
        }
        SpellCastReservationEvent reservationEvent = NeoForge.EVENT_BUS.post(new SpellCastReservationEvent(player, spell, spellLevel, cost));
        return reservationEvent.isCanceled() ? CastDecision.deny(CastDecision.Failure.EVENT_CANCELED) : CastDecision.allow();
    }

    public void beginSuccessfulCast(ServerPlayer player, AbstractSpell spell, int spellLevel, CastSource castSource) {
        if (!ServerConfig.enabled() || !manages(castSource)) return;
        if (isFollowupRecast(player, spell)) return;
        SpellOverride override = SpellOverrideManager.get(spell.getSpellId());
        double cost = castingDraw(player, spell, spellLevel);
        double duration = rechargeDuration(spell, MagicManager.getEffectiveSpellCooldown(spell, player, castSource),
                override.cooldownMultiplier());
        boolean reserves = override.occupiesCastingReserve(true);
        boolean appliesLoad = override.appliesLoad(true);
        data(player).setPendingCast(new PendingCast(spell.getSpellId(), spellLevel, cost, duration, reserves, appliesLoad));
        sync(player);
    }

    public void commitCast(ServerPlayer player, AbstractSpell spell, int spellLevel, double eventManaCost, CastSource castSource) {
        if (!ServerConfig.enabled() || !manages(castSource)) return;
        // Iron posts SpellOnCastEvent before decrementing the recast meter, so every follow-up blast is identifiable here.
        if (isFollowupRecast(player, spell)) return;
        PlayerCooldownData data = data(player);
        if (spell.getSpellId().equals(data.committedCastingSpellId())) return;

        PendingCast pending = data.pendingCast();
        SpellOverride override = SpellOverrideManager.get(spell.getSpellId());
        double cost = castingDraw(player, spell, spellLevel, eventManaCost);
        double duration = rechargeDuration(spell, MagicManager.getEffectiveSpellCooldown(spell, player, castSource),
                override.cooldownMultiplier());
        boolean reserves = pending != null && pending.spellId().equals(spell.getSpellId())
                ? pending.occupiesCastingReserve()
                : override.occupiesCastingReserve(true);
        boolean appliesLoad = pending != null && pending.spellId().equals(spell.getSpellId())
                ? pending.appliesLoad()
                : override.appliesLoad(true);

        boolean reserveApplies = ServerConfig.CAPACITY_ENABLED.get() && reserves
                && !(player.isCreative() && ServerConfig.CREATIVE_BYPASSES_CAPACITY.get());
        double reservedCost = reserveApplies ? data.consumeCastingReserveCredit(cost) : cost;
        data.add(spell.getSpellId(), spellLevel, reservedCost, duration, true, reserves, appliesLoad);
        data.setPendingCast(null);
        data.setCommittedCastingSpellId(spell.getSpellId());
        if (ServerConfig.DEBUG_LOGGING.get()) {
            TempoNotTime.LOGGER.info("Committed {} recharge for {}: castingDraw={}, duration={}", spell.getSpellId(), player.getGameProfile().getName(), cost, duration);
        }
        sync(player);
    }

    public void activateNext(ServerPlayer player, AbstractSpell spell, int effectiveDuration) {
        SpellOverride override = SpellOverrideManager.get(spell.getSpellId());
        data(player).forSpell(spell.getSpellId()).stream()
                .filter(CooldownInstance::waitingForIronCooldown)
                .min(Comparator.comparingLong(CooldownInstance::id))
                .ifPresent(instance -> {
                    instance.activate(rechargeDuration(spell, effectiveDuration, override.cooldownMultiplier()));
                    data(player).markDirty();
                    sync(player);
                });
    }

    private static double rechargeDuration(AbstractSpell spell, double effectiveDurationTicks, double overrideMultiplier) {
        double normalizedEffective = RechargeNormalizer.normalizeEffectiveTicks(spell.getSpellCooldown(), effectiveDurationTicks,
                ServerConfig.RECHARGE_NORMALIZATION_ENABLED.get(), ServerConfig.NORMAL_RECHARGE_SECONDS.get(),
                ServerConfig.SHORT_RECHARGE_STRENGTH.get(), ServerConfig.LONG_RECHARGE_STRENGTH.get(),
                ServerConfig.NORMALIZATION_SPREAD.get());
        double duration = normalizedEffective * overrideMultiplier;
        return Double.isFinite(duration) && duration > 0.0 ? Math.max(1.0, duration) : 1.0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ServerConfig.enabled() || !manages(event.getCastSource())) return;
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        CastDecision decision = canBeginCast(player, spell, event.getSpellLevel(), event.getCastSource());
        if (!decision.allowed()) {
            event.setCanceled(true);
            showRejection(player, decision.failure());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ServerConfig.enabled() || !manages(event.getCastSource())) return;
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        commitCast(player, spell, event.getSpellLevel(), event.getManaCost(), event.getCastSource());
        if (ServerConfig.DISABLE_MANA_CONSUMPTION.get()) {
            event.setManaCost(0);
        }
    }

    @SubscribeEvent
    public void onCooldownAdded(SpellCooldownAddedEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.enabled() && manages(event.getCastSource())) {
            activateNext(player, event.getSpell(), event.getEffectiveCooldown());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerCooldownData data = data(player);
        if (!ServerConfig.enabled()) {
            if (!data.allInstances().isEmpty() || data.pendingCast() != null) {
                data.clear();
                sync(player);
            }
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (!magicData.isCasting()) {
            if (data.pendingCast() != null) {
                data.setPendingCast(null);
            }
            data.setCommittedCastingSpellId(null);
        }

        reconcileChargeMode(data);
        activateOrphanedWaitingInstances(player, data, magicData);
        boolean completed = advanceCooldowns(player, data);
        boolean manaCompatibilityChanged = player.tickCount % MANA_COMPATIBILITY_SYNC_INTERVAL == 0
                && manaCompatibilityChanged(player);

        if (completed || data.isDirty() || manaCompatibilityChanged
                || (!data.allInstances().isEmpty() && player.tickCount % PROGRESS_SYNC_INTERVAL == 0)) {
            sync(player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) || !event.isWasDeath()) return;
        PlayerCooldownData replacement = data(newPlayer);
        if (ServerConfig.DEATH_BEHAVIOR.get() == ServerConfig.DeathCooldownBehavior.PRESERVE) {
            replacement.copyFrom(data(event.getOriginal()));
        } else {
            replacement.clear();
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastFeedbackTicks.remove(event.getEntity().getUUID());
        lastManaCompatibilityValues.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        SpellOverrideManager.reload();
        String ironsVersion = ModList.get().getModContainerById("irons_spellbooks")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("missing");
        TempoNotTime.LOGGER.info("{} initialized against Iron's Spells 'n Spellbooks {}", TempoNotTime.DISPLAY_NAME, ironsVersion);
    }

    public void clear(ServerPlayer player) {
        data(player).clear();
        MagicData.getPlayerMagicData(player).getPlayerCooldowns().clearCooldowns();
        MagicData.getPlayerMagicData(player).getPlayerCooldowns().syncToPlayer(player);
        sync(player);
    }

    public List<SpellData> selectedSpells(ServerPlayer player) {
        return new SpellSelectionManager(player).getAllSpells().stream().map(option -> option.spellData).toList();
    }

    public void sync(ServerPlayer player) {
        data(player).markClean();
        TempoNetwork.sync(player);
        rememberManaCompatibility(player);
    }

    private void reconcileChargeMode(PlayerCooldownData data) {
        if (ServerConfig.CHARGES_ENABLED.get()) return;
        for (List<CooldownInstance> instances : data.cooldowns().values()) {
            if (instances.size() <= 1) continue;
            CooldownInstance keep = instances.stream().max(Comparator.comparingDouble(CooldownInstance::remainingTicks)).orElseThrow();
            instances.removeIf(instance -> instance != keep);
            data.markDirty();
        }
        data.removeCompleted();
    }

    private void activateOrphanedWaitingInstances(ServerPlayer player, PlayerCooldownData data, MagicData magicData) {
        for (CooldownInstance instance : data.allInstances()) {
            if (!instance.waitingForIronCooldown()) continue;
            boolean hasRecast = magicData.getPlayerRecasts().hasRecastForSpell(instance.spellId());
            boolean isStillCastingThisSpell = magicData.isCasting() && instance.spellId().equals(magicData.getCastingSpellId());
            if (!hasRecast && !isStillCastingThisSpell) {
                instance.activate(instance.durationTicks());
                data.markDirty();
            }
        }
    }

    private boolean advanceCooldowns(ServerPlayer player, PlayerCooldownData data) {
        if (data.allInstances().isEmpty()) return false;
        double amount = recoveryMultiplier(player);
        boolean completed = false;

        for (List<CooldownInstance> instances : new ArrayList<>(data.cooldowns().values())) {
            if (ServerConfig.RECOVERY_MODE.get() == ServerConfig.RecoveryMode.PARALLEL) {
                completed |= instances.removeIf(instance -> instance.advance(amount));
            } else {
                CooldownInstance next = instances.stream().filter(instance -> !instance.waitingForIronCooldown())
                        .min(Comparator.comparingLong(CooldownInstance::id)).orElse(null);
                if (next != null && next.advance(amount)) {
                    instances.remove(next);
                    completed = true;
                }
            }
        }
        if (completed) data.markDirty();
        data.removeCompleted();
        return completed;
    }

    private int committedUses(PlayerCooldownData data, String spellId) {
        int count = data.forSpell(spellId).size();
        if (data.pendingCast() != null && data.pendingCast().spellId().equals(spellId)) count++;
        return count;
    }

    private void showRejection(ServerPlayer player, CastDecision.Failure failure) {
        long now = player.level().getGameTime();
        long last = lastFeedbackTicks.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - last < 20) return;
        lastFeedbackTicks.put(player.getUUID(), now);
        String key = failure == CastDecision.Failure.NO_CAPACITY
                ? "message.temponottime.not_enough_reserve"
                : failure == CastDecision.Failure.EVENT_CANCELED
                ? "message.temponottime.reservation_denied"
                : "message.temponottime.no_charges";
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }

    private boolean manaCompatibilityChanged(ServerPlayer player) {
        if (!manaCompatibilityActive()) {
            return lastManaCompatibilityValues.remove(player.getUUID()) != null;
        }
        return !manaCompatibility(player).approximatelyEquals(lastManaCompatibilityValues.get(player.getUUID()));
    }

    private void rememberManaCompatibility(ServerPlayer player) {
        if (manaCompatibilityActive()) {
            lastManaCompatibilityValues.put(player.getUUID(), manaCompatibility(player));
        } else {
            lastManaCompatibilityValues.remove(player.getUUID());
        }
    }

    private static boolean manaCompatibilityActive() {
        return ServerConfig.enabled() && ServerConfig.DISABLE_MANA_CONSUMPTION.get();
    }

    private static double safeNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static boolean isFollowupRecast(ServerPlayer player, AbstractSpell spell) {
        boolean activeIronRecast = MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(spell);
        return !RecastReservationPolicy.consumesTempoUse(activeIronRecast);
    }

    public static boolean manages(CastSource castSource) {
        return castSource == CastSource.SPELLBOOK || castSource == CastSource.SWORD;
    }
}
