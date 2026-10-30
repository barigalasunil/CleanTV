package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

/**
 * The two readings behind the RAM and storage widgets. Both are cheap enough to ask for on a timer:
 * one binder call for memory and one stat on the data partition.
 */
final class SysInfo {

    private SysInfo() {
    }

    /** Memory in use and total memory, in bytes; {0, 0} when the TV will not say. */
    static long[] memory(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return new long[]{0L, 0L};
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            if (info.totalMem <= 0) return new long[]{0L, 0L};
            // availMem is what a new app could take, so caches count as used, as on the TV's own screen.
            return new long[]{info.totalMem - info.availMem, info.totalMem};
        } catch (Exception e) {
            return new long[]{0L, 0L};
        }
    }

    /** Space in use and total space on the data partition, in bytes; {0, 0} on failure. */
    static long[] storage() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            if (total <= 0) return new long[]{0L, 0L};
            return new long[]{total - free, total};
        } catch (Exception e) {
            return new long[]{0L, 0L};
        }
    }

    /** Bytes as the number of gigabytes a widget shows, without the unit. */
    static String gigabytes(long bytes) {
        return String.format(java.util.Locale.US, "%.1f", bytes / (double) (1L << 30));
    }

    /** How full a partition is, from 0 to 1. */
    static float fraction(long[] usedTotal) {
        if (usedTotal == null || usedTotal.length < 2 || usedTotal[1] <= 0) return 0f;
        float f = usedTotal[0] / (float) usedTotal[1];
        return Math.max(0f, Math.min(1f, f));
    }
}
