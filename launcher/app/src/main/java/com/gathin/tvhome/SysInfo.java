package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Environment;
import android.os.StatFs;

import java.util.Locale;

/** Cheap readings for the small status pills: memory, internal storage and link speed. */
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

    static String network(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network net = cm != null ? cm.getActiveNetwork() : null;
            NetworkCapabilities caps = net != null ? cm.getNetworkCapabilities(net) : null;
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return ctx.getString(R.string.offline);
            }
            int kbps = caps.getLinkDownstreamBandwidthKbps();
            if (kbps <= 0) return ctx.getString(R.string.online);
            if (kbps >= 1_000_000) return String.format(Locale.US, "%.1f Gbps", kbps / 1_000_000f);
            if (kbps >= 1_000) return String.format(Locale.US, "%.0f Mbps", kbps / 1_000f);
            return kbps + " Kbps";
        } catch (Exception e) {
            return ctx.getString(R.string.offline);
        }
    }

    private static String size(long bytes) {
        if (bytes >= 1L << 30) return String.format(Locale.US, "%.1f GB", bytes / (float) (1L << 30));
        if (bytes >= 1L << 20) return String.format(Locale.US, "%.0f MB", bytes / (float) (1L << 20));
        return String.format(Locale.US, "%.0f KB", bytes / 1024f);
    }
}
