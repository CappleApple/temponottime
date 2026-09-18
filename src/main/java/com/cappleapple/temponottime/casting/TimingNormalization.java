package com.cappleapple.temponottime.casting;

/** Shared settings for recharge and cast-time balancing; all durations are in seconds. */
public record TimingNormalization(boolean enabled, double flatModifier, double normalSeconds,
                                  double shortStrength, double longStrength, double spreadSeconds,
                                  boolean affectCustomCastTimes, boolean affectNoCastTimeSpells) {
    public TimingNormalization(boolean enabled, double flatModifier, double normalSeconds,
                               double shortStrength, double longStrength, double spreadSeconds) {
        this(enabled, flatModifier, normalSeconds, shortStrength, longStrength, spreadSeconds, false, false);
    }
    public static final TimingNormalization UNCHANGED = new TimingNormalization(false, 0, 1, 0, 0, 1);

    public int adjustedBaseTicks(double baseTicks) {
        double base = Double.isFinite(baseTicks) ? Math.max(0.0, baseTicks) : 0.0;
        double flat = Double.isFinite(flatModifier) ? flatModifier : 0.0;
        return ticks(base + flat * 20.0);
    }

    public double normalizeAdjustedTicks(double adjustedBaseTicks, double effectiveTicks) {
        return RechargeNormalizer.normalizeEffectiveTicks(adjustedBaseTicks, effectiveTicks, enabled,
                normalSeconds, shortStrength, longStrength, spreadSeconds);
    }

    public int castTicks(int baseTicks, int effectiveTicks) {
        if (!enabled && flatModifier == 0.0) return effectiveTicks;
        boolean noCastTime = baseTicks <= 0 || effectiveTicks <= 0;
        if (noCastTime && !affectNoCastTimeSpells) return effectiveTicks;
        double adjusted = adjustedBaseTicks(noCastTime ? 0 : baseTicks);
        double normalized = normalizeAdjustedTicks(adjusted, adjusted);
        return ticks(normalized * (noCastTime ? 1.0 : (double) effectiveTicks / baseTicks));
    }

    private static int ticks(double ticks) {
        return (int) Math.clamp(Math.round(ticks), 1L, Integer.MAX_VALUE);
    }
}
