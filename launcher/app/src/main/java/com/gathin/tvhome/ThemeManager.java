package com.gathin.tvhome;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

/**
 * One place that turns the clock, the sunrise and the sunset into the colours every widget uses.
 * Nothing else in the launcher keeps its own copy of a foreground or surface colour.
 */
final class ThemeManager {

    private static final int DAY_PRIMARY = 0xFF1B1D22;
    private static final int DAY_SECONDARY = 0xB31B1D22;
    private static final int DAY_SURFACE = 0x33FFFFFF;
    private static final int DAY_PANEL = 0xF2F4EFE7;
    private static final int DAY_TILE = 0xF2E9DED0;
    private static final int DAY_TILE_TEXT = 0xFF17191E;
    private static final int DAY_FOCUS_SURFACE = 0xFFF8F3EB;
    private static final int DAY_SEPARATOR = 0x261B1D22;
    private static final int DAY_ACCENT = 0xFFB86B3F;

    private static final int NIGHT_PRIMARY = 0xFFFFFFFF;
    private static final int NIGHT_SECONDARY = 0xB3FFFFFF;
    private static final int NIGHT_SURFACE = 0x1FFFFFFF;
    private static final int NIGHT_PANEL = 0xF2161618;
    private static final int NIGHT_TILE = 0xFF1C1C1E;
    private static final int NIGHT_TILE_TEXT = 0xFFFFFFFF;
    private static final int NIGHT_FOCUS_SURFACE = 0xFFF5F5F5;
    private static final int NIGHT_SEPARATOR = 0x1FFFFFFF;
    private static final int NIGHT_ACCENT = 0xFF9AA9FF;

    private static final int FOCUS_TEXT = 0xFF111111;
    private static final int WARM = 0xFFFFB36B;
    private static final int NIGHT_BACKGROUND = 0xFF000000;

    /** True only in debug builds, so theme_preview_blend is ignored in release. */
    private static final boolean DEBUG = isDebugBuild();

    private final Config cfg;
    private final LocalTime dayStart;
    private final LocalTime nightStart;

    /** The project has no BuildConfig class (AGP 8), so it is looked up by name when it exists. */
    private static boolean isDebugBuild() {
        try {
            return (Boolean) Class.forName("com.gathin.tvhome.BuildConfig").getField("DEBUG").get(null);
        } catch (Exception e) {
            return false;
        }
    }

    ThemeManager(Config cfg) {
        this.cfg = cfg;
        dayStart = ThemeMath.clock(cfg.themeDayStart, LocalTime.of(6, 0));
        nightStart = ThemeMath.clock(cfg.themeNightStart, LocalTime.of(18, 30));
    }

    Tokens tokens(LocalTime now, Weather.Report report) {
        Weather.Sun sun = report == null ? null : report.sunFor(LocalDate.now());
        LocalTime sunrise = sun == null ? null : sun.sunrise;
        LocalTime sunset = sun == null ? null : sun.sunset;

        ThemeMath.State state = ThemeMath.compute(now, sunrise, sunset,
                cfg.themeTransitionMinutes, dayStart, nightStart, cfg.themeMode);

        if (DEBUG && cfg.themePreviewBlend >= 0f) {
            float preview = Math.max(0f, Math.min(1f, cfg.themePreviewBlend));
            state = new ThemeMath.State(preview < 0.5f ? ThemeMath.Phase.DAY : ThemeMath.Phase.NIGHT,
                    preview, 0f, false);
        }
        return new Tokens(cfg, state);
    }

    static final class Tokens {
        final ThemeMath.Phase phase;
        /** 0 = full day palette, 1 = full night palette. */
        final float blend;
        /** 0 = no warm tint, 1 = the warm tint at sunrise or sunset. */
        final float warm;
        final boolean blending;

        final int background;
        final int primaryText;
        final int secondaryText;
        /** Translucent fill behind a status pill or a row. */
        final int surface;
        final int focusSurface;
        final int focusText;
        /** Nearly opaque surface for the weather card, menus and Now Playing. */
        final int panelSurface;
        final int tileSurface;
        final int tileText;
        final int separator;
        final int accent;
        final int glassRim;

        /** How much to darken a wallpaper: 1 in the day, the configured dimming at night. */
        final float wallpaperBrightness;
        final float wallpaperSaturation;
        final float wallpaperWarmth;
        final ColorFilter wallpaperFilter;

        Tokens(Config cfg, ThemeMath.State state) {
            phase = state.phase;
            blend = state.blend;
            warm = state.warm;
            blending = state.blending;

            int dayBackground = cfg.themeDayBackground;
            background = tint(mix(dayBackground, NIGHT_BACKGROUND, blend), WARM, warm * 0.08f);
            primaryText = mix(DAY_PRIMARY, NIGHT_PRIMARY, blend);
            secondaryText = mix(DAY_SECONDARY, NIGHT_SECONDARY, blend);
            surface = mix(DAY_SURFACE, NIGHT_SURFACE, blend);
            focusSurface = mix(DAY_FOCUS_SURFACE, NIGHT_FOCUS_SURFACE, blend);
            focusText = FOCUS_TEXT;
            panelSurface = tint(mix(DAY_PANEL, NIGHT_PANEL, blend), WARM, warm * 0.05f);
            tileSurface = mix(DAY_TILE, NIGHT_TILE, blend);
            tileText = mix(DAY_TILE_TEXT, NIGHT_TILE_TEXT, blend);
            separator = mix(DAY_SEPARATOR, NIGHT_SEPARATOR, blend);
            accent = mix(mix(DAY_ACCENT, NIGHT_ACCENT, blend), WARM, warm * 0.65f);
            glassRim = mix(0x66FFFFFF, 0xCCFFFFFF, blend);

            float dim = Math.max(0.3f, Math.min(1f, cfg.themeNightDim));
            wallpaperBrightness = 1f - (1f - dim) * blend;
            wallpaperSaturation = 1f - 0.16f * blend;
            wallpaperWarmth = warm;
            wallpaperFilter = filter(wallpaperBrightness, wallpaperSaturation, wallpaperWarmth);
        }

        boolean isNight() {
            return blend >= 0.5f;
        }

        String phaseName() {
            return phase.name().toLowerCase(Locale.US);
        }

        ColorStateList textStates(int normal) {
            return new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                    new int[]{focusText, normal});
        }

        ColorStateList rowSurfaceStates() {
            return new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                    new int[]{focusSurface, Color.TRANSPARENT});
        }

        ColorStateList pillSurfaceStates() {
            return new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                    new int[]{focusSurface, surface});
        }

        /** Keeps a condition colour (sun, rain) but lets the phase's accent show through a little. */
        int weatherTint(int conditionTint) {
            return Contrast.mix(conditionTint, accent, 0.22f + 0.28f * warm);
        }

        private static ColorFilter filter(float brightness, float saturation, float warmth) {
            float r = 1f + 0.10f * warmth;
            float g = 1f + 0.03f * warmth;
            float b = 1f - 0.10f * warmth;
            ColorMatrix sat = new ColorMatrix();
            sat.setSaturation(saturation);
            ColorMatrix shine = new ColorMatrix(new float[]{
                    brightness * r, 0, 0, 0, 0,
                    0, brightness * g, 0, 0, 0,
                    0, 0, brightness * b, 0, 0,
                    0, 0, 0, 1, 0});
            sat.postConcat(shine);
            return new ColorMatrixColorFilter(sat);
        }
    }

    static void tintBackground(View view, int color) {
        if (view != null) view.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    static void tintBackground(View view, ColorStateList colors) {
        if (view != null) view.setBackgroundTintList(colors);
    }

    private static int mix(int day, int night, float t) {
        return Contrast.mix(day, night, t);
    }

    /** Moves a colour towards the warm tint without changing its alpha. */
    private static int tint(int color, int warm, float t) {
        return Contrast.mix(color, (color & 0xFF000000) | (warm & 0x00FFFFFF), t);
    }
}
