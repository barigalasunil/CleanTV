package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * The welcome splash: shown once per boot, before the home screen, and skipped with any key.
 *
 * <p>"Once per boot" is decided by the boot counter the system keeps
 * ({@link Settings.Global#BOOT_COUNT}). On a TV without that setting the clock since boot is used
 * instead: it restarts at zero on every boot, so a smaller value than the one saved last time means
 * the TV has been restarted.
 */
final class Splash {

    private static final String PREFS = "splash";
    private static final String KEY_BOOT = "boot_count";
    private static final String KEY_ELAPSED = "elapsed";

    private Splash() {
    }

    /** True when the splash has not been shown since the TV last booted. */
    static boolean due(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int boot = bootCount(ctx);
        long elapsed = SystemClock.elapsedRealtime();
        int savedBoot = prefs.getInt(KEY_BOOT, -1);
        long savedElapsed = prefs.getLong(KEY_ELAPSED, -1L);

        if (boot >= 0) {
            if (savedBoot == boot) return false;
            prefs.edit().putInt(KEY_BOOT, boot).putLong(KEY_ELAPSED, elapsed).apply();
            return true;
        }
        // No boot counter: the clock since boot only goes backwards when the TV has been restarted.
        if (savedElapsed < 0L) {
            prefs.edit().putInt(KEY_BOOT, -1).putLong(KEY_ELAPSED, elapsed).apply();
            return true;
        }
        if (elapsed < savedElapsed) {
            prefs.edit().putInt(KEY_BOOT, -1).putLong(KEY_ELAPSED, elapsed).apply();
            return true;
        }
        return false;
    }

    /** The boot counter, or -1 when the TV does not keep one. */
    private static int bootCount(Context ctx) {
        try {
            return Settings.Global.getInt(ctx.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (Exception e) {
            return -1;
        }
    }

    /** The splash activity, started on top of the home screen. */
    static Intent intent(Context ctx) {
        return new Intent(ctx, SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }
}
