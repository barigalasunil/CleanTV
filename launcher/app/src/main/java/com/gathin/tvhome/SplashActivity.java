package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The welcome screen: the splash picture full screen, the message near the bottom, and the home
 * screen after it. It is shown once per boot (see {@link Splash}) and any remote key skips it.
 *
 * <p>The Android boot logo and the boot animation are not touched: that needs root. This is the
 * launcher's own welcome screen, in the app's own window.
 */
public class SplashActivity extends Activity {

    /** The picture in the app's assets. */
    private static final String IMAGE = "splash/splash.jpg";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View root;
    private boolean leaving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config cfg = Config.load(this);
        if (!cfg.splashEnabled) {
            finish();
            return;
        }
        Bitmap picture = decode();   // may be null: the splash is then skipped
        if (picture == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_splash);
        root = findViewById(R.id.splashRoot);
        ImageView image = findViewById(R.id.splashImage);
        image.setImageBitmap(picture);
        TextView text = findViewById(R.id.splashText);
        text.setText(message(cfg));

        root.post(() -> place(text, cfg.splashTextBottomPercent, cfg.splashTextAlign));

        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(420).start();
        handler.postDelayed(this::leave, Math.max(500, cfg.splashDurationMs));
    }

    /** The message with the name filled in; without a name only the text before %s is kept. */
    private String message(Config cfg) {
        String text = cfg.splashMessage == null ? "" : cfg.splashMessage;
        String name = cfg.greetingName;
        if (text.contains("%s")) {
            text = name.isEmpty() ? text.replace("%s", "") : text.replace("%s", name);
        }
        return text.trim();
    }

    /**
     * Puts the message the configured distance above the bottom edge, and lines it up to the left,
     * to the right or in the middle. The distance is a percentage of the screen height.
     */
    private void place(TextView text, int percent, String align) {
        if (root == null) return;
        ViewGroup.LayoutParams lp = text.getLayoutParams();
        if (!(lp instanceof FrameLayout.LayoutParams)) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
        int gravity;
        int inside;
        switch (align) {
            case "left":
                gravity = Gravity.BOTTOM | Gravity.START;
                inside = Gravity.START;
                break;
            case "right":
                gravity = Gravity.BOTTOM | Gravity.END;
                inside = Gravity.END;
                break;
            default:
                gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                inside = Gravity.CENTER_HORIZONTAL;
                break;
        }
        params.gravity = gravity;
        params.bottomMargin = Math.round(root.getHeight() * Math.max(0, Math.min(60, percent)) / 100f);
        text.setLayoutParams(params);
        // The message fills the whole width, so the layout gravity cannot line it up: the text
        // itself has to be told where it goes inside the view.
        text.setGravity(Gravity.TOP | inside);
    }

    /** Any key skips the splash; it does not come back until the next boot. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            leave();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        leave();
    }

    /** Fades out and returns to the home screen underneath. */
    private void leave() {
        if (leaving) return;
        leaving = true;
        handler.removeCallbacksAndMessages(null);
        if (root != null) {
            root.animate().alpha(0f).setDuration(320).withEndAction(this::finish).start();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /** Decodes the splash picture, downsampled to the screen so a 4K picture costs no more than a 1080p one. */
    private Bitmap decode() {
        java.io.InputStream in = null;
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            in = getAssets().open(IMAGE);
            BitmapFactory.decodeStream(in, null, opts);
            in.close();
            in = null;
            int sample = 1;
            while (opts.outWidth / (sample * 2) >= dm.widthPixels && opts.outHeight / (sample * 2) >= dm.heightPixels) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            in = getAssets().open(IMAGE);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            return bmp;
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
}
