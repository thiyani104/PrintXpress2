package com.printxpress.printxpress.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.utils.SessionManager;

/**
 * Shared behaviour for every screen:
 * - consistent edge-to-edge handling (the app targets Android 15+, where content otherwise
 *   draws underneath the status and navigation bars)
 * - back-arrow toolbar wiring
 * - access to the session
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        applyWindowInsets();
    }

    private void applyWindowInsets() {
        final View root = findViewById(android.R.id.content);
        final View bottomBar = findViewById(R.id.bottom_bar);
        final View bottomNav = findViewById(R.id.bottom_nav);
        final int barPaddingBottom = bottomBar != null ? bottomBar.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboardOpen = ime.bottom > 0;
            boolean handledByChild = bottomBar != null || bottomNav != null;

            int bottom;
            if (keyboardOpen) bottom = ime.bottom;
            else bottom = handledByChild ? 0 : bars.bottom;

            v.setPadding(bars.left, bars.top, bars.right, bottom);

            if (bottomBar != null) {
                int extra = keyboardOpen ? 0 : bars.bottom;
                bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(),
                        bottomBar.getPaddingRight(), barPaddingBottom + extra);
            }
            return windowInsets;
        });
    }

    /** Sets up the standard toolbar (id: toolbar) with a back arrow. */
    protected void setupToolbar(int titleRes) {
        setupToolbar(getString(titleRes), true);
    }

    protected void setupToolbar(CharSequence title, boolean showBack) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;
        toolbar.setTitle(title);
        if (showBack) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationContentDescription(R.string.cd_back);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    protected void setText(int viewId, CharSequence text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text);
    }
}
