package com.gathin.livetv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The settings of the app: which languages to load, how often to refresh, and the cache.
 *
 * <p>Everything here is written into the app's own preferences. The launcher has its own
 * {@code live_tv_languages} setting, which is what a fresh install starts from.
 */
public class SettingsActivity extends AppCompatActivity {

    /**
     * Set when the screen was opened from the channel list's "Languages" button: the cursor then goes
     * to the first language, so the button does what it says.
     */
    public static final String EXTRA_LANGUAGES = "focus_languages";

    private final Handler main = new Handler(Looper.getMainLooper());

    private Prefs prefs;
    private PlaylistStore store;
    private TextView summary;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new Prefs(this);
        store = new PlaylistStore(this);
        summary = findViewById(R.id.summary);

        findViewById(R.id.close).setOnClickListener(v -> finish());
        findViewById(R.id.save).setOnClickListener(v -> {
            setLanguages();
            prefs.setRefreshHours(hours());
            finish();
        });
        findViewById(R.id.clearCache).setOnClickListener(v -> {
            store.clearCache();
            summary.setText(R.string.cache_cleared);
        });

        showLanguages();
        showHours();
        showCache();

        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_LANGUAGES, false)) {
            focusFirstLanguage();
        }
    }

    /** Puts the cursor on the first language. */
    private void focusFirstLanguage() {
        ViewGroup box = findViewById(R.id.languageBox);
        if (box.getChildCount() == 0) return;
        View first = box.getChildAt(0);
        first.post(() -> first.requestFocus());
    }

    /** One check box per language, the picked ones ticked. */
    private void showLanguages() {
        ViewGroup box = findViewById(R.id.languageBox);
        box.removeAllViews();
        Set<String> picked = prefs.languages();
        List<String[]> options = Languages.options(new ArrayList<>(picked));

        for (String[] option : options) {
            CheckBox check = new CheckBox(this);
            check.setText(option[1]);
            check.setTextSize(20f);
            check.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            check.setChecked(picked.contains(option[0]));
            check.setTag(option[0]);
            check.setFocusable(true);
            check.setOnFocusChangeListener((v, hasFocus) -> v.setAlpha(hasFocus ? 1f : 0.75f));
            box.addView(check);
        }
    }

    /** How many hours a cached playlist may get before it is downloaded again. */
    private void showHours() {
        SeekBar bar = findViewById(R.id.hours);
        TextView label = findViewById(R.id.hoursLabel);
        bar.setMax(47);
        bar.setProgress(prefs.refreshHours() - 1);
        label.setText(getString(R.string.refresh_every, prefs.refreshHours()));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText(getString(R.string.refresh_every, progress + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /** How many channels are cached right now. */
    private void showCache() {
        new Thread(() -> {
            int count = store.cachedCount(prefs.languages());
            main.post(() -> summary.setText(getString(R.string.cached_channels, count)));
        }, "livetv-cache").start();
    }

    /** Saves the ticked languages; nothing ticked means the three defaults. */
    private void setLanguages() {
        ViewGroup box = findViewById(R.id.languageBox);
        Set<String> picked = new LinkedHashSet<>();
        for (int i = 0; i < box.getChildCount(); i++) {
            CheckBox check = (CheckBox) box.getChildAt(i);
            if (check.isChecked()) picked.add((String) check.getTag());
        }
        if (picked.isEmpty()) picked = Prefs.defaultLanguages(this);
        prefs.setLanguages(picked);
    }

    private int hours() {
        SeekBar bar = findViewById(R.id.hours);
        return bar.getProgress() + 1;
    }
}
