package com.cappleapple.temponottime.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerCooldownData {
    public static final int DATA_VERSION = 5;

    private final Map<String, List<CooldownInstance>> cooldowns = new LinkedHashMap<>();
    private final Map<String, ChargeCastDelay> chargeCastDelays = new LinkedHashMap<>();
    private long nextId = 1;
    private transient PendingCast pendingCast;
    private transient String committedCastingSpellId;
    private transient boolean dirty = true;
    private double castingReserveCredit;
    private double castingReserveOverchargeLimit;

    public Map<String, List<CooldownInstance>> cooldowns() {
        return cooldowns;
    }

    public Map<String, ChargeCastDelay> chargeCastDelays() { return java.util.Collections.unmodifiableMap(chargeCastDelays); }
    public int chargeCastDelayTicks(String spellId) {
        var delay = chargeCastDelays.get(spellId);
        return delay == null ? 0 : delay.remainingTicks();
    }
    public void startChargeCastDelay(String spellId, int ticks) {
        if (ticks > 0) chargeCastDelays.put(spellId, new ChargeCastDelay(ticks, ticks));
        else chargeCastDelays.remove(spellId);
        dirty = true;
    }
    public boolean tickChargeCastDelays(String stillCastingSpellId) {
        chargeCastDelays.replaceAll((spell, delay) -> spell.equals(stillCastingSpellId) ? delay : delay.tick());
        boolean expired = chargeCastDelays.values().removeIf(delay -> delay.remainingTicks() <= 0);
        if (expired) dirty = true;
        return expired;
    }
    public void clearChargeCastDelays() {
        if (!chargeCastDelays.isEmpty()) { chargeCastDelays.clear(); dirty = true; }
    }

    public List<CooldownInstance> forSpell(String spellId) {
        return cooldowns.getOrDefault(spellId, List.of());
    }

    public Collection<CooldownInstance> allInstances() {
        return cooldowns.values().stream().flatMap(Collection::stream).toList();
    }

    public CooldownInstance add(String spellId, int spellLevel, double cost, double duration,
                                boolean waiting, boolean reserves, boolean appliesLoad) {
        CooldownInstance instance = new CooldownInstance(nextId++, spellId, spellLevel, cost, duration, 0.0, waiting, reserves, appliesLoad);
        cooldowns.computeIfAbsent(spellId, ignored -> new ArrayList<>()).add(instance);
        dirty = true;
        return instance;
    }

    public void removeCompleted() {
        cooldowns.values().removeIf(List::isEmpty);
    }

    public void clear() {
        cooldowns.clear();
        chargeCastDelays.clear();
        castingReserveCredit = 0.0;
        castingReserveOverchargeLimit = 0.0;
        pendingCast = null;
        committedCastingSpellId = null;
        dirty = true;
    }

    public PendingCast pendingCast() { return pendingCast; }
    public void setPendingCast(PendingCast pendingCast) {
        this.pendingCast = pendingCast;
        this.committedCastingSpellId = null;
        dirty = true;
    }
    public String committedCastingSpellId() { return committedCastingSpellId; }
    public void setCommittedCastingSpellId(String spellId) { this.committedCastingSpellId = spellId; }
    public void clearCastTracking() { pendingCast = null; committedCastingSpellId = null; }
    public double castingReserveCredit() { return castingReserveCredit; }
    public double castingReserveOverchargeLimit() { return castingReserveOverchargeLimit; }

    public void clearCastingReserveCredit() {
        if (castingReserveCredit != 0 || castingReserveOverchargeLimit != 0) {
            castingReserveCredit = 0;
            castingReserveOverchargeLimit = 0;
            dirty = true;
        }
    }

    public void addCastingReserveCredit(double amount, double occupiedReserve) {
        if (!Double.isFinite(amount) || amount <= 0.0) return;
        double occupied = Double.isFinite(occupiedReserve) ? Math.max(0.0, occupiedReserve) : 0.0;
        castingReserveOverchargeLimit = amount;
        castingReserveCredit = Math.min(occupied + castingReserveOverchargeLimit, castingReserveCredit + amount);
        dirty = true;
    }

    /** Returns the part of a new Casting Draw that must become ordinary recharge debt. */
    public double consumeCastingReserveCredit(double castingDraw) {
        double draw = Double.isFinite(castingDraw) ? Math.max(0.0, castingDraw) : 0.0;
        double consumed = Math.min(castingReserveCredit, draw);
        castingReserveCredit -= consumed;
        if (castingReserveCredit <= 1.0e-7) {
            castingReserveCredit = 0.0;
            castingReserveOverchargeLimit = 0.0;
        }
        if (consumed > 0.0) dirty = true;
        return draw - consumed;
    }
    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void markClean() { dirty = false; }

    public void copyFrom(PlayerCooldownData other) {
        clear();
        nextId = other.nextId;
        chargeCastDelays.putAll(other.chargeCastDelays);
        castingReserveCredit = other.castingReserveCredit;
        castingReserveOverchargeLimit = other.castingReserveOverchargeLimit;
        other.cooldowns.forEach((spell, instances) -> {
            List<CooldownInstance> copies = new ArrayList<>();
            for (CooldownInstance instance : instances) {
                copies.add(CooldownInstance.load(instance.save()));
            }
            cooldowns.put(spell, copies);
        });
        dirty = true;
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        root.putInt("version", DATA_VERSION);
        root.putLong("next_id", nextId);
        root.putDouble("casting_reserve_credit", castingReserveCredit);
        root.putDouble("casting_reserve_overcharge_limit", castingReserveOverchargeLimit);
        ListTag list = new ListTag();
        for (CooldownInstance instance : allInstances()) {
            list.add(instance.save());
        }
        root.put("instances", list);
        CompoundTag delays = new CompoundTag();
        chargeCastDelays.forEach((spell, delay) -> {
            CompoundTag value = new CompoundTag();
            value.putInt("duration_ticks", delay.durationTicks());
            value.putInt("remaining_ticks", delay.remainingTicks());
            delays.put(spell, value);
        });
        root.put("charge_cast_delays", delays);
        return root;
    }

    public void load(CompoundTag root) {
        cooldowns.clear();
        chargeCastDelays.clear();
        pendingCast = null;
        committedCastingSpellId = null;
        CompoundTag delays = root.getCompound("charge_cast_delays");
        for (String spell : delays.getAllKeys()) {
            CompoundTag value = delays.getCompound(spell);
            var delay = new ChargeCastDelay(value.getInt("duration_ticks"), value.getInt("remaining_ticks"));
            if (!spell.isBlank() && delay.remainingTicks() > 0) chargeCastDelays.put(spell, delay);
        }
        nextId = Math.max(1, root.getLong("next_id"));
        castingReserveCredit = safeNonNegative(root.getDouble("casting_reserve_credit"));
        castingReserveOverchargeLimit = safeNonNegative(root.getDouble("casting_reserve_overcharge_limit"));
        ListTag list = root.getList("instances", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CooldownInstance instance = CooldownInstance.load((CompoundTag) element);
            if (!instance.spellId().isBlank()) {
                cooldowns.computeIfAbsent(instance.spellId(), ignored -> new ArrayList<>()).add(instance);
                nextId = Math.max(nextId, instance.id() + 1);
            }
        }
        dirty = true;
    }

    private static double safeNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
