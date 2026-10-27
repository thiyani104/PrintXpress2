package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.CartRepository;
import com.printxpress.printxpress.database.DBContract.OrderEntry;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.models.User;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.Pricing;
import com.printxpress.printxpress.utils.UiUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CheckoutDeliveryActivity extends BaseActivity {

    public static final String EXTRA_PROMO_CODE = "PROMO_CODE";
    public static final String EXTRA_METHOD = "DELIVERY_METHOD";
    public static final String EXTRA_ADDRESS = "DELIVERY_ADDRESS";
    public static final String EXTRA_DATE = "DELIVERY_DATE";

    private static final String STATE_HOME = "is_home";
    private static final String STATE_DATE = "date_iso";

    private UserRepository users;
    private List<CartItem> items;
    private String promoCode;
    private boolean homeDelivery = true;
    private String address = "";
    private String dateIso;

    private View addressSection;
    private TextView tvAddress, tvPhone, tvDate, tvDateLabel;
    private TextView tvFee, tvTotal;
    private View btnChangeAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_delivery);
        setupToolbar(R.string.checkout_title);

        users = new UserRepository(this);
        items = new CartRepository(this, session.getUserId()).getItems();
        promoCode = getIntent().getStringExtra(EXTRA_PROMO_CODE);
        if (items.isEmpty()) {
            finish();
            return;
        }
        if (savedInstanceState != null) {
            homeDelivery = savedInstanceState.getBoolean(STATE_HOME, true);
            dateIso = savedInstanceState.getString(STATE_DATE);
        }

        addressSection = findViewById(R.id.section_address);
        tvAddress = findViewById(R.id.tv_delivery_address);
        tvPhone = findViewById(R.id.tv_delivery_phone);
        tvDate = findViewById(R.id.tv_delivery_date);
        tvDateLabel = findViewById(R.id.tv_date_label);
        tvFee = findViewById(R.id.tv_summary_delivery_fee);
        tvTotal = findViewById(R.id.tv_checkout_bar_total);
        btnChangeAddress = findViewById(R.id.btn_change_address);

        setText(R.id.tv_fee_home, Money.format(Pricing.HOME_DELIVERY_FEE));

        MaterialCardView cardHome = findViewById(R.id.card_method_home);
        MaterialCardView cardPickup = findViewById(R.id.card_method_pickup);
        UiUtils.bindSingleChoice(new MaterialCardView[]{cardHome, cardPickup}, homeDelivery ? 0 : 1, index -> {
            homeDelivery = index == 0;
            renderMethod();
        });

        User user = users.getById(session.getUserId());
        if (user != null) {
            address = user.getAddress() == null ? "" : user.getAddress();
            tvPhone.setText(user.getPhone());
        }
        renderAddress();
        renderDate();
        renderMethod();

        btnChangeAddress.setOnClickListener(v -> showAddressDialog());
        findViewById(R.id.card_delivery_date).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btn_continue_payment).setOnClickListener(v -> proceed());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_HOME, homeDelivery);
        outState.putString(STATE_DATE, dateIso);
    }

    private void renderMethod() {
        addressSection.setVisibility(homeDelivery ? View.VISIBLE : View.GONE);
        tvDateLabel.setText(homeDelivery ? R.string.delivery_date_label : R.string.pickup_date_label);

        Pricing.Quote q = Pricing.quote(items, promoCode, homeDelivery);
        if (!homeDelivery) tvFee.setText(R.string.free);
        else tvFee.setText(q.deliveryFee <= 0 ? getString(R.string.free) : Money.format(q.deliveryFee));
        tvTotal.setText(Money.format(q.total));
    }

    private void renderAddress() {
        if (address.trim().isEmpty()) {
            tvAddress.setText(R.string.no_address_saved);
            ((TextView) btnChangeAddress).setText(R.string.action_add);
        } else {
            tvAddress.setText(address);
            ((TextView) btnChangeAddress).setText(R.string.action_change);
        }
    }

    private void renderDate() {
        if (dateIso == null) {
            tvDate.setText(R.string.select_date);
            return;
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
            tvDate.setText(out.format(in.parse(dateIso)));
        } catch (Exception e) {
            tvDate.setText(dateIso);
        }
    }

    private void showAddressDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address, null);
        final TextInputLayout til = view.findViewById(R.id.til_dialog_address);
        final TextInputEditText et = view.findViewById(R.id.et_dialog_address);
        et.setText(address);
        if (et.getText() != null) et.setSelection(et.getText().length());

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delivery_address)
                .setView(view)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = et.getText() == null ? "" : et.getText().toString().trim();
                    if (value.length() < 10) {
                        til.setError(getString(R.string.error_address_short));
                        return;
                    }
                    address = value;
                    users.updateAddress(session.getUserId(), value);
                    renderAddress();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now()).build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(homeDelivery ? R.string.delivery_date_label : R.string.pickup_date_label)
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC")); // the picker returns UTC midnight
            dateIso = fmt.format(selection);
            renderDate();
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void proceed() {
        if (homeDelivery && address.trim().length() < 10) {
            UiUtils.snack(this, R.string.error_address_required);
            return;
        }
        if (dateIso == null) {
            UiUtils.snack(this, R.string.error_date_required);
            return;
        }
        Intent intent = new Intent(this, CheckoutPaymentActivity.class);
        intent.putExtra(EXTRA_PROMO_CODE, promoCode);
        intent.putExtra(EXTRA_METHOD, homeDelivery ? OrderEntry.DELIVERY_HOME : OrderEntry.DELIVERY_PICKUP);
        intent.putExtra(EXTRA_ADDRESS, homeDelivery ? address.trim() : getString(R.string.store_pickup_address));
        intent.putExtra(EXTRA_DATE, dateIso);
        startActivity(intent);
    }
}
