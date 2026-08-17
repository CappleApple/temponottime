package com.cappleapple.temponottime.config;

public record SpellOverride(Double castingDraw, Integer maxCharges, double cooldownMultiplier,
                            Boolean chargesAllowed, Boolean occupiesCastingReserve, Boolean appliesLoad) {
    public static final SpellOverride DEFAULT = new SpellOverride(null, null, 1.0, null, null, null);

    public boolean chargesAllowed(boolean fallback) {
        return chargesAllowed == null ? fallback : chargesAllowed;
    }

    public boolean occupiesCastingReserve(boolean fallback) {
        return occupiesCastingReserve == null ? fallback : occupiesCastingReserve;
    }

    public boolean appliesLoad(boolean fallback) {
        return appliesLoad == null ? fallback : appliesLoad;
    }
}
