package com.asukanemu.mi9clicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

final class ClickConfig {
    static final String PREFS = "clicker_settings";
    static final String KEY_X = "target_x";
    static final String KEY_Y = "target_y";
    static final String KEY_CPS = "cps";
    static final String KEY_COUNT = "count";

    private ClickConfig() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int getX(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return clamp(prefs(context).getInt(KEY_X, metrics.widthPixels / 2), 0, metrics.widthPixels - 1);
    }

    static int getY(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return clamp(prefs(context).getInt(KEY_Y, metrics.heightPixels / 2), 0, metrics.heightPixels - 1);
    }

    static int getCps(Context context) {
        return clamp(prefs(context).getInt(KEY_CPS, 10), 1, 50);
    }

    static int getCount(Context context) {
        return clamp(prefs(context).getInt(KEY_COUNT, 0), 0, 1_000_000);
    }

    static void save(Context context, int x, int y, int cps, int count) {
        prefs(context).edit()
                .putInt(KEY_X, x)
                .putInt(KEY_Y, y)
                .putInt(KEY_CPS, clamp(cps, 1, 50))
                .putInt(KEY_COUNT, clamp(count, 0, 1_000_000))
                .apply();
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
