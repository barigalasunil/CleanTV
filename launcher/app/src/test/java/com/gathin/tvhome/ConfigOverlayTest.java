package com.gathin.tvhome;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Plain JVM tests for the theme overlay: every key is optional and a bad key keeps the default. */
public class ConfigOverlayTest {

    private static JSONObject json(String text) throws Exception {
        return new JSONObject(text);
    }

    @Test
    public void anEmptyOverlayKeepsEveryConfigXmlValue() throws Exception {
        JSONObject o = json("{}");
        assertEquals("Home", ConfigOverlay.string(o, "greeting_name", "Home"));
        assertTrue(ConfigOverlay.bool(o, "pill_show_time", true));
        assertEquals(4, ConfigOverlay.integer(o, "columns", 4, 1, 8));
        assertEquals(19.07, ConfigOverlay.coordinate(o, "weather_lat", 19.07), 0.0001);
        assertEquals(Collections.singletonList("com.a"), ConfigOverlay.stringList(o, "dock_apps",
                Collections.singletonList("com.a")));
    }

    @Test
    public void valuesOfTheRightTypeReplaceTheDefaults() throws Exception {
        JSONObject o = json("{\"greeting_name\":\"  Sam  \",\"pill_show_time\":false,"
                + "\"columns\":6,\"weather_lat\":18.5}");
        assertEquals("Sam", ConfigOverlay.string(o, "greeting_name", "Home"));
        assertFalse(ConfigOverlay.bool(o, "pill_show_time", true));
        assertEquals(6, ConfigOverlay.integer(o, "columns", 4, 1, 8));
        assertEquals(18.5, ConfigOverlay.coordinate(o, "weather_lat", 0), 0.0001);
    }

    @Test
    public void keysOfTheWrongTypeAreIgnored() throws Exception {
        JSONObject o = json("{\"greeting_name\":7,\"pill_show_time\":\"yes\",\"columns\":\"six\","
                + "\"weather_lat\":true,\"dock_apps\":\"com.a\"}");
        assertEquals("Home", ConfigOverlay.string(o, "greeting_name", "Home"));
        assertTrue(ConfigOverlay.bool(o, "pill_show_time", true));
        assertEquals(4, ConfigOverlay.integer(o, "columns", 4, 1, 8));
        assertEquals(1.5, ConfigOverlay.coordinate(o, "weather_lat", 1.5), 0.0001);
        assertEquals(Collections.singletonList("com.a"), ConfigOverlay.stringList(o, "dock_apps",
                Collections.singletonList("com.a")));
    }

    @Test
    public void numbersOutsideTheirRangeKeepTheDefault() throws Exception {
        assertEquals(4, ConfigOverlay.integer(json("{\"columns\":0}"), "columns", 4, 1, 8));
        assertEquals(4, ConfigOverlay.integer(json("{\"columns\":9}"), "columns", 4, 1, 8));
        assertEquals(0, ConfigOverlay.integer(json("{\"rows\":0}"), "rows", 2, 0, 3));
        assertEquals(4, ConfigOverlay.integer(json("{\"columns\":4.9}"), "columns", 4, 1, 8));
    }

    @Test
    public void aCoordinateMayBeWrittenAsAString() throws Exception {
        assertEquals(19.07, ConfigOverlay.coordinate(json("{\"weather_lat\":\"19.07\"}"),
                "weather_lat", 0), 0.0001);
        assertEquals(51.5, ConfigOverlay.coordinate(json("{\"weather_lat\":\"north\"}"),
                "weather_lat", 51.5), 0.0001);
    }

    @Test
    public void coordinatesFromConfigXmlAreNumbersOrNaN() {
        assertEquals(19.07, ConfigOverlay.number("19.07"), 0.0001);
        assertEquals(19.07, ConfigOverlay.number(" 19.07 "), 0.0001);
        assertTrue(Double.isNaN(ConfigOverlay.number("")));
        assertTrue(Double.isNaN(ConfigOverlay.number(null)));
        assertEquals(3.5, ConfigOverlay.number("x", 3.5), 0.0001);
    }

    @Test
    public void listsDropEmptyAndNonStringItems() throws Exception {
        JSONObject o = json("{\"dock_apps\":[\"com.a\",\"\",\"  \",5,\"com.b\",null]}");
        List<String> out = ConfigOverlay.stringList(o, "dock_apps", Collections.emptyList());
        assertEquals(Arrays.asList("com.a", "com.b"), out);
    }

    @Test
    public void alignmentIsAlwaysOneOfThree() {
        assertEquals("left", ConfigOverlay.align(" LEFT "));
        assertEquals("right", ConfigOverlay.align("right"));
        assertEquals("center", ConfigOverlay.align("middle"));
        assertEquals("center", ConfigOverlay.align(null));
        assertEquals("center", ConfigOverlay.align(""));
    }

    @Test
    public void clampingHoldsTheSplashInsideTheScreen() {
        assertEquals(0, ConfigOverlay.clamp(-5, 0, 60));
        assertEquals(60, ConfigOverlay.clamp(90, 0, 60));
        assertEquals(42, ConfigOverlay.clamp(42, 0, 60));
    }
}
