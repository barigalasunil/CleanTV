package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalTime;

/** The phase maths: sunrise and sunset day and night, blended at the edges. */
public class ThemeMathTest {

    private static ThemeMath.State compute(LocalTime now, String mode) {
        return ThemeMath.compute(now, LocalTime.of(6, 0), LocalTime.of(18, 0), 30,
                LocalTime.of(6, 0), LocalTime.of(18, 30), mode);
    }

    @Test
    public void forcedModes() {
        assertEquals(ThemeMath.Phase.DAY, ThemeMath.compute(LocalTime.of(12, 0), null, null, 30, null, null, "day").phase);
        assertEquals(ThemeMath.Phase.NIGHT, ThemeMath.compute(LocalTime.of(12, 0), null, null, 30, null, null, "night").phase);
        assertEquals(0f, compute(LocalTime.of(12, 0), "day").blend, 1e-6);
        assertEquals(1f, compute(LocalTime.of(12, 0), "night").blend, 1e-6);
    }

    @Test
    public void cleanDayAndNightAroundSunriseAndSunset() {
        // Sunrise 06:00, 30-minute blend: 03:00 is deep in the night, 09:00 fully in the day.
        assertEquals(ThemeMath.Phase.NIGHT, compute(LocalTime.of(3, 0), "auto").phase);
        assertEquals(ThemeMath.Phase.DAY, compute(LocalTime.of(9, 0), "auto").phase);
        // Sunset 18:00 with a 45-minute evening tail: 22:00 is night, 15:00 is day.
        assertEquals(ThemeMath.Phase.DAY, compute(LocalTime.of(15, 0), "auto").phase);
        assertEquals(ThemeMath.Phase.NIGHT, compute(LocalTime.of(22, 0), "auto").phase);
    }

    @Test
    public void dawnBlendRunsFromNightToDay() {
        // Before sunrise still night, at sunrise half-way, after it day.
        assertEquals(1f, compute(LocalTime.of(5, 30), "auto").blend, 1e-6);
        assertEquals(0.5f, compute(LocalTime.of(6, 0), "auto").blend, 0.01f);
        assertEquals(0f, compute(LocalTime.of(6, 30), "auto").blend, 1e-6);
        assertTrue(compute(LocalTime.of(6, 0), "auto").blending);
        assertFalse(compute(LocalTime.of(9, 0), "auto").blending);
    }

    @Test
    public void duskBlendRunsFromDayToNight() {
        // Evening: dusk starts 45 minutes before sunset (a 30-minute setting, times 1.5).
        assertEquals(0f, compute(LocalTime.of(17, 14), "auto").blend, 1e-6);
        assertTrue(compute(LocalTime.of(17, 15), "auto").blend < compute(LocalTime.of(18, 0), "auto").blend);
        assertEquals(1f, compute(LocalTime.of(18, 45), "auto").blend, 1e-6);
        assertTrue(compute(LocalTime.of(18, 0), "auto").blending);
    }

    @Test
    public void warmTintPeaksAtTheSun() {
        assertEquals(1f, compute(LocalTime.of(6, 0), "auto").warm, 0.01f);
        assertEquals(1f, compute(LocalTime.of(18, 0), "auto").warm, 0.01f);
        assertEquals(0f, compute(LocalTime.of(12, 0), "auto").warm, 1e-6);
        assertEquals(0f, compute(LocalTime.of(22, 0), "auto").warm, 1e-6);
    }

    @Test
    public void noSunDataFallsBackToFixedHours() {
        ThemeMath.State state = ThemeMath.compute(LocalTime.of(19, 0), null, null, 30,
                LocalTime.of(6, 0), LocalTime.of(18, 30), "auto");
        // 19:00 is still inside the 45-minute evening blend around the fixed night time.
        assertTrue(state.blend >= 0.5f);
        ThemeMath.State night = ThemeMath.compute(LocalTime.of(20, 30), null, null, 30,
                LocalTime.of(6, 0), LocalTime.of(18, 30), "auto");
        assertEquals(ThemeMath.Phase.NIGHT, night.phase);
        ThemeMath.State day = ThemeMath.compute(LocalTime.of(12, 0), null, null, 30,
                LocalTime.of(6, 0), LocalTime.of(18, 30), "auto");
        assertEquals(ThemeMath.Phase.DAY, day.phase);
        // The blend around the fixed night time still happens.
        ThemeMath.State soft = ThemeMath.compute(LocalTime.of(18, 30), null, null, 30,
                LocalTime.of(6, 0), LocalTime.of(18, 30), "auto");
        assertEquals(0.5f, soft.blend, 0.01f);
        assertTrue(soft.blending);
    }

    @Test
    public void midnightNeverBreaksTheNight() {
        assertEquals(ThemeMath.Phase.NIGHT, compute(LocalTime.of(0, 0), "auto").phase);
        assertEquals(1f, compute(LocalTime.of(0, 0), "auto").blend, 1e-6);
        assertEquals(ThemeMath.Phase.NIGHT, compute(LocalTime.of(23, 59), "auto").phase);
    }

    @Test
    public void clockIsParsedLeniently() {
        assertEquals(LocalTime.of(6, 0), ThemeMath.clock("06:00", LocalTime.of(7, 0)));
        assertEquals(LocalTime.of(6, 0), ThemeMath.clock("6:00", LocalTime.of(7, 0)));
        assertEquals(LocalTime.of(7, 0), ThemeMath.clock("nonsense", LocalTime.of(7, 0)));
        assertEquals(LocalTime.of(7, 0), ThemeMath.clock("", LocalTime.of(7, 0)));
    }
}
