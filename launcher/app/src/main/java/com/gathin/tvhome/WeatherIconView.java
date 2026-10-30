package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * The small sky icon in the weather widget, drawn rather than shipped as seven vector files.
 * Everything is laid out in a 64 by 64 box and scaled to the view, so one drawing works at any size.
 */
final class WeatherIconView extends View {

    private static final int SUN = 0, MOON = 1, CLOUD = 2, RAIN = 3, STORM = 4, SNOW = 5, FOG = 6,
            SUN_CLOUD = 7, MOON_CLOUD = 8;

    private static final int SUN_COLOR = 0xFFFFCC66;
    private static final int MOON_COLOR = 0xFFDCE4FF;
    private static final int LIGHT = 0xFFFFFFFF;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path bite = new Path();
    private final RectF rect = new RectF();
    private int kind = -1;

    WeatherIconView(Context ctx) {
        super(ctx);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    /** The icon for a weather code and whether it is day, as Open-Meteo reports them. */
    void setWeather(int code, boolean day) {
        int next = kindFor(code, day);
        if (next == kind) return;
        kind = next;
        invalidate();
    }

    private static int kindFor(int code, boolean day) {
        if (code == 0) return day ? SUN : MOON;
        if (code == 1 || code == 2) return day ? SUN_CLOUD : MOON_CLOUD;
        if (code == 3) return CLOUD;
        if (code == 45 || code == 48) return FOG;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SNOW;
        if (code >= 95) return STORM;
        if (code >= 51) return RAIN;
        return CLOUD;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0 || kind < 0) return;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        canvas.scale(size / 64f, size / 64f);
        paint.setStyle(Paint.Style.FILL);
        switch (kind) {
            case SUN:
                sun(canvas, 32f, 32f, 12f, true);
                break;
            case MOON:
                moon(canvas, 32f, 32f, 15f);
                break;
            case CLOUD:
                cloud(canvas, 0f, 0f, 1f);
                break;
            case RAIN:
                cloud(canvas, 0f, -4f, 1f);
                drops(canvas, 0f, -2f);
                break;
            case STORM:
                cloud(canvas, 0f, -6f, 1f);
                bolt(canvas, 0f, -4f);
                break;
            case SNOW:
                cloud(canvas, 0f, -6f, 1f);
                flakes(canvas, 0f, -4f);
                break;
            case FOG:
                fog(canvas);
                break;
            case SUN_CLOUD:
                sun(canvas, 21f, 19f, 8f, true);
                cloud(canvas, 7f, 10f, 0.78f);
                break;
            default:
                moon(canvas, 21f, 19f, 10f);
                cloud(canvas, 7f, 10f, 0.78f);
                break;
        }
        canvas.restore();
    }

    private void sun(Canvas canvas, float cx, float cy, float r, boolean rays) {
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(3f);
        paint.setColor(SUN_COLOR);
        canvas.drawCircle(cx, cy, r, paint);
        if (!rays) return;
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4.0;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            canvas.drawLine(cx + cos * (r + 4f), cy + sin * (r + 4f),
                    cx + cos * (r + 9f), cy + sin * (r + 9f), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** A crescent: a disc with a second disc subtracted from it, so no arc is left behind. */
    private void moon(Canvas canvas, float cx, float cy, float r) {
        path.reset();
        path.setFillType(Path.FillType.WINDING);
        path.addCircle(cx, cy, r, Path.Direction.CW);
        bite.reset();
        bite.addCircle(cx + r * 0.72f, cy - r * 0.28f, r * 0.92f, Path.Direction.CW);
        path.op(bite, Path.Op.DIFFERENCE);
        paint.setColor(MOON_COLOR);
        canvas.drawPath(path, paint);
    }

    /** A rounded base with three discs on top: one cloud, optionally moved and scaled. */
    private void cloud(Canvas canvas, float dx, float dy, float scale) {
        path.reset();
        // Winding, not the even-odd rule the moon uses: overlapping discs must merge, not punch holes.
        path.setFillType(Path.FillType.WINDING);
        rect.set(15f, 31f, 51f, 43f);
        path.addRoundRect(rect, 6f, 6f, Path.Direction.CW);
        path.addCircle(25f, 32f, 8f, Path.Direction.CW);
        path.addCircle(35f, 28f, 11f, Path.Direction.CW);
        path.addCircle(44f, 33f, 7f, Path.Direction.CW);
        paint.setColor(LIGHT);
        canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale, 32f, 32f);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private void drops(Canvas canvas, float dx, float dy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setColor(LIGHT);
        for (int i = 0; i < 3; i++) {
            float x = 24f + i * 9f + dx;
            canvas.drawLine(x, 47f + dy, x - 3f, 56f + dy, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void bolt(Canvas canvas, float dx, float dy) {
        path.reset();
        path.setFillType(Path.FillType.WINDING);
        path.moveTo(35f + dx, 43f + dy);
        path.lineTo(27f + dx, 55f + dy);
        path.lineTo(33f + dx, 55f + dy);
        path.lineTo(29f + dx, 64f + dy);
        path.lineTo(41f + dx, 51f + dy);
        path.lineTo(34f + dx, 51f + dy);
        path.close();
        paint.setColor(SUN_COLOR);
        canvas.drawPath(path, paint);
    }

    private void flakes(Canvas canvas, float dx, float dy) {
        paint.setColor(LIGHT);
        for (int i = 0; i < 3; i++) {
            float x = 23f + i * 9f + dx, y = 51f + (i == 1 ? 3f : 0f) + dy;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            canvas.drawLine(x - 3f, y - 3f, x + 3f, y + 3f, paint);
            canvas.drawLine(x - 3f, y + 3f, x + 3f, y - 3f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void fog(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(LIGHT);
        canvas.drawLine(10f, 24f, 50f, 24f, paint);
        canvas.drawLine(16f, 35f, 54f, 35f, paint);
        canvas.drawLine(10f, 46f, 44f, 46f, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
