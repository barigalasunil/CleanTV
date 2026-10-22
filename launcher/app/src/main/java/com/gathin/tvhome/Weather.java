package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
 * <p>The answer is cached on the TV, so the pill has something to show before the network answers
 * and no request is made more than once every half hour.
 */
final class Weather {

    private static final String TAG = "TvHome.Weather";
    private static final String PREFS = "weather";
    private static final String KEY_JSON = "json";
    private static final String KEY_AT = "at";
    /** How long a reading is used before a new one is asked for. */
    private static final long MAX_AGE_MS = 30 * 60_000L;
    private static final int TIMEOUT_MS = 8000;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** One reading: the temperature in degrees, the weather code and whether it is day. */
    static final class Report {
        final double temperature;
        final int code;
        final boolean day;

        Report(double temperature, int code, boolean day) {
            this.temperature = temperature;
            this.code = code;
            this.day = day;
        }

        /** The temperature as the pill shows it: a whole number and the degree sign. */
        String temperatureText() {
            return String.format(java.util.Locale.US, "%d°", Math.round(temperature));
        }

        /** The icon for this weather. */
        int icon() {
            if (code == 0) return day ? R.drawable.ic_weather_sun : R.drawable.ic_weather_moon;
            if (code == 1 || code == 2) return day ? R.drawable.ic_weather_cloud_sun : R.drawable.ic_weather_cloud;
            if (code == 3) return R.drawable.ic_weather_cloud;
            if (code == 45 || code == 48) return R.drawable.ic_weather_fog;
            if ((code >= 71 && code <= 77) || code == 85 || code == 86) return R.drawable.ic_weather_snow;
            if (code >= 95) return R.drawable.ic_weather_storm;
            return R.drawable.ic_weather_rain;
        }

        /** The colour of the icon and the text: warm and bright in the sun, cool and dim at night. */
        int tint() {
            if (code == 0) return day ? 0xFFFFC24D : 0xFF9FB8FF;
            if (code == 1) return day ? 0xFFFFD479 : 0xFF8FA8D8;
            if (code == 2) return 0xFFD6DCE4;
            if (code == 3) return 0xFFB4BDC7;
            if (code == 45 || code == 48) return 0xFFC2C9D1;
            if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 0xFFD7E7F4;
            if (code >= 95) return 0xFFB9A6FF;
            return 0xFF9FB6C9;
        }

        /** How bright the pill's text is: a clear day is the brightest, a storm the dimmest. */
        float brightness() {
            if (code == 0) return day ? 1f : 0.7f;
            if (code == 1) return day ? 0.95f : 0.68f;
            if (code == 2) return 0.85f;
            if (code == 3) return 0.75f;
            if (code == 45 || code == 48) return 0.72f;
            if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 0.85f;
            if (code >= 95) return 0.8f;
            return 0.72f;
        }
    }

    interface Callback {
        void onWeather(Report report);
    }

    private Weather() {
    }

    /** The cached reading, or null when there is none or it is too old. */
    static Report cached(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = prefs.getLong(KEY_AT, 0L);
        if (at <= 0L || System.currentTimeMillis() - at > MAX_AGE_MS) return null;
        return parse(prefs.getString(KEY_JSON, null));
    }

    /**
     * Asks Open-Meteo for the weather and hands it to the callback on the main thread. Nothing
     * happens when the coordinates are not known.
     */
    static void refresh(Context ctx, double lat, double lon, Callback callback) {
        if (Double.isNaN(lat) || Double.isNaN(lon)) return;
        IO.execute(() -> {
            String json = fetch(lat, lon);
            if (json == null) return;
            Report report = parse(json);
            if (report == null) return;
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_JSON, json)
                    .putLong(KEY_AT, System.currentTimeMillis())
                    .apply();
            Log.i(TAG, String.format(java.util.Locale.US, "weather: %.0fC, code %d, day %s",
                    report.temperature, report.code, report.day ? "yes" : "no"));
            MAIN.post(() -> callback.onWeather(report));
        });
    }

    /** Asks Open-Meteo for the current weather at these coordinates. */
    private static String fetch(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,is_day,weather_code&timezone=auto";
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

    /** Reads the answer: the temperature, the day flag and the weather code. */
    private static Report parse(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject current = new JSONObject(json).getJSONObject("current");
            double temperature = current.getDouble("temperature_2m");
            int code = current.optInt("weather_code", 0);
            boolean day = current.optInt("is_day", 1) == 1;
            return new Report(temperature, code, day);
        } catch (Exception e) {
            Log.w(TAG, "weather: " + e.getMessage());
            return null;
        }
    }
}
