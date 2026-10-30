package com.gathin.tvhome;

/**
 * What the owner asked the home screen and the screensaver to do with the pictures: rotate all of
 * them, rotate one mood, or pin a single picture. Pure Java, so the three modes and the cleanup of
 * values read back from storage can be tested without a TV.
 */
final class WallpaperChoice {

    static final String ALL = "all";
    static final String MOOD = "mood";
    static final String PIN = "pin";

    final String mode;
    final String mood;
    final String key;

    private WallpaperChoice(String mode, String mood, String key) {
        this.mode = mode;
        this.mood = mood;
        this.key = key;
    }

    static WallpaperChoice all() {
        return new WallpaperChoice(ALL, "", "");
    }

    static WallpaperChoice mood(String mood) {
        String clean = mood == null ? "" : mood.trim();
        return clean.isEmpty() ? all() : new WallpaperChoice(MOOD, clean, "");
    }

    static WallpaperChoice pin(String key) {
        String clean = key == null ? "" : key.trim();
        return clean.isEmpty() ? all() : new WallpaperChoice(PIN, "", clean);
    }

    /**
     * The saved values, with anything unusable falling back to rotating all: an unknown mode, a
     * mood mode without a mood, or a pin without a picture all mean "just rotate everything".
     */
    static WallpaperChoice of(String mode, String mood, String key) {
        if (PIN.equals(mode)) return pin(key);
        if (MOOD.equals(mode)) return mood(mood);
        return all();
    }

    boolean isAll() {
        return ALL.equals(mode);
    }

    boolean isMood() {
        return MOOD.equals(mode);
    }

    boolean isPin() {
        return PIN.equals(mode);
    }
}
