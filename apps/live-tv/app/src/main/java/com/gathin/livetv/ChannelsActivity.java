package com.gathin.livetv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * The channel list.
 *
 * <p>Loads the playlists in the background, shows them as a grid of cards, and opens the player for
 * the channel that was picked. A failed download keeps the list that was already there.
 */
public class ChannelsActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClick {

    private final Handler main = new Handler(Looper.getMainLooper());
    private final LogoLoader logos = new LogoLoader();

    private Prefs prefs;
    private PlaylistStore store;
    private ChannelAdapter adapter;

    private RecyclerView list;
    private ProgressBar spinner;
    private TextView status;
    private TextView errorText;
    private Button retry;

    /** True while a download is running, so a second one cannot start on top of it. */
    private volatile boolean busy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels);

        prefs = new Prefs(this);
        store = new PlaylistStore(this);

        list = findViewById(R.id.channelList);
        spinner = findViewById(R.id.loading);
        status = findViewById(R.id.status);
        errorText = findViewById(R.id.errorText);
        retry = findViewById(R.id.retry);

        adapter = new ChannelAdapter(logos, this);
        list.setLayoutManager(new GridLayoutManager(this, 4));
        list.setAdapter(adapter);

        findViewById(R.id.refresh).setOnClickListener(v -> load(true));
        findViewById(R.id.settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)
                        .putExtra(SettingsActivity.EXTRA_LANGUAGES, true)));
        retry.setOnClickListener(v -> load(true));

        findViewById(R.id.close).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load(false);
    }

    /** Loads the channels: {@code force} downloads again even when the cache is fresh. */
    private void load(boolean force) {
        if (busy) return;
        busy = true;
        spinner.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        status.setText(R.string.loading);

        new Thread(() -> {
            List<Channel> channels = null;
            String problem = null;
            try {
                channels = store.load(prefs.languages(), prefs.refreshHours(), force);
                prefs.setLastUpdate(System.currentTimeMillis());
            } catch (IOException e) {
                problem = e.getMessage() == null ? e.toString() : e.getMessage();
            } catch (RuntimeException e) {
                problem = e.getMessage() == null ? e.toString() : e.getMessage();
            }

            final List<Channel> result = channels;
            final String error = problem;
            main.post(() -> {
                busy = false;
                spinner.setVisibility(View.GONE);
                if (result == null) {
                    showError(error);
                    return;
                }
                errorText.setVisibility(View.GONE);
                retry.setVisibility(View.GONE);
                adapter.setChannels(result);
                list.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
                status.setText(getString(R.string.channel_count, result.size()));
                showUpdated();
            });
        }, "livetv-load").start();
    }

    private void showError(String problem) {
        list.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(getString(R.string.load_failed, problem == null ? "" : problem));
        retry.setVisibility(View.VISIBLE);
        status.setText(R.string.load_failed_short);
    }

    /** The line under the title: when the playlists were downloaded. */
    private void showUpdated() {
        long when = prefs.lastUpdate();
        if (when <= 0L) {
            status.setText(R.string.not_updated_yet);
            return;
        }
        String stamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(when));
        status.setText(getString(R.string.updated_at, stamp));
    }

    @Override
    public void onChannel(Channel channel) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL, channel.url);
        intent.putExtra(PlayerActivity.EXTRA_NAME, channel.label());
        intent.putExtra(PlayerActivity.EXTRA_LOGO, channel.logo);
        startActivity(intent);
    }
}
