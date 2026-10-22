package com.gathin.livetv;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** Plays one stream, full screen, with the channel name on top of it. */
public class PlayerActivity extends AppCompatActivity {

    static final String EXTRA_URL = "url";
    static final String EXTRA_NAME = "name";
    static final String EXTRA_LOGO = "logo";

    private ExoPlayer player;
    private PlayerView playerView;
    private TextView channelName;
    private TextView errorText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.player);
        channelName = findViewById(R.id.channelName);
        errorText = findViewById(R.id.playerError);

        String url = getIntent().getStringExtra(EXTRA_URL);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        channelName.setText(name == null ? "" : name);

        if (url == null || url.isEmpty()) {
            errorText.setVisibility(View.VISIBLE);
            errorText.setText(R.string.no_stream);
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText(getString(R.string.play_failed,
                        error.getErrorCodeName()));
            }
        });
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.setPlayWhenReady(true);
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }
}
