package com.gathin.tvhome;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * The one place every animation in the launcher takes its timing from: three durations, one
 * ease-out curve, a gentle overshoot for focus landing, and one fade/shrink for closing a menu.
 *
 * <p>The owner's own animator speed setting (Developer options, Animator duration scale) is
 * honoured, because a TV tuned that way should keep the motion it was tuned for: at 0.5 the
 * designed duration is divided by 0.5, which doubles it and so keeps the designed speed; at 0
 * animations are off and everything happens at once. The setting is only read, never written.
 *
 * <p>The interpolators are created once and shared: no per-frame, per-key or per-bind allocation.
 */
final class Motion {

    /** A colour or alpha change, a highlight landing. */
    static final long FAST = 160L;
    /** A tile taking focus, a panel opening. */
    static final long MEDIUM = 260L;
    /** Something arriving on screen or settling from far away. */
    static final long SLOW = 420L;

    /** Everything that starts and settles. */
    static final Interpolator EASE_OUT = new DecelerateInterpolator(1.8f);
    /** Only for focus landing on a tile: a small, gentle overshoot, never a bounce. */
    static final Interpolator FOCUS = new OvershootInterpolator(1.15f);
    /** The curve a menu follows as it leaves. */
    static final Interpolator CLOSE = new AccelerateInterpolator(1.6f);

    /** The dock highlight's spring: quick, softly damped, no visible wobble. */
    private static final float STIFFNESS = 260f;
    private static final float DAMPING = 0.60f;

    private static float scale = 1f;
    private static boolean known;

    private Motion() {
    }

    /** Reads the TV's animator scale. Call once per visit, from onResume and onDreamingStarted. */
    static void refresh(Context ctx) {
        float value = 1f;
        try {
            value = Settings.Global.getFloat(ctx.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        } catch (Exception ignored) {
            // A ROM without the setting behaves like a normal one, at full speed.
        }
        if (Float.isNaN(value) || value < 0f) value = 0f;
        if (value > 1f) value = 1f;      // the TV is never told to run faster than designed
        scale = value;
        known = true;
    }

    /** True when the owner turned animations off: callers then jump straight to the end state. */
    static boolean off() {
        return known && scale == 0f;
    }

    /** The designed duration, stretched so the motion keeps its speed at any animator scale. */
    static long ms(long designed) {
        return MotionScale.ms(designed, known ? scale : 1f);
    }

    /** Applies the shared spring to a position animation. */
    static void spring(SpringAnimation animation) {
        animation.setSpring(new SpringForce().setStiffness(STIFFNESS).setDampingRatio(DAMPING));
    }

    /**
     * The one close for every menu: fade and shrink away, then run {@code after} (which dismisses
     * the popup, so a menu never vanishes in a single frame). Instant when animations are off.
     */
    static void close(View panel, Runnable after) {
        panel.animate().cancel();
        if (off()) {
            panel.setAlpha(0f);
            if (after != null) after.run();
            return;
        }
        panel.setPivotY(0f);
        panel.animate().alpha(0f).scaleX(0.97f).scaleY(0.97f)
                .setDuration(ms(FAST))
                .setInterpolator(CLOSE)
                .withEndAction(after)
                .start();
    }
}
