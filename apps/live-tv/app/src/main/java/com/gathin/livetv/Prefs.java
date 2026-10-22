package com.gathin.livetv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** The app's own settings: which languages to load, how often to refresh, and the last refresh time. */
final class Prefs {

    private static final String FILE = "livetv";
    private static final String KEY_LANGUAGES = "languages";
    private static final String KEY_REFRESH_HOURS = "refresh_hours";
    private static final String KEY_LAST_UPDATE = "last_update";

    /** What the launcher asks for on a fresh install: the user's own three languages. */
    static final Set<String> FALLBACK_LANGUAGES =
            new LinkedHashSet<>(Arrays.asList("tel", "hin", "eng"));

    private final SharedPreferences prefs;
    private final Context ctx;

    Prefs(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        prefs = this.ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** The languages from res/values/config.xml, which a fresh install starts with. */
    static Set<String> defaultLanguages(Context ctx) {
        Set<String> out = new LinkedHashSet<>(
                Arrays.asList(ctx.getResources().getStringArray(R.array.live_tv_languages)));
        return out.isEmpty() ? FALLBACK_LANGUAGES : out;
    }

    /** The language codes whose playlists are loaded, in the order the user picked them. */
    Set<String> languages() {
        Set<String> saved = prefs.getStringSet(KEY_LANGUAGES, null);
        if (saved == null || saved.isEmpty()) return defaultLanguages(ctx);
        return new LinkedHashSet<>(saved);
    }

    void setLanguages(Set<String> languages) {
        prefs.edit().putStringSet(KEY_LANGUAGES, new LinkedHashSet<>(languages)).apply();
    }

    /** How old a cached playlist may get before it is downloaded again. */
    int refreshHours() {
        return Math.max(1, prefs.getInt(KEY_REFRESH_HOURS, 12));
    }

    void setRefreshHours(int hours) {
        prefs.edit().putInt(KEY_REFRESH_HOURS, Math.max(1, hours)).apply();
    }

    /** When the playlists were last downloaded, in wall-clock milliseconds. */
    long lastUpdate() {
        return prefs.getLong(KEY_LAST_UPDATE, 0L);
    }

    void setLastUpdate(long when) {
        prefs.edit().putLong(KEY_LAST_UPDATE, when).apply();
    }
}
