package com.codex.m3566lighttester;

import android.content.Context;
import android.content.SharedPreferences;

final class AutostartSettings {
    private static final String PREFS = "m3566_settings";
    private static final String KEY_AUTOSTART = "autostart_enabled";

    private AutostartSettings() {
    }

    static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTOSTART, true);
    }

    static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTOSTART, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
