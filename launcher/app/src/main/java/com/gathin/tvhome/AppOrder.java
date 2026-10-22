package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * The order of the app grid.
 *
 * <p>The order the owner made on the TV is kept here, as the list of app keys (the flattened
 * component name) in the order they should sit in. Apps that are no longer installed are dropped
 * when the list is read, and apps that were installed later go to the end.
 */
final class AppOrder {

    private static final String PREFS = "grid_order";
    private static final String KEY = "order";
    private static final String SEP = "\n";

    private AppOrder() {
    }

    /** The saved order, oldest first; empty when the owner never arranged the grid. */
    static List<String> load(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY, "");
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String part : raw.split(SEP)) {
            String key = part.trim();
            if (!key.isEmpty() && !out.contains(key)) out.add(key);
        }
        return out;
    }

    /** Saves the order; only the keys that are still there are kept. */
    static void save(Context ctx, List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(SEP);
            sb.append(key.trim());
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, sb.toString()).apply();
    }
}
