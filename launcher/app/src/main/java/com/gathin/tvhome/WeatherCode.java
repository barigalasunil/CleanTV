package com.gathin.tvhome;

/**
 * The weather codes Open-Meteo reports, grouped into the few conditions the widget can name.
 *
 * <p>Pure Java on purpose: the grouping is the interesting part and it is worth testing without a
 * TV. The word itself comes from the string resources, chosen in {@link Weather#condition}.
 */
final class WeatherCode {

    static final String CLEAR = "clear";
    static final String MAINLY_CLEAR = "mainly_clear";
    static final String PARTLY_CLOUDY = "partly_cloudy";
    static final String CLOUDY = "cloudy";
    static final String FOG = "fog";
    static final String DRIZZLE = "drizzle";
    static final String RAIN = "rain";
    static final String SNOW = "snow";
    static final String STORM = "storm";
    static final String UNKNOWN = "unknown";

    private WeatherCode() {
    }

    /** The condition for a WMO weather code; anything not in the table is unknown. */
    static String key(int code) {
        if (code == 0) return CLEAR;
        if (code == 1) return MAINLY_CLEAR;
        if (code == 2) return PARTLY_CLOUDY;
        if (code == 3) return CLOUDY;
        if (code == 45 || code == 48) return FOG;
        if (code >= 51 && code <= 57) return DRIZZLE;
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return RAIN;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if (code >= 95) return STORM;
        return UNKNOWN;
    }
}
