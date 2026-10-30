package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The wallpaper behind the home screen and the screensaver: the pictures the owner pushed into the
 * theme folder ({@code theme/wallpapers/}), or the stock pictures in {@code assets/wallpapers/} when
 * the theme has none. One picture at a time, each on screen for a while and then a slow crossfade.
 *
 * <p>The owner's selection is one of three modes, stored in the app's own preferences: rotate all
 * pictures (the default), rotate only one mood, or pin one picture. The home screen and the
 * screensaver both read it through {@link #choice(Context)}.
 *
 * <p>Only two full pictures are held in memory at a time (one on screen, one coming), each
 * downsampled to the screen: a 1080p wallpaper in 16-bit colour is about 4 MB, which a TV with
 * 1 GB of RAM can spare.
 */
final class Wallpapers {

    /** Where the stock pictures live in the app's assets. */
    private static final String ASSET_DIR = "wallpapers";
    /** The saved mode, mood and pinned picture. */
    private static final String PREFS = "wallpapers";
    private static final String MODE = "mode";
    private static final String MOOD = "mood";
    private static final String KEY = "key";
    /** The single choice saved before the modes existed; a pin to that picture is kept. */
    private static final String LEGACY_SELECTED = "selected";
    /** How long the crossfade takes. */
    static final long FADE_MS = 1200L;

    /**
     * Told when a picture is on screen, so that text on top of it can be given a colour that can be
     * read on any wallpaper.
     */
    interface Listener {
        /** A picture is on screen, without a fade. */
        void onPicture(float[] grid);

        /** A crossfade from one picture to the next is running. */
        void onFade(float[] from, float[] to);
    }

    /** One picture the picker can show: a file from the theme, or one of the stock pictures. */
    static final class Picture {
        final String key;
        final String name;
        final String mood;
        /** The asset path for a stock picture, null for a theme picture. */
        final String asset;
        final File file;
        final File thumb;

        Picture(String key, String name, String mood, String asset, File file, File thumb) {
            this.key = key;
            this.name = name;
            this.mood = mood;
            this.asset = asset;
            this.file = file;
            this.thumb = thumb;
        }

        boolean stock() {
            return asset != null;
        }
    }

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Picture> pictures;
    private WallpaperChoice choice;
    /** The pictures the current choice actually rotates through. */
    private List<Picture> active;

    private ImageView current;
    private ImageView incoming;
    private Bitmap currentBitmap;
    /** How bright the picture on screen is, cell by cell. */
    private float[] currentGrid;
    private Listener listener;
    private int index;
    private long intervalMs = 600_000L;
    private boolean stopped;

    Wallpapers(Context ctx) {
        this.ctx = ctx;
        pictures = pictures(ctx);
        choice = choice(ctx);
        active = scope();
    }

    /**
     * Every picture the picker and the rotation can use: the theme's pictures in the order
     * {@code wallpapers.json} gives, capped at {@link ThemeIndex#MAX}. With no theme pictures at
     * all, the stock pictures in the APK are listed instead.
     */
    static List<Picture> pictures(Context ctx) {
        List<Picture> out = new ArrayList<>();
        for (Theme.Wallpaper wallpaper : Theme.wallpapers(ctx)) {
            out.add(new Picture(wallpaper.key(), wallpaper.name, wallpaper.mood,
                    null, wallpaper.file, wallpaper.thumb));
        }
        if (out.isEmpty()) {
            for (String asset : list(ctx)) {
                out.add(new Picture("stock:" + asset, stockName(asset), "", asset, null, null));
            }
        }
        if (out.size() > ThemeIndex.MAX) return new ArrayList<>(out.subList(0, ThemeIndex.MAX));
        return out;
    }

    /** The stock wallpaper pictures, sorted by name; empty when there are none. */
    static List<String> list(Context ctx) {
        String[] names;
        try {
            names = ctx.getAssets().list(ASSET_DIR);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (names == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String n : names) {
            String lower = n.toLowerCase(Locale.US);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                out.add(ASSET_DIR + "/" + n);
            }
        }
        Collections.sort(out);
        return out;
    }

    boolean isEmpty() {
        return pictures.isEmpty();
    }

    /** The saved choice, with an old single-picture choice read as a pin. */
    static WallpaperChoice choice(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String mode = prefs.getString(MODE, "");
        if (mode == null || mode.isEmpty()) {
            String legacy = prefs.getString(LEGACY_SELECTED, null);
            return legacy == null || legacy.isEmpty() ? WallpaperChoice.all() : WallpaperChoice.pin(legacy);
        }
        return WallpaperChoice.of(mode, prefs.getString(MOOD, ""), prefs.getString(KEY, ""));
    }

    /** Remembers the mode, the mood and the pinned picture. */
    static void saveChoice(Context ctx, WallpaperChoice choice) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(MODE, choice.mode)
                .putString(MOOD, choice.mood)
                .putString(KEY, choice.key)
                .remove(LEGACY_SELECTED)
                .apply();
    }

    /** Adds the wallpaper views behind everything else in the launcher. */
    void attach(FrameLayout root) {
        current = new ImageView(ctx);
        incoming = new ImageView(ctx);
        for (ImageView v : new ImageView[]{current, incoming}) {
            v.setScaleType(ImageView.ScaleType.CENTER_CROP);
            v.setImportantForAccessibility(ViewGroup.IMPORTANT_FOR_ACCESSIBILITY_NO);
            root.addView(v, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        incoming.setVisibility(View.INVISIBLE);
    }

    /**
     * Shows the wallpaper that belongs to this time of day and changes it every {@code seconds}.
     * The change is tied to the clock, so a restart of the launcher does not reset the cycle.
     */
    void start(int seconds) {
        startMs(Math.max(1, seconds) * 1000L);
    }

    /** The same, but with the time between two pictures in milliseconds. */
    void startMs(long interval) {
        if (pictures.isEmpty() || stopped) return;
        intervalMs = Math.max(1000L, interval);
        active = scope();
        long now = System.currentTimeMillis();
        long window = now / intervalMs;
        index = (int) Math.floorMod(window, active.size());
        show(index, false);
        schedule(window);
    }

    /**
     * Applies the owner's choice now: the rotation is rebuilt around the new mode and the first
     * picture of it crossfades in without waiting for the next turn.
     */
    void applyChoice(WallpaperChoice chosen, int seconds) {
        choice = chosen;
        saveChoice(ctx, chosen);
        active = scope();
        handler.removeCallbacksAndMessages(null);
        stopped = false;
        intervalMs = Math.max(1000L, seconds * 1000L);
        long window = System.currentTimeMillis() / intervalMs;
        index = chosen.isPin() ? 0 : (int) Math.floorMod(window, active.size());
        if (active.isEmpty()) return;
        show(index, currentBitmap != null);
        schedule(window);
    }

    /** Told when a new picture is on screen. */
    void setListener(Listener l) {
        listener = l;
    }

    /** The next picture, with a crossfade. */
    private void next() {
        if (stopped || active.size() < 2) return;
        index = (index + 1) % active.size();
        show(index, true);
        handler.postDelayed(this::next, intervalMs);
    }

    /** Plans the next change on the clock's own boundary; a pinned picture never changes. */
    private void schedule(long window) {
        if (stopped || choice.isPin() || active.size() < 2) return;
        long nextAt = (window + 1) * intervalMs;
        handler.postDelayed(this::next, Math.max(1000L, nextAt - System.currentTimeMillis()));
    }

    /** The pictures the current choice rotates through. */
    private List<Picture> scope() {
        if (choice.isPin()) {
            for (Picture picture : pictures) {
                if (picture.key.equals(choice.key)) return Collections.singletonList(picture);
            }
            return pictures;
        }
        if (choice.isMood()) {
            List<Picture> out = new ArrayList<>();
            for (Picture picture : pictures) {
                if (picture.mood.equalsIgnoreCase(choice.mood)) out.add(picture);
            }
            if (!out.isEmpty()) return out;
        }
        return pictures;
    }

    /** Shows the picture at this index; with a fade it crossfades over the one on screen. */
    private void show(int target, boolean fade) {
        if (target < 0 || target >= active.size()) return;
        final Picture picture = active.get(target);
        io.execute(() -> {
            Bitmap bmp = picture.stock()
                    ? decodeAsset(ctx, picture.asset, screenWidth(), screenHeight())
                    : Theme.decodeScreen(ctx, picture.file);
            // How bright the picture is, read at a few hundred points: the text on top of it needs to
            // know, and the whole picture must not be walked over.
            float[] grid = bmp == null ? null : Contrast.sampleScreen(bmp, screenWidth(), screenHeight());
            handler.post(() -> {
                if (bmp == null || stopped || current == null) {
                    if (bmp != null) bmp.recycle();
                    return;
                }
                if (!fade) {
                    currentBitmap = bmp;
                    currentGrid = grid;
                    current.setImageBitmap(bmp);
                    current.setAlpha(1f);
                    if (listener != null) listener.onPicture(grid);
                    return;
                }
                incoming.setImageBitmap(bmp);
                incoming.setAlpha(0f);
                incoming.setVisibility(View.VISIBLE);
                incoming.animate().alpha(1f).setDuration(FADE_MS).start();
                current.animate().alpha(0f).setDuration(FADE_MS).start();
                if (listener != null) listener.onFade(currentGrid, grid);
                handler.postDelayed(() -> {
                    Bitmap old = currentBitmap;
                    currentBitmap = bmp;
                    currentGrid = grid;
                    ImageView swap = current;
                    current = incoming;
                    incoming = swap;
                    incoming.setImageBitmap(null);
                    incoming.setAlpha(1f);
                    if (old != null && !old.isRecycled()) old.recycle();
                }, FADE_MS + 120L);
            });
        });
    }

    /** The width of the screen, in pixels. */
    private int screenWidth() {
        return ctx.getResources().getDisplayMetrics().widthPixels;
    }

    /** The height of the screen, in pixels. */
    private int screenHeight() {
        return ctx.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * Decodes a stock picture, downsampled so it fits the box at most: a 4K picture costs no more
     * than a 1080p one. Used for the full wallpaper and for the picker's previews.
     */
    static Bitmap decodeAsset(Context ctx, String name, int maxWidth, int maxHeight) {
        InputStream in = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            in = ctx.getAssets().open(name);
            BitmapFactory.decodeStream(in, null, opts);
            in.close();
            in = null;
            int sample = 1;
            while (opts.outWidth / (sample * 2) >= maxWidth && opts.outHeight / (sample * 2) >= maxHeight) {
                sample *= 2;
            }
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            in = ctx.getAssets().open(name);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);
            if (bitmap == null) return null;
            if (bitmap.getWidth() <= maxWidth && bitmap.getHeight() <= maxHeight) return bitmap;
            float scale = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.max(1, Math.round(bitmap.getWidth() * scale)),
                    Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
            if (scaled != bitmap) bitmap.recycle();
            return scaled;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** "wall-01" for the stock pictures; only a fallback label, never a file name. */
    private static String stockName(String asset) {
        String name = asset.substring(asset.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        name = name.replace('-', ' ').replace('_', ' ').trim();
        if (name.isEmpty()) return "Wallpaper";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** Frees the pictures; the launcher stops the wallpaper when it goes away. */
    void stop() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
        io.shutdownNow();
        if (currentBitmap != null && !currentBitmap.isRecycled()) currentBitmap.recycle();
        currentBitmap = null;
    }
}
