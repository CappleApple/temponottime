package com.cappleapple.temponottime.casting;

public final class ExternalManaPolicy {
    private ExternalManaPolicy() {
    }

    public static double castingDraw(double manaCost, double conversionMultiplier) {
        double source = Double.isFinite(manaCost) ? Math.max(0.0, manaCost) : 0.0;
        double multiplier = Double.isFinite(conversionMultiplier) ? Math.max(0.0, conversionMultiplier) : 1.0;
        double draw = source * multiplier;
        return Double.isFinite(draw) ? Math.min(1_000_000.0, Math.max(0.0, draw)) : 0.0;
    }

    public static boolean canAfford(boolean capacityEnabled, boolean bypassesCapacity,
                                    double maximum, double used, double credit, double prospectiveDraw,
                                    boolean allowOvercapacitySingleCast) {
        if (!capacityEnabled || bypassesCapacity) {
            return true;
        }
        double availableMaximum = finiteNonNegative(maximum) + finiteNonNegative(credit);
        return CapacityCalculator.canReserve(availableMaximum, used, prospectiveDraw,
                allowOvercapacitySingleCast);
    }

    public static double rechargeDuration(double effectiveCooldownTicks, boolean normalizationEnabled,
                                          double normalSeconds, double shortStrength,
                                          double longStrength, double spreadSeconds) {
        double effective = Double.isFinite(effectiveCooldownTicks) && effectiveCooldownTicks > 0.0
                ? effectiveCooldownTicks : 1.0;
        return RechargeNormalizer.normalizeEffectiveTicks(effective, effective, normalizationEnabled,
                normalSeconds, shortStrength, longStrength, spreadSeconds);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
