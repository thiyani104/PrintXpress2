package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.UiUtils;
import com.printxpress.printxpress.utils.Validation;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirm;
    private MaterialButton btnRegister;
    private UserRepository users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setupToolbar(R.string.register_title);

        users = new UserRepository(this);
        tilName = findViewById(R.id.til_register_name);
        tilEmail = findViewById(R.id.til_register_email);
        tilPhone = findViewById(R.id.til_register_phone);
        tilPassword = findViewById(R.id.til_register_password);
        tilConfirm = findViewById(R.id.til_register_confirm);
        etName = findViewById(R.id.et_register_name);
        etEmail = findViewById(R.id.et_register_email);
        etPhone = findViewById(R.id.et_register_phone);
        etPassword = findViewById(R.id.et_register_password);
        etConfirm = findViewById(R.id.et_register_confirm);
        btnRegister = findViewById(R.id.btn_register_submit);

        btnRegister.setOnClickListener(v -> processRegistration());
        etConfirm.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                processRegistration();
                return true;
            }
            return false;
        });
        findViewById(R.id.tv_login_redirect).setOnClickListener(v -> finish());
    }

    private void processRegistration() {
        final String name = text(etName).trim();
        final String email = text(etEmail).trim();
        final String phone = text(etPhone).trim();
        final String password = text(etPassword);
        final String confirm = text(etConfirm);

        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);

        boolean ok = true;
        if (!Validation.isValidName(name)) {
            tilName.setError(getString(R.string.error_name_invalid));
            ok = false;
        }
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
        btnRegister.setEnabled(false);
        btnRegister.setText(R.string.creating_account);

        Async.io(() -> {
            final boolean exists = users.emailExists(email);
            final long id = exists ? -1 : users.register(name, email, phone, password);
            Async.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                btnRegister.setEnabled(true);
                btnRegister.setText(R.string.create_account);
                if (exists) {
                    tilEmail.setError(getString(R.string.error_email_taken));
                } else if (id == -1) {
                    UiUtils.snack(this, R.string.error_generic);
                } else {
                    session.login(id, name);
                    session.setOnboardingDone();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
