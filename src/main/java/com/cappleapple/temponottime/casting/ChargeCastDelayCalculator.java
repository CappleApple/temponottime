package com.cappleapple.temponottime.casting;

public final class ChargeCastDelayCalculator {
    private ChargeCastDelayCalculator() { }

    public static int ticks(double castTicks, double flatSeconds, double percentage,
                            double minimumSeconds, double maximumSeconds) {
        double minimum = Math.max(0, finite(minimumSeconds));
        double maximum = Math.max(minimum, finite(maximumSeconds));
        double seconds = finite(flatSeconds) + Math.max(0, finite(castTicks)) / 20.0 * finite(percentage) / 100.0;
        return (int) Math.clamp(Math.ceil(Math.clamp(seconds, minimum, maximum) * 20.0), 0L, Integer.MAX_VALUE);
    }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0; }
}
