package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The wallpaper behind the home screen: the pictures in {@code assets/wallpapers/} (wall-01.jpg …),
 * one at a time, each on screen for a while and then a slow crossfade to the next.
 *
 * <p>Only two pictures are held in memory at a time (one on screen, one coming), each downsampled to
 * the screen: a 1080p wallpaper in 16-bit colour is about 4 MB, which a TV with 1 GB of RAM can spare.
 */
final class Wallpapers {

    /** Where the pictures live in the app's assets. */
    private static final String DIR = "wallpapers";
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

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<String> files = new ArrayList<>();

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
        files.addAll(list(ctx));
    }

    /** The wallpaper pictures in the app's assets, sorted by name; empty when there are none. */
    static List<String> list(Context ctx) {
        String[] names;
        try {
            names = ctx.getAssets().list(DIR);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (names == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String n : names) {
            String lower = n.toLowerCase(java.util.Locale.US);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                out.add(DIR + "/" + n);
            }
        }
        Collections.sort(out);
        return out;
    }

    boolean isEmpty() {
        return files.isEmpty();
    }

    int count() {
        return files.size();
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
        if (files.isEmpty() || stopped) return;
        intervalMs = Math.max(1000L, interval);
        long now = System.currentTimeMillis();
        long window = now / intervalMs;
        index = (int) Math.floorMod(window, files.size());
        show(index, false);
        if (files.size() < 2) return;
        long nextAt = (window + 1) * intervalMs;
        handler.postDelayed(this::next, Math.max(1000L, nextAt - now));
    }

    /** Told when a new picture is on screen. */
    void setListener(Listener l) {
        listener = l;
    }

    /** The next picture, with a crossfade. */
    private void next() {
        if (stopped || files.isEmpty()) return;
        index = (index + 1) % files.size();
        show(index, true);
        handler.postDelayed(this::next, intervalMs);
    }

    /** Shows the picture at this index; with a fade it crossfades over the one on screen. */
    private void show(int target, boolean fade) {
        final String name = files.get(target);
        io.execute(() -> {
            Bitmap bmp = decode(name);
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

    /** Decodes a wallpaper, downsampled to the screen so a 4K picture costs no more than a 1080p one. */
    private Bitmap decode(String name) {
        java.io.InputStream in = null;
        try {
            android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            in = ctx.getAssets().open(name);
            BitmapFactory.decodeStream(in, null, opts);
            in.close();
            in = null;
            int sample = 1;
            while (opts.outWidth / (sample * 2) >= dm.widthPixels && opts.outHeight / (sample * 2) >= dm.heightPixels) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            in = ctx.getAssets().open(name);
            return BitmapFactory.decodeStream(in, null, opts);
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

    /** Frees the pictures; the launcher stops the wallpaper when it goes away. */
    void stop() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
        io.shutdownNow();
        if (currentBitmap != null && !currentBitmap.isRecycled()) currentBitmap.recycle();
        currentBitmap = null;
    }
}
