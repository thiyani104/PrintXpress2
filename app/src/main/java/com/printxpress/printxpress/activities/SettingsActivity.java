package com.printxpress.printxpress.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.utils.FileUtils;
import com.printxpress.printxpress.utils.UiUtils;

public class SettingsActivity extends BaseActivity {

    private static final int[] MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    private TextView tvThemeValue;
    private TextView tvCacheValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupToolbar(R.string.settings_title);

        LinearLayout prefs = findViewById(R.id.ll_settings_preferences);
        tvThemeValue = addRow(prefs, R.drawable.ic_theme, R.string.setting_appearance, true, this::showThemeDialog);
        tvCacheValue = addRow(prefs, R.drawable.ic_clean, R.string.setting_clear_cache, false, this::confirmClearCache);

        LinearLayout about = findViewById(R.id.ll_settings_about);
        addRow(about, R.drawable.ic_shield, R.string.privacy_policy, true,
                () -> showText(R.string.privacy_policy, R.string.privacy_policy_text));
        addRow(about, R.drawable.ic_file, R.string.terms_of_service, true,
                () -> showText(R.string.terms_of_service, R.string.terms_text));
        TextView version = addRow(about, R.drawable.ic_info, R.string.app_version, false, null);
        version.setText(versionName());

        renderValues();
    }

    private TextView addRow(LinearLayout parent, @DrawableRes int icon, @StringRes int title,
                            boolean chevron, Runnable action) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_menu_row, parent, false);
        ((ImageView) row.findViewById(R.id.iv_row_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.tv_row_title)).setText(title);
        row.findViewById(R.id.iv_row_chevron).setVisibility(chevron ? View.VISIBLE : View.GONE);
        TextView value = row.findViewById(R.id.tv_row_value);
        value.setVisibility(View.VISIBLE);
        if (action != null) row.setOnClickListener(v -> action.run());
        else row.setClickable(false);
        parent.addView(row);
        return value;
    }

    private void renderValues() {
        int mode = session.getThemeMode();
        int index = 0;
        for (int i = 0; i < MODES.length; i++) if (MODES[i] == mode) index = i;
        tvThemeValue.setText(getResources().getStringArray(R.array.theme_options)[index]);
        tvCacheValue.setText(FileUtils.formatSize(FileUtils.folderSize(getCacheDir())));
    }

    private void showThemeDialog() {
        String[] options = getResources().getStringArray(R.array.theme_options);
        int current = 0;
        for (int i = 0; i < MODES.length; i++) if (MODES[i] == session.getThemeMode()) current = i;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_appearance)
                .setSingleChoiceItems(options, current, (d, which) -> {
                    session.setThemeMode(MODES[which]);
                    d.dismiss();
                    AppCompatDelegate.setDefaultNightMode(MODES[which]);
                })
                .show();
    }

    private void confirmClearCache() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setting_clear_cache)
                .setMessage(R.string.clear_cache_message)
                .setPositiveButton(R.string.action_clear, (d, w) -> {
                    java.io.File[] files = getCacheDir().listFiles();
                    if (files != null) for (java.io.File f : files) FileUtils.deleteRecursively(f);
                    renderValues();
                    UiUtils.snack(this, R.string.cache_cleared);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showText(@StringRes int title, @StringRes int body) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton(R.string.action_close, null)
                .show();
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }
}
