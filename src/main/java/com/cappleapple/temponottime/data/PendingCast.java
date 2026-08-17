package com.cappleapple.temponottime.data;

public record PendingCast(String spellId, int spellLevel, double castingDraw, double durationTicks,
                          boolean occupiesCastingReserve, boolean appliesLoad) {
}
