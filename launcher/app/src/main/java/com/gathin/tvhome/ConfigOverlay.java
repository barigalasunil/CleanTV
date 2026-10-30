package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The readers that lay {@code theme/config.json} over the values in res/values/config.xml.
 *
 * <p>Every key is optional, and a key that is missing, of the wrong type or outside its range keeps
 * the value config.xml gave. That rule is the whole of the overlay, so it lives here as plain Java
 * where it can be tested without a TV or an Android runtime; {@link Config} only adds the colour
 * parser and the constructor.
 */
final class ConfigOverlay {

    private ConfigOverlay() {
    }

    /** A trimmed string; the fallback when the key is not a string. */
    static String string(JSONObject o, String key, String fallback) {
        Object v = o.opt(key);
        return v instanceof String ? ((String) v).trim() : fallback;
    }

    /** A boolean; the fallback when the key is not a boolean. */
    static boolean bool(JSONObject o, String key, boolean fallback) {
        Object v = o.opt(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    /** A whole number inside its range; the fallback when it is missing, not a number or out of range. */
    static int integer(JSONObject o, String key, int fallback, int min, int max) {
        Object v = o.opt(key);
        if (!(v instanceof Number)) return fallback;
        int i = ((Number) v).intValue();
        return i < min || i > max ? fallback : i;
    }

    /** A coordinate written as a JSON number (the usual case) or as a string. */
    static double coordinate(JSONObject o, String key, double fallback) {
        Object v = o.opt(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) return number((String) v, fallback);
        return fallback;
    }

    /** A number from config.xml, where every value is a string; NaN when it is missing. */
    static double number(String value) {
        return number(value, Double.NaN);
    }

    static double number(String value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** The non-empty strings in an array; the fallback when the key is not an array. */
    static List<String> stringList(JSONObject o, String key, List<String> fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (item instanceof String && !((String) item).trim().isEmpty()) out.add(((String) item).trim());
        }
        return out;
    }

    /** The splash message alignment, always one of the three the layout understands. */
    static String align(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if ("left".equals(v) || "right".equals(v)) return v;
        return "center";
    }

    static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
