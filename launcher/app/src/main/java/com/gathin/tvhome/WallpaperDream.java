package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Random;

/**
 * Screensaver: the launcher's own wallpapers, one after the other with a slow crossfade, and the
 * time and the date drifting over them.
 *
 * <p>Only the pictures in {@code assets/wallpapers} are used, and only two of them are held in
 * memory at a time (the one on screen and the one coming in), each downsampled to the screen: the
 * same price as the home screen's wallpaper. The clock is coloured for the picture under it, so it
 * can be read on a dark and on a bright wallpaper alike. Any key press ends the screensaver.
 */
public class WallpaperDream extends DreamService {

    /** How long the clock stays where it is before it drifts to a new spot. */
    private static final long MOVE_MS = 15_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private Wallpapers wallpapers;
    private FrameLayout root;
    private View content;
    private View scrim;
    private GradientDrawable scrimGlow;
    private TextClock time;
    private TextClock meridiem;
    private TextClock date;
    private View nowCard;
    private TextView nowTitle;
    private TextView nowArtist;
    /** How bright the wallpaper on screen is, cell by cell. */
    private float[] grid;
    /** The colours the clock had before the wallpaper was read. */
    private int timeColor, meridiemColor, dateColor;
    /** How long each wallpaper is on screen, in milliseconds. */
    private long intervalMs = 30_000L;
    /** True once the clock has been placed on the screen. */
    private boolean placed;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    /** The wallpaper changed: the clock is given a colour that can be read on it. */
    private final Wallpapers.Listener wallpaperListener = new Wallpapers.Listener() {
        @Override
        public void onPicture(float[] grid) {
            WallpaperDream.this.grid = grid;
            applyContrast();
        }

        @Override
        public void onFade(float[] from, float[] to) {
            float fromBrightness = brightnessUnder(from);
            float toBrightness = brightnessUnder(to);
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(Motion.ms(Wallpapers.FADE_MS));
            anim.setInterpolator(Motion.EASE_OUT);
            anim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                applyContrast(Contrast.mix(fromBrightness, toBrightness, t));
            });
            anim.start();
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_wallpaper);

        root = findViewById(R.id.dreamRoot);
        content = findViewById(R.id.dreamContent);
        scrim = findViewById(R.id.dreamScrim);
        time = findViewById(R.id.dreamTime);
        meridiem = findViewById(R.id.dreamMeridiem);
        date = findViewById(R.id.dreamDate);
        nowCard = findViewById(R.id.dreamNowPlaying);
        nowTitle = findViewById(R.id.dreamNowTitle);
        nowArtist = findViewById(R.id.dreamNowArtist);
        timeColor = time.getTextColors().getDefaultColor();
        meridiemColor = meridiem.getTextColors().getDefaultColor();
        dateColor = date.getTextColors().getDefaultColor();

        // The shade behind the clock: dark in the middle, fading out towards the edges.
        scrimGlow = new GradientDrawable();
        scrimGlow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        scrimGlow.setColors(new int[]{0xB3000000, 0x66000000, 0x00000000});
        scrim.setBackground(scrimGlow);
        scrim.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                scrimGlow.setGradientRadius((r - l) * 0.6f));

        Config cfg = Config.load(this);
        intervalMs = cfg.screensaverIntervalSeconds * 1000L;
        if (cfg.screensaverEnabled) {
            wallpapers = new Wallpapers(this);
            if (!wallpapers.isEmpty()) {
                wallpapers.setListener(wallpaperListener);
                wallpapers.attach(root);
            } else {
                wallpapers = null;
            }
        }
        // The clock is placed once the screen is measured.
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (!placed && r - l > 0 && content.getWidth() > 0) {
                placed = true;
                placeClock();
            }
        });
    }

    /** The screensaver only reads the player; its keys are never touched from here. */
    private final MediaNoteService.Listener mediaListener = now -> {
        if (nowCard == null) return;
        nowCard.setVisibility(now == null ? View.GONE : View.VISIBLE);
        if (now == null) return;
        nowTitle.setText(now.title);
        nowArtist.setText(now.artist);
        nowArtist.setVisibility(now.artist.isEmpty() ? View.GONE : View.VISIBLE);
    };

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        MediaNoteService.addListener(mediaListener);
        // The screensaver honours the same animator speed setting as Home.
        Motion.refresh(this);
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(Motion.ms(Motion.SLOW))
                    .setInterpolator(Motion.EASE_OUT).start();
        });
        if (wallpapers != null) wallpapers.startMs(intervalMs);
        handler.postDelayed(tick, MOVE_MS);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        MediaNoteService.removeListener(mediaListener);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        MediaNoteService.removeListener(mediaListener);
        if (wallpapers != null) {
            wallpapers.stop();
            wallpapers = null;
        }
        super.onDetachedFromWindow();
    }

    /** Any key press ends the screensaver. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void moveClock() {
        content.animate().cancel();
        if (Motion.off()) {
            placeClock();
            content.setAlpha(1f);
            return;
        }
        // The card fades where it is, comes back at the new spot: one motion, from Motion.
        content.animate().alpha(0f).setDuration(Motion.ms(Motion.SLOW))
                .setInterpolator(Motion.EASE_OUT)
                .withEndAction(() -> {
                    placeClock();
                    content.animate().alpha(1f).setDuration(Motion.ms(Motion.SLOW))
                            .setInterpolator(Motion.EASE_OUT).start();
                }).start();
    }

    /** A random spot that keeps the whole clock on screen. */
    private void placeClock() {
        ViewGroup parent = (ViewGroup) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        content.setX(maxX * (0.1f + 0.8f * random.nextFloat()));
        content.setY(maxY * (0.1f + 0.8f * random.nextFloat()));
        // The picture under the new spot may be brighter or darker than the old one.
        applyContrast();
    }

    /** How bright the wallpaper is under the clock. */
    private float brightnessUnder(float[] grid) {
        if (grid == null || content == null || root == null) return 0f;
        int w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0 || content.getWidth() == 0) return 0f;
        Rect r = new Rect(0, 0, content.getWidth(), content.getHeight());
        root.offsetDescendantRectToMyCoords(content, r);
        return Contrast.behind(grid, r.left, r.top, r.right, r.bottom, w, h);
    }

    /** Colours the clock for the wallpaper under it: white on a dark picture, near-black on a bright one. */
    private void applyContrast() {
        applyContrast(brightnessUnder(grid));
    }

    private void applyContrast(float brightness) {
        if (time == null) return;
        boolean onBright = brightness > Contrast.BRIGHT;
        int light = Contrast.textColor(brightness);
        int shadow = onBright ? 0x59FFFFFF : 0x8C000000;
        time.setTextColor(Contrast.tint(timeColor, light, onBright ? 0xD9 : 0x00));
        meridiem.setTextColor(Contrast.tint(meridiemColor, light, onBright ? 0xD9 : 0x00));
        date.setTextColor(Contrast.tint(dateColor, light, onBright ? 0xD9 : 0x00));
        time.setShadowLayer(4 * getResources().getDisplayMetrics().density, 0, 2, shadow);
        meridiem.setShadowLayer(4 * getResources().getDisplayMetrics().density, 0, 2, shadow);
        date.setShadowLayer(4 * getResources().getDisplayMetrics().density, 0, 2, shadow);
        // The shade fades away on a bright picture, where the clock is dark anyway.
        if (scrim != null) scrim.setAlpha(Contrast.scrimAlpha(brightness) / 255f);
    }
}
