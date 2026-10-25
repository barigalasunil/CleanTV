package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/** The small Now Playing card beside the weather; it is hidden whenever nothing is playing. */
final class NowPlayingCard {

    private final Activity ctx;
    private final View card;
    private final ImageView art;
    private final ImageView artIcon;
    private final TextView title;
    private final TextView artist;
    private final ImageButton previous;
    private final ImageButton play;
    private final ImageButton next;
    private final Runnable onVisibilityChanged;
    private Bitmap shownArt;

    NowPlayingCard(Activity activity, Runnable onVisibilityChanged) {
        ctx = activity;
        this.onVisibilityChanged = onVisibilityChanged;
        card = activity.findViewById(R.id.nowPlayingCard);
        art = activity.findViewById(R.id.nowArt);
        artIcon = activity.findViewById(R.id.nowArtIcon);
        title = activity.findViewById(R.id.nowTitle);
        artist = activity.findViewById(R.id.nowArtist);
        previous = activity.findViewById(R.id.nowPrevious);
        play = activity.findViewById(R.id.nowPlay);
        next = activity.findViewById(R.id.nowNext);

        previous.setOnClickListener(v -> MediaNoteService.previous());
        play.setOnClickListener(v -> MediaNoteService.toggle());
        next.setOnClickListener(v -> MediaNoteService.next());
    }

    /** Null hides the card and gives the space back to the weather. */
    void bind(MediaNoteService.Now now) {
        int wanted = now == null ? View.GONE : View.VISIBLE;
        if (card.getVisibility() != wanted) {
            card.setVisibility(wanted);
            onVisibilityChanged.run();
        }
        if (now == null) return;

        title.setText(now.title);
        artist.setText(now.artist);
        artist.setVisibility(now.artist.isEmpty() ? View.GONE : View.VISIBLE);
        play.setImageResource(now.playing ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
        play.setContentDescription(ctx.getString(now.playing ? R.string.media_pause : R.string.media_play));
        previous.setEnabled(now.canPrevious);
        previous.setAlpha(now.canPrevious ? 1f : 0.35f);
        next.setEnabled(now.canNext);
        next.setAlpha(now.canNext ? 1f : 0.35f);

        if (now.art != shownArt) {
            shownArt = now.art;
            art.setImageBitmap(now.art);
        }
        boolean hasArt = now.art != null;
        art.setVisibility(hasArt ? View.VISIBLE : View.INVISIBLE);
        artIcon.setVisibility(hasArt ? View.GONE : View.VISIBLE);
        art.setContentDescription(hasArt ? ctx.getString(R.string.media_art, now.title) : null);
    }
}
