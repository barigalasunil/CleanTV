package com.gathin.livetv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fills the logo of a channel card.
 *
 * <p>Small on purpose: a memory cache, three download threads, and a placeholder until the picture
 * arrives. Only the cards on screen ask for a logo, so a list of a few thousand channels costs little.
 */
final class LogoLoader {

    private static final int TIMEOUT_MS = 12_000;
    private static final int MAX_EDGE = 320;

    private final LruCache<String, Bitmap> cache;
    private final ExecutorService pool = Executors.newFixedThreadPool(3);

    LogoLoader() {
        int budget = (int) (Runtime.getRuntime().maxMemory() / 8192L);
        cache = new LruCache<String, Bitmap>(Math.max(2 * 1024 * 1024, budget)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    /** Shows the logo of the given URL, or a placeholder when there is none. */
    void load(ImageView view, String url) {
        if (url == null || url.isEmpty()) {
            view.setTag(null);
            view.setImageResource(R.drawable.ic_channel);
            return;
        }

        Bitmap cached = cache.get(url);
        if (cached != null) {
            view.setTag(url);
            view.setImageBitmap(cached);
            return;
        }

        view.setTag(url);
        view.setImageResource(R.drawable.ic_channel);
        pool.execute(() -> {
            Bitmap bitmap = fetch(url);
            if (bitmap == null) return;
            cache.put(url, bitmap);
            view.post(() -> {
                // The card may have been recycled onto another channel while the download ran.
                if (url.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap fetch(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) return null;
            try (InputStream in = conn.getInputStream()) {
                byte[] data = readAll(in);
                Bitmap full = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (full == null) return null;
                int edge = Math.max(full.getWidth(), full.getHeight());
                if (edge <= MAX_EDGE) return full;
                float scale = (float) MAX_EDGE / edge;
                return Bitmap.createScaledBitmap(full,
                        Math.max(1, Math.round(full.getWidth() * scale)),
                        Math.max(1, Math.round(full.getHeight() * scale)), true);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(16 * 1024);
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
