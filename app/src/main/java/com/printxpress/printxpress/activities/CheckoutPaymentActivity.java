package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.backend.OrderSyncManager;
import com.printxpress.printxpress.database.CartRepository;
import com.printxpress.printxpress.database.DBContract.OrderEntry;
import com.printxpress.printxpress.database.OrderRepository;
import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.Pricing;
import com.printxpress.printxpress.utils.UiUtils;

import java.util.List;
import java.util.Locale;

public class CheckoutPaymentActivity extends BaseActivity {

    private static final String[] METHODS = {
            OrderEntry.PAYMENT_COD, OrderEntry.PAYMENT_CARD, OrderEntry.PAYMENT_BANK
    };
    private static final String STATE_PAYMENT = "payment_index";

    private List<CartItem> items;
    private Pricing.Quote quote;
    private String promoCode, deliveryMethod, address, dateIso;
    private int paymentIndex = 0;
    private MaterialButton btnPlace;
    private View bankNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_payment);
        setupToolbar(R.string.payment_title);

        Intent in = getIntent();
        promoCode = in.getStringExtra(CheckoutDeliveryActivity.EXTRA_PROMO_CODE);
        deliveryMethod = in.getStringExtra(CheckoutDeliveryActivity.EXTRA_METHOD);
        address = in.getStringExtra(CheckoutDeliveryActivity.EXTRA_ADDRESS);
        dateIso = in.getStringExtra(CheckoutDeliveryActivity.EXTRA_DATE);
        if (savedInstanceState != null) paymentIndex = savedInstanceState.getInt(STATE_PAYMENT, 0);

        items = new CartRepository(this, session.getUserId()).getItems();
        if (items.isEmpty() || deliveryMethod == null) {
            finish();
            return;
        }
        quote = Pricing.quote(items, promoCode, OrderEntry.DELIVERY_HOME.equals(deliveryMethod));

        btnPlace = findViewById(R.id.btn_complete_order_submit);
        bankNote = findViewById(R.id.tv_bank_note);

        MaterialCardView[] cards = {
                findViewById(R.id.card_pay_cod),
                findViewById(R.id.card_pay_card),
                findViewById(R.id.card_pay_bank)
        };
        UiUtils.bindSingleChoice(cards, paymentIndex, index -> {
            paymentIndex = index;
            renderNote();
        });
        renderNote();
        renderSummary();

        btnPlace.setOnClickListener(v -> placeOrder());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAYMENT, paymentIndex);
    }

    private void renderNote() {
        bankNote.setVisibility(paymentIndex == 2 ? View.VISIBLE : View.GONE);
    }

    private void renderSummary() {
        int units = 0;
        for (CartItem item : items) units += item.getQuantity();
        setText(R.id.tv_pay_items,
                getResources().getQuantityString(R.plurals.summary_items, items.size(), items.size(), units));
        setText(R.id.tv_pay_subtotal, Money.format(quote.subtotal));

        boolean hasDiscount = quote.discount > 0;
        findViewById(R.id.row_pay_discount).setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
        if (hasDiscount) {
            setText(R.id.tv_pay_discount, String.format(Locale.US, "- %s", Money.format(quote.discount)));
        }

        TextView fee = findViewById(R.id.tv_pay_delivery);
        fee.setText(quote.deliveryFee <= 0 ? getString(R.string.free) : Money.format(quote.deliveryFee));
        setText(R.id.tv_pay_total, Money.format(quote.total));
        setText(R.id.tv_pay_bar_total, Money.format(quote.total));
        setText(R.id.tv_pay_method_line, deliveryMethod);
    }

    private void placeOrder() {
        btnPlace.setEnabled(false);
        btnPlace.setText(R.string.placing_order);
        final long userId = session.getUserId();
        final String payment = METHODS[paymentIndex];

        Async.io(() -> {
            long orderId = new OrderRepository(this).placeOrder(
                    userId, items, quote, deliveryMethod, address, dateIso, payment);
            Async.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (orderId == -1) {
                    btnPlace.setEnabled(true);
                    btnPlace.setText(R.string.place_order);
                    UiUtils.snack(this, R.string.error_order_failed);
                    return;
                }
                // Send the order to the print shop in the background; retried automatically if offline.
                OrderSyncManager.syncAsync(getApplicationContext(), userId, true, null);
                Intent intent = new Intent(this, OrderSuccessActivity.class);
                intent.putExtra(OrderSuccessActivity.EXTRA_ORDER_ID, orderId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
