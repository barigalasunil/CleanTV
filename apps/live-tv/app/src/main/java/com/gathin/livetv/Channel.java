package com.gathin.livetv;

/** One playable channel: what the list shows and what the player needs. */
final class Channel {

    /** The name shown on the card, e.g. "9 Plus News (1080p)". */
    final String name;
    /** The stream URL. */
    final String url;
    /** The logo URL, often empty. */
    final String logo;
    /** The group the playlist filed it under, e.g. "News". */
    final String group;
    /** The playlist language it came from, e.g. "tel". */
    final String language;

    Channel(String name, String url, String logo, String group, String language) {
        this.name = name;
        this.url = url;
        this.logo = logo == null ? "" : logo;
        this.group = group == null ? "" : group;
        this.language = language;
    }

    /** The text on the card: the name, or the stream URL when the playlist had no name for it. */
    String label() {
        return name == null || name.isEmpty() ? url : name;
    }
}
