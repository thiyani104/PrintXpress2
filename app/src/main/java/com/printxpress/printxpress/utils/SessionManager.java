package com.printxpress.printxpress.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Keeps the logged-in user and a few app preferences across launches. */
public class SessionManager {

    private static final String PREFS = "printxpress_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_THEME_MODE = "theme_mode";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return getUserId() > 0;
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void login(long userId, String name) {
        prefs.edit().putLong(KEY_USER_ID, userId).putString(KEY_USER_NAME, name).apply();
    }

    public void updateName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public void logout() {
        prefs.edit().remove(KEY_USER_ID).remove(KEY_USER_NAME).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }
}
