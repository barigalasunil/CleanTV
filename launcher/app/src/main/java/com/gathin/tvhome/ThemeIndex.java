package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The index behind the wallpaper picker: the owner's {@code theme/wallpapers.json}, or the pictures
 * in {@code theme/wallpapers/} when that file is missing.
 *
 * <p>Pure Java on purpose: it is the one part of the theme that is worth testing without a TV. It
 * never touches Android and it never reads a file itself; the caller says which names exist.
 * Entries whose picture is gone are skipped, a malformed file is treated as missing, and the list
 * never grows past {@link #MAX} pictures.
 */
final class ThemeIndex {

    /** The most pictures the picker will ever list. */
    static final int MAX = 20;

    /** One wallpapers.json row. */
    static final class Entry {
        final String file;
        final String thumb;
        final String name;
        final String mood;

        Entry(String file, String thumb, String name, String mood) {
            this.file = file;
            this.thumb = thumb;
            this.name = name;
            this.mood = mood;
        }
    }

    private ThemeIndex() {
    }

    /**
     * Reads the index. Accepts a list or {@code {"wallpapers": [...]}}; every key but {@code file}
     * is optional. Entries with an unsafe file name, a duplicate or a picture that {@code exists}
     * rejects are left out. Returns an empty list when the text is missing or does not parse.
     */
    static List<Entry> parse(String text, Predicate<String> exists) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        Object root;
        try {
            root = new JSONTokener(text).nextValue();
        } catch (Exception e) {
            return Collections.emptyList();
        }
        JSONArray items;
        if (root instanceof JSONArray) {
            items = (JSONArray) root;
        } else if (root instanceof JSONObject) {
            items = ((JSONObject) root).optJSONArray("wallpapers");
        } else {
            return Collections.emptyList();
        }
        if (items == null) return Collections.emptyList();

        List<Entry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.length() && out.size() < MAX; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String file = name(item.optString("file", ""));
            if (file == null || !seen.add(file.toLowerCase(Locale.US))) continue;
            if (exists != null && !exists.test(file)) continue;
            String thumb = name(item.optString("thumb", ""));
            if (thumb != null && exists != null && !exists.test(thumb)) thumb = null;
            if (thumb == null) thumb = defaultThumb(file);
            String label = item.optString("name", "").trim();
            if (label.isEmpty()) label = withoutExtension(file);
            String mood = item.optString("mood", "").trim();
            out.add(new Entry(file, thumb, label, mood));
        }
        return out;
    }

    /**
     * The fallback: every picture in the folder, in name order, labelled by its file name.
     * The caller passes the names it found; this only sorts and caps them.
     */
    static List<Entry> scan(Collection<String> files) {
        if (files == null || files.isEmpty()) return Collections.emptyList();
        List<String> sorted = new ArrayList<>(files);
        Collections.sort(sorted, (a, b) -> a.compareToIgnoreCase(b));
        List<Entry> out = new ArrayList<>();
        for (String file : sorted) {
            if (out.size() == MAX) break;
            String clean = name(file);
            if (clean == null) continue;
            out.add(new Entry(clean, defaultThumb(clean), withoutExtension(clean), ""));
        }
        return out;
    }

    /** The preview that belongs to a picture: a name that starts with "w-" becomes "t-". */
    static String defaultThumb(String picture) {
        String name = picture;
        if (name.toLowerCase(Locale.US).startsWith("w-")) name = "t-" + name.substring(2);
        return withoutExtension(name) + ".jpg";
    }

    /** The file name part of a theme-relative path; null for empty or unsafe names. */
    static String name(String value) {
        if (value == null) return null;
        String name = value.trim().replace('\\', '/');
        if (name.isEmpty() || name.contains("..")) return null;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        if (name.isEmpty() || name.startsWith(".")) return null;
        return name;
    }

    static String withoutExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
