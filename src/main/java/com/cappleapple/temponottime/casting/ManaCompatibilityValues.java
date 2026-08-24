package com.cappleapple.temponottime.casting;

/**
 * Read-only Iron mana values exposed for HUD and addon compatibility.
 * Tempo gameplay must continue to use its reserve, charge, and cooldown state directly.
 */
public final class ManaCompatibilityValues {
    public record Snapshot(double maximum, double current) {
        public float currentAsFloat() {
            return (float) Math.min(Float.MAX_VALUE, current);
        }

        public int currentAsInt() {
            return (int) Math.min(Integer.MAX_VALUE, current);
        }

        public boolean approximatelyEquals(Snapshot other) {
            return other != null
                    && Math.abs(maximum - other.maximum) <= 1.0e-7
                    && Math.abs(current - other.current) <= 1.0e-7;
        }
    }

    private ManaCompatibilityValues() {
    }

    public static Snapshot snapshot(double maximumCastingReserve, double usedCastingReserve,
                                    double castingReserveCredit) {
        double maximum = safeNonNegative(maximumCastingReserve);
        double credit = safeNonNegative(castingReserveCredit);
        double current = CapacityCalculator.free(saturatedAdd(maximum, credit), usedCastingReserve);
        return new Snapshot(maximum, current);
    }

    private static double saturatedAdd(double first, double second) {
        double total = first + second;
        return Double.isFinite(total) ? total : Double.MAX_VALUE;
    }

    private static double safeNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
