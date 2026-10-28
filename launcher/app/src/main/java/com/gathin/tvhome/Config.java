package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The launcher's settings: the values in res/values/config.xml, with an optional overlay read from
 * {@code theme/config.json} in the app's external files directory
 * ({@code /sdcard/Android/data/com.gathin.tvhome/files/theme/}). Every key in the JSON is optional
 * and an unknown or badly typed key is ignored, so a theme can change the dock and the look without
 * a rebuild: {@code tools/theme.sh <theme-folder>} pushes the folder there and restarts Home.
 *
 * Read once in {@link MainActivity#onCreate}; nothing here touches the network.
 */
final class Config {
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
    /** Whether the row of home widgets under the top buttons is shown at all. */
    final boolean widgetsEnabled;
    /** Whether the weather, RAM and storage widgets are each drawn. */
    final boolean widgetWeather;
    final boolean widgetRam;
    final boolean widgetStorage;
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

    private Config(List<String> dockApps, List<String> hiddenApps, String liveTvPackage, String liveTvAction,
                   String sourcePanelService, String sourcePanelAction, String inputAction,
                   int columns, int rowsBelowDock,
                   boolean shamsiDate, boolean moduleCards, String vpnApp, boolean showHomeSettingsShortcut, int backgroundColor,
                   boolean overlayApplied,
                   String greetingName, boolean useWallpaper, int wallpaperIntervalSeconds,
                   boolean screensaverEnabled, int screensaverIntervalSeconds,
                   boolean splashEnabled, String splashMessage, int splashDurationMs,
                   int splashTextBottomPercent, String splashTextAlign,
                   boolean widgetsEnabled, boolean widgetWeather, boolean widgetRam, boolean widgetStorage,
                   double weatherLat, double weatherLon,
                   boolean liveTvEnabled, String liveTvAppPackage, List<String> liveTvLanguages) {
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
        this.widgetsEnabled = widgetsEnabled;
        this.widgetWeather = widgetWeather;
        this.widgetRam = widgetRam;
        this.widgetStorage = widgetStorage;
        this.weatherLat = weatherLat;
        this.weatherLon = weatherLon;
        this.liveTvEnabled = liveTvEnabled;
        this.liveTvAppPackage = liveTvEnabled ? liveTvAppPackage : "";
        this.liveTvLanguages = Collections.unmodifiableList(liveTvLanguages);
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
        boolean widgetsEnabled = res.getBoolean(R.bool.widgets_enabled);
        boolean widgetWeather = res.getBoolean(R.bool.widget_weather_enabled);
        boolean widgetRam = res.getBoolean(R.bool.widget_ram_enabled);
        boolean widgetStorage = res.getBoolean(R.bool.widget_storage_enabled);
        double weatherLat = number(res.getString(R.string.weather_lat));
        double weatherLon = number(res.getString(R.string.weather_lon));
        boolean liveTvEnabled = res.getBoolean(R.bool.live_tv_enabled);
        String liveTvApp = res.getString(R.string.live_tv_app_package).trim();
        List<String> liveTvLanguages = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.live_tv_languages)));

        JSONObject json = Theme.readConfig(ctx);
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
            widgetsEnabled = bool(json, "widgets_enabled", widgetsEnabled);
            widgetWeather = bool(json, "widget_weather_enabled", widgetWeather);
            widgetRam = bool(json, "widget_ram_enabled", widgetRam);
            widgetStorage = bool(json, "widget_storage_enabled", widgetStorage);
            weatherLat = coordinate(json, "weather_lat", weatherLat);
            weatherLon = coordinate(json, "weather_lon", weatherLon);
            liveTvEnabled = bool(json, "live_tv_enabled", liveTvEnabled);
            liveTvApp = string(json, "live_tv_app_package", liveTvApp);
            liveTvLanguages = stringList(json, "live_tv_languages", liveTvLanguages);
        }
        return new Config(dock, hidden, liveTv, liveTvAction, panelService, panelAction, inputAction,
                Math.max(1, columns), Math.max(0, rowsBelow), shamsi, moduleCards, vpn, homeSettings, background,
                applied,
                greetingName, useWallpaper, Math.max(1, wallpaperInterval),
                screensaverEnabled, Math.max(5, screensaverInterval),
                splashEnabled, splashMessage, splashDuration,
                clamp(splashBottom, 0, 60), align(splashAlign),
                widgetsEnabled, widgetWeather, widgetRam, widgetStorage, weatherLat, weatherLon,
                liveTvEnabled, liveTvApp, liveTvLanguages);
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
