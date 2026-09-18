package com.cappleapple.temponottime.casting;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChargeCastDelayCalculatorTest {
    @Test void defaultsUseTenPercentOfCastTimePlusHalfASecond() {
        assertEquals(10, ChargeCastDelayCalculator.ticks(0, .5, 10, .1, 10));
        assertEquals(14, ChargeCastDelayCalculator.ticks(40, .5, 10, .1, 10));
        assertEquals(24, ChargeCastDelayCalculator.ticks(140, .5, 10, .1, 10));
    }
    @Test void signedBaseAndBoundsApplyBeforeTickRounding() {
        assertEquals(2, ChargeCastDelayCalculator.ticks(40, -5, 10, .1, 10));
        assertEquals(200, ChargeCastDelayCalculator.ticks(10000, .5, 10, .1, 10));
        assertEquals(11, ChargeCastDelayCalculator.ticks(1, .5, 10, .1, 10));
        assertEquals(0, ChargeCastDelayCalculator.ticks(0, -1, 10, 0, 10));
        assertEquals(40, ChargeCastDelayCalculator.ticks(0, 0, 10, 2, 1));
    }
}
