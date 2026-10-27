package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import android.content.res.ColorStateList;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.models.User;
import com.printxpress.printxpress.utils.BottomNavHelper;

public class ProfileActivity extends BaseActivity {

    private UserRepository users;
    private TextView tvInitials, tvName, tvEmail, tvPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupToolbar(getString(R.string.profile_title), false);
        BottomNavHelper.attach(this, R.id.nav_profile);

        users = new UserRepository(this);
        tvInitials = findViewById(R.id.tv_profile_initials);
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPhone = findViewById(R.id.tv_profile_phone);

        LinearLayout menu = findViewById(R.id.ll_profile_menu);
        addRow(menu, R.drawable.ic_edit, R.string.edit_profile, false,
                () -> startActivity(new Intent(this, EditProfileActivity.class)));
        addRow(menu, R.drawable.ic_settings, R.string.settings_title, false,
                () -> startActivity(new Intent(this, SettingsActivity.class)));
        addRow(menu, R.drawable.ic_help, R.string.help_support, false,
                () -> startActivity(new Intent(this, ContactSupportActivity.class)));
        addRow(menu, R.drawable.ic_design, R.string.design_guidelines_title, false,
                () -> startActivity(new Intent(this, DesignGuidelinesActivity.class)));
        addRow(menu, R.drawable.ic_info, R.string.about_title, false,
                () -> startActivity(new Intent(this, AboutUsActivity.class)));
        addRow(menu, R.drawable.ic_logout, R.string.logout, true, this::confirmLogout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        User user = users.getById(session.getUserId());
        if (user == null) {
            logout();
            return;
        }
        tvInitials.setText(user.getInitials());
        tvName.setText(user.getFullName());
        tvEmail.setText(user.getEmail());
        tvPhone.setText(user.getPhone());
    }

    private void addRow(LinearLayout parent, @DrawableRes int icon, @StringRes int title,
                        boolean danger, Runnable action) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_menu_row, parent, false);
        ImageView iv = row.findViewById(R.id.iv_row_icon);
        TextView tv = row.findViewById(R.id.tv_row_title);
        iv.setImageResource(icon);
        tv.setText(title);
        if (danger) {
            int color = ContextCompat.getColor(this, R.color.colorError);
            tv.setTextColor(color);
            ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(color));
            row.findViewById(R.id.iv_row_chevron).setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> action.run());
        parent.addView(row);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.logout, (d, w) -> logout())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void logout() {
        session.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
