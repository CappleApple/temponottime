package com.cappleapple.temponottime.casting;

/** Shortfall as a fraction of the whole spell cost: half the cost available means 50% longer. */
public final class ChargeCooldownPenalty {
    private ChargeCooldownPenalty() {
    }

    public static double multiplier(double capacity, double cost) {
        if (!Double.isFinite(cost) || cost <= 0.0) return 1.0;
        double available = Double.isFinite(capacity) ? Math.max(0.0, capacity) : 0.0;
        return 1.0 + Math.clamp((cost - available) / cost, 0.0, 1.0);
    }
}
