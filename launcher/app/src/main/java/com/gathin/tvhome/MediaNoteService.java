package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Watches the TV's media sessions and tells the home screen what is playing.
 *
 * <p>The notification-listener grant is the only way a normal app may call
 * {@link MediaSessionManager#getActiveSessions}. The card is simply absent while that grant is off,
 * and the home screen keeps working. Transport buttons are sent back to the app that owns the
 * session, so the card controls the same player the notification does.
 */
public class MediaNoteService extends NotificationListenerService {

    /** What the card needs to draw. Rebuilt whenever the player changes. */
    static final class Now {
        final String title;
        final String artist;
        final Bitmap art;
        final boolean playing;
        final boolean canPrevious;
        final boolean canNext;

        Now(String title, String artist, Bitmap art, boolean playing, boolean canPrevious, boolean canNext) {
            this.title = title;
            this.artist = artist;
            this.art = art;
            this.playing = playing;
            this.canPrevious = canPrevious;
            this.canNext = canNext;
        }
    }

    interface Listener {
        /** Called on the main thread; null means nothing is playing (or access was taken away). */
        void onNowPlaying(Now now);
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static MediaNoteService instance;
    /** The last state, so a home screen that resumes late still draws the card immediately. */
    private static Now latest;

    private MediaSessionManager sessions;
    private MediaController controller;
    private boolean watchingSessions;
    private final Handler callbackHandler = new Handler(Looper.getMainLooper());
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            controllers -> MAIN.post(this::refreshSessions);

    static void addListener(Listener listener) {
        if (!LISTENERS.contains(listener)) LISTENERS.add(listener);
        listener.onNowPlaying(latest);
        if (instance != null) instance.refreshSessions();
    }

    static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    /** Play/pause whichever player currently owns the card. */
    static void toggle() {
        if (instance == null || instance.controller == null) return;
        PlaybackState state = instance.controller.getPlaybackState();
        boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
        if (playing) instance.controller.getTransportControls().pause();
        else instance.controller.getTransportControls().play();
    }

    static void previous() {
        if (instance != null && instance.controller != null) {
            instance.controller.getTransportControls().skipToPrevious();
        }
    }

    static void next() {
        if (instance != null && instance.controller != null) {
            instance.controller.getTransportControls().skipToNext();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sessions = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        if (sessions != null && !watchingSessions) {
            try {
                sessions.addOnActiveSessionsChangedListener(sessionsListener,
                        new ComponentName(this, MediaNoteService.class));
                watchingSessions = true;
            } catch (SecurityException ignored) {
                // Access was revoked while the service was starting; the card stays away.
            }
        }
        refreshSessions();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        release();
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }

    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            publish();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            publish();
        }

        @Override
        public void onSessionDestroyed() {
            MAIN.post(MediaNoteService.this::refreshSessions);
        }
    };

    private void refreshSessions() {
        if (sessions == null) sessions = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (sessions == null) {
            bind(null);
            return;
        }
        try {
            List<MediaController> controllers = sessions.getActiveSessions(
                    new ComponentName(this, MediaNoteService.class));
            bind(pick(controllers));
        } catch (SecurityException e) {
            // The owner turned notification access off: no sessions, no card.
            bind(null);
        }
    }

    /**
     * Playing wins over paused, then other active states; among equals the player that changed
     * most recently wins. A player registers its session before it reports PLAYING, so we bind to
     * it even while its state is still NONE: otherwise its first play never reaches us.
     */
    private MediaController pick(List<MediaController> controllers) {
        MediaController best = null;
        int bestScore = -1;
        long bestAt = -1L;
        if (controllers == null) return null;
        for (MediaController candidate : controllers) {
            PlaybackState state = candidate.getPlaybackState();
            MediaMetadata metadata = candidate.getMetadata();
            if (state == null || metadata == null) continue;
            if (TextUtils.isEmpty(titleOf(metadata))) continue;
            int score;
            switch (state.getState()) {
                case PlaybackState.STATE_PLAYING: score = 4; break;
                case PlaybackState.STATE_PAUSED: score = 3; break;
                case PlaybackState.STATE_BUFFERING: score = 2; break;
                case PlaybackState.STATE_CONNECTING: score = 1; break;
                default: score = 0; break;
            }
            long at = state.getLastPositionUpdateTime();
            if (score > bestScore || (score == bestScore && at > bestAt)) {
                best = candidate;
                bestScore = score;
                bestAt = at;
            }
        }
        return best;
    }

    private void bind(MediaController next) {
        if (controller == next) {
            publish();
            return;
        }
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
            controller = null;
        }
        controller = next;
        if (controller != null) controller.registerCallback(controllerCallback, callbackHandler);
        publish();
    }

    private void publish() {
        if (controller == null) {
            share(null);
            return;
        }
        PlaybackState state = controller.getPlaybackState();
        MediaMetadata metadata = controller.getMetadata();
        if (state == null || metadata == null) {
            share(null);
            return;
        }
        int playback = state.getState();
        if (playback == PlaybackState.STATE_STOPPED || playback == PlaybackState.STATE_NONE
                || playback == PlaybackState.STATE_ERROR) {
            share(null);
            return;
        }
        String title = titleOf(metadata);
        if (TextUtils.isEmpty(title)) {
            share(null);
            return;
        }
        String artist = text(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
        if (artist.isEmpty() && metadata.getDescription() != null) {
            artist = text(metadata.getDescription().getSubtitle());
        }
        if (artist.isEmpty()) {
            artist = text(metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
        }
        if (artist.isEmpty()) artist = appLabel(controller.getPackageName());

        long actions = state.getActions();
        share(new Now(
                title,
                artist,
                albumArt(metadata),
                state.getState() == PlaybackState.STATE_PLAYING,
                (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0,
                (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0));
    }

    private void share(Now now) {
        latest = now;
        for (Listener listener : LISTENERS) listener.onNowPlaying(now);
    }

    private void release() {
        if (sessions != null && watchingSessions) {
            sessions.removeOnActiveSessionsChangedListener(sessionsListener);
            watchingSessions = false;
        }
        if (controller != null) {
            controller.unregisterCallback(controllerCallback);
            controller = null;
        }
        if (instance == this) instance = null;
        share(null);
    }

    private static String titleOf(MediaMetadata metadata) {
        String title = "";
        if (metadata.getDescription() != null) title = text(metadata.getDescription().getTitle());
        if (title.isEmpty()) title = text(metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        return title;
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private Bitmap albumArt(MediaMetadata metadata) {
        try {
            Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            if (art == null) return null;
            int longest = Math.max(art.getWidth(), art.getHeight());
            if (longest <= 512) return art;
            float scale = 512f / longest;
            return Bitmap.createScaledBitmap(art,
                    Math.max(1, Math.round(art.getWidth() * scale)),
                    Math.max(1, Math.round(art.getHeight() * scale)), true);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private String appLabel(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return "";
        }
    }
}

