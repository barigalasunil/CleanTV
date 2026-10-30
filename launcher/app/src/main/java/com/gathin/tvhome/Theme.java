package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The owner's theme, pushed over ADB into the app's external files directory:
 * {@code /sdcard/Android/data/com.gathin.tvhome/files/theme/}.
 *
 * <p>It holds {@code config.json}, an optional {@code avatar.jpg} and {@code splash.jpg}, the
 * wallpapers in {@code wallpapers/}, their thumbnails in {@code thumbs/} and an optional
 * {@code wallpapers.json} that names them. Nothing here is bundled in the APK and the app needs no
 * storage permission: this is the app's own external folder.
 */
final class Theme {
    private static final String TAG = "TvHome.Theme";

    static final String DIR = "theme";
    static final String CONFIG = "config.json";
    static final String AVATAR = "avatar.jpg";
    static final String SPLASH = "splash.jpg";
    static final String WALLPAPERS = "wallpapers";
    static final String THUMBS = "thumbs";
    static final String INDEX = "wallpapers.json";

    static final class Wallpaper {
        final File file;
        final File thumb;
        final String name;
        final String mood;

        Wallpaper(File file, File thumb, String name, String mood) {
            this.file = file;
            this.thumb = thumb;
            this.name = name;
            this.mood = mood;
        }

        /** Stable key for saving the owner's choice: the file name without its folder. */
        String key() {
            return file.getName();
        }
    }

    private Theme() {
    }

    /** The theme folder; null when external storage is unavailable. */
    static File dir(Context ctx) {
        File base = Config.dir(ctx);
        return base == null ? null : new File(base, DIR);
    }

    static File config(Context ctx) {
        return file(ctx, CONFIG);
    }

    static File avatar(Context ctx) {
        return picture(ctx, AVATAR);
    }

    static File splash(Context ctx) {
        return picture(ctx, SPLASH);
    }

    /** The folder the wallpaper json refers to; null when external storage is unavailable. */
    static File wallpapersDir(Context ctx) {
        return folder(ctx, WALLPAPERS);
    }

    static File thumbsDir(Context ctx) {
        return folder(ctx, THUMBS);
    }

    static File index(Context ctx) {
        return file(ctx, INDEX);
    }

    /** Reads the theme settings; null when there is no readable file. */
    static JSONObject readConfig(Context ctx) {
        File file = config(ctx);
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > 128 * 1024) return null;
        try {
            JSONObject json = new JSONObject(readText(file));
            return json;
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * The wallpapers a theme brought, in the order given by {@code wallpapers.json}; without that
     * file every picture in {@code wallpapers/} is listed by name. Missing or corrupt entries are
     * left out, never fatal, and the list is capped at {@link ThemeIndex#MAX}.
     */
    static List<Wallpaper> wallpapers(Context ctx) {
        File pictures = wallpapersDir(ctx);
        if (pictures == null) return Collections.emptyList();
        File thumbs = thumbsDir(ctx);

        List<String> names = new ArrayList<>();
        File[] files = pictures.listFiles();
        if (files != null) {
            for (File file : files) {
                if (isPicture(file)) names.add(file.getName());
            }
        }
        List<ThemeIndex.Entry> entries = ThemeIndex.parse(readTextOrNull(index(ctx)), names::contains);
        if (entries.isEmpty()) entries = ThemeIndex.scan(names);

        List<Wallpaper> out = new ArrayList<>();
        for (ThemeIndex.Entry entry : entries) {
            File picture = new File(pictures, entry.file);
            File thumb = entry.thumb == null ? null : new File(thumbs, entry.thumb);
            if (thumb != null && !isPicture(thumb)) thumb = null;
            out.add(new Wallpaper(picture, thumb, entry.name, entry.mood));
        }
        return out;
    }

    /** The picture for one wallpapers.json entry; missing or unreadable files are skipped. */
    static Wallpaper wallpaper(Context ctx, String key) {
        if (key == null || key.isEmpty()) return null;
        for (Wallpaper wallpaper : wallpapers(ctx)) {
            if (key.equals(wallpaper.key())) return wallpaper;
        }
        return null;
    }

    /** Decodes a theme picture, downsampled to the box; null when it is missing or damaged. */
    static Bitmap decode(File file, int maxWidth, int maxHeight) {
        if (file == null || !file.isFile() || file.length() <= 0) return null;
        InputStream in = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            in = new FileInputStream(file);
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            in = null;
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= maxWidth
                    && bounds.outHeight / (sample * 2) >= maxHeight) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            in = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);
            if (bitmap == null) return null;

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width <= maxWidth && height <= maxHeight) return bitmap;
            float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
            int scaledWidth = Math.max(1, Math.round(width * scale));
            int scaledHeight = Math.max(1, Math.round(height * scale));
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
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

    /** Decodes for the current screen, for a full-screen picture. */
    static Bitmap decodeScreen(Context ctx, File file) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        return decode(file, dm.widthPixels, dm.heightPixels);
    }

    /** True when the file is a picture this launcher can read. */
    static boolean isPicture(File file) {
        if (file == null || !file.isFile() || file.length() <= 0) return false;
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".webp") || name.endsWith(".bmp");
    }

    private static File file(Context ctx, String name) {
        File dir = dir(ctx);
        return dir == null ? null : new File(dir, name);
    }

    private static File folder(Context ctx, String name) {
        File dir = dir(ctx);
        return dir == null ? null : new File(dir, name);
    }

    private static File picture(Context ctx, String name) {
        File file = file(ctx, name);
        return isPicture(file) ? file : null;
    }

    private static String readTextOrNull(File file) {
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > 256 * 1024) return null;
        try {
            return readText(file);
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String readText(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int read = 0, n;
            while (read < buffer.length && (n = in.read(buffer, read, buffer.length - read)) > 0) {
                read += n;
            }
            String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        }
    }

    /** The file name part of a theme-relative path; null for empty or unsafe names. */
    static String baseName(String value) {
        return ThemeIndex.name(value);
    }
}
