package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.models.User;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.UiUtils;
import com.printxpress.printxpress.utils.Validation;

public class LoginActivity extends BaseActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private UserRepository users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        users = new UserRepository(this);
        tilEmail = findViewById(R.id.til_login_email);
        tilPassword = findViewById(R.id.til_login_password);
        etEmail = findViewById(R.id.et_login_identity);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login_submit);

        btnLogin.setOnClickListener(v -> attemptLogin());
        etPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        findViewById(R.id.tv_forgot_password).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.tv_register_redirect).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        final String email = text(etEmail).trim();
        final String password = text(etPassword);

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!Validation.isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            return;
        }

        UiUtils.hideKeyboard(this);
        setLoading(true);
        Async.io(() -> {
            final User user = users.authenticate(email, password);
            Async.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                if (user == null) {
                    tilPassword.setError(getString(R.string.error_login_failed));
                } else {
                    session.login(user.getId(), user.getFullName());
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? R.string.signing_in : R.string.login);
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
