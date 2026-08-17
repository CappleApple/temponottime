package com.cappleapple.temponottime.casting;

public final class ChargeCalculator {
    private ChargeCalculator() {
    }

    public static int maxCharges(double capacity, double cost, int minimum, int maximum, double zeroCostFallback) {
        int safeMinimum = Math.max(1, minimum);
        int safeMaximum = Math.max(safeMinimum, maximum);
        double safeCapacity = finiteNonNegative(capacity);
        double safeCost = Double.isFinite(cost) && cost > 0.0 ? cost : Math.max(0.0001, finitePositive(zeroCostFallback, 1.0));
        double quotient = Math.floor(safeCapacity / safeCost);
        if (!Double.isFinite(quotient) || quotient >= safeMaximum) {
            return safeMaximum;
        }
        return Math.clamp((int) quotient, safeMinimum, safeMaximum);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0 ? value : fallback;
    }
}
