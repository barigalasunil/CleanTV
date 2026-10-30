package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Plain JVM tests for the Open-Meteo code to condition word mapping. */
public class WeatherCodeTest {

    @Test
    public void clearAndCloudy() {
        assertEquals(WeatherCode.CLEAR, WeatherCode.key(0));
        assertEquals(WeatherCode.MAINLY_CLEAR, WeatherCode.key(1));
        assertEquals(WeatherCode.PARTLY_CLOUDY, WeatherCode.key(2));
        assertEquals(WeatherCode.CLOUDY, WeatherCode.key(3));
    }

    @Test
    public void fogAndDrizzle() {
        assertEquals(WeatherCode.FOG, WeatherCode.key(45));
        assertEquals(WeatherCode.FOG, WeatherCode.key(48));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(49));
        assertEquals(WeatherCode.DRIZZLE, WeatherCode.key(51));
        assertEquals(WeatherCode.DRIZZLE, WeatherCode.key(55));
        assertEquals(WeatherCode.DRIZZLE, WeatherCode.key(57));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(58));
    }

    @Test
    public void rainIncludesTheShowers() {
        assertEquals(WeatherCode.RAIN, WeatherCode.key(61));
        assertEquals(WeatherCode.RAIN, WeatherCode.key(67));
        assertEquals(WeatherCode.RAIN, WeatherCode.key(80));
        assertEquals(WeatherCode.RAIN, WeatherCode.key(82));
    }

    @Test
    public void snowIncludesTheSnowShowers() {
        assertEquals(WeatherCode.SNOW, WeatherCode.key(71));
        assertEquals(WeatherCode.SNOW, WeatherCode.key(75));
        assertEquals(WeatherCode.SNOW, WeatherCode.key(77));
        assertEquals(WeatherCode.SNOW, WeatherCode.key(85));
        assertEquals(WeatherCode.SNOW, WeatherCode.key(86));
    }

    @Test
    public void stormsTopTheTable() {
        assertEquals(WeatherCode.STORM, WeatherCode.key(95));
        assertEquals(WeatherCode.STORM, WeatherCode.key(99));
    }

    @Test
    public void everythingElseIsUnknown() {
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(-1));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(4));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(44));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(68));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(79));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(83));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(84));
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.key(94));
    }
}
