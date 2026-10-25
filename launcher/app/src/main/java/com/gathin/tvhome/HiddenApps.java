package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The apps the owner hid from the home grid with the tile menu.
 *
 * <p>This is only a display choice: nothing is disabled or uninstalled. The list lives in the
 * launcher's own SharedPreferences, separate from the curated hidden_apps in config.xml, so a theme
 * can never delete an app the owner hid, and the owner can always unhide it again.
 */
final class HiddenApps {

    private static final String PREFS = "hidden_apps";
    private static final String KEY = "packages";

    private HiddenApps() {
    }

    static Set<String> load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> stored = p.getStringSet(KEY, null);
        return stored == null ? Collections.emptySet() : new HashSet<>(stored);
    }

    static boolean hide(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        Set<String> all = new HashSet<>(load(ctx));
        if (!all.add(pkg)) return false;
        return save(ctx, all);
    }

    static boolean unhide(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        Set<String> all = new HashSet<>(load(ctx));
        if (!all.remove(pkg)) return false;
        return save(ctx, all);
    }

    private static boolean save(Context ctx, Set<String> all) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY, all).commit();
    }
}
