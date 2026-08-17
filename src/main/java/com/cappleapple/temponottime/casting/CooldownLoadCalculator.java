package com.cappleapple.temponottime.casting;

public final class CooldownLoadCalculator {
    private CooldownLoadCalculator() {
    }

    public static double multiplier(int activeCooldowns, int freeCooldowns, double penaltyPerAdditional, double minimumMultiplier) {
        int penalized = Math.max(0, Math.max(0, activeCooldowns) - Math.max(0, freeCooldowns));
        double penalty = Double.isFinite(penaltyPerAdditional) ? Math.max(0.0, penaltyPerAdditional) : 0.0;
        double minimum = Double.isFinite(minimumMultiplier) ? Math.clamp(minimumMultiplier, 0.0001, 1.0) : 0.25;
        double calculated = 1.0 / (1.0 + penalty * penalized);
        return Math.max(minimum, calculated);
    }
}
