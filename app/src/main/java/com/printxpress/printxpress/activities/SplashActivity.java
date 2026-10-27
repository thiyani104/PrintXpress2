package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_MS = 1200;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable route = this::routeNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(route, SPLASH_MS);
    }

    private void routeNext() {
        SessionManager session = new SessionManager(this);
        Class<?> target;
        if (session.isLoggedIn()) target = MainActivity.class;
        else if (session.isOnboardingDone()) target = LoginActivity.class;
        else target = OnboardingActivity.class;

        startActivity(new Intent(this, target));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(route);
        super.onDestroy();
    }
}
