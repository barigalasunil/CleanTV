package com.gathin.tvhome;

import android.content.Context;
import android.widget.TextView;

/** Draws one status pill and keeps its spoken label in step with the reading. */
final class InfoPill {
    static final int RAM = 0, STORAGE = 1, NETWORK = 2;

    private InfoPill() {
    }

    static void update(Context ctx, TextView pill, int kind) {
        if (pill == null) return;
        String value;
        String name;
        switch (kind) {
            case RAM:
                value = SysInfo.ram(ctx);
                name = ctx.getString(R.string.ram);
                break;
            case STORAGE:
                value = SysInfo.storage(ctx);
                name = ctx.getString(R.string.storage);
                break;
            default:
                value = SysInfo.network(ctx);
                name = ctx.getString(R.string.network);
                break;
        }
        if (value.isEmpty()) return;
        if (!value.contentEquals(pill.getText())) pill.setText(value);
        pill.setContentDescription(name + " " + value);
    }
}
