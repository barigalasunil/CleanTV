package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One row of display-only widgets under the control pill: today's weather over two cells, then a RAM
 * and a storage ring gauge. The row hangs from the pill's right edge and keeps the cell size it had
 * under the old pill row, so four 81 dp cells and three 10 dp gaps fit from that edge leftwards.
 *
 * <p>Nothing here takes focus or a key, holds a wake lock or runs in the background: the values are
 * read on the home screen's own ticker, so they stop the moment another app is in front.
 */
final class Widgets {

    /** 72% black, the same frosted band as the dock. */
    private static final int GLASS = 0xB8000000;
    private static final int BORDER = 0x33FFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    /** Secondary text: 70% white. */
    private static final int DIM = 0xB3FFFFFF;

    private final Activity activity;
    private final LinearLayout row;
    private final View pillRow;
    private final float density;
    /** The cell size the row had under the old pill row, and the gap between two cells. */
    private static final float CELL_DP = 81f;
    private static final float GAP_DP = 10f;
    private static final float BAND_DP = 4 * CELL_DP + 3 * GAP_DP;

    private final Typeface face;
    private final int gap;
    private final int radiusDp;

    private View weatherCell;
    private View ramCell;
    private View storageCell;
    private WeatherIconView weatherIcon;
    private TextView weatherTemp;
    private TextView weatherCondition;
    private GaugeView ramGauge;
    private TextView ramUsed;
    private TextView ramTotal;
    private GaugeView storageGauge;
    private TextView storageUsed;
    private TextView storageTotal;
    private View nowPlaying;
    private int cell;
    private int lastWidth = -1;

    private Widgets(Activity activity, LinearLayout row, View pillRow, float density) {
        this.activity = activity;
        this.row = row;
        this.pillRow = pillRow;
        this.density = density;
        this.face = outfit(activity);
        this.gap = px(GAP_DP);
        this.radiusDp = px(1);
    }

    /** Builds the row and keeps it sized to the pill row from then on. */
    static Widgets attach(Activity activity, LinearLayout row, View pillRow, Config cfg, float density) {
        Widgets widgets = new Widgets(activity, row, pillRow, density);
        widgets.build(cfg);
        return widgets;
    }

    private static Typeface outfit(Context ctx) {
        try {
            return ctx.getResources().getFont(R.font.outfit);
        } catch (Exception e) {
            return Typeface.SANS_SERIF;
        }
    }

    private void build(Config cfg) {
        row.removeAllViews();
        row.setFocusable(false);
        row.setClickable(false);
        // Nothing in the row may take focus: the D-pad must still go from the pills straight to the grid.
        row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        boolean weather = cfg.widgetsEnabled && cfg.widgetWeather
                && !Double.isNaN(cfg.weatherLat) && !Double.isNaN(cfg.weatherLon);
        if (weather) {
            weatherCell = weatherCell();
            row.addView(weatherCell, new LinearLayout.LayoutParams(0, 0));
        }
        if (cfg.widgetsEnabled && cfg.widgetRam) {
            ramCell = gaugeCell(true);
            row.addView(ramCell, new LinearLayout.LayoutParams(0, 0));
        }
        if (cfg.widgetsEnabled && cfg.widgetStorage) {
            storageCell = gaugeCell(false);
            row.addView(storageCell, new LinearLayout.LayoutParams(0, 0));
        }

        // The band no longer measures the pill (a single avatar is far narrower than the old row):
        // it keeps its old cell size and hangs from the pill's right edge instead. The width is a
        // constant, so the cells are sized now as well: the row is never measured 0 px wide while
        // waiting for the first layout pass.
        layoutCells(bandWidth());
        sizeNowPlaying();
        pillRow.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> sizeNowPlaying());
        pillRow.post(() -> {
            layoutCells(bandWidth());
            sizeNowPlaying();
        });
        updateRam();
        updateStorage();
    }

    /**
     * Four cell widths and two gaps fill the band: the weather takes two cells, RAM and storage one
     * each. A hidden widget keeps the others at this size, still right aligned.
     */
    private int bandWidth() {
        return px(BAND_DP);
    }

    private void layoutCells(int width) {
        if (width <= 0 || width == lastWidth) return;
        lastWidth = width;
        // Four slots, three gaps: the weather spans two slots, RAM and storage one each.
        int size = (width - 3 * gap) / 4;
        if (size <= 0) return;
        cell = size;
        int radius = Math.round(size / 6f);
        // The worst-case remainder of the division goes to the widest cell, so both edges still line up.
        int leftover = Math.max(0, width - (4 * size + 3 * gap));
        int weatherWidth = 2 * size + gap + (weatherCell != null ? leftover : 0);
        size(weatherCell, weatherWidth, size, radius, gap);
        size(ramCell, size, size, radius, gap);
        size(storageCell, size, size, radius, 0);
        sizeNowPlaying();
        row.requestLayout();
    }

    /** The Now Playing card shares this band: same top, same height, and clear of the widgets. */
    void placeNowPlaying(View card) {
        nowPlaying = card;
        sizeNowPlaying();
    }

    private void sizeNowPlaying() {
        if (nowPlaying == null || cell <= 0) return;
        int widgetLeft = innerLeft();
        int width = Math.min(px(350), Math.max(px(160), widgetLeft - px(30)));
        ViewGroup.LayoutParams lp = nowPlaying.getLayoutParams();
        lp.width = width;
        lp.height = cell;
        nowPlaying.setLayoutParams(lp);
        // The same dark glass as the cells, with the same radius: one band, one material.
        nowPlaying.setBackground(glass(Math.round(cell / 6f)));
    }

    /** Where the band starts, in the widget row's own parent: the pill's right edge minus its width. */
    private int innerLeft() {
        int right = pillRow.getRight();
        if (right <= 0) {
            View parent = (View) pillRow.getParent();
            right = parent == null ? pillRow.getWidth() : parent.getWidth();
        }
        return Math.max(0, right - bandWidth());
    }

    private void size(View view, int width, int height, int radius, int marginEnd) {
        if (view == null) return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.width = width;
        lp.height = height;
        // rightMargin, not setMarginEnd: the layout direction is already resolved by now, so the
        // end margin would be stored where the linear layout no longer reads it.
        lp.rightMargin = marginEnd;
        view.setLayoutParams(lp);
        view.setBackground(glass(radius));
    }

    private GradientDrawable glass(int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(GLASS);
        g.setCornerRadius(radius);
        g.setStroke(Math.max(1, radiusDp), BORDER);
        return g;
    }

    private LinearLayout weatherCell() {
        LinearLayout cellView = new LinearLayout(activity);
        cellView.setOrientation(LinearLayout.HORIZONTAL);
        cellView.setGravity(Gravity.CENTER_VERTICAL);
        cellView.setPadding(px(14), px(10), px(14), px(10));
        cellView.setFocusable(false);
        cellView.setClickable(false);
        cellView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_VERTICAL);
        weatherTemp = text(28, WHITE);
        weatherCondition = text(11, DIM);
        column.addView(weatherTemp);
        LinearLayout.LayoutParams conditionLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        conditionLp.topMargin = px(2);
        column.addView(weatherCondition, conditionLp);
        cellView.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        weatherIcon = new WeatherIconView(activity);
        cellView.addView(weatherIcon, new LinearLayout.LayoutParams(px(42), px(42)));
        return cellView;
    }

    /** A ring gauge with its label above and the used and total amounts below. */
    private LinearLayout gaugeCell(boolean ram) {
        LinearLayout cellView = new LinearLayout(activity);
        cellView.setOrientation(LinearLayout.VERTICAL);
        cellView.setGravity(Gravity.CENTER_HORIZONTAL);
        cellView.setPadding(px(4), px(4), px(4), px(4));
        cellView.setFocusable(false);
        cellView.setClickable(false);
        cellView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        TextView label = text(11, DIM);
        label.setText(activity.getString(ram ? R.string.ram : R.string.storage));
        label.setLetterSpacing(0.12f);
        cellView.addView(label);

        GaugeView gauge = new GaugeView(activity);
        LinearLayout.LayoutParams gaugeLp = new LinearLayout.LayoutParams(px(22), px(22));
        gaugeLp.topMargin = px(1);
        cellView.addView(gauge, gaugeLp);

        TextView used = text(13, WHITE);
        LinearLayout.LayoutParams usedLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        usedLp.topMargin = px(1);
        cellView.addView(used, usedLp);

        TextView total = text(11, DIM);
        cellView.addView(total);

        if (ram) {
            ramGauge = gauge;
            ramUsed = used;
            ramTotal = total;
        } else {
            storageGauge = gauge;
            storageUsed = used;
            storageTotal = total;
        }
        return cellView;
    }

    private TextView text(float sizeDp, int color) {
        TextView v = new TextView(activity);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        v.setTextColor(color);
        v.setTypeface(face);
        v.setIncludeFontPadding(false);
        v.setSingleLine(true);
        v.setFocusable(false);
        v.setClickable(false);
        return v;
    }

    /** The memory reading; called at start and every five seconds while the home screen is up. */
    void updateRam() {
        if (ramCell == null) return;
        long[] memory = SysInfo.memory(activity);
        if (memory[1] <= 0) return;
        ramGauge.animateTo(SysInfo.fraction(memory));
        update(ramUsed, activity.getString(R.string.widget_gb, SysInfo.gigabytes(memory[0])));
        update(ramTotal, activity.getString(R.string.widget_of_total, SysInfo.gigabytes(memory[1])));
        ramCell.setContentDescription(activity.getString(R.string.widget_ram_description,
                SysInfo.gigabytes(memory[0]), SysInfo.gigabytes(memory[1])));
    }

    /** The data partition reading; called at start and every thirty seconds. */
    void updateStorage() {
        if (storageCell == null) return;
        long[] storage = SysInfo.storage();
        if (storage[1] <= 0) return;
        storageGauge.animateTo(SysInfo.fraction(storage));
        update(storageUsed, activity.getString(R.string.widget_gb, SysInfo.gigabytes(storage[0])));
        update(storageTotal, activity.getString(R.string.widget_of_total, SysInfo.gigabytes(storage[1])));
        storageCell.setContentDescription(activity.getString(R.string.widget_storage_description,
                SysInfo.gigabytes(storage[0]), SysInfo.gigabytes(storage[1])));
    }

    /** Shows a reading; a failed refresh simply leaves the last one on screen. */
    void setWeather(Weather.Report report) {
        if (weatherCell == null || report == null) return;
        String sky = Weather.condition(activity, report.code);
        weatherIcon.setWeather(report.code, report.day);
        update(weatherTemp, report.temperatureText());
        update(weatherCondition, sky);
        weatherCell.setContentDescription(activity.getString(R.string.weather_description,
                report.temperatureText(), sky));
    }

    /** Runs on the home screen's shared tick, which only ticks while the home screen is visible. */
    void tick(long tickCount) {
        if (ramCell != null && tickCount % 5 == 0) updateRam();
        if (storageCell != null && tickCount % 30 == 0) updateStorage();
    }

    /** A value that really changed fades in, so a tick reads as motion instead of a silent swap. */
    private void update(TextView view, String value) {
        if (view.getText().toString().equals(value)) return;
        view.setText(value);
        if (Motion.off()) {
            view.setAlpha(1f);
            return;
        }
        view.setAlpha(0.35f);
        view.animate().alpha(1f).setDuration(Motion.ms(Motion.FAST))
                .setInterpolator(Motion.EASE_OUT).start();
    }

    private int px(float dp) {
        return Math.round(dp * density);
    }
}
