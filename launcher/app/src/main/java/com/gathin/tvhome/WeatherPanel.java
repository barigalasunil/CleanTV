package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Draws the weather card: current conditions on top, five small days below. */
final class WeatherPanel {

    private final Context ctx;
    private final View panel;
    private final ImageView icon;
    private final TextView temperature;
    private final TextView condition;
    private final TextView meta;
    private final TextView updated;
    private final LinearLayout days;

    WeatherPanel(Activity activity) {
        ctx = activity;
        panel = activity.findViewById(R.id.weatherPanel);
        icon = activity.findViewById(R.id.weatherIcon);
        temperature = activity.findViewById(R.id.weatherTemp);
        condition = activity.findViewById(R.id.weatherCondition);
        meta = activity.findViewById(R.id.weatherMeta);
        updated = activity.findViewById(R.id.weatherUpdated);
        days = activity.findViewById(R.id.forecastRow);
    }

    /** Shows a reading; an old reading or a failed refresh is dimmed and labelled, never hidden. */
    void bind(Weather.Report report, boolean offline) {
        boolean dim = offline || report.stale();
        panel.setAlpha(dim ? 0.62f : 1f);
        icon.setImageResource(report.icon());
        icon.setColorFilter(report.tint());
        temperature.setText(report.temperatureText());
        String sky = Weather.condition(ctx, report.code);
        condition.setText(sky);
        meta.setText(ctx.getString(R.string.weather_meta, report.feelsText(), report.humidity, report.windText()));
        updated.setText(when(report, offline));
        panel.setContentDescription(ctx.getString(R.string.weather_description, report.temperatureText(), sky));

        days.removeAllViews();
        for (Weather.Day day : report.days) {
            View tile = View.inflate(ctx, R.layout.item_weather_day, null);
            ((TextView) tile.findViewById(R.id.day)).setText(day.label(ctx));
            ImageView dayIcon = tile.findViewById(R.id.icon);
            dayIcon.setImageResource(day.icon());
            dayIcon.setColorFilter(day.tint());
            ((TextView) tile.findViewById(R.id.temps)).setText(
                    ctx.getString(R.string.weather_range, Weather.degrees(day.max), Weather.degrees(day.min)));
            TextView rain = tile.findViewById(R.id.rain);
            if (day.rainChance >= 0) {
                rain.setText(ctx.getString(R.string.weather_rain_chance, day.rainChance));
                rain.setVisibility(View.VISIBLE);
            } else {
                rain.setVisibility(View.INVISIBLE);
            }
            days.addView(tile, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }
    }

    void loading(Weather.Report known) {
        if (known != null) {
            bind(known, false);
            return;
        }
        reset(R.string.weather_updating, R.string.weather_updating_description);
    }

    void unavailable(Weather.Report known) {
        if (known != null) {
            bind(known, true);
            return;
        }
        reset(R.string.weather_unavailable, R.string.weather_unavailable_description);
    }

    private void reset(int message, int description) {
        panel.setContentDescription(ctx.getString(description));
        panel.setAlpha(0.62f);
        icon.setImageResource(R.drawable.ic_weather_cloud);
        icon.clearColorFilter();
        temperature.setText("—");
        condition.setText(message);
        meta.setText("");
        updated.setText("");
        days.removeAllViews();
    }

    private String when(Weather.Report report, boolean offline) {
        String time = report.at > 0
                ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(report.at))
                : "";
        if (time.isEmpty()) return ctx.getString(offline ? R.string.offline : R.string.weather_last_reading);
        int res = offline ? R.string.weather_offline : (report.stale() ? R.string.weather_stale : R.string.weather_updated);
        return ctx.getString(res, time);
    }
}
