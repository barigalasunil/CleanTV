package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Today's weather for the home screen.
 *
 * <p>Open-Meteo is used because it needs no key and no account: the launcher only ever sends the
 * coordinates it was given in config.xml. The place behind those coordinates was looked up once,
 * during setup, with OpenStreetMap (see tools/pincode-to-coords.py); the pincode itself is never
 * sent anywhere at run time.
 *
 * <p>One request, current conditions only: the temperature, the weather code and whether it is day.
 * The answer is cached on the TV, so the widget has something to show before the network answers,
 * and a failed request leaves the last reading in place without a word.
 */
final class Weather {

    private static final String PREFS = "weather";
    private static final String KEY_JSON = "json";
    private static final String KEY_AT = "at";
    /** How long a reading is fresh; the home screen asks again after this. */
    static final long MAX_AGE_MS = 30 * 60_000L;
    private static final int TIMEOUT_MS = 8000;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** One reading: what it is like outside right now. */
    static final class Report {
        final double temperature;
        final int code;
        final boolean day;
        final long at;

        Report(double temperature, int code, boolean day, long at) {
            this.temperature = temperature;
            this.code = code;
            this.day = day;
            this.at = at;
        }

        /** True when the reading is old enough that it is worth asking again. */
        boolean stale() {
            return at <= 0L || System.currentTimeMillis() - at > MAX_AGE_MS;
        }

        String temperatureText() {
            return degrees(temperature);
        }
    }

    interface Callback {
        /** The new reading, or null when it could not be fetched (the old one is kept). */
        void onWeather(Report report);
    }

    private Weather() {
    }

    static String degrees(double value) {
        return String.format(Locale.US, "%d°", Math.round(value));
    }

    /** The condition word for a weather code; the grouping itself is in {@link WeatherCode}. */
    static String condition(Context ctx, int code) {
        return ctx.getString(resource(code));
    }

    /** The string resource for a weather code. */
    private static int resource(int code) {
        switch (WeatherCode.key(code)) {
            case WeatherCode.CLEAR: return R.string.weather_clear;
            case WeatherCode.MAINLY_CLEAR: return R.string.weather_mainly_clear;
            case WeatherCode.PARTLY_CLOUDY: return R.string.weather_partly_cloudy;
            case WeatherCode.CLOUDY: return R.string.weather_cloudy;
            case WeatherCode.FOG: return R.string.weather_fog;
            case WeatherCode.DRIZZLE: return R.string.weather_drizzle;
            case WeatherCode.RAIN: return R.string.weather_rain;
            case WeatherCode.SNOW: return R.string.weather_snow;
            case WeatherCode.STORM: return R.string.weather_storm;
            default: return R.string.weather_unknown;
        }
    }

    /** The last reading, even when it is old, so the widget keeps showing it while offline. */
    static Report cached(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = prefs.getLong(KEY_AT, 0L);
        return parse(prefs.getString(KEY_JSON, null), at);
    }

    /**
     * Asks Open-Meteo for the weather and hands it to the callback on the main thread. The callback
     * gets null when the request failed; nothing is cached in that case and nothing is logged.
     */
    static void refresh(Context ctx, double lat, double lon, Callback callback) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) return;
        IO.execute(() -> {
            String json = fetch(lat, lon);
            Report report = json == null ? null : parse(json, System.currentTimeMillis());
            if (report == null) {
                MAIN.post(() -> callback.onWeather(null));
                return;
            }
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_JSON, json)
                    .putLong(KEY_AT, report.at)
                    .apply();
            MAIN.post(() -> callback.onWeather(report));
        });
    }

    /** Asks Open-Meteo for the current temperature, weather code and day/night at these coordinates. */
    private static String fetch(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code,is_day&timezone=auto";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "TvHome-launcher");
            if (conn.getResponseCode() != 200) return null;
            StringBuilder out = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) out.append(line);
            }
            return out.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static Report parse(String json, long at) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject current = new JSONObject(json).optJSONObject("current");
            if (current == null) return null;
            return new Report(current.getDouble("temperature_2m"),
                    current.optInt("weather_code", 0),
                    current.optInt("is_day", 1) == 1,
                    at);
        } catch (Exception e) {
            return null;
        }
    }
}
