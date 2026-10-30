package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Plain JVM tests for the three wallpaper modes and for values read back from storage. */
public class WallpaperChoiceTest {

    @Test
    public void rotatingEverythingIsTheDefault() {
        WallpaperChoice choice = WallpaperChoice.all();
        assertTrue(choice.isAll());
        assertFalse(choice.isMood());
        assertFalse(choice.isPin());
        assertEquals("", choice.mood);
        assertEquals("", choice.key);
    }

    @Test
    public void aMoodModeNeedsAMood() {
        WallpaperChoice mood = WallpaperChoice.mood(" Night ");
        assertTrue(mood.isMood());
        assertEquals("Night", mood.mood);
        assertTrue(WallpaperChoice.mood("").isAll());
        assertTrue(WallpaperChoice.mood(null).isAll());
    }

    @Test
    public void aPinNeedsAPicture() {
        WallpaperChoice pin = WallpaperChoice.pin(" w-01.jpg ");
        assertTrue(pin.isPin());
        assertEquals("w-01.jpg", pin.key);
        assertTrue(WallpaperChoice.pin("  ").isAll());
        assertTrue(WallpaperChoice.pin(null).isAll());
    }

    @Test
    public void savedValuesFallBackToRotatingEverything() {
        assertTrue(WallpaperChoice.of("", "", "").isAll());
        assertTrue(WallpaperChoice.of("mood", "", "").isAll());
        assertTrue(WallpaperChoice.of("pin", "", "").isAll());
        assertTrue(WallpaperChoice.of("something", "Day", "w-01.jpg").isAll());
        assertEquals("Day", WallpaperChoice.of("mood", "Day", "").mood);
        assertEquals("w-01.jpg", WallpaperChoice.of("pin", "", "w-01.jpg").key);
    }
}
