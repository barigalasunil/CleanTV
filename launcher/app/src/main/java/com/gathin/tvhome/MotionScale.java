package com.gathin.tvhome;

/**
 * The arithmetic behind {@link Motion}: the TV's animator scale, applied to a designed duration.
 *
 * <p>Pure Java, kept out of {@link Motion} so the rule can be tested without an Android runtime. At
 * 0.5 the designed duration is divided by 0.5, which doubles it and so keeps the designed speed; at
 * 0 animations are off; the TV is never told to run faster than designed, so a scale above 1 is read
 * as 1.
 */
final class MotionScale {

    private MotionScale() {
    }

    /** The duration to use, in milliseconds; 0 when animations are off. */
    static long ms(long designed, float scale) {
        if (designed <= 0L) return 0L;
        if (Float.isNaN(scale) || scale >= 1f) return designed;
        if (scale <= 0f) return 0L;
        return Math.max(1L, Math.round(designed / scale));
    }
}
