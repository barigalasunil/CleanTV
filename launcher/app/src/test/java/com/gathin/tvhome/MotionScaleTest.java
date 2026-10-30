package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Plain JVM tests for the animator-scale arithmetic behind Motion.ms. */
public class MotionScaleTest {

    @Test
    public void normalSpeedKeepsTheDesignedDuration() {
        assertEquals(Motion.FAST, MotionScale.ms(Motion.FAST, 1f));
        assertEquals(Motion.MEDIUM, MotionScale.ms(Motion.MEDIUM, 1f));
    }

    @Test
    public void halfSpeedDoublesTheDuration() {
        assertEquals(Motion.FAST * 2, MotionScale.ms(Motion.FAST, 0.5f));
        assertEquals(Motion.SLOW * 2, MotionScale.ms(Motion.SLOW, 0.5f));
        assertEquals(320L, MotionScale.ms(160L, 0.5f));
    }

    @Test
    public void zeroTurnsMotionOff() {
        assertEquals(0L, MotionScale.ms(Motion.SLOW, 0f));
    }

    @Test
    public void theTvIsNeverToldToRunFasterThanDesigned() {
        assertEquals(160L, MotionScale.ms(160L, 2f));
        assertEquals(160L, MotionScale.ms(160L, 1.5f));
    }

    @Test
    public void anUnknownScaleIsFullSpeed() {
        assertEquals(160L, MotionScale.ms(160L, Float.NaN));
    }

    @Test
    public void aDurationOfZeroStaysZero() {
        assertEquals(0L, MotionScale.ms(0L, 0.5f));
        assertEquals(0L, MotionScale.ms(-10L, 1f));
    }
}
