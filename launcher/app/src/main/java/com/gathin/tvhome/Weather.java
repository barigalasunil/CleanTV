package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The weather on the home screen.
 *
 * <p>Open-Meteo is used because it needs no key and no account: the launcher only ever sends the
 * coordinates it was given in config.xml. The place behind those coordinates was looked up once,
 * during setup, with OpenStreetMap (see tools/pincode-to-coords.py); the pincode itself is never
 * sent anywhere at run time.
 *
 * <p>The answer is cached on the TV, so the panel has something to show before the network answers.
 * A reading older than half an hour is marked stale and shown dimmed when the network is away; no
 * request is made more than once every half hour.
 */
final class Weather {

    private static final String TAG = "TvHome.Weather";
    private static final String PREFS = "weather";
    private static final String KEY_JSON = "json";
    private static final String KEY_AT = "at";
    /** How long a reading is fresh; older readings are still shown, dimmed. */
    private static final long MAX_AGE_MS = 30 * 60_000L;
    private static final int TIMEOUT_MS = 8000;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** One day of the forecast. */
    static final class Day {
        final int index;
        final LocalDate date;
        final int code;
        final double max;
        final double min;
        final int rainChance;

        Day(int index, LocalDate date, int code, double max, double min, int rainChance) {
            this.index = index;
            this.date = date;
            this.code = code;
            this.max = max;
            this.min = min;
            this.rainChance = rainChance;
        }

        String label(Context ctx) {
            if (index == 0) return ctx.getString(R.string.weather_today);
            return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        }

        int icon() {
            return iconFor(code, true);
        }

        int tint() {
            return tintFor(code, true);
        }
    }

    /** One reading: current conditions and up to five forecast days. */
    static final class Report {
        final double temperature;
        final double feelsLike;
        final int humidity;
        final double wind;
        final int code;
        final boolean day;
        final long at;
        final List<Day> days;

        Report(double temperature, double feelsLike, int humidity, double wind, int code, boolean day,
               long at, List<Day> days) {
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.humidity = humidity;
            this.wind = wind;
            this.code = code;
            this.day = day;
            this.at = at;
            this.days = Collections.unmodifiableList(days);
        }

        /** True when the reading is old enough that it must not look live. */
        boolean stale() {
            return at <= 0L || System.currentTimeMillis() - at > MAX_AGE_MS;
        }

        String temperatureText() {
            return degrees(temperature);
        }

        String feelsText() {
            return degrees(feelsLike);
        }

        String windText() {
            return String.format(Locale.US, "%.0f km/h", wind);
        }

        int icon() {
            return iconFor(code, day);
        }

        int tint() {
            return tintFor(code, day);
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

    static int iconFor(int code, boolean day) {
        if (code == 0) return day ? R.drawable.ic_weather_sun : R.drawable.ic_weather_moon;
        if (code == 1 || code == 2) return day ? R.drawable.ic_weather_cloud_sun : R.drawable.ic_weather_cloud;
        if (code == 3) return R.drawable.ic_weather_cloud;
        if (code == 45 || code == 48) return R.drawable.ic_weather_fog;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.drawable.ic_weather_snow;
        if (code >= 95) return R.drawable.ic_weather_storm;
        return R.drawable.ic_weather_rain;
    }

    static int tintFor(int code, boolean day) {
        if (code == 0) return day ? 0xFFFFC24D : 0xFF9FB8FF;
        if (code == 1) return day ? 0xFFFFD479 : 0xFF8FA8D8;
        if (code == 2) return 0xFFD6DCE4;
        if (code == 3) return 0xFFB4BDC7;
        if (code == 45 || code == 48) return 0xFFC2C9D1;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 0xFFD7E7F4;
        if (code >= 95) return 0xFFB9A6FF;
        return 0xFF9FB6C9;
    }

    static String condition(Context ctx, int code) {
        int res;
        if (code == 0) res = R.string.weather_clear;
        else if (code == 1) res = R.string.weather_mainly_clear;
        else if (code == 2) res = R.string.weather_partly_cloudy;
        else if (code == 3) res = R.string.weather_cloudy;
        else if (code == 45 || code == 48) res = R.string.weather_fog;
        else if (code >= 51 && code <= 57) res = R.string.weather_drizzle;
        else if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) res = R.string.weather_rain;
        else if ((code >= 71 && code <= 77) || code == 85 || code == 86) res = R.string.weather_snow;
        else if (code >= 95) res = R.string.weather_storm;
        else res = R.string.weather_unknown;
        return ctx.getString(res);
    }

    /** The last reading, even when it is old, so the panel can keep showing it while offline. */
    static Report cached(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = prefs.getLong(KEY_AT, 0L);
        return parse(prefs.getString(KEY_JSON, null), at);
    }

    /**
     * Asks Open-Meteo for the weather and hands it to the callback on the main thread. The callback
     * gets null when the request failed; nothing is cached in that case.
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
            Log.i(TAG, String.format(Locale.US, "weather: %.0fC, code %d, day %s, %d forecast days",
                    report.temperature, report.code, report.day ? "yes" : "no", report.days.size()));
            MAIN.post(() -> callback.onWeather(report));
        });
    }

    /** Asks Open-Meteo for the current weather and the next five days at these coordinates. */
    private static String fetch(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,is_day,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&forecast_days=5&timezone=auto&wind_speed_unit=kmh";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "TvHome-launcher");
            if (conn.getResponseCode() != 200) {
                Log.w(TAG, "weather: HTTP " + conn.getResponseCode());
                return null;
            }
            StringBuilder out = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) out.append(line);
            }
            return out.toString();
        } catch (Exception e) {
            Log.w(TAG, "weather: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static Report parse(String json, long at) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(json);
            JSONObject current = root.optJSONObject("current");
            if (current == null) return null;
            double temperature = current.getDouble("temperature_2m");
            double feels = current.optDouble("apparent_temperature", temperature);
            int humidity = current.optInt("relative_humidity_2m", 0);
            double wind = current.optDouble("wind_speed_10m", 0);
            int code = current.optInt("weather_code", 0);
            boolean day = current.optInt("is_day", 1) == 1;
            return new Report(temperature, feels, humidity, wind, code, day, at, parseDays(root.optJSONObject("daily")));
        } catch (Exception e) {
            Log.w(TAG, "weather: " + e.getMessage());
            return null;
        }
    }

    private static List<Day> parseDays(JSONObject daily) {
        List<Day> days = new ArrayList<>();
        if (daily == null) return days;
        JSONArray dates = daily.optJSONArray("time");
        if (dates == null) return days;
        JSONArray codes = daily.optJSONArray("weather_code");
        JSONArray max = daily.optJSONArray("temperature_2m_max");
        JSONArray min = daily.optJSONArray("temperature_2m_min");
        JSONArray rain = daily.optJSONArray("precipitation_probability_max");
        for (int i = 0; i < dates.length() && days.size() < 5; i++) {
            try {
                LocalDate date = LocalDate.parse(dates.getString(i));
                days.add(new Day(days.size(), date, intAt(codes, i, 0), doubleAt(max, i, 0),
                        doubleAt(min, i, 0), intAt(rain, i, -1)));
            } catch (Exception ignored) {
                // A day the API could not date is simply left out of the five.
            }
        }
        return days;
    }

    private static double doubleAt(JSONArray array, int index, double fallback) {
        if (array == null || index >= array.length()) return fallback;
        Object v = array.opt(index);
        return v instanceof Number ? ((Number) v).doubleValue() : fallback;
    }

    private static int intAt(JSONArray array, int index, int fallback) {
        if (array == null || index >= array.length()) return fallback;
        Object v = array.opt(index);
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }
}
