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
    public static final int DATA_VERSION = 2;

    private final Map<String, List<CooldownInstance>> cooldowns = new LinkedHashMap<>();
    private long nextId = 1;
    private transient PendingCast pendingCast;
    private transient String committedCastingSpellId;
    private transient boolean dirty = true;
    private double castingReserveCredit;
    private double castingReserveOverchargeLimit;

    public Map<String, List<CooldownInstance>> cooldowns() {
        return cooldowns;
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
        return root;
    }

    public void load(CompoundTag root) {
        cooldowns.clear();
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
