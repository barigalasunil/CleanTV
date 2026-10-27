package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tvOS style home: black, a pinned dock row, an app grid, and one glass highlight that springs between tiles.
 */
public class MainActivity extends Activity implements AppAdapter.TileListener {

    /** Tiles per row and dock slots (res/values/config.xml, columns). Set in onCreate. */
    static int COLUMNS = 4;
    /** One shared home-screen tick; the readings below are refreshed on multiples of it. */
    private static final long TICK_MS = 1_000L;
    /** How long to wait before asking for the weather again, and how many times to ask. */
    private static final long WEATHER_RETRY_MS = 10_000L;
    private static final int WEATHER_TRIES = 6;
    private String liveTvPkg;
    private String liveTvAction;
    private String inputAction;
    private String sourcePanelService;
    private String sourcePanelAction;

    private AppAdapter adapter;
    private DockDecoration dockDecor;
    private RecyclerView grid;
    private FrameLayout root;
    private GlassView glass;
    private View ambient;
    private TextView heroName;
    private GradientDrawable ambientGlow;
    private int ambientColor;
    private boolean heroShown;
    private TextView greeting;
    private TextView btnInputs;
    private TextView btnWifi;
    private TextView btnSettings;
    private TextView btnRam;
    private TextView btnStorage;
    private TextView btnNetwork;
    private TextView clock;
    private TextView ampm;
    private TextView date;
    private View pillRow;
    private TextView shamsiDate;
    private boolean shamsi;
    private String vpnApp;
    /** The name in the greeting, empty when there is none. */
    private String greetingName = "";
    /** The wallpapers behind the home screen. */
    private Wallpapers wallpapers;
    /** True when the wallpapers added their views to the home screen. */
    private boolean wallpapersAttached;
    /** The single wallpaper a theme pushed (when the app's own wallpapers folder is empty). */
    private ImageView themeWall;
    /**
     * The day/night theme. Recomputed at most once a minute (every 30 seconds while a blend runs),
     * only while the home screen is visible, and applied with filters and colour state lists only.
     */
    private ThemeManager themes;
    private ThemeManager.Tokens tokens;
    /** How dark the wallpaper shows right now (the theme's night dimming, 1 in the day). */
    private float wallpaperDim = 1f;
    /** Whether walls/wallpapers/day and /night may replace the single wallpaper set (config.xml). */
    private boolean phaseWallpapers;
    /** The text that sits on the wallpaper, and the colour it had before the wallpaper was read. */
    private final List<TextView> contrastTexts = new ArrayList<>();
    private final List<Integer> contrastBase = new ArrayList<>();
    /** The shade behind the clock and the pills. */
    private View scrim;
    /** The gradient that paints the shade, so the theme can recolour it without a new view. */
    private GradientDrawable scrimGlow;
    /** How bright the wallpaper is under the clock and under the pills. */
    private float clockBrightness = -1f, pillBrightness = -1f;
    /** The brightness of the wallpaper on screen, cell by cell. */
    private float[] wallpaperGrid;
    /** True while the wallpaper is fading from one picture into the next. */
    private boolean wallpaperFading;
    /** The coordinates the weather is for, NaN when they are not known. */
    private double weatherLat = Double.NaN, weatherLon = Double.NaN;
    /** How many times the weather has been asked for since the last reading came in. */
    private int weatherTries;
    /** The last weather reading, kept so an offline refresh can leave it on screen, dimmed. */
    private Weather.Report weatherReport;
    private LinearLayout contentRow;
    private View weatherPanel;
    private WeatherPanel weatherCard;
    private View nowPlayingView;
    private NowPlayingCard nowPlayingCard;
    /** The player updates the card, never the other way around. */
    private final MediaNoteService.Listener mediaListener = now -> {
        if (nowPlayingCard == null) return;
        nowPlayingCard.bind(now);
        updateContentNavigation();
    };
    /** Whether the weather is wanted at all (res/values/config.xml, weather_enabled). */
    private boolean weatherEnabled;
    /** Whole seconds since the shared home ticker started. */
    private long tickCount;
    /** The hint shown while a tile is being moved. */
    private TextView moveHint;
    /** True while a tile is being moved around. */
    private boolean moveMode;
    /** The order before the move started, so Back can put everything back. */
    private final List<String> orderBeforeMove = new ArrayList<>();
    private SpringAnimation springX, springY;
    private float glassTargetX, glassTargetY;
    // How far the grid has scrolled the glass. springY works without it, in grid content space, so a
    // scroll moves the glass in lockstep with its tile instead of becoming a moving spring target.
    private float glassScrollShift;
    private boolean glassShown;
    // Static so they survive the activity being destroyed while another app is open (low-memory TVs,
    // or Developer options → "Don't keep activities"); they only reset when the process dies.
    private static int lastFocused = 0;
    /** App of the last focused tile, so focus follows it when the app list changes. */
    private static String lastFocusedKey;
    private float density;
    /** Module cards (res/values/config.xml, module_cards): discovered on resume, shown only after the owner's Show. */
    private boolean moduleCards;
    /** hidden_apps from config.xml or a theme's config.json; the tile menu adds to this at runtime. */
    private Set<String> configuredHidden = Collections.emptySet();
    /** Everything the adapter hides, configured plus the owner's tile-menu choices. */
    private Set<String> hiddenApps = Collections.emptySet();
    private List<Cards.Card> cards = Collections.emptyList();
    /** The grid has been laid out and focused since the last resume; the card prompt waits for this. */
    private boolean tilesReady;
    private Dialog cardPrompt;
    /** Set when a decision could not be saved: no more prompts until the next resume, instead of asking in a loop. */
    private boolean promptsPaused;
    /** The Inputs or Module cards dropdown while one is open, so onPause can close it. */
    private PopupWindow openPopup;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Refreshes the network pill when the connection changes: at boot Wi-Fi often connects after Home is shown. */
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            handler.post(() -> {
                updateWifiLabel();
                // The network is here now, so a missing or stale reading is asked for again.
                if (weatherEnabled && (weatherReport == null || weatherReport.stale())) {
                    weatherTries = 0;
                    if (weatherReport == null && weatherCard != null) weatherCard.loading(null);
                    refreshWeather();
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }
    };
    /** Shows the Inputs pill once the TV registers an input: at boot HDMI inputs can appear after Home is shown. */
    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputAdded(String inputId) {
            updateInputsButton();
        }

        @Override
        public void onInputRemoved(String inputId) {
            updateInputsButton();
        }
    };
    /** Listens for any VPN coming up or going down, even one this app's traffic is excluded from. */
    private final ConnectivityManager.NetworkCallback vpnCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }

        @Override
        public void onLost(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }
    };
    private final IntentFilter packageFilter = new IntentFilter();
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Updates send REMOVED followed by ADDED; keep the tile until the new APK is ready.
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            reloadApps(false);
        }
    };
    /**
     * One ticker for the whole home screen: link speed every second, memory every five, storage every
     * thirty, greeting and Wi-Fi every minute, and the weather every half hour. One Handler instead of
     * several keeps the TV's single main thread predictable.
     */
    private final Runnable homeTicker = new Runnable() {
        @Override
        public void run() {
            tickCount++;
            InfoPill.update(MainActivity.this, btnNetwork, InfoPill.NETWORK);
            if (tickCount % 5 == 0) InfoPill.update(MainActivity.this, btnRam, InfoPill.RAM);
            if (tickCount % 30 == 0) InfoPill.update(MainActivity.this, btnStorage, InfoPill.STORAGE);
            if (tickCount % 60 == 0) {
                updateGreeting();
                updateWifiLabel();
            }
            // The theme refreshes on the same ticker, at most once a minute (30 while blending).
            if (tokens == null || (tokens.blending ? tickCount % 30 == 0 : tickCount % 60 == 0)) {
                recomputeTheme();
            }
            if (weatherEnabled && tickCount % 1800 == 0) {
                weatherTries = 0;
                refreshWeather();
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // config.xml, overlaid by config.json from a pushed theme (see Config and tools/theme.sh).
        Config cfg = Config.load(this);
        COLUMNS = cfg.columns;
        liveTvPkg = cfg.liveTvPackage;
        liveTvAction = cfg.liveTvAction;
        inputAction = cfg.inputAction;
        sourcePanelService = cfg.sourcePanelService;
        sourcePanelAction = cfg.sourcePanelAction;
        greetingName = cfg.greetingName;
        weatherEnabled = cfg.weatherEnabled;
        weatherLat = cfg.weatherLat;
        weatherLon = cfg.weatherLon;
        // The one place the colours come from (config keys, or a theme's config.json).
        themes = new ThemeManager(cfg);
        phaseWallpapers = cfg.themeUsePhaseWallpapers;

        // The welcome splash comes once per boot, on top of the home screen (see Splash).
        boolean splash = cfg.splashEnabled && Splash.due(this);

        setContentView(R.layout.activity_main);
        density = getResources().getDisplayMetrics().density;

        root = findViewById(R.id.root);
        root.setBackgroundColor(cfg.backgroundColor);
        // The launcher's own wallpapers; the picture a theme brought is only used when there are none.
        wallpapers = new Wallpapers(this);
        wallpapers.setListener(wallpaperListener);
        if (cfg.useWallpaper) {
            if (!wallpapers.isEmpty()) {
                wallpapers.attach(root);
                wallpapersAttached = true;
                wallpapers.start(cfg.wallpaperIntervalSeconds);
            } else if (cfg.wallpaper != null) {
                showWallpaper(cfg.wallpaper);
            }
        }
        // A faint shade over the top of the wallpaper, so the clock and the pills stay readable.
        addScrim();
        glass = findViewById(R.id.glass);
        ambient = findViewById(R.id.ambient);
        heroName = findViewById(R.id.heroName);
        ambientGlow = new GradientDrawable();
        ambientGlow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambientGlow.setGradientCenter(0.32f, 0.42f);
        ambient.setBackground(ambientGlow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                ambientGlow.setGradientRadius((r - l) * 0.62f));
        greeting = findViewById(R.id.greeting);
        btnWifi = findViewById(R.id.btnWifi);
        shamsiDate = findViewById(R.id.shamsiDate);
        clock = findViewById(R.id.clock);
        ampm = findViewById(R.id.ampm);
        date = findViewById(R.id.date);
        pillRow = findViewById(R.id.topButtons);
        shamsi = cfg.shamsiDate;
        if (shamsi) {
            shamsiDate.setVisibility(View.VISIBLE);
            date.setTextSize(11f);
            date.setPadding(0, (int) (4 * density), 0, 0);
        }
        // The clock, the date and the greeting sit on the wallpaper: their colour follows it.
    // The hero name rides over the wallpaper like the clock texts, so it is contrast-tracked too.
        trackContrast(clock);
        trackContrast(ampm);
        trackContrast(date);
        trackContrast(greeting);
        trackContrast(shamsiDate);
        trackContrast(heroName);
        // The views are only placed after this, so the wallpaper is read again once they are.
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateContrast());
        vpnApp = cfg.vpnApp;
        moduleCards = cfg.moduleCards;
        configuredHidden = new HashSet<>(cfg.hiddenApps);
        hiddenApps = new HashSet<>(configuredHidden);
        hiddenApps.addAll(HiddenApps.load(this));

        grid = findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, COLUMNS));
        grid.setHasFixedSize(true);
        grid.setItemAnimator(null);
dockDecor = new DockDecoration(COLUMNS, density);
        grid.addItemDecoration(dockDecor);
        grid.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(this, R.anim.tile_enter), 0.06f));
        adapter = new AppAdapter(this, this, COLUMNS, cfg);
        adapter.setHidden(hiddenApps);
        grid.setAdapter(adapter);

        // tvOS layout: push the dock row towards the bottom of the screen, leaving room for
        // rows_below_dock grid rows under it; the rest of the grid stays below until you press Down.
        int rowsBelowDock = cfg.rowsBelowDock;
        grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int h = b - t;
            // Item height follows the tile size, which follows the column count (AppAdapter.tileWidthPx).
            int inner = (r - l) - grid.getPaddingLeft() - grid.getPaddingRight();
            int itemH = AppAdapter.itemHeightPx(AppAdapter.tileHeightPx(AppAdapter.tileWidthPx(inner, COLUMNS, density)), density);
            int dockRow = itemH + (int) ((6 + 12) * density);   // item height plus DockDecoration offsets
            int gridRows = itemH * rowsBelowDock;
            int want = Math.max(0, h - dockRow - gridRows - (int) (30 * density));
            if (grid.getPaddingTop() != want) {
                grid.setPadding(grid.getPaddingLeft(), want, grid.getPaddingRight(), grid.getPaddingBottom());
            }
        });

        // Keep the glass glued to its tile while the grid scrolls.
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!glassShown) return;
                glassTargetY -= dy;
                glassScrollShift -= dy;
                glass.setTranslationY(glass.getTranslationY() - dy);
            }
        });

        // Liquid feel: soft springs on the highlight's position.
        springX = new SpringAnimation(glass, DynamicAnimation.TRANSLATION_X);
        springY = new SpringAnimation(glass, new FloatPropertyCompat<View>("glassContentY") {
            @Override
            public float getValue(View v) {
                return v.getTranslationY() - glassScrollShift;
            }

            @Override
            public void setValue(View v, float value) {
                v.setTranslationY(value + glassScrollShift);
            }
        });
        for (SpringAnimation s : new SpringAnimation[]{springX, springY}) {
            s.setSpring(new SpringForce().setStiffness(260f).setDampingRatio(0.58f));
        }

        btnInputs = findViewById(R.id.btnInputs);
        btnSettings = findViewById(R.id.btnSettings);
        contentRow = findViewById(R.id.contentRow);
        weatherPanel = findViewById(R.id.weatherPanel);
        weatherCard = new WeatherPanel(this);
        nowPlayingView = findViewById(R.id.nowPlayingCard);
        nowPlayingCard = new NowPlayingCard(this, this::updateContentRow);
        btnRam = findViewById(R.id.btnRam);
        btnStorage = findViewById(R.id.btnStorage);
        btnNetwork = findViewById(R.id.btnNetwork);
        moveHint = findViewById(R.id.moveHint);
        btnInputs.setOnClickListener(v -> showInputs());
        btnWifi.setOnClickListener(v -> openNetworkPill());
        btnRam.setOnClickListener(v -> open(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        btnStorage.setOnClickListener(v -> open(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        btnNetwork.setOnClickListener(v -> open(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        btnWifi.setOnLongClickListener(v -> {
            open(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return true;
        });
        weatherPanel.setOnClickListener(v -> {
            if (!weatherEnabled || Double.isNaN(weatherLat) || Double.isNaN(weatherLon)) return;
            weatherTries = 0;
            refreshWeather();
        });
        btnSettings.setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        btnSettings.setOnLongClickListener(v -> {
            if (!moduleCards) return false;   // off: a long press does nothing, as before the feature
            showModuleCards();
            return true;
        });

        // The weather shelf: shown when coordinates are known or a reading is already cached.
        Weather.Report known = weatherEnabled ? Weather.cached(this) : null;
        boolean showPanel = weatherEnabled && (!Double.isNaN(weatherLat) || known != null);
        weatherPanel.setVisibility(showPanel ? View.VISIBLE : View.GONE);
        updateContentRow();
        if (showPanel) {
            if (known != null) showWeather(known);
            else weatherCard.loading(null);
            refreshWeather();
        }

        // Keep focus on the top bar at its ends and at the top: Android's focus finder would otherwise
        // jump to a tile far below when there is nothing in that direction.
        View.OnKeyListener pillKeys = (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int id = v.getId();
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                // The top row has nothing above it; the second row moves up by nextFocusUp.
                return id == R.id.btnInputs || id == R.id.btnWifi || id == R.id.btnSettings;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return id == R.id.btnInputs || id == R.id.btnRam;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return id == R.id.btnSettings || id == R.id.btnNetwork;
            return false;
        };
        for (TextView pill : new TextView[]{btnInputs, btnWifi, btnSettings, btnRam, btnStorage, btnNetwork}) {
            pill.setOnKeyListener(pillKeys);
        }

        // Hide the glass when focus leaves the tiles.
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((oldF, newF) -> {
            boolean onTile = newF != null && newF.getId() == R.id.card;
            if (!onTile) hideGlass();
        });

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addDataScheme("package");

        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim != null) tim.registerCallback(inputCallback, handler);
        if (cm != null) {
            cm.registerDefaultNetworkCallback(networkCallback);
            cm.registerNetworkCallback(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(), vpnCallback);
        }

        // The first theme pass, before the first frame is drawn.
        recomputeTheme();

        // The welcome splash goes over the home screen and fades out again.
        if (splash) root.post(() -> startActivity(Splash.intent(this)));
    }

    /**
     * Asks for the weather again; the pill keeps the reading it has until the new one arrives.
     *
     * <p>At boot the TV's network is often not up yet when Home starts, and the first question goes
     * nowhere (no name to look up). Without asking again the pill stays away for the whole session,
     * so the question is repeated a few times and once more when the network says it is there.
     */
    private void refreshWeather() {
        if (!weatherEnabled || Double.isNaN(weatherLat) || Double.isNaN(weatherLon)) return;
        if (weatherTries >= WEATHER_TRIES) return;
        weatherTries++;
        Weather.refresh(this, weatherLat, weatherLon, this::showWeather);
        handler.postDelayed(weatherRetry, WEATHER_RETRY_MS);
    }

    /** Asks for the weather once more, a little later. */
    private final Runnable weatherRetry = new Runnable() {
        @Override
        public void run() {
            refreshWeather();
        }
    };

    /** Shows a fresh reading, or keeps the old one dimmed when the network could not answer. */
    private void showWeather(Weather.Report report) {
        if (report == null) {
            if (weatherReport != null) weatherCard.unavailable(weatherReport);
            else weatherCard.unavailable(null);
            return;
        }
        weatherReport = report;
        // Sunrise and sunset may have arrived: the phase is worth one extra pass.
        recomputeTheme();
        weatherTries = 0;
        handler.removeCallbacks(weatherRetry);
        weatherPanel.setVisibility(View.VISIBLE);
        updateContentRow();
        weatherCard.bind(report, false);
    }

    /** Weather or music can appear on their own; the shelf exists while either one does. */
    private void updateContentRow() {
        boolean weather = weatherPanel.getVisibility() == View.VISIBLE;
        boolean media = nowPlayingView.getVisibility() == View.VISIBLE;
        contentRow.setVisibility(weather || media ? View.VISIBLE : View.GONE);
        contentRow.setGravity(media && !weather ? Gravity.END : Gravity.START);
        updateContentNavigation();
    }

    /** The top row drops into the first shelf item that exists, or into the grid when there is none. */
    private void updateContentNavigation() {
        boolean weather = weatherPanel.getVisibility() == View.VISIBLE;
        boolean media = nowPlayingView.getVisibility() == View.VISIBLE;
        int down = weather ? R.id.weatherPanel : (media ? R.id.nowPrevious : R.id.grid);
        btnRam.setNextFocusDownId(down);
        btnStorage.setNextFocusDownId(down);
        btnNetwork.setNextFocusDownId(down);
        weatherPanel.setNextFocusRightId(media ? R.id.nowPrevious : View.NO_ID);
        grid.setNextFocusUpId(weather ? R.id.weatherPanel : (media ? R.id.nowPrevious : View.NO_ID));
    }

    /**
     * A theme's full-screen picture behind everything. Decoded once, downsampled to the screen so a 4K
     * JPEG costs a 1080p bitmap at most (about 8 MB), which is the price of a wallpaper on a 1 GB TV.
     */
    private void showWallpaper(File file) {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(file.getPath(), opts);
            int sample = 1;
            while (opts.outWidth / (sample * 2) >= dm.widthPixels && opts.outHeight / (sample * 2) >= dm.heightPixels) sample *= 2;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            opts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;   // no alpha needed, half the memory
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getPath(), opts);
            if (bmp == null) return;
            android.widget.ImageView wall = new android.widget.ImageView(this);
            wall.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            wall.setImageBitmap(bmp);
            root.addView(wall, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            // The tint is applied on the view, never decoded into the bitmap again.
            themeWall = wall;
            if (tokens != null) {
                wall.setColorFilter(tokens.wallpaperFilter);
                wallpaperDim = tokens.wallpaperBrightness;
            }
            // The text on top of it needs to know how bright the picture is.
            wallpaperGrid = Contrast.sampleScreen(bmp, dm.widthPixels, dm.heightPixels);
            updateContrast();
        } catch (Exception | OutOfMemoryError e) {
            // A bad or huge picture must never take the home screen down; the plain colour stays.
        }
    }

    // ---- day / night theme -----------------------------------------------------------------------

    /** The theme refreshes on the shared ticker, and once more when the weather brings a sunset. */
    private void recomputeTheme() {
        if (themes == null) return;
        ThemeManager.Tokens t = themes.tokens(LocalTime.now(), weatherReport);
        boolean changed = tokens == null
                || Math.abs(t.blend - tokens.blend) > 0.002f
                || Math.abs(t.warm - tokens.warm) > 0.002f
                || t.isNight() != tokens.isNight();
        tokens = t;
        if (changed) applyTokens();
    }

    /** Puts the current phase on everything the home screen shows; colours only, never a layout. */
    private void applyTokens() {
        ThemeManager.Tokens t = tokens;
        // The room behind everything, and (by the time the text pass runs) the wallpaper itself.
        root.setBackgroundColor(t.background);
        applyWallpaperTheme(t);
        if (scrimGlow != null) {
            int c = t.scrim & 0x00FFFFFF;
            scrimGlow.setColors(new int[]{0x99000000 | c, 0x99000000 | c, 0x00000000});
        }
        // The texts on the wallpaper get their theme base first; the contrast pass decides the pole.
        for (int i = 0; i < contrastTexts.size(); i++) {
            int id = contrastTexts.get(i).getId();
            contrastBase.set(i, id == R.id.ampm || id == R.id.date || id == R.id.greeting
                    ? t.secondaryText : t.primaryText);
        }
        updateContrast();
        // Pills and the moving hint: fresh state drawables, only when the tokens really changed.
        for (TextView v : new TextView[]{btnInputs, btnWifi, btnSettings, btnRam, btnStorage, btnNetwork}) {
            if (v != null) v.setBackground(ThemeManager.pillBackground(t, 16 * density));
        }
        if (moveHint != null) moveHint.setBackground(ThemeManager.pillBackground(t, 20 * density));
        weatherCard.setTokens(t);
        nowPlayingCard.applyTokens(t);
        glass.applyTokens(t);
        dockDecor.applyTokens(t);
        // The ambient glow follows the phase too, without losing the app's own colour.
        if (heroShown) setAmbient(ambientColor);
        // The dock band's paints changed: one repaint of the grid, no layout.
        grid.invalidate();
        retintTileLabels();
    }

    /** The wallpaper gets the phase's own folder when there is one, otherwise the single tint. */
    private void applyWallpaperTheme(ThemeManager.Tokens t) {
        if (wallpapers == null) {
            // The single picture a theme pushed: it takes the tint, it has no folders of its own.
            if (themeWall != null) {
                themeWall.setColorFilter(t.wallpaperFilter);
                wallpaperDim = t.wallpaperBrightness;
            }
            return;
        }
        if (!wallpapersAttached) return;
        boolean phaseSet = phaseWallpapers && Wallpapers.hasPhaseSets(this);
        if (phaseSet) wallpapers.setPhaseSet("wallpapers/" + (t.isNight() ? "night" : "day"));
        // On its own folder a picture is already right for the phase; on the single set the theme tints it.
        boolean filterNeeded = !(phaseSet && wallpapers.usesPhaseSet());
        wallpapers.applyFilter(filterNeeded ? t.wallpaperFilter : null);
        wallpaperDim = filterNeeded ? t.wallpaperBrightness : 1f;
    }

    /**
     * What a tile label should read on the wallpaper under it: the theme's light or dark pole for
     * the brightness of the lower half (the rows the grid lives in). Measured once per theme pass,
     * never per tile, so a scroll can't fight the theme over every frame.
     */
    private int tileLabelRgb() {
        ThemeManager.Tokens t = tokens;
        if (t == null) return 0xFFFFFFFF;
        float b;
        if (wallpaperGrid != null) {
            float sum = 0f;
            int n = 0;
            for (int row = 7; row < Contrast.ROWS; row++) {
                for (int col = 0; col < Contrast.COLS; col++) {
                    sum += wallpaperGrid[row * Contrast.COLS + col];
                    n++;
                }
            }
            b = n == 0 ? 0.3f : sum / n;
        } else {
            b = Contrast.brightness(t.background);
        }
        return t.wallpaperTextRgb(b) & 0x00FFFFFF;
    }

    /** Gives the theme colours to the tiles already on screen without rebinding the grid. */
    private void retintTileLabels() {
        if (grid == null) return;
        int rgb = tileLabelRgb();
        adapter.setLabelColors(rgb, tokens.tileText, tokens.tileSurface);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View item = grid.getChildAt(i);
            if (item == null) continue;
            TextView name = item.findViewById(R.id.name);
            if (name != null) name.setTextColor(rgb);
            TextView label = item.findViewById(R.id.label);
            if (label != null) label.setTextColor(tokens.tileText);
            View card = item.findViewById(R.id.card);
            if (card != null) {
                card.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tokens.tileSurface));
            }
        }
    }

    // ---- contrast on the wallpaper -------------------------------------------------------------------

    /**
     * A faint shade over the top of the wallpaper, behind the clock and the pills. It is strongest on
     * a dark picture, where the text is light, and fades away on a bright one, where the text is dark.
     */
    private void addScrim() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x99000000, 0x99000000, 0x00000000});
        scrimGlow = g;
        View v = new View(this);
        v.setBackground(g);
        v.setImportantForAccessibility(ViewGroup.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (150 * density));
        // Above the wallpaper, below everything else.
        root.addView(v, wallpapersAttached ? 2 : 0, lp);
        scrim = v;
    }

    /** Remembers a text view whose colour follows the wallpaper, and the colour it has now. */
    private void trackContrast(TextView v) {
        if (v == null) return;
        contrastTexts.add(v);
        contrastBase.add(v.getTextColors().getDefaultColor());
    }

    /** The wallpaper changed: the text on top of it is given a colour that can be read on it. */
    private final Wallpapers.Listener wallpaperListener = new Wallpapers.Listener() {
        @Override
        public void onPicture(float[] grid) {
            wallpaperGrid = grid;
            wallpaperFading = false;
            updateContrast();
        }

        @Override
        public void onFade(float[] from, float[] to) {
            wallpaperFading = true;
            float fromClock = brightnessOf(from, clock);
            float fromPills = brightnessOf(from, pillRow);
            float toClock = brightnessOf(to, clock);
            float toPills = brightnessOf(to, pillRow);
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(Wallpapers.FADE_MS);
            anim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                setBrightness(Contrast.mix(fromClock, toClock, t), Contrast.mix(fromPills, toPills, t));
            });
            anim.start();
        }
    };

    /** Reads the wallpaper's brightness again (the views moved) and colours the text. */
    private void updateContrast() {
        if (wallpaperFading) return;
        setBrightness(brightnessOf(wallpaperGrid, clock), brightnessOf(wallpaperGrid, pillRow));
    }

    /** How bright the wallpaper is under a view. */
    private float brightnessOf(float[] grid, View v) {
        if (grid == null || v == null || root == null) return 0f;
        int w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0 || v.getWidth() == 0 || v.getHeight() == 0) return 0f;
        Rect r = new Rect(0, 0, v.getWidth(), v.getHeight());
        root.offsetDescendantRectToMyCoords(v, r);
        // The theme dims the picture; brightness is read off it, never off a copy.
        return Contrast.behind(grid, r.left, r.top, r.right, r.bottom, w, h) * wallpaperDim;
    }

    /** Sets how bright the wallpaper is under the clock and under the pills, and colours the text. */
    private void setBrightness(float clock, float pills) {
        if (Math.abs(clock - clockBrightness) < 0.004f && Math.abs(pills - pillBrightness) < 0.004f) return;
        clockBrightness = clock;
        pillBrightness = pills;
        applyContrast();
    }

    /**
     * The contrast pass, the last layer: it never invents its own colours, only flips between the
     * two poles the theme gives it, so the two can't fight. It runs only when the wallpaper under
     * a text (or the theme) has actually changed.
     */
    private void applyContrast() {
        boolean onBright = clockBrightness > Contrast.BRIGHT;
        int rgb = (tokens != null ? tokens.wallpaperTextRgb(clockBrightness)
                : Contrast.textColor(clockBrightness)) & 0x00FFFFFF;
        int shadow = onBright ? 0x59FFFFFF : 0x8C000000;
        for (int i = 0; i < contrastTexts.size(); i++) {
            TextView v = contrastTexts.get(i);
            int base = contrastBase.get(i);
            // Light text on a dark picture keeps its own transparency; dark text needs to be solid.
            v.setTextColor(Contrast.tint(base, rgb, onBright ? 0xD9 : 0x00));
            v.setShadowLayer(3 * density, 0, 1 * density, shadow);
        }
        applyPillColors();
    }

    /**
     * The pills: the focused one keeps its dark text, the others follow the wallpaper. The weather
     * pill keeps the colour of the weather, only darkened when the wallpaper is bright.
     */
    private void applyPillColors() {
        boolean onBright = pillBrightness > Contrast.BRIGHT;
        int light = (tokens != null ? tokens.wallpaperTextRgb(pillBrightness)
                : Contrast.textColor(pillBrightness)) & 0x00FFFFFF;
        int focusText = tokens != null ? tokens.focusText : 0xFF111111;
        int shadow = onBright ? 0x59FFFFFF : 0x8C000000;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{}};
        android.content.res.ColorStateList cs = new android.content.res.ColorStateList(states,
                new int[]{focusText, light});
        for (TextView v : new TextView[]{btnInputs, btnWifi, btnSettings, btnRam, btnStorage, btnNetwork}) {
            if (v == null) continue;
            v.setTextColor(cs);
            v.setCompoundDrawableTintList(cs);
            v.setShadowLayer(3 * density, 0, 1 * density, shadow);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, packageFilter);
        }
        tilesReady = false;
        promptsPaused = false;
        reloadApps(true);
        updateGreeting();
        updateWifiLabel();
        updateInputsButton();
        tickCount = 0;
        InfoPill.update(this, btnRam, InfoPill.RAM);
        InfoPill.update(this, btnStorage, InfoPill.STORAGE);
        InfoPill.update(this, btnNetwork, InfoPill.NETWORK);
        handler.postDelayed(homeTicker, TICK_MS);
        MediaNoteService.addListener(mediaListener);
    }

    /** Rebuilds the tiles (and the module cards), then puts focus back where it was. */
    private void reloadApps(boolean animate) {
        // Capture the app before rebinding can move focus and overwrite its key.
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        final boolean restoreFocus = animate || grid.hasFocus();
        if (restoreFocus) {
            glassShown = false;
            glass.setAlpha(0f);
            heroShown = false;
            heroName.setAlpha(0f);
            ambient.setAlpha(0f);
        }
        reloadTiles();
        if (animate) grid.scheduleLayoutAnimation();

        if (restoreFocus) {
            // Follow the same app, or the nearest remaining slot when it was removed.
            restoreFocus();
        } else {
            // A package change must not steal focus from a top button or the Inputs popup.
            int pos = adapter.positionOf(returnKey);
            lastFocused = Math.max(0, Math.min(pos >= 0 ? pos : returnSlot, adapter.getItemCount() - 1));
            lastFocusedKey = adapter.keyAt(lastFocused);
            if (pos < 0) hideHero();
            // A newly installed app may have brought a card with it.
            tilesReady = true;
            showCardPrompt();
        }
    }

    /**
     * Rebuilds the tiles; the module cards the owner chose to show stand in for their app's tile.
     * Every step into the card code runs under CardFeature: if it fails, the tiles are the ordinary
     * ones and cards stay off for this process.
     */
    private void reloadTiles() {
        // Read these before reload(): rebinding can move focus to the first tile and overwrite them.
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        final List<Cards.Card> none = Collections.emptyList();
        hiddenApps = new HashSet<>(configuredHidden);
        hiddenApps.addAll(HiddenApps.load(this));
        adapter.setHidden(hiddenApps);
        cards = moduleCards ? CardFeature.run(() -> Cards.discover(this, hiddenApps, Cards.load(Cards.storeFile(this))), none) : none;
        adapter.reload(cards.isEmpty() ? none : CardFeature.run(() -> Cards.shown(cards), none));
        lastFocusedKey = returnKey;
        lastFocused = returnSlot;
    }

    /**
     * Return to the app we left from, or the same slot if that app is gone. Always refocus: after
     * reload() the view that holds focus may no longer show the app it had.
     */
    private void restoreFocus() {
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        grid.post(() -> {
            int pos = adapter.positionOf(returnKey);
            focusTile(pos >= 0 ? pos : returnSlot);
            tilesReady = true;
            showCardPrompt();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(packageReceiver);
        handler.removeCallbacks(homeTicker);
        MediaNoteService.removeListener(mediaListener);
        // Nothing of ours stays on screen over another app; an undecided prompt simply returns next time.
        if (cardPrompt != null) cardPrompt.dismiss();
        if (openPopup != null) openPopup.dismiss();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) showCardPrompt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wallpapers != null) wallpapers.stop();
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim != null) tim.unregisterCallback(inputCallback);
        if (cm != null) {
            cm.unregisterNetworkCallback(networkCallback);
            cm.unregisterNetworkCallback(vpnCallback);
        }
    }

    @Override
    public void onBackPressed() {
        // While moving a tile, Back puts the grid back as it was.
        if (moveMode) {
            cancelMove();
            return;
        }
        // Home screen: Back goes nowhere.
    }

    // ---- glass highlight -------------------------------------------------------------------------

    @Override
    public void onTileFocused(int position, View card) {
        lastFocused = position;
        lastFocusedKey = adapter.keyAt(position);
        // Tile rect in root coordinates, using layout positions (ignores the scale animation).
        Rect r = new Rect(0, 0, card.getWidth(), card.getHeight());
        root.offsetDescendantRectToMyCoords(card, r);
        float pad = 6f * density;        // matches GlassView's glow inset
        float w = card.getWidth() * AppAdapter.FOCUS_SCALE + 2 * pad;
        float h = card.getHeight() * AppAdapter.FOCUS_SCALE + 2 * pad;
        float x = r.exactCenterX() - w / 2f;
        float y = r.exactCenterY() - h / 2f;

        ViewGroup.LayoutParams lp = glass.getLayoutParams();
        if (lp.width != (int) w || lp.height != (int) h) {
            lp.width = (int) w;
            lp.height = (int) h;
            glass.setLayoutParams(lp);
            glass.setRadius(AppAdapter.cornerRadius(card.getWidth(), density) * AppAdapter.FOCUS_SCALE + pad);
        }
        glassTargetX = x;
        glassTargetY = y;
        if (!glassShown) {
            springX.cancel();
            springY.cancel();
            glass.setTranslationX(x);
            glass.setTranslationY(y);
            glass.animate().alpha(1f).setDuration(180).start();
            glassShown = true;
        } else {
            springX.animateToFinalPosition(x);
            springY.animateToFinalPosition(y - glassScrollShift);
        }
        View item = (View) card.getParent();
        if (position < COLUMNS) {
            // Dock: park it at the bottom of the screen and show the top shelf for this app.
            int decoratedTop = item.getTop() - (int) (6 * density);
            int dy = decoratedTop - grid.getPaddingTop();
            if (Math.abs(dy) > 1) grid.smoothScrollBy(0, dy);
            showHero(position, r.top - dy);
        } else {
            hideHero();
            // Scroll just enough so the whole glass (not only the tile) is visible.
            Rect want = new Rect(0, -(int) pad * 2, item.getWidth(), item.getHeight() + (int) pad);
            grid.requestChildRectangleOnScreen(item, want, false);
        }
    }

    // ---- top shelf -------------------------------------------------------------------------------

    private void showHero(int position, float dockTopInRoot) {
        String label = adapter.labelAt(position);
        int color = adapter.ambientColorAt(position);
        float y = dockTopInRoot - 14 * density - 26 * density - heroName.getHeight();
        boolean changed = !label.contentEquals(heroName.getText());

        if (!heroShown) {
            heroName.setText(label);
            heroName.setY(y);
            heroName.setTranslationX(0f);
            heroName.animate().alpha(1f).setDuration(260).start();
            setAmbient(color);
            ambient.animate().alpha(1f).setDuration(600).start();
            heroShown = true;
        } else if (changed) {
            heroName.animate().alpha(0f).translationX(-12 * density).setDuration(120).withEndAction(() -> {
                heroName.setText(label);
                heroName.setY(y);
                heroName.setTranslationX(12 * density);
                heroName.animate().alpha(1f).translationX(0f).setDuration(200).start();
            }).start();
            if (color != ambientColor) {
                ambient.animate().alpha(0.35f).setDuration(200).withEndAction(() -> {
                    setAmbient(color);
                    ambient.animate().alpha(1f).setDuration(500).start();
                }).start();
            }
        } else {
            heroName.setY(y);
        }
    }

    private void setAmbient(int color) {
        ambientColor = color;
        int c = color & 0x00FFFFFF;
        int strong = 0x5C;
        int mid = 0x26;
        if (tokens != null) {
            // Keep the app's colour, but let the phase's accent and the night dimming show.
            c = Contrast.mix(c, tokens.weatherGlow & 0x00FFFFFF, 0.16f + 0.16f * tokens.warm);
            strong = Math.round(0x5C - 0x1C * tokens.blend);
            mid = Math.round(0x26 - 0x12 * tokens.blend);
        }
        ambientGlow.setColors(new int[]{(strong << 24) | c, (mid << 24) | c, 0x00000000 | c});
    }

    private void hideHero() {
        if (!heroShown) return;
        heroShown = false;
        heroName.animate().alpha(0f).setDuration(160).start();
        ambient.animate().alpha(0f).setDuration(300).start();
    }

    private void hideGlass() {
        if (!glassShown) return;
        glassShown = false;
        glass.animate().alpha(0f).setDuration(160).start();
    }

    // ---- navigation ------------------------------------------------------------------------------

    @Override
    public boolean onTileVerticalKey(int position, int keyCode) {
        if (moveMode) return moveTile(position, keyCode);
        int count = adapter.getItemCount();
        int target;
        // Horizontal: swallow the key at a row's ends so focus never wraps to the next row or
        // escapes up into the top bar. Mid-row moves fall through to the default focus search.
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return position % COLUMNS == 0;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return position % COLUMNS == COLUMNS - 1 || position == count - 1;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (position < COLUMNS) return false;          // dock row: let focus move to the top bar
            target = position - COLUMNS;
        } else {
            int lastRowStart = ((count - 1) / COLUMNS) * COLUMNS;
            if (position >= lastRowStart) return true;     // already on the last row: stay put
            target = Math.min(position + COLUMNS, count - 1);
        }
        focusTile(target);
        return true;
    }

    // ---- tile menu --------------------------------------------------------------------------------

    /** Holding OK on a grid tile opens its menu; the dock stays as it is. */
    @Override
    public void onTileLongPressed(int position, View card) {
        if (position < COLUMNS) return;                     // the dock is fixed
        String pkg = adapter.packageAt(position);
        if (pkg == null) return;
        String label = adapter.labelAt(position);
        View panel = getLayoutInflater().inflate(R.layout.popup_tile_menu, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        ((TextView) panel.findViewById(R.id.menuTitle)).setText(label);
        final PopupWindow popup = glassPopup(panel, (int) (276 * density));

        if (adapter.getItemCount() > COLUMNS) {
            addMenuRow(rows, popup, R.drawable.ic_menu_move, getString(R.string.tile_move),
                    () -> startMove(position));
        }
        addMenuRow(rows, popup, R.drawable.ic_menu_hide, getString(R.string.tile_hide), () -> {
            if (!HiddenApps.hide(this, pkg)) {
                Toast.makeText(this, R.string.tile_save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, getString(R.string.tile_hidden_toast, label), Toast.LENGTH_SHORT).show();
            reloadApps(true);
        });
        addMenuRow(rows, popup, R.drawable.ic_menu_info, getString(R.string.tile_app_info),
                () -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + pkg))));
        if (!HiddenApps.load(this).isEmpty()) {
            addMenuRow(rows, popup, R.drawable.ic_menu_unhide, getString(R.string.tile_show_hidden),
                    () -> showHiddenApps(card));
        }

        themeMenu(panel);
        showMenuAt(popup, panel, card);
        rows.getChildAt(0).requestFocus();
    }

    /** The hidden apps, so one can be brought back without editing config files. */
    private void showHiddenApps(View anchor) {
        View panel = getLayoutInflater().inflate(R.layout.popup_tile_menu, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        ((TextView) panel.findViewById(R.id.menuTitle)).setText(R.string.hidden_title);
        final PopupWindow popup = glassPopup(panel, (int) (276 * density));
        List<String> hidden = new ArrayList<>(HiddenApps.load(this));
        Collections.sort(hidden, (a, b) -> appLabel(a).compareToIgnoreCase(appLabel(b)));
        for (String pkg : hidden) {
            addMenuRow(rows, popup, R.drawable.ic_menu_unhide, appLabel(pkg), () -> {
                if (!HiddenApps.unhide(this, pkg)) {
                    Toast.makeText(this, R.string.tile_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, getString(R.string.tile_unhidden_toast, appLabel(pkg)), Toast.LENGTH_SHORT).show();
                reloadApps(true);
            });
        }
        if (rows.getChildCount() == 0) return;
        themeMenu(panel);
        showMenuAt(popup, panel, anchor);
        rows.getChildAt(0).requestFocus();
    }

    private void addMenuRow(LinearLayout rows, PopupWindow popup, int iconRes, String text, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_menu, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.title)).setText(text);
        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
        rows.addView(row);
    }

    /** Places the menu under its tile, above it when there is no room below, kept inside the screen. */
    private void showMenuAt(PopupWindow popup, View panel, View anchor) {
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int w = Math.max(popup.getWidth(), panel.getMeasuredWidth());
        int h = panel.getMeasuredHeight();
        popup.setWidth(w);
        int[] at = new int[2];
        int[] rootAt = new int[2];
        anchor.getLocationOnScreen(at);
        root.getLocationOnScreen(rootAt);
        int edge = (int) (12 * density), gap = (int) (8 * density);
        int x = at[0] - rootAt[0];
        int y = at[1] - rootAt[1] + anchor.getHeight() + gap;
        x = Math.max(edge, Math.min(x, root.getWidth() - w - edge));
        if (y + h > root.getHeight() - edge) y = at[1] - rootAt[1] - h - gap;
        y = Math.max(edge, y);
        panel.setAlpha(0f);
        panel.setPivotY(0f);
        panel.setScaleY(0.94f);
        panel.setTranslationY(-6 * density);
        popup.showAtLocation(root, Gravity.TOP | Gravity.START, x, y);
        panel.animate().alpha(1f).scaleY(1f).translationY(0f).setDuration(180)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            if (label != null && label.length() > 0) return label.toString();
        } catch (PackageManager.NameNotFoundException ignored) {
            // Uninstalled after it was hidden: show the package so it can still be removed from the list.
        }
        return pkg;
    }

    // ---- moving a tile around ---------------------------------------------------------------------

    /** Starts moving a grid tile, from the tile's menu; the dock stays where it is. */
    private void startMove(int position) {
        if (position < COLUMNS) return;                     // the dock is fixed
        if (adapter.getItemCount() <= COLUMNS) return;       // nothing below the dock to move
        moveMode = true;
        orderBeforeMove.clear();
        orderBeforeMove.addAll(adapter.gridKeys());
        moveHint.setVisibility(View.VISIBLE);
        focusTile(position);
    }

    /** OK while moving places the tile; otherwise the app is opened as usual. */
    @Override
    public boolean onTileClicked(int position) {
        if (!moveMode) return false;
        AppOrder.save(this, adapter.gridKeys());
        moveMode = false;
        moveHint.setVisibility(View.GONE);
        return true;
    }

    /** Moves the tile one step in the direction of the key; the dock is never crossed. */
    private boolean moveTile(int position, int keyCode) {
        int count = adapter.getItemCount();
        int column = position % COLUMNS;
        int target;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                target = column > 0 ? position - 1 : position;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                target = (column < COLUMNS - 1 && position + 1 < count) ? position + 1 : position;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                target = position - COLUMNS >= COLUMNS ? position - COLUMNS : position;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                target = position + COLUMNS < count ? position + COLUMNS : position;
                break;
            default:
                return true;
        }
        if (target == position) return true;
        adapter.move(position, target);
        focusTile(target);
        return true;
    }

    /** Back while moving puts the grid back as it was. */
    private void cancelMove() {
        if (!moveMode) return;
        adapter.setGridOrder(orderBeforeMove);
        moveMode = false;
        moveHint.setVisibility(View.GONE);
    }

    private void focusTile(int position) {
        if (adapter.getItemCount() == 0) {
            btnWifi.requestFocus();
            return;
        }
        final int pos = Math.max(0, Math.min(position, adapter.getItemCount() - 1));
        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
        if (vh != null) {
            View card = vh.itemView.findViewById(R.id.card);
            // Already focused (e.g. back from an app): no focus change fires, so restore the glass by hand.
            if (card.isFocused()) onTileFocused(pos, card);
            else card.requestFocus();
            return;
        }
        // Not laid out yet: scroll there, then focus once the scroll settles.
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                rv.removeOnScrollListener(this);
                RecyclerView.ViewHolder h = rv.findViewHolderForAdapterPosition(pos);
                View target = h != null ? h.itemView.findViewById(R.id.card) : rv.getChildAt(0);
                if (target != null) target.requestFocus();
            }
        });
        grid.smoothScrollToPosition(pos);
    }

    // ---- top bar ---------------------------------------------------------------------------------

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int res;
        if (hour < 5) res = R.string.good_night;
        else if (hour < 12) res = R.string.good_morning;
        else if (hour < 17) res = R.string.good_afternoon;
        else if (hour < 22) res = R.string.good_evening;
        else res = R.string.good_night;
        // With a name the greeting reads "Good evening, <name>"; without one it stays as it was.
        String text = getString(res);
        greeting.setText(greetingName.isEmpty() ? text : getString(R.string.greeting_named, text, greetingName));
        if (shamsi) shamsiDate.setText(Shamsi.today());
    }

    /** Shows the Wi-Fi network name on the button when we can read it, otherwise the connection type. */
    private void updateWifiLabel() {
        String text = getString(R.string.wifi);
        boolean vpn = false, online = false;
        int icon = R.drawable.ic_wifi;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean wifi = false, ethernet = false;
            // All networks, not just the active one: with a VPN up the active network is the VPN itself.
            for (Network net : cm != null ? cm.getAllNetworks() : new Network[0]) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                if (caps == null) continue;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpn = true;
                    continue;
                }
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue;
                online = true;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) ethernet = true;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true;
            }
            if (!online) {
                text = getString(R.string.offline);
                vpn = false;
            } else if (ethernet) {
                text = getString(R.string.ethernet);
                icon = R.drawable.ic_ethernet;
            } else if (wifi) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                String ssid = info != null ? info.getSSID() : null;
                if (ssid != null) {
                    ssid = ssid.replace("\"", "").trim();
                    if (!ssid.isEmpty() && !ssid.contains("unknown") && !ssid.equals("0x")) text = ssid;
                }
            }
        } catch (Exception ignored) {
        }
        // With a VPN app configured, a VPN that is down matters too: show a red "VPN off" capsule then.
        boolean vpnDown = !vpn && online && !vpnApp.isEmpty();
        if (vpn || vpnDown) {
            // Network name, then the green lock-and-VPN (or red "VPN off") capsule drawn in place of the word.
            String badge = getString(vpn ? R.string.vpn : R.string.vpn_off);
            VpnBadge span = new VpnBadge(vpn ? getDrawable(R.drawable.ic_lock) : null, density);
            // The pill has a maximum width and ellipsizes at the end, which would cut the badge off after
            // a long network name, so shorten the name to whatever room the badge leaves. The total paddings
            // include the Wi-Fi icon.
            TextPaint paint = btnWifi.getPaint();
            float room = btnWifi.getMaxWidth() - btnWifi.getTotalPaddingStart() - btnWifi.getTotalPaddingEnd()
                    - span.getSize(paint, badge, 0, badge.length(), null) - paint.measureText(" ") - 2 * density;
            text = TextUtils.ellipsize(text, paint, Math.max(0, room), TextUtils.TruncateAt.END).toString();
            SpannableString label = new SpannableString(text + " " + badge);
            label.setSpan(span, text.length() + 1, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            btnWifi.setText(label);
        } else {
            btnWifi.setText(text);
        }
        btnWifi.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
    }

    /** The network pill opens the VPN app when one is configured and installed, otherwise Wi-Fi settings. */
    private void openNetworkPill() {
        Intent i = vpnApp.isEmpty() ? null : getPackageManager().getLeanbackLaunchIntentForPackage(vpnApp);
        if (i == null && !vpnApp.isEmpty()) i = getPackageManager().getLaunchIntentForPackage(vpnApp);
        open(i != null ? i : new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    /** Hides the Inputs pill when it would have nothing to list: streaming boxes have no HDMI-in, AV or tuner. */
    private void updateInputsButton() {
        boolean any = liveTvIntent() != null || !sourcePanelService.isEmpty();
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (!any && tim != null) {
            for (TvInputInfo info : tim.getTvInputList()) {
                if (info.isPassthroughInput()) {
                    any = true;
                    break;
                }
            }
        }
        btnInputs.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    /** The Live TV app's launch intent: its leanback entry point, or the configured action if it has none. */
    private Intent liveTvIntent() {
        if (liveTvPkg.isEmpty()) return null;
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(liveTvPkg);
        if (i != null || liveTvAction.isEmpty()) return i;
        Intent byAction = new Intent(liveTvAction).setPackage(liveTvPkg);
        return getPackageManager().resolveActivity(byAction, 0) != null ? byAction : null;
    }

    /** The TV's own source-selection panel, for TVs that have no TvInputManager (TCL Android TV 9). */
    private void openSourcePanel() {
        Intent i = new Intent(sourcePanelAction);
        ComponentName c = ComponentName.unflattenFromString(sourcePanelService);
        if (c != null) i.setComponent(c);
        try {
            startService(i);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    private void open(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    /** Switches to a passthrough input (HDMI, AV). The standard way is ACTION_VIEW on its channel URI,
     *  which the Live TV app handles; where nothing takes that, the configured `input_action` is sent
     *  with the same URI instead (XGIMI's HDMI player only answers com.xgimi.action.hdmiPlayer). */
    private void openInput(String inputId) {
        Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
        Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.cursor.item/channel")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
            return;
        } catch (ActivityNotFoundException e) {
            if (inputAction.isEmpty()) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        open(new Intent(inputAction).setDataAndType(uri, "vnd.android.cursor.item/channel"));
    }

    /** Dropdown menu under the Inputs pill: HDMI ports (with connection state), AV and Live TV. */
    private void showInputs() {
        View anchor = findViewById(R.id.btnInputs);
        View panel = getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);

        // Compact: as wide as the Inputs pill (a touch wider so "HDMI 1" fits), hanging right below it.
        int width = Math.max(anchor.getWidth(), (int) (150 * density));
        final PopupWindow popup = glassPopup(panel, width);

        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        boolean sawNonHdmi = false;
        String tunerId = null;
        boolean tunerIsHw = false;
        if (tim != null) {
            List<TvInputInfo> inputs = new ArrayList<>(tim.getTvInputList());
            // HDMI ports first in port order, then AV and the rest.
            Collections.sort(inputs, (a, b) -> {
                boolean ha = a.getType() == TvInputInfo.TYPE_HDMI, hb = b.getType() == TvInputInfo.TYPE_HDMI;
                if (ha != hb) return ha ? -1 : 1;
                return String.valueOf(a.loadLabel(this)).compareToIgnoreCase(String.valueOf(b.loadLabel(this)));
            });
            // Some TVs (Sony) report every HDMI port as connected whether or not anything is plugged in,
            // so drop the HDMI status then. A TV with every port really in use loses it too.
            int hdmiPorts = 0, hdmiConnected = 0;
            for (TvInputInfo info : inputs) {
                if (!info.isPassthroughInput() || info.getType() != TvInputInfo.TYPE_HDMI) continue;
                hdmiPorts++;
                if (tim.getInputState(info.getId()) == TvInputManager.INPUT_STATE_CONNECTED) hdmiConnected++;
            }
            boolean hdmiStateBogus = hdmiPorts > 1 && hdmiConnected == hdmiPorts;
            for (TvInputInfo info : inputs) {
                if (info.getType() == TvInputInfo.TYPE_TUNER
                        && (info.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // Prefer a hardware tuner: the framework names those ".../HW<n>". System apps such as
                    // AirPlay also register software inputs of type TUNER and may sort first (seen on TCL G05).
                    boolean hw = info.getId().matches(".*/HW\\d+$");
                    if (tunerId == null || (hw && !tunerIsHw) || (hw == tunerIsHw && info.getId().compareTo(tunerId) < 0)) {
                        tunerId = info.getId();
                        tunerIsHw = hw;
                    }
                }
                if (!info.isPassthroughInput()) continue;
                CharSequence l = info.loadLabel(this);
                String label = l == null ? info.getId() : l.toString();
                boolean hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
                if (hdmi && !label.toUpperCase(Locale.ROOT).contains("HDMI")) label = "HDMI " + label;
                final String id = info.getId();
                int state = hdmi && hdmiStateBogus ? -1 : tim.getInputState(id);
                if (!hdmi && rows.getChildCount() > 0 && !sawNonHdmi) addSeparator(rows);
                if (!hdmi) sawNonHdmi = true;
                addInputRow(rows, popup, hdmi ? R.drawable.ic_input : R.drawable.ic_av, label, state,
                        () -> openInput(id));
            }
        }
        // Nothing to list where there is no TvInputManager: the TV's own source panel switches inputs.
        if (rows.getChildCount() == 0 && !sourcePanelService.isEmpty()) {
            addInputRow(rows, popup, R.drawable.ic_input, getString(R.string.inputs), -1, this::openSourcePanel);
        }
        Intent liveTv = liveTvIntent();
        if (liveTv != null && tunerId != null) {
            liveTv = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(TvContract.buildChannelUriForPassthroughInput(tunerId), "vnd.android.cursor.item/channel")
                    .setPackage(liveTvPkg);
        }
        if (liveTv != null) {
            if (rows.getChildCount() > 0) addSeparator(rows);
            final Intent liveTvIntent = liveTv;
            addInputRow(rows, popup, R.drawable.ic_livetv, getString(R.string.live_tv), -1, () -> open(liveTvIntent));
        }
        if (rows.getChildCount() == 0) {
            Toast.makeText(this, R.string.no_inputs, Toast.LENGTH_SHORT).show();
            return;
        }

        // Fit the widest row: TVs that report a status on every port (Sony) would otherwise squeeze the names out.
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popup.setWidth(Math.max(width, panel.getMeasuredWidth()));

        themeMenu(panel);
        dropIn(popup, panel, anchor, Gravity.START);
        rows.getChildAt(0).requestFocus();
    }

    /** Colours a popup or dialog in the phase that is on screen at the moment it opens. */
    private void themeMenu(View panel) {
        if (tokens == null) return;
        panel.setBackground(ThemeManager.panelBackground(tokens, 20 * density));
        TextView title = panel.findViewById(R.id.menuTitle);
        if (title != null) title.setTextColor(tokens.secondaryText);
        LinearLayout rows = panel.findViewById(R.id.rows);
        if (rows != null) {
            for (int i = 0; i < rows.getChildCount(); i++) {
                View row = rows.getChildAt(i);
                if (!(row instanceof LinearLayout)) continue;   // separators stay thin lines
                row.setBackgroundTintList(tokens.rowSurfaceStates());
                TextView text = row.findViewById(R.id.title);
                if (text != null) text.setTextColor(tokens.textStates(tokens.panelPrimaryText));
                TextView status = row.findViewById(R.id.status);
                if (status != null) status.setTextColor(tokens.panelSecondaryText);
                ImageView icon = row.findViewById(R.id.icon);
                if (icon != null) icon.setImageTintList(tokens.textStates(tokens.panelPrimaryText));
            }
        }
    }

    /** The card prompt and the module cards list: same glass and colours as the inputs menu. */
    private void themeDialog(View panel) {
        if (tokens == null) return;
        ThemeManager.Tokens t = tokens;
        panel.setBackground(ThemeManager.panelBackground(t, 20 * density));
        for (int id : new int[]{R.id.title}) {
            TextView v = panel.findViewById(id);
            if (v != null) v.setTextColor(t.panelPrimaryText);
        }
        for (int id : new int[]{R.id.body, R.id.names, R.id.hint, R.id.identity, R.id.app,
                R.id.pkg, R.id.digest, R.id.state}) {
            TextView v = panel.findViewById(id);
            if (v != null) v.setTextColor(t.panelSecondaryText);
        }
        for (int id : new int[]{R.id.btnKeep, R.id.btnShow}) {
            TextView v = panel.findViewById(id);
            if (v != null) {
                v.setBackground(ThemeManager.pillBackground(t, 21 * density));
                v.setTextColor(t.textStates(t.panelPrimaryText));
            }
        }
        LinearLayout rows = panel.findViewById(R.id.rows);
        if (rows != null) {
            for (int i = 0; i < rows.getChildCount(); i++) {
                View row = rows.getChildAt(i);
                if (!(row instanceof LinearLayout)) continue;
                row.setBackgroundTintList(t.rowSurfaceStates());
                TextView text = row.findViewById(R.id.title);
                if (text != null) text.setTextColor(t.textStates(t.panelPrimaryText));
                ImageView icon = row.findViewById(R.id.icon);
                if (icon != null) icon.setImageTintList(t.textStates(t.panelPrimaryText));
            }
        }
    }

    private PopupWindow glassPopup(View panel, int width) {
        final PopupWindow popup = new PopupWindow(panel, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));   // needed for Back to dismiss
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setElevation(0f);
        popup.setOnDismissListener(() -> {
            if (openPopup == popup) openPopup = null;
        });
        openPopup = popup;
        return popup;
    }

    /** Drop in: fade and unfold from the top edge, hanging under the anchor pill. */
    private void dropIn(PopupWindow popup, View panel, View anchor, int gravity) {
        panel.setAlpha(0f);
        panel.setPivotY(0f);
        panel.setScaleY(0.92f);
        panel.setTranslationY(-8 * density);
        popup.showAsDropDown(anchor, 0, (int) (10 * density), gravity);
        panel.animate().alpha(1f).scaleY(1f).translationY(0f).setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
    }

    private void addSeparator(LinearLayout rows) {
        View line = new View(this);
        line.setBackgroundColor(tokens != null ? tokens.separator : 0x1FFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) density);
        int m = (int) (6 * density), h = (int) (12 * density);
        lp.setMargins(h, m, h, m);
        rows.addView(line, lp);
    }

    private void addInputRow(LinearLayout rows, PopupWindow popup, int iconRes, String title, int state, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        TextView status = row.findViewById(R.id.status);
        View dot = row.findViewById(R.id.dot);
        // This TV reports "standby" for every port whether or not something is plugged in, so only
        // definite states are shown.
        if (state == TvInputManager.INPUT_STATE_CONNECTED) {
            status.setText(R.string.connected);
            dot.setVisibility(View.VISIBLE);
        } else if (state == TvInputManager.INPUT_STATE_DISCONNECTED) {
            status.setText(R.string.not_connected);
            dot.setVisibility(View.GONE);
        } else {
            status.setVisibility(View.GONE);
            dot.setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
        rows.addView(row);
    }

    // ---- module cards ----------------------------------------------------------------------------

    /**
     * Asks about the next card that has no decision for its current signing key. One prompt at a
     * time, only over a laid out grid with window focus (not under a popup or another app), and
     * the next one follows as soon as this one is answered. Nothing is shown until the owner says Show.
     */
    private void showCardPrompt() {
        if (!moduleCards || !tilesReady || promptsPaused || cardPrompt != null || !hasWindowFocus() || isFinishing()) return;
        if (cards.isEmpty() || !CardFeature.working()) return;
        CardFeature.run(() -> {
            showCardPromptNow();
            return null;
        }, null);
    }

    private void showCardPromptNow() {
        final Cards.Card card = Cards.firstPending(cards);
        if (card == null) return;

        View panel = getLayoutInflater().inflate(R.layout.dialog_card_prompt, root, false);
        ((TextView) panel.findViewById(R.id.title)).setText(getString(R.string.card_prompt_title, card.appLabel));
        ((TextView) panel.findViewById(R.id.body)).setText(card.state == Cards.State.RESIGNED
                ? getString(R.string.card_prompt_resigned, card.appLabel)
                : card.state == Cards.State.RELABELLED
                ? getString(R.string.card_prompt_relabelled, card.appLabel)
                : getString(R.string.card_prompt_body, card.appLabel));
        TextView names = panel.findViewById(R.id.names);
        if (Cards.fold(card.label).equals(Cards.fold(card.appLabel))) names.setVisibility(View.GONE);
        else names.setText(getString(R.string.card_prompt_names, card.label, card.appLabel));
        ((TextView) panel.findViewById(R.id.identity)).setText(
                getString(R.string.card_prompt_identity, card.pkg, Cards.shortDigest(card.digest)));
        TextView keep = panel.findViewById(R.id.btnKeep);
        TextView show = panel.findViewById(R.id.btnShow);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(false);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (440 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.6f);
        }
        // One decision per prompt: the buttons and Back (cancel) all end here.
        final boolean[] decided = {false};
        View.OnClickListener choose = v -> {
            if (decided[0]) return;
            decided[0] = true;
            cardPrompt = null;
            dialog.dismiss();
            decideCard(card, v.getId() == R.id.btnShow);   // guards its own card calls; the tile reload stays outside
            restoreFocus();   // the tiles changed under the prompt; this also chains the next prompt
        };
        keep.setOnClickListener(choose);
        show.setOnClickListener(choose);
        dialog.setOnCancelListener(d -> choose.onClick(keep));
        dialog.setOnDismissListener(d -> {
            if (cardPrompt == dialog) cardPrompt = null;
        });
        // Left and Right move between the two buttons; Up and Down stay put; the ends don't wrap.
        View.OnKeyListener keys = (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return true;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return v.getId() == R.id.btnKeep;
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v.getId() == R.id.btnShow;
        };
        keep.setOnKeyListener(keys);
        show.setOnKeyListener(keys);
        themeDialog(panel);
        cardPrompt = dialog;
        dialog.show();
        keep.requestFocus();
    }

    /** Writes the owner's decision and rebuilds the tiles; focus is left where it is. */
    private void decideCard(Cards.Card card, boolean show) {
        boolean saved = CardFeature.run(() -> Cards.decide(Cards.storeFile(this), card, show), false);
        if (!saved) {
            Toast.makeText(this, R.string.card_not_saved, Toast.LENGTH_SHORT).show();
            promptsPaused = true;
        }
        reloadTiles();
    }

    /** Dropdown under the Settings pill (hold OK): every discovered card, its package and key; OK toggles Shown and Hidden. */
    private void showModuleCards() {
        if (cards.isEmpty() || !CardFeature.working()) {
            Toast.makeText(this, R.string.no_module_cards, Toast.LENGTH_SHORT).show();
            return;
        }
        CardFeature.run(() -> {
            showModuleCardsNow();
            return null;
        }, null);
    }

    private void showModuleCardsNow() {
        View anchor = findViewById(R.id.btnSettings);
        View panel = getLayoutInflater().inflate(R.layout.popup_cards, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);
        final PopupWindow popup = glassPopup(panel, (int) (400 * density));
        List<Cards.Card> listed = new ArrayList<>(cards);
        Collections.sort(listed, Cards::byLabel);
        for (Cards.Card card : listed) {
            View row = getLayoutInflater().inflate(R.layout.item_card_row, rows, false);
            row.setTag(card.pkg);
            ((TextView) row.findViewById(R.id.title)).setText(card.label);
            TextView app = row.findViewById(R.id.app);
            if (Cards.fold(card.label).equals(Cards.fold(card.appLabel))) app.setVisibility(View.GONE);
            else app.setText(getString(R.string.card_app_name, card.appLabel));
            ((TextView) row.findViewById(R.id.pkg)).setText(card.pkg);
            ((TextView) row.findViewById(R.id.digest)).setText(Cards.longDigest(card.digest));
            final String pkg = card.pkg;
            row.setOnClickListener(v -> CardFeature.run(() -> {
                // The list stays open; the decision is for the card as it is now, not as it was listed.
                Cards.Card now = cardFor(pkg);
                if (now == null) return null;
                decideCard(now, now.state != Cards.State.SHOWN);
                // Every row, not only this one: a toggle can move another card across the limit of 8.
                updateCardStates(rows);
                return null;
            }, null));
            rows.addView(row);
        }
        updateCardStates(rows);
        // Scroll inside the panel instead of running off the bottom of a 720p screen.
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] at = new int[2];
        anchor.getLocationInWindow(at);
        int room = root.getHeight() - (at[1] + anchor.getHeight()) - (int) (34 * density);
        popup.setHeight(Math.min(panel.getMeasuredHeight(), Math.max(room, (int) (120 * density))));
        themeDialog(panel);
        dropIn(popup, panel, anchor, Gravity.END);
        rows.getChildAt(0).requestFocus();
    }

    private Cards.Card cardFor(String pkg) {
        for (Cards.Card c : cards) if (c.pkg.equals(pkg)) return c;
        return null;
    }

    /** Writes the state of every row in the Module cards list from the current card list. */
    private void updateCardStates(LinearLayout rows) {
        List<Cards.Card> drawn = Cards.shown(cards);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            Cards.Card card = row.getTag() instanceof String ? cardFor((String) row.getTag()) : null;
            ((TextView) row.findViewById(R.id.state)).setText(card != null ? stateText(card, drawn.contains(card)) : "");
        }
    }

    /** The row's state: a card the owner set to Shown but which is beyond the eight drawn says so. */
    private String stateText(Cards.Card card, boolean drawn) {
        switch (card.state) {
            case SHOWN:
                return getString(drawn ? R.string.card_state_shown : R.string.card_state_over_limit);
            case HIDDEN:
                return getString(R.string.card_state_hidden);
            case RESIGNED:
                return getString(R.string.card_state_new_key);
            case RELABELLED:
                return getString(R.string.card_state_new_label);
            default:
                return getString(R.string.card_state_new);
        }
    }
}
