package com.cappleapple.temponottime.casting;

public final class RecoveryCalculator {
    private RecoveryCalculator() {
    }

    public static double multiplier(double manaRegenAttribute, double conversionMultiplier, double maximumTotalMultiplier, double loadMultiplier) {
        double regen = Double.isFinite(manaRegenAttribute) ? manaRegenAttribute : 1.0;
        double conversion = Double.isFinite(conversionMultiplier) ? Math.max(0.0, conversionMultiplier) : 1.0;
        double maximum = Double.isFinite(maximumTotalMultiplier) ? Math.max(0.05, maximumTotalMultiplier) : 4.0;
        double converted = Math.clamp(1.0 + (regen - 1.0) * conversion, 0.05, maximum);
        double load = Double.isFinite(loadMultiplier) ? Math.clamp(loadMultiplier, 0.0001, 1.0) : 1.0;
        return converted * load;
    }
}
