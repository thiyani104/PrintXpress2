package com.printxpress.printxpress.activities;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.models.User;
import com.printxpress.printxpress.utils.UiUtils;
import com.printxpress.printxpress.utils.Validation;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EditProfileActivity extends BaseActivity {

    private UserRepository users;
    private User user;
    private TextInputLayout tilName, tilPhone, tilAddress;
    private TextInputEditText etName, etEmail, etPhone, etAddress, etDob;
    private String dobIso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        setupToolbar(R.string.edit_profile);

        users = new UserRepository(this);
        user = users.getById(session.getUserId());
        if (user == null) {
            finish();
            return;
        }

        tilName = findViewById(R.id.til_edit_name);
        tilPhone = findViewById(R.id.til_edit_phone);
        tilAddress = findViewById(R.id.til_edit_address);
        etName = findViewById(R.id.et_edit_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        etAddress = findViewById(R.id.et_edit_address);
        etDob = findViewById(R.id.et_edit_dob);

        ((TextView) findViewById(R.id.tv_edit_initials)).setText(user.getInitials());

        if (savedInstanceState == null) {
            etName.setText(user.getFullName());
            etPhone.setText(user.getPhone());
            etAddress.setText(user.getAddress());
            dobIso = user.getDob();
        } else {
            dobIso = savedInstanceState.getString("dob");
        }
        etEmail.setText(user.getEmail());
        renderDob();

        etDob.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.til_edit_dob).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btn_save_profile).setOnClickListener(v -> save());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("dob", dobIso);
    }

    private void renderDob() {
        if (dobIso == null || dobIso.isEmpty()) {
            etDob.setText("");
            return;
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            etDob.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(in.parse(dobIso)));
        } catch (Exception e) {
            etDob.setText(dobIso);
        }
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now()).build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date_of_birth)
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            dobIso = fmt.format(selection);
            renderDob();
        });
        picker.show(getSupportFragmentManager(), "dob_picker");
    }

    private void save() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String phone = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
        String address = etAddress.getText() == null ? "" : etAddress.getText().toString().trim();

        tilName.setError(null);
        tilPhone.setError(null);
        tilAddress.setError(null);

        boolean ok = true;
        if (!Validation.isValidName(name)) {
            tilName.setError(getString(R.string.error_name_invalid));
            ok = false;
        }
        if (!Validation.isValidPhone(phone)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            ok = false;
        }
        if (!address.isEmpty() && address.length() < 10) {
            tilAddress.setError(getString(R.string.error_address_short));
            ok = false;
        }
        if (!ok) return;

        UiUtils.hideKeyboard(this);
        if (users.updateProfile(user.getId(), name, phone, dobIso, address)) {
            session.updateName(name);
            android.widget.Toast.makeText(this, R.string.profile_saved, android.widget.Toast.LENGTH_SHORT).show();
            finish();
        } else {
            UiUtils.snack(this, R.string.error_generic);
        }
    }
}
