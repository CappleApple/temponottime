package com.cappleapple.temponottime.casting;

import java.util.Collection;

public final class CapacityCalculator {
    private CapacityCalculator() {
    }

    public static double used(Collection<Double> costs) {
        double total = 0.0;
        for (double cost : costs) {
            if (Double.isFinite(cost) && cost > 0.0) {
                total = Math.min(Double.MAX_VALUE, total + cost);
            }
        }
        return total;
    }

    public static double free(double maximum, double used) {
        double safeMaximum = Double.isFinite(maximum) ? Math.max(0.0, maximum) : 0.0;
        double safeUsed = Double.isFinite(used) ? Math.max(0.0, used) : safeMaximum;
        return Math.max(0.0, safeMaximum - safeUsed);
    }

    public static boolean canReserve(double maximum, double used, double prospectiveCost, boolean allowOvercapacitySingleCast) {
        if (!Double.isFinite(prospectiveCost) || prospectiveCost < 0.0) {
            return false;
        }
        double safeMaximum = Double.isFinite(maximum) ? Math.max(0.0, maximum) : 0.0;
        double safeUsed = Double.isFinite(used) ? Math.max(0.0, used) : safeMaximum;
        if (allowOvercapacitySingleCast && safeUsed == 0.0 && prospectiveCost > safeMaximum) {
            return true;
        }
        return prospectiveCost <= free(safeMaximum, safeUsed) + 1.0e-7;
    }
}
