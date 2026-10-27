package com.printxpress.printxpress.activities;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.UiUtils;
import com.printxpress.printxpress.utils.Validation;

/**
 * Offline password reset: the app has no mail server, so the person proves who they are
 * with the phone number they registered with.
 */
public class ForgotPasswordActivity extends BaseActivity {

    private TextInputLayout tilEmail, tilPhone, tilPassword, tilConfirm;
    private TextInputEditText etEmail, etPhone, etPassword, etConfirm;
    private MaterialButton btnReset;
    private UserRepository users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        setupToolbar(R.string.forgot_title);

        users = new UserRepository(this);
        tilEmail = findViewById(R.id.til_forgot_email);
        tilPhone = findViewById(R.id.til_forgot_phone);
        tilPassword = findViewById(R.id.til_forgot_password);
        tilConfirm = findViewById(R.id.til_forgot_confirm);
        etEmail = findViewById(R.id.et_forgot_input);
        etPhone = findViewById(R.id.et_forgot_phone);
        etPassword = findViewById(R.id.et_forgot_password);
        etConfirm = findViewById(R.id.et_forgot_confirm);
        btnReset = findViewById(R.id.btn_send_reset_link);

        btnReset.setOnClickListener(v -> resetPassword());
        etConfirm.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                resetPassword();
                return true;
            }
            return false;
        });
    }

    private void resetPassword() {
        final String email = text(etEmail).trim();
        final String phone = text(etPhone).trim();
        final String password = text(etPassword);
        final String confirm = text(etConfirm);

        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);

        boolean ok = true;
        if (!Validation.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            ok = false;
        }
        if (!Validation.isValidPhone(phone)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            ok = false;
        }
        if (!Validation.isValidPassword(password)) {
            tilPassword.setError(getString(R.string.error_password_weak));
            ok = false;
        }
        if (!password.equals(confirm)) {
            tilConfirm.setError(getString(R.string.error_password_mismatch));
            ok = false;
        }
        if (!ok) return;

        UiUtils.hideKeyboard(this);
        btnReset.setEnabled(false);
        Async.io(() -> {
            final boolean success = users.resetPassword(email, phone, password);
            Async.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                btnReset.setEnabled(true);
                if (success) {
                    Toast.makeText(this, R.string.reset_success, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    tilPhone.setError(getString(R.string.error_reset_no_match));
                }
            });
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
