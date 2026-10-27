package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.adapters.CartAdapter;
import com.printxpress.printxpress.database.CartRepository;
import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.Pricing;
import com.printxpress.printxpress.utils.UiUtils;

import java.util.List;
import java.util.Locale;

public class CartActivity extends BaseActivity implements CartAdapter.Listener {

    private static final String STATE_PROMO = "applied_promo";

    private CartRepository cart;
    private CartAdapter adapter;
    private String appliedPromo;

    private View contentGroup;
    private View emptyState;
    private View bottomBar;
    private TextInputLayout tilPromo;
    private TextInputEditText etPromo;
    private View promoInputRow;
    private Chip chipPromo;
    private TextView tvSubtotal, tvDiscount, tvDiscountLabel, tvBarTotal;
    private MaterialButton btnCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        setupToolbar(R.string.cart_title);

        cart = new CartRepository(this, session.getUserId());
        if (savedInstanceState != null) appliedPromo = savedInstanceState.getString(STATE_PROMO);

        contentGroup = findViewById(R.id.cart_content);
        emptyState = findViewById(R.id.empty_state);
        bottomBar = findViewById(R.id.bottom_bar);
        tilPromo = findViewById(R.id.til_promo);
        etPromo = findViewById(R.id.et_promo);
        promoInputRow = findViewById(R.id.promo_input_row);
        chipPromo = findViewById(R.id.chip_promo_applied);
        tvSubtotal = findViewById(R.id.tv_summary_subtotal);
        tvDiscount = findViewById(R.id.tv_summary_discount);
        tvDiscountLabel = findViewById(R.id.tv_summary_discount_label);
        tvBarTotal = findViewById(R.id.tv_checkout_bar_total);
        btnCheckout = findViewById(R.id.btn_place_order_submit);

        RecyclerView rv = findViewById(R.id.rv_cart_items);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        adapter = new CartAdapter(this);
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);

        findViewById(R.id.btn_apply_promo).setOnClickListener(v -> applyPromo());
        chipPromo.setOnCloseIconClickListener(v -> {
            appliedPromo = null;
            refresh();
        });
        findViewById(R.id.btn_browse_products).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(this, CheckoutDeliveryActivity.class);
            intent.putExtra(CheckoutDeliveryActivity.EXTRA_PROMO_CODE, appliedPromo);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PROMO, appliedPromo);
    }

    private void applyPromo() {
        String code = etPromo.getText() == null ? "" : etPromo.getText().toString().trim();
        tilPromo.setError(null);
        if (code.isEmpty()) {
            tilPromo.setError(getString(R.string.error_promo_empty));
            return;
        }
        UiUtils.hideKeyboard(this);
        Pricing.Quote q = Pricing.quote(cart.getItems(), code, false);
        if (q.promoError != Pricing.OK) {
            tilPromo.setError(promoMessage(q));
            return;
        }
        appliedPromo = q.appliedCode;
        etPromo.setText("");
        refresh();
    }

    private String promoMessage(Pricing.Quote q) {
        switch (q.promoError) {
            case Pricing.ERR_EXPIRED: return getString(R.string.error_promo_expired);
            case Pricing.ERR_NOT_APPLICABLE: return getString(R.string.error_promo_not_applicable);
            case Pricing.ERR_MIN_SUBTOTAL:
                return getString(R.string.error_promo_min_subtotal, Money.format(q.minSubtotal));
            default: return getString(R.string.error_promo_invalid);
        }
    }

    private void refresh() {
        List<CartItem> items = cart.getItems();
        adapter.submitList(items);

        boolean empty = items.isEmpty();
        contentGroup.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        bottomBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            appliedPromo = null;
            return;
        }

        Pricing.Quote q = Pricing.quote(items, appliedPromo, false);
        if (appliedPromo != null && q.promoError != Pricing.OK) {
            // The cart changed so the code no longer qualifies.
            UiUtils.snack(this, getString(R.string.promo_removed, appliedPromo));
            appliedPromo = null;
            q = Pricing.quote(items, null, false);
        }

        tvSubtotal.setText(Money.format(q.subtotal));
        boolean hasDiscount = q.discount > 0;
        tvDiscount.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
        tvDiscountLabel.setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
        if (hasDiscount) {
            tvDiscount.setText(String.format(Locale.US, "- %s", Money.format(q.discount)));
        }

        boolean promoOn = appliedPromo != null;
        chipPromo.setVisibility(promoOn ? View.VISIBLE : View.GONE);
        promoInputRow.setVisibility(promoOn ? View.GONE : View.VISIBLE);
        if (promoOn) {
            chipPromo.setText(q.freeDeliveryApplied || q.discount <= 0
                    ? getString(R.string.promo_applied_free_delivery, appliedPromo)
                    : getString(R.string.promo_applied, appliedPromo, Money.format(q.discount)));
        }

        double barTotal = q.subtotal - q.discount;
        tvBarTotal.setText(Money.format(barTotal));
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        cart.setQuantity(item.getId(), newQuantity);
        refresh();
    }

    @Override
    public void onRemove(final CartItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_item_title)
                .setMessage(getString(R.string.remove_item_message, item.getProductName()))
                .setPositiveButton(R.string.action_remove, (d, w) -> {
                    cart.remove(item.getId());
                    refresh();
                    Snackbar.make(findViewById(android.R.id.content), R.string.item_removed, Snackbar.LENGTH_SHORT)
                            .setAnchorView(bottomBar.getVisibility() == View.VISIBLE ? bottomBar : null)
                            .show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
