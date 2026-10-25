package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The launcher's settings: the values in res/values/config.xml, with an optional overlay read from
 * {@code config.json} in the app's external files directory
 * ({@code /sdcard/Android/data/com.gathin.tvhome/files/}). Every key in the JSON is optional and an
 * unknown or badly typed key is ignored, so a theme can change the dock and the look without a rebuild:
 * {@code tools/theme.sh themes/<name>} pushes a theme folder there and restarts Home.
 *
 * Read once in {@link MainActivity#onCreate}; nothing here touches the network.
 */
final class Config {
    private static final String TAG = "TvHome.Config";
    static final String FILE_NAME = "config.json";
    static final String WALLPAPER_NAME = "wallpaper.jpg";

    final List<String> dockApps;
    final List<String> hiddenApps;
    final String liveTvPackage;
    final String liveTvAction;
    final String sourcePanelService;
    final String sourcePanelAction;
    final String inputAction;
    final int columns;
    final int rowsBelowDock;
    final boolean shamsiDate;
    final boolean moduleCards;
    final String vpnApp;
    final boolean showHomeSettingsShortcut;
    /** Background colour behind everything, #RRGGBB; black unless a theme says otherwise. */
    final int backgroundColor;
    /** Full-screen picture behind the launcher, or null for the plain colour. */
    final File wallpaper;
    /** Whether the overlay file was present and parsed. */
    final boolean overlayApplied;
    /** The name in the greeting and on the welcome splash; empty when there is none. */
    final String greetingName;
    /** Whether the launcher draws one of its own wallpapers behind the tiles. */
    final boolean useWallpaper;
    /** Seconds each wallpaper is on screen before the next one fades in. */
    final int wallpaperIntervalSeconds;
    /** Whether the screensaver shows the wallpapers. */
    final boolean screensaverEnabled;
    /** Seconds each wallpaper is on screen in the screensaver before the next one fades in. */
    final int screensaverIntervalSeconds;
    /** Whether the welcome splash is shown once per boot. */
    final boolean splashEnabled;
    /** The splash message; %s is the name from {@link #greetingName}. */
    final String splashMessage;
    /** How long the splash is held before it fades out, in milliseconds. */
    final int splashDurationMs;
    /** How far the splash message sits above the bottom edge, in percent of the screen height. */
    final int splashTextBottomPercent;
    /** Where the splash message sits horizontally: center, left or right. */
    final String splashTextAlign;
    /** Whether the weather pill is shown. */
    final boolean weatherEnabled;
    /** Latitude of the place the weather is for, or NaN when it is not known. */
    final double weatherLat;
    /** Longitude of the place the weather is for, or NaN when it is not known. */
    final double weatherLon;
    /** Whether the optional Live TV app is wanted. */
    final boolean liveTvEnabled;
    /** The package of that app, pinned as the first tile of the grid; empty when it is not wanted. */
    final String liveTvAppPackage;
    /** The languages whose playlists the Live TV app loads. */
    final List<String> liveTvLanguages;
    /** auto, day or night. */
    final String themeMode;
    /** Fixed day boundary, used until sunrise is known. */
    final String themeDayStart;
    /** Fixed night boundary, used until sunset is known. */
    final String themeNightStart;
    /** Soft warm colour used behind the launcher in daylight. */
    final int themeDayBackground;
    /** How much a wallpaper is dimmed at night, 0.3 to 1. */
    final float themeNightDim;
    /** Length of the sunrise and sunset blend, in minutes. */
    final int themeTransitionMinutes;
    /** Whether the day and night palette follows the wallpapers in the screensaver. */
    final boolean themeApplyToScreensaver;
    /** Whether assets/wallpapers/day and /night may replace the single wallpaper set. */
    final boolean themeUsePhaseWallpapers;
    /** Debug-only blend override, -1 when it is not used. */
    final float themePreviewBlend;

    private Config(List<String> dockApps, List<String> hiddenApps, String liveTvPackage, String liveTvAction,
                   String sourcePanelService, String sourcePanelAction, String inputAction,
                   int columns, int rowsBelowDock,
                   boolean shamsiDate, boolean moduleCards, String vpnApp, boolean showHomeSettingsShortcut, int backgroundColor,
                   File wallpaper, boolean overlayApplied,
                   String greetingName, boolean useWallpaper, int wallpaperIntervalSeconds,
                   boolean screensaverEnabled, int screensaverIntervalSeconds,
                   boolean splashEnabled, String splashMessage, int splashDurationMs,
                   int splashTextBottomPercent, String splashTextAlign,
                   boolean weatherEnabled, double weatherLat, double weatherLon,
                   boolean liveTvEnabled, String liveTvAppPackage, List<String> liveTvLanguages,
                   String themeMode, String themeDayStart, String themeNightStart, int themeDayBackground,
                   float themeNightDim, int themeTransitionMinutes,
                   boolean themeApplyToScreensaver, boolean themeUsePhaseWallpapers,
                   float themePreviewBlend) {
        this.dockApps = Collections.unmodifiableList(dockApps);
        this.hiddenApps = Collections.unmodifiableList(hiddenApps);
        this.liveTvPackage = liveTvPackage;
        this.liveTvAction = liveTvAction;
        this.sourcePanelService = sourcePanelService;
        this.sourcePanelAction = sourcePanelAction;
        this.inputAction = inputAction;
        this.columns = columns;
        this.rowsBelowDock = rowsBelowDock;
        this.shamsiDate = shamsiDate;
        this.moduleCards = moduleCards;
        this.vpnApp = vpnApp;
        this.showHomeSettingsShortcut = showHomeSettingsShortcut;
        this.backgroundColor = backgroundColor;
        this.wallpaper = wallpaper;
        this.overlayApplied = overlayApplied;
        this.greetingName = greetingName;
        this.useWallpaper = useWallpaper;
        this.wallpaperIntervalSeconds = wallpaperIntervalSeconds;
        this.screensaverEnabled = screensaverEnabled;
        this.screensaverIntervalSeconds = screensaverIntervalSeconds;
        this.splashEnabled = splashEnabled;
        this.splashMessage = splashMessage;
        this.splashDurationMs = splashDurationMs;
        this.splashTextBottomPercent = splashTextBottomPercent;
        this.splashTextAlign = splashTextAlign;
        this.weatherEnabled = weatherEnabled;
        this.weatherLat = weatherLat;
        this.weatherLon = weatherLon;
        this.liveTvEnabled = liveTvEnabled;
        this.liveTvAppPackage = liveTvEnabled ? liveTvAppPackage : "";
        this.liveTvLanguages = Collections.unmodifiableList(liveTvLanguages);
        this.themeMode = mode(themeMode);
        this.themeDayStart = themeDayStart;
        this.themeNightStart = themeNightStart;
        this.themeDayBackground = themeDayBackground;
        this.themeNightDim = themeNightDim;
        this.themeTransitionMinutes = themeTransitionMinutes;
        this.themeApplyToScreensaver = themeApplyToScreensaver;
        this.themeUsePhaseWallpapers = themeUsePhaseWallpapers;
        this.themePreviewBlend = themePreviewBlend;
    }

    /** The folder the theme tools push into; null when external storage is unavailable. */
    static File dir(Context ctx) {
        return ctx.getExternalFilesDir(null);
    }

    static Config load(Context ctx) {
        Resources res = ctx.getResources();
        List<String> dock = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.dock_apps)));
        List<String> hidden = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.hidden_apps)));
        String liveTv = res.getString(R.string.live_tv_package).trim();
        String liveTvAction = res.getString(R.string.live_tv_action).trim();
        String panelService = res.getString(R.string.source_panel_service).trim();
        String panelAction = res.getString(R.string.source_panel_action).trim();
        String inputAction = res.getString(R.string.input_action).trim();
        int columns = res.getInteger(R.integer.columns);
        int rowsBelow = res.getInteger(R.integer.rows_below_dock);
        boolean shamsi = res.getBoolean(R.bool.shamsi_date);
        boolean moduleCards = res.getBoolean(R.bool.module_cards);
        String vpn = res.getString(R.string.vpn_app).trim();
        boolean homeSettings = res.getBoolean(R.bool.show_home_settings_shortcut);
        int background = 0xFF000000;
        boolean applied = false;

        String greetingName = res.getString(R.string.greeting_name).trim();
        boolean useWallpaper = res.getBoolean(R.bool.use_wallpaper);
        int wallpaperInterval = res.getInteger(R.integer.wallpaper_interval_seconds);
        boolean screensaverEnabled = res.getBoolean(R.bool.screensaver_enabled);
        int screensaverInterval = res.getInteger(R.integer.screensaver_interval_seconds);
        boolean splashEnabled = res.getBoolean(R.bool.splash_enabled);
        String splashMessage = res.getString(R.string.splash_message);
        int splashDuration = res.getInteger(R.integer.splash_duration_ms);
        int splashBottom = res.getInteger(R.integer.splash_text_bottom_percent);
        String splashAlign = res.getString(R.string.splash_text_align).trim();
        boolean weatherEnabled = res.getBoolean(R.bool.weather_enabled);
        double weatherLat = number(res.getString(R.string.weather_lat));
        double weatherLon = number(res.getString(R.string.weather_lon));
        boolean liveTvEnabled = res.getBoolean(R.bool.live_tv_enabled);
        String liveTvApp = res.getString(R.string.live_tv_app_package).trim();
        List<String> liveTvLanguages = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.live_tv_languages)));
        String themeMode = res.getString(R.string.theme_mode).trim();
        String themeDayStart = res.getString(R.string.theme_day_start).trim();
        String themeNightStart = res.getString(R.string.theme_night_start).trim();
        int themeDayBackground = colorText(res.getString(R.string.theme_day_background), 0xFFF3EFE8);
        float themeNightDim = decimal(res.getString(R.string.theme_night_dim), 0.6f);
        int themeTransition = res.getInteger(R.integer.theme_transition_minutes);
        boolean themeScreensaver = res.getBoolean(R.bool.theme_apply_to_screensaver);
        boolean themePhaseWallpapers = res.getBoolean(R.bool.theme_use_phase_wallpapers);
        float themePreview = -1f;

        File dir = dir(ctx);
        File file = dir != null ? new File(dir, FILE_NAME) : null;
        JSONObject json = read(file);
        if (json != null) {
            applied = true;
            dock = stringList(json, "dock_apps", dock);
            hidden = stringList(json, "hidden_apps", hidden);
            liveTv = string(json, "live_tv_package", liveTv);
            liveTvAction = string(json, "live_tv_action", liveTvAction);
            panelService = string(json, "source_panel_service", panelService);
            panelAction = string(json, "source_panel_action", panelAction);
            inputAction = string(json, "input_action", inputAction);
            columns = integer(json, "columns", columns, 1, 8);
            rowsBelow = integer(json, "rows_below_dock", rowsBelow, 0, 3);
            shamsi = bool(json, "shamsi_date", shamsi);
            moduleCards = bool(json, "module_cards", moduleCards);
            vpn = string(json, "vpn_app", vpn);
            homeSettings = bool(json, "show_home_settings_shortcut", homeSettings);
            background = color(json, "background_color", background);
            greetingName = string(json, "greeting_name", greetingName);
            useWallpaper = bool(json, "use_wallpaper", useWallpaper);
            wallpaperInterval = integer(json, "wallpaper_interval_seconds", wallpaperInterval, 5, 86400);
            screensaverEnabled = bool(json, "screensaver_enabled", screensaverEnabled);
            screensaverInterval = integer(json, "screensaver_interval_seconds", screensaverInterval, 5, 3600);
            splashEnabled = bool(json, "splash_enabled", splashEnabled);
            splashMessage = string(json, "splash_message", splashMessage);
            splashDuration = integer(json, "splash_duration_ms", splashDuration, 500, 30000);
            splashBottom = integer(json, "splash_text_bottom_percent", splashBottom, 0, 60);
            splashAlign = string(json, "splash_text_align", splashAlign);
            weatherEnabled = bool(json, "weather_enabled", weatherEnabled);
            weatherLat = coordinate(json, "weather_lat", weatherLat);
            weatherLon = coordinate(json, "weather_lon", weatherLon);
            liveTvEnabled = bool(json, "live_tv_enabled", liveTvEnabled);
            liveTvApp = string(json, "live_tv_app_package", liveTvApp);
            liveTvLanguages = stringList(json, "live_tv_languages", liveTvLanguages);
            themeMode = string(json, "theme_mode", themeMode);
            themeDayStart = string(json, "theme_day_start", themeDayStart);
            themeNightStart = string(json, "theme_night_start", themeNightStart);
            themeDayBackground = color(json, "theme_day_background", themeDayBackground);
            themeNightDim = decimal(json, "theme_night_dim", themeNightDim, 0.3f, 1f);
            themeTransition = integer(json, "theme_transition_minutes", themeTransition, 5, 120);
            themeScreensaver = bool(json, "theme_apply_to_screensaver", themeScreensaver);
            themePhaseWallpapers = bool(json, "theme_use_phase_wallpapers", themePhaseWallpapers);
            themePreview = decimal(json, "theme_preview_blend", themePreview, 0f, 1f);
        }
        File wallpaper = dir != null ? new File(dir, WALLPAPER_NAME) : null;
        if (wallpaper != null && !(wallpaper.isFile() && wallpaper.length() > 0)) wallpaper = null;
        // A theme can also switch the wallpaper off while the file is still there.
        if (json != null && json.has("wallpaper") && !bool(json, "wallpaper", true)) wallpaper = null;
        // The picture a theme brought is only used when the launcher has no wallpapers of its own.
        if (!useWallpaper) wallpaper = null;

        return new Config(dock, hidden, liveTv, liveTvAction, panelService, panelAction, inputAction,
                Math.max(1, columns), Math.max(0, rowsBelow), shamsi, moduleCards, vpn, homeSettings, background,
                wallpaper, applied,
                greetingName, useWallpaper, Math.max(1, wallpaperInterval),
                screensaverEnabled, Math.max(5, screensaverInterval),
                splashEnabled, splashMessage, splashDuration,
                clamp(splashBottom, 0, 60), align(splashAlign),
                weatherEnabled, weatherLat, weatherLon,
                liveTvEnabled, liveTvApp, liveTvLanguages,
                mode(themeMode), themeDayStart, themeNightStart, themeDayBackground,
                clamp(themeNightDim, 0.3f, 1f), clamp(themeTransition, 5, 120),
                themeScreensaver, themePhaseWallpapers, themePreview);
    }

    private static String mode(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(java.util.Locale.US);
        if ("day".equals(v) || "night".equals(v)) return v;
        return "auto";
    }

    /** The splash message alignment, always one of the three the layout understands. */
    static String align(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(java.util.Locale.US);
        if ("left".equals(v) || "right".equals(v)) return v;
        return "center";
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static float decimal(String value, float fallback) {
        return (float) number(value, fallback);
    }

    private static float decimal(JSONObject o, String key, float fallback, float min, float max) {
        Object v = o.opt(key);
        float f;
        if (v instanceof Number) {
            f = ((Number) v).floatValue();
        } else if (v instanceof String) {
            f = decimal((String) v, fallback);
        } else {
            return fallback;
        }
        return f < min || f > max ? fallback : f;
    }

    private static int colorText(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Color.parseColor(value.trim()) | 0xFF000000;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Parses a coordinate from the config; NaN when it is missing or not a number. */
    private static double number(String value) {
        return number(value, Double.NaN);
    }

    private static double number(String value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** A coordinate written as a JSON number (the usual case) or as a string. */
    private static double coordinate(JSONObject o, String key, double fallback) {
        Object v = o.opt(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) return number((String) v, fallback);
        return fallback;
    }

    /** Reads and parses the overlay; any problem means "no overlay", never a crash on the home screen. */
    private static JSONObject read(File file) {
        if (file == null || !file.isFile()) return null;
        if (file.length() > 64 * 1024) {
            Log.w(TAG, "ignoring " + file + ": larger than 64 KB");
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            String text = new String(buf, 0, n, StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) text = text.substring(1);   // Windows editors add a BOM
            return new JSONObject(text);
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String string(JSONObject o, String key, String fallback) {
        Object v = o.opt(key);
        return v instanceof String ? ((String) v).trim() : fallback;
    }

    private static boolean bool(JSONObject o, String key, boolean fallback) {
        Object v = o.opt(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    private static int integer(JSONObject o, String key, int fallback, int min, int max) {
        Object v = o.opt(key);
        if (!(v instanceof Number)) return fallback;
        int i = ((Number) v).intValue();
        return i < min || i > max ? fallback : i;
    }

    private static int color(JSONObject o, String key, int fallback) {
        Object v = o.opt(key);
        if (!(v instanceof String)) return fallback;
        try {
            int c = Color.parseColor(((String) v).trim());
            return c | 0xFF000000;   // always opaque: the window behind is black anyway
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static List<String> stringList(JSONObject o, String key, List<String> fallback) {
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
}
