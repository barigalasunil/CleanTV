package com.gathin.livetv;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads an M3U playlist into channels.
 *
 * <p>The shape the iptv-org playlists use:
 * <pre>
 * #EXTM3U
 * #EXTINF:-1 tvg-id="9PlusNews.in@SD" tvg-logo="https://.../9plusnews.png" group-title="News",9 Plus News (1080p)
 * https://stream.example/9plusnews/index.m3u8
 * </pre>
 */
final class PlaylistParser {

    private PlaylistParser() {
    }

    /** Every playable entry in the text, in playlist order. */
    static List<Channel> parse(String text, String language) {
        List<Channel> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        String name = null;
        String logo = null;
        String group = null;

        for (String raw : text.split("\r?\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                name = null;
                logo = null;
                group = null;
                int comma = line.lastIndexOf(',');
                if (comma >= 0) {
                    name = line.substring(comma + 1).trim();
                    String attrs = line.substring(0, comma);
                    logo = attribute(attrs, "tvg-logo");
                    group = attribute(attrs, "group-title");
                }
                continue;
            }

            if (line.startsWith("#")) continue;   // #EXTM3U, #EXTGRP, comments

            // A URL line finishes the entry the last #EXTINF started.
            String label = name == null || name.isEmpty() ? line : name;
            out.add(new Channel(label, line, logo, group, language));
            name = null;
            logo = null;
            group = null;
        }
        return out;
    }

    /** The value of an attribute such as tvg-logo="https://...", or an empty string when it is absent. */
    private static String attribute(String attrs, String key) {
        int at = attrs.indexOf(key + "=\"");
        if (at < 0) return "";
        int from = at + key.length() + 2;
        int to = attrs.indexOf('"', from);
        if (to < 0) return "";
        return attrs.substring(from, to);
    }
}
