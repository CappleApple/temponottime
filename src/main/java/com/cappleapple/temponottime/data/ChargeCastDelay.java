package com.cappleapple.temponottime.data;

public record ChargeCastDelay(int durationTicks, int remainingTicks) {
    public ChargeCastDelay {
        durationTicks = Math.max(1, durationTicks);
        remainingTicks = Math.clamp(remainingTicks, 0, durationTicks);
    }
    public float remainingFraction() { return (float) remainingTicks / durationTicks; }
    public ChargeCastDelay tick() { return new ChargeCastDelay(durationTicks, remainingTicks - 1); }
}
