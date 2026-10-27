package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.util.Locale;

/** Cheap readings for the home widgets: memory and internal storage. */
final class SysInfo {

    private SysInfo() {
    }

    static String ram(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return "";
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            if (info.totalMem <= 0) return "";
            return size(info.totalMem - info.availMem) + " / " + size(info.totalMem);
        } catch (Exception e) {
            return "";
        }
    }

    static String storage(Context ctx) {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            if (total <= 0) return "";
            return size(total - free) + " / " + size(total);
        } catch (Exception e) {
            return "";
        }
    }

    private static String size(long bytes) {
        if (bytes >= 1L << 30) return String.format(Locale.US, "%.1f GB", bytes / (float) (1L << 30));
        if (bytes >= 1L << 20) return String.format(Locale.US, "%.0f MB", bytes / (float) (1L << 20));
        return String.format(Locale.US, "%.0f KB", bytes / 1024f);
    }
}
