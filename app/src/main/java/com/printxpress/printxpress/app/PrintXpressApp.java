package com.printxpress.printxpress.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.printxpress.printxpress.utils.SessionManager;

public class PrintXpressApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply the saved theme before any activity is created, so there is no flash.
        AppCompatDelegate.setDefaultNightMode(new SessionManager(this).getThemeMode());
    }
}
