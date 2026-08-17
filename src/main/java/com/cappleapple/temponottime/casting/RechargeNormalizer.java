package com.cappleapple.temponottime.casting;

/** Smooth, asymmetric recharge-duration normalization with a strictly monotonic tanh curve. */
public final class RechargeNormalizer {
    private static final double MINIMUM_SECONDS = 0.05;
    private static final double ORDERING_SLOPE = 1.0e-9;

    private RechargeNormalizer() {
    }

    public static double normalizeSeconds(double originalSeconds, boolean enabled, double normalSeconds,
                                          double shortStrength, double longStrength, double spreadSeconds) {
        double original = positiveFiniteOr(originalSeconds, MINIMUM_SECONDS);
        if (!enabled) {
            return original;
        }

        double normal = positiveFiniteOr(normalSeconds, 10.0);
        double spread = positiveFiniteOr(spreadSeconds, 8.0);
        double strength = original < normal ? finiteNonNegative(shortStrength) : finiteNonNegative(longStrength);
        if (strength == 0.0 || original == normal) {
            return original;
        }

        double scale = spread / strength;
        if (!Double.isFinite(scale) || scale <= 0.0) {
            return original;
        }

        double offset = original - normal;
        double curved = Math.tanh(offset / scale);
        // This smooth term is negligible near normal but preserves ordering after floating-point tanh saturation.
        double orderingTerm = ORDERING_SLOPE * offset * Math.pow(curved, 4.0);
        double normalized = normal + scale * curved + orderingTerm;
        return Double.isFinite(normalized) && normalized > 0.0 ? normalized : original;
    }

    public static double normalizeEffectiveTicks(double baseTicks, double effectiveTicks, boolean enabled,
                                                 double normalSeconds, double shortStrength,
                                                 double longStrength, double spreadSeconds) {
        double effective = positiveFiniteOr(effectiveTicks, 1.0);
        double base = positiveFiniteOr(baseTicks, effective);
        double normalizedBase = normalizeSeconds(base / 20.0, enabled, normalSeconds,
                shortStrength, longStrength, spreadSeconds) * 20.0;
        double normalizedEffective = normalizedBase * effective / base;
        return Double.isFinite(normalizedEffective) && normalizedEffective > 0.0
                ? Math.max(1.0, normalizedEffective) : 1.0;
    }

    private static double positiveFiniteOr(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0 ? value : fallback;
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 0.0;
    }
}
