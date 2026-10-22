package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * How bright a wallpaper is, so that the text on top of it can be read on any picture.
 *
 * <p>The picture is not walked over pixel by pixel: a sparse grid of a few hundred points is read
 * instead, which is cheap enough to do for every wallpaper and keeps the whole bitmap out of it.
 */
final class Contrast {

    /** The picture is sampled in this many cells across and down. */
    static final int COLS = 24;
    static final int ROWS = 14;

    /** Above this brightness the text turns dark; below it the text stays light. */
    static final float BRIGHT = 0.56f;

    private Contrast() {
    }

    /**
     * The brightness (0 = black, 1 = white) of every cell of the picture, in the order the cells
     * appear on the screen: the picture is shown cropped to the screen, so its middle is what is seen.
     */
    static float[] sampleScreen(Bitmap bmp, int screenW, int screenH) {
        float[] grid = new float[COLS * ROWS];
        if (bmp == null || bmp.isRecycled()) return grid;
        int bw = bmp.getWidth(), bh = bmp.getHeight();
        if (bw <= 0 || bh <= 0 || screenW <= 0 || screenH <= 0) return grid;
        // The picture is scaled up until it covers the screen; the middle of it stays in view.
        float scale = Math.max((float) screenW / bw, (float) screenH / bh);
        float offX = (bw - screenW / scale) / 2f;
        float offY = (bh - screenH / scale) / 2f;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int px = (int) (offX + (col + 0.5f) * screenW / COLS / scale);
                int py = (int) (offY + (row + 0.5f) * screenH / ROWS / scale);
                if (px < 0) px = 0;
                else if (px >= bw) px = bw - 1;
                if (py < 0) py = 0;
                else if (py >= bh) py = bh - 1;
                grid[row * COLS + col] = brightness(bmp.getPixel(px, py));
            }
        }
        return grid;
    }

    /** The brightness of one pixel. */
    static float brightness(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    /** The average brightness under a rectangle of the screen. */
    static float behind(float[] grid, int left, int top, int right, int bottom, int screenW, int screenH) {
        if (grid == null || screenW <= 0 || screenH <= 0) return 0f;
        int c0 = clamp((int) ((long) left * COLS / screenW), 0, COLS - 1);
        int c1 = clamp((int) ((long) right * COLS / screenW), 0, COLS - 1);
        int r0 = clamp((int) ((long) top * ROWS / screenH), 0, ROWS - 1);
        int r1 = clamp((int) ((long) bottom * ROWS / screenH), 0, ROWS - 1);
        float sum = 0f;
        int n = 0;
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                sum += grid[row * COLS + col];
                n++;
            }
        }
        return n == 0 ? 0f : sum / n;
    }

    /** White on a dark picture, near-black on a bright one. */
    static int textColor(float brightness) {
        return brightness > BRIGHT ? 0xFF111114 : 0xFFFFFFFF;
    }

    /** How strong the shade behind the text may be: none on a bright picture, more on a dark one. */
    static int scrimAlpha(float brightness) {
        float dark = 1f - brightness;
        return Math.round(0x70 * Math.min(1f, dark * 1.7f));
    }

    /** The same colour, but darker, so a bright icon stays visible on a bright picture. */
    static int darken(int color, float factor) {
        int a = (color >>> 24);
        int r = Math.round(((color >> 16) & 0xFF) * factor);
        int g = Math.round(((color >> 8) & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** The colour between two colours, for a fade. */
    static int mix(int from, int to, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        int a = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * f);
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * f);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * f);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** The brightness between two brightnesses, for a fade. */
    static float mix(float from, float to, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        return from + (to - from) * f;
    }

    /** The same colour, but in the light or the dark one, keeping its own transparency. */
    static int tint(int color, int rgb, int minAlpha) {
        int a = Color.alpha(color);
        if (a < minAlpha) a = minAlpha;
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
