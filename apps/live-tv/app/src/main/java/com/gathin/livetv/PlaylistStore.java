package com.gathin.livetv;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Downloads the channel playlists and keeps them on disk.
 *
 * <p>The playlists come from the iptv-org project, one file per language. Each file is cached in the
 * app's own storage so the list still opens when the TV has no internet. A failed download keeps the
 * previous copy instead of wiping the list.
 */
final class PlaylistStore {

    private static final String TAG = "LiveTv";
    private static final String LANG_BASE = "https://iptv-org.github.io/iptv/languages/";
    private static final String ALL_URL = "https://iptv-org.github.io/iptv/index.m3u";
    private static final int TIMEOUT_MS = 25_000;

    /** The pseudo language that loads every channel at once: a much bigger download. */
    static final String ALL = "all";

    private final Context ctx;

    PlaylistStore(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /**
     * The channels of the wanted languages, from the cache when the download fails or is not needed.
     *
     * @param languages    the language codes to load
     * @param refreshHours how old a cached playlist may get before it is downloaded again
     * @param force        download again even when the cache is fresh
     */
    List<Channel> load(Set<String> languages, int refreshHours, boolean force) throws IOException {
        File dir = new File(ctx.getFilesDir(), "playlists");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }

        List<Channel> out = new ArrayList<>();
        IOException failure = null;

        for (String language : languages) {
            File file = new File(dir, language + ".m3u");
            if (force || ageHours(file) > refreshHours) {
                try {
                    download(language, file);
                } catch (IOException e) {
                    Log.w(TAG, "playlist download failed for " + language + ": " + e.getMessage());
                    failure = e;
                }
            }
            if (file.isFile() && file.length() > 0L) {
                out.addAll(PlaylistParser.parse(read(file), language));
            }
        }

        if (out.isEmpty() && failure != null) throw failure;
        return out;
    }

    /** Removes every cached playlist, so the next open downloads them again. */
    void clearCache() {
        File dir = new File(ctx.getFilesDir(), "playlists");
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.delete()) Log.w(TAG, "could not delete " + file);
        }
    }

    /** How many channels the cached playlists hold, for the settings screen. */
    int cachedCount(Set<String> languages) {
        File dir = new File(ctx.getFilesDir(), "playlists");
        int count = 0;
        for (String language : languages) {
            File file = new File(dir, language + ".m3u");
            if (!file.isFile()) continue;
            try {
                count += PlaylistParser.parse(read(file), language).size();
            } catch (IOException e) {
                Log.w(TAG, "cannot read " + file + ": " + e.getMessage());
            }
        }
        return count;
    }

    private static String urlFor(String language) {
        return ALL.equals(language) ? ALL_URL : LANG_BASE + language + ".m3u";
    }

    /** Downloads one playlist to its cache file, replacing it only when the download worked. */
    private void download(String language, File target) throws IOException {
        URL url = new URL(urlFor(language));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "CleanYourTV-LiveTV/1.0");

        File temp = new File(target.getParentFile(), target.getName() + ".part");
        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + url);
            }
            try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
                out.flush();
            }
            if (temp.length() == 0L) throw new IOException("empty download for " + url);
            if (!temp.renameTo(target)) {
                // Some file systems refuse the rename over an existing file: copy instead.
                copy(temp, target);
                if (!temp.delete()) Log.w(TAG, "could not delete " + temp);
            }
        } finally {
            conn.disconnect();
            if (temp.exists() && !temp.delete()) Log.w(TAG, "could not delete " + temp);
        }
    }

    private static void copy(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            out.flush();
        }
    }

    private static String read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, (int) file.length()));
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return out.toString("UTF-8");
        }
    }

    /** The age of the file in hours; a file that is not there counts as infinitely old. */
    private static double ageHours(File file) {
        if (!file.isFile()) return Double.MAX_VALUE;
        long age = System.currentTimeMillis() - file.lastModified();
        return age / 3_600_000.0d;
    }
}
