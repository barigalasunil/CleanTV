package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * The ring behind a RAM or storage widget: a faint full circle and a filled arc for the part in use.
 * The arc is white below 75%, amber from 75% and red from 90%, so a full disk reads at a glance.
 */
final class GaugeView extends View {

    private static final int TRACK = 0x33FFFFFF;
    private static final int NORMAL = 0xFFFFFFFF;
    private static final int AMBER = 0xFFFFB020;
    private static final int RED = 0xFFFF5A5A;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float fraction;
    private ValueAnimator animator;

    GaugeView(Context ctx) {
        super(ctx);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);
        track.setStyle(Paint.Style.STROKE);
        arc.setStyle(Paint.Style.STROKE);
        track.setColor(TRACK);
        arc.setColor(NORMAL);
        arc.setStrokeCap(Paint.Cap.ROUND);
    }

    GaugeView(Context ctx, AttributeSet attrs) {
        this(ctx);
    }

    void setFraction(float value) {
        float f = Math.max(0f, Math.min(1f, value));
        if (Math.abs(f - fraction) < 0.002f) return;
        apply(f);
    }

    /**
     * Moves the arc to a new reading instead of jumping to it, so a five-second widget tick reads
     * as live. Instant when the owner has animations off, and it keeps the colour thresholds.
     */
    void animateTo(float value) {
        float target = Math.max(0f, Math.min(1f, value));
        if (Motion.off()) {
            apply(target);
            return;
        }
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(fraction, target);
        animator.setDuration(Motion.ms(Motion.MEDIUM));
        animator.setInterpolator(Motion.EASE_OUT);
        animator.addUpdateListener(a -> apply((float) a.getAnimatedValue()));
        animator.start();
    }

    private void apply(float f) {
        fraction = f;
        arc.setColor(f >= 0.90f ? RED : (f >= 0.75f ? AMBER : NORMAL));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = Math.max(3f, getHeight() * 0.09f);
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        float inset = stroke / 2f + 1f;
        box.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(box, 0f, 360f, false, track);
        if (fraction > 0f) canvas.drawArc(box, -90f, 360f * fraction, false, arc);
    }
}
