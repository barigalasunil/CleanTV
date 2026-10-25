package com.gathin.tvhome;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The time-of-day part of the theme, kept free of Android so it can be tested on a plain JVM.
 *
 * <p>The day is split into a day, a dawn, a dusk and a night. The change between day and night is
 * not a switch: at dawn and dusk a warm tint peaks at sunrise and sunset while the day palette
 * crosses over to the night one. The configured transition is 30 minutes at dawn and 45 minutes at
 * dusk by default, which matches the launcher's normal sunrise and sunset margins.
 */
final class ThemeMath {

    enum Phase { DAY, DAWN, DUSK, NIGHT }

    static final class State {
        final Phase phase;
        /** 0 = full day palette, 1 = full night palette. */
        final float blend;
        /** 0 = no warm tint, 1 = the warm tint at its peak (sunrise or sunset). */
        final float warm;
        /** True while the palette is crossing over, so the caller may refresh twice a minute. */
        final boolean blending;

        State(Phase phase, float blend, float warm, boolean blending) {
            this.phase = phase;
            this.blend = clamp(blend);
            this.warm = clamp(warm);
            this.blending = blending;
        }
    }

    private static final int DAY_SECONDS = 24 * 60 * 60;

    private ThemeMath() {
    }

    static LocalTime clock(String value, LocalTime fallback) {
        if (value == null) return fallback;
        String text = value.trim();
        if (text.isEmpty()) return fallback;
        try {
            return LocalTime.parse(text);
        } catch (Exception ignored) {
            // "6:00" is accepted as well as "06:00".
        }
        try {
            return LocalTime.parse(text, DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static State compute(LocalTime now, LocalTime sunrise, LocalTime sunset, int transitionMinutes,
                         LocalTime dayStart, LocalTime nightStart, String mode) {
        String m = mode == null ? "" : mode.trim().toLowerCase(java.util.Locale.US);
        if ("day".equals(m)) return new State(Phase.DAY, 0f, 0f, false);
        if ("night".equals(m)) return new State(Phase.NIGHT, 1f, 0f, false);

        LocalTime dawnCentre = sunrise;
        LocalTime duskCentre = sunset;
        if (dawnCentre == null || duskCentre == null || dawnCentre.equals(duskCentre)) {
            dawnCentre = dayStart != null ? dayStart : LocalTime.of(6, 0);
            duskCentre = nightStart != null ? nightStart : LocalTime.of(18, 30);
        }
        if (dawnCentre.equals(duskCentre)) {
            duskCentre = dawnCentre.plusHours(12);
        }

        int t = Math.max(5, Math.min(120, transitionMinutes));
        int dawnStart = wrap(seconds(dawnCentre) - t * 60);
        int dawnEnd = wrap(seconds(dawnCentre) + t * 60);
        // The evening change is a little longer, as sunset light lingers after the sun is down.
        int duskHalf = Math.round(t * 1.5f) * 60;
        int duskStart = wrap(seconds(duskCentre) - duskHalf);
        int duskEnd = wrap(seconds(duskCentre) + duskHalf);
        int nowSeconds = seconds(now);

        if (within(nowSeconds, dawnStart, dawnEnd)) {
            float p = progress(nowSeconds, dawnStart, dawnEnd);
            return new State(Phase.DAWN, 1f - p, (float) Math.sin(Math.PI * p), true);
        }
        if (within(nowSeconds, duskStart, duskEnd)) {
            float p = progress(nowSeconds, duskStart, duskEnd);
            return new State(Phase.DUSK, p, (float) Math.sin(Math.PI * p), true);
        }
        if (within(nowSeconds, dawnEnd, duskStart)) {
            return new State(Phase.DAY, 0f, 0f, false);
        }
        return new State(Phase.NIGHT, 1f, 0f, false);
    }

    private static int seconds(LocalTime time) {
        return time.getHour() * 3600 + time.getMinute() * 60 + time.getSecond();
    }

    private static int wrap(int seconds) {
        return ((seconds % DAY_SECONDS) + DAY_SECONDS) % DAY_SECONDS;
    }

    /** True when {@code now} is in [start, end), wrapping over midnight when needed. */
    private static boolean within(int now, int start, int end) {
        if (start == end) return false;
        return start < end ? now >= start && now < end : now >= start || now < end;
    }

    private static float progress(int now, int start, int end) {
        int span = (end - start + DAY_SECONDS) % DAY_SECONDS;
        if (span == 0) return 0f;
        int elapsed = (now - start + DAY_SECONDS) % DAY_SECONDS;
        return clamp((float) elapsed / span);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
