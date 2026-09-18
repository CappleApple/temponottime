package com.cappleapple.temponottime.data;

import net.minecraft.nbt.CompoundTag;

public final class CooldownInstance {
    private final long id;
    private final String spellId;
    private final int spellLevel;
    private final double castingDraw;
    private final boolean occupiesCastingReserve;
    private final boolean appliesLoad;
    private double recoveryManaCost;
    private double durationTicks;
    private double progressTicks;
    private boolean waitingForIronCooldown;

    public CooldownInstance(long id, String spellId, int spellLevel, double castingDraw, double durationTicks,
                            double progressTicks, boolean waitingForIronCooldown, boolean occupiesCastingReserve, boolean appliesLoad) {
        this.id = id;
        this.spellId = spellId;
        this.spellLevel = Math.max(1, spellLevel);
        this.castingDraw = safeNonNegative(castingDraw);
        this.recoveryManaCost = Math.max(1.0, this.castingDraw);
        this.durationTicks = safeDuration(durationTicks);
        this.progressTicks = Math.clamp(Double.isFinite(progressTicks) ? progressTicks : 0.0, 0.0, this.durationTicks);
        this.waitingForIronCooldown = waitingForIronCooldown;
        this.occupiesCastingReserve = occupiesCastingReserve;
        this.appliesLoad = appliesLoad;
    }

    public long id() { return id; }
    public String spellId() { return spellId; }
    public int spellLevel() { return spellLevel; }
    public double castingDraw() { return castingDraw; }
    public double durationTicks() { return durationTicks; }
    public double recoveryManaCost() { return recoveryManaCost; }
    public void setRecoveryManaCost(double manaCost) {
        recoveryManaCost = Double.isFinite(manaCost) && manaCost > 0.0 ? manaCost : 1.0;
    }
    public double progressTicks() { return progressTicks; }
    public boolean waitingForIronCooldown() { return waitingForIronCooldown; }
    public boolean occupiesCastingReserve() { return occupiesCastingReserve; }
    public boolean appliesLoad() { return appliesLoad; }

    public void activate(double effectiveDurationTicks) {
        durationTicks = safeDuration(effectiveDurationTicks);
        progressTicks = 0.0;
        waitingForIronCooldown = false;
    }

    public boolean advance(double amount) {
        if (!waitingForIronCooldown && Double.isFinite(amount) && amount > 0.0) {
            progressTicks = Math.min(durationTicks, progressTicks + amount);
        }
        return progressTicks >= durationTicks;
    }

    public double remainingTicks() {
        return Math.max(0.0, durationTicks - progressTicks);
    }

    public double remainingFraction() {
        return durationTicks <= 0.0 ? 0.0 : Math.clamp(remainingTicks() / durationTicks, 0.0, 1.0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", id);
        tag.putString("spell", spellId);
        tag.putInt("level", spellLevel);
        tag.putDouble("cost", castingDraw);
        tag.putDouble("duration", durationTicks);
        tag.putDouble("recovery_mana_cost", recoveryManaCost);
        tag.putDouble("progress", progressTicks);
        tag.putBoolean("waiting", waitingForIronCooldown);
        tag.putBoolean("reserves", occupiesCastingReserve);
        tag.putBoolean("load", appliesLoad);
        return tag;
    }

    public static CooldownInstance load(CompoundTag tag) {
        CooldownInstance instance = new CooldownInstance(tag.getLong("id"), tag.getString("spell"), tag.getInt("level"),
                tag.getDouble("cost"), tag.getDouble("duration"), tag.getDouble("progress"),
                tag.getBoolean("waiting"), tag.getBoolean("reserves"), tag.getBoolean("load"));
        if (tag.contains("recovery_mana_cost")) instance.setRecoveryManaCost(tag.getDouble("recovery_mana_cost"));
        return instance;
    }

    private static double safeNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double safeDuration(double value) {
        return Double.isFinite(value) ? Math.max(1.0, value) : 1.0;
    }
}
