package com.cappleapple.temponottime.casting;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimingNormalizationTest {
    @Test void signedOffsetsPrecedeSpeedModifiersEvenWithoutTheCurve() {
        assertEquals(60, new TimingNormalization(false, 2, 1, .8, .5, .8).castTicks(80, 40));
        assertEquals(20, new TimingNormalization(false, -2, 1, .8, .5, .8).castTicks(80, 40));
        assertEquals(1, new TimingNormalization(false, -100, 1, .8, .5, .8).castTicks(80, 40));
    }

    @Test void curveOperatesOnAdjustedBaseBeforeSpeedRatio() {
        var settings = new TimingNormalization(true, 1, 3, .8, .5, .8);
        assertEquals(30, settings.castTicks(40, 20)); // 2s + 1s at curve center, then 50% speed ratio.
        assertTrue(settings.castTicks(200, 200) < 220);
        assertTrue(settings.castTicks(1, 1) > 21);
    }

    @Test void zeroTimeSpellsRequireOptIn() {
        var defaults = new TimingNormalization(true, 1, 1, .8, .5, .8);
        assertEquals(0, defaults.castTicks(0, 0));
        assertEquals(0, defaults.castTicks(40, 0));
        var optedIn = new TimingNormalization(false, 1, 1, .8, .5, .8, false, true);
        assertEquals(20, optedIn.castTicks(0, 0));
        assertEquals(20, optedIn.castTicks(40, 0));
        assertEquals(0, new TimingNormalization(false, 0, 1, .8, .5, .8, true, true).castTicks(0, 0));
    }

    @Test void defaultsPreserveTimingAndLongDurationsSaturate() {
        assertEquals(43, TimingNormalization.UNCHANGED.castTicks(80, 43));
        assertEquals(0, TimingNormalization.UNCHANGED.castTicks(0, 0));
        assertEquals(Integer.MAX_VALUE, new TimingNormalization(false, 86400, 1, 0, 0, 1).adjustedBaseTicks(Integer.MAX_VALUE));
    }

    @Test void externalDebtPreservesNativeReductionAfterOffset() {
        var settings = new TimingNormalization(false, 2, 10, .8, .5, 8);
        assertEquals(120, ExternalManaPolicy.rechargeDuration(200, 100, settings));
        assertEquals(80, ExternalManaPolicy.rechargeDuration(200, 100,
                new TimingNormalization(false, -2, 10, .8, .5, 8)));
    }
}
