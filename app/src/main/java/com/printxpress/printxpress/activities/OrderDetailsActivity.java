package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.backend.OrderSyncManager;
import com.printxpress.printxpress.database.CartRepository;
import com.printxpress.printxpress.database.OrderRepository;
import com.printxpress.printxpress.models.Order;
import com.printxpress.printxpress.models.OrderItem;
import com.printxpress.printxpress.models.Product;
import com.printxpress.printxpress.models.ProductCatalog;
import com.printxpress.printxpress.utils.FileUtils;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.StatusStyler;
import com.printxpress.printxpress.utils.UiUtils;

import java.util.Locale;

public class OrderDetailsActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "ORDER_ID";

    private OrderRepository repository;
    private long orderId;
    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        setupToolbar(R.string.order_details_title);

        repository = new OrderRepository(this);
        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);

        findViewById(R.id.btn_cancel_order).setOnClickListener(v -> showCancelDialog());
        findViewById(R.id.btn_reorder).setOnClickListener(v -> reorder());
        findViewById(R.id.btn_retry_sync).setOnClickListener(v ->
                OrderSyncManager.syncAsync(this, session.getUserId(), true, this::refreshAfterSync));

        View debug = findViewById(R.id.btn_debug_advance);
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        debug.setVisibility(debuggable ? View.VISIBLE : View.GONE);
        debug.setOnClickListener(v -> {
            if (order != null) {
                repository.advanceStatus(orderId, session.getUserId(), order.getStatus());
                load();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
        OrderSyncManager.syncAsync(this, session.getUserId(), false, this::refreshAfterSync);
    }

    private void refreshAfterSync() {
        if (!isFinishing() && !isDestroyed()) load();
    }

    private void load() {
        order = repository.getOrder(orderId, session.getUserId());
        if (order == null) {
            finish();
            return;
        }

        setText(R.id.tv_detail_order_id, order.getDisplayId());
        setText(R.id.tv_detail_date, order.getFormattedDate());
        StatusStyler.apply((TextView) findViewById(R.id.tv_detail_status_badge), order.getStatus());

        renderTimeline();
        renderItems();

        setText(R.id.tv_detail_method, order.getDeliveryMethod());
        setText(R.id.tv_detail_address, order.getAddress());
        setText(R.id.tv_detail_delivery_date, order.getFormattedDeliveryDate());
        setText(R.id.tv_detail_payment, order.getPaymentMethod());

        setText(R.id.tv_detail_subtotal, Money.format(order.getSubtotal()));
        boolean hasDiscount = order.getDiscount() > 0;
        findViewById(R.id.row_detail_discount).setVisibility(hasDiscount ? View.VISIBLE : View.GONE);
        if (hasDiscount) {
            String code = order.getPromoCode() == null ? "" : order.getPromoCode();
            setText(R.id.tv_detail_discount_label, getString(R.string.discount_with_code, code));
            setText(R.id.tv_detail_discount, String.format(Locale.US, "- %s", Money.format(order.getDiscount())));
        }
        setText(R.id.tv_detail_fee, order.getDeliveryFee() <= 0
                ? getString(R.string.free) : Money.format(order.getDeliveryFee()));
        setText(R.id.tv_detail_total, Money.format(order.getTotal()));

        findViewById(R.id.btn_cancel_order).setVisibility(order.isPending() ? View.VISIBLE : View.GONE);
        renderSyncState();
    }

    /** Shows whether the print shop has received this order (only when a backend is configured). */
    private void renderSyncState() {
        TextView sync = findViewById(R.id.tv_detail_sync);
        View retry = findViewById(R.id.btn_retry_sync);
        boolean show = OrderSyncManager.isConfigured(this) && order.getRemoteId() != null && !order.isCancelled();
        if (!show) {
            sync.setVisibility(View.GONE);
            retry.setVisibility(View.GONE);
            return;
        }
        sync.setVisibility(View.VISIBLE);
        sync.setText(order.isSynced() ? R.string.sync_received : R.string.sync_waiting);
        retry.setVisibility(order.isSynced() ? View.GONE : View.VISIBLE);
    }

    private void renderTimeline() {
        View timeline = findViewById(R.id.card_timeline);
        View cancelled = findViewById(R.id.card_cancelled);
        if (order.isCancelled()) {
            timeline.setVisibility(View.GONE);
            cancelled.setVisibility(View.VISIBLE);
            String reason = order.getCancelReason();
            setText(R.id.tv_cancel_reason, reason == null || reason.isEmpty()
                    ? getString(R.string.cancelled_no_reason)
                    : getString(R.string.cancelled_reason, reason));
            return;
        }
        timeline.setVisibility(View.VISIBLE);
        cancelled.setVisibility(View.GONE);

        int step = order.getProgressStep();
        LinearProgressIndicator progress = findViewById(R.id.progress_order);
        progress.setProgressCompat((step + 1) * 25, true);

        int[] ids = {R.id.tv_step_1, R.id.tv_step_2, R.id.tv_step_3, R.id.tv_step_4};
        for (int i = 0; i < ids.length; i++) {
            TextView tv = findViewById(ids[i]);
            boolean reached = i <= step;
            tv.setTextColor(ContextCompat.getColor(this, reached ? R.color.textPrimary : R.color.textSecondary));
            tv.setAlpha(reached ? 1f : 0.7f);
        }
    }

    private void renderItems() {
        LinearLayout container = findViewById(R.id.ll_items);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (OrderItem item : order.getItems()) {
            View row = inflater.inflate(R.layout.item_order_line, container, false);
            ((TextView) row.findViewById(R.id.tv_line_name)).setText(item.getProductName());
            ((TextView) row.findViewById(R.id.tv_line_spec)).setText(getString(R.string.line_spec,
                    item.getSpec() == null ? "" : item.getSpec(), item.getQuantity(), Money.format(item.getUnitPrice())));
            TextView file = row.findViewById(R.id.tv_line_file);
            String fileName = FileUtils.displayName(item.getFilePath());
            file.setVisibility(fileName.isEmpty() ? View.GONE : View.VISIBLE);
            file.setText(fileName);
            ((TextView) row.findViewById(R.id.tv_line_total)).setText(Money.format(item.getLineTotal()));
            container.addView(row);
        }
    }

    private void showCancelDialog() {
        final String[] reasons = getResources().getStringArray(R.array.cancel_reasons);
        final int[] selected = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancel_order_title)
                .setSingleChoiceItems(reasons, 0, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.cancel_order_confirm, (d, w) -> cancelOrder(reasons[selected[0]]))
                .setNegativeButton(R.string.cancel_order_keep, null)
                .show();
    }

    private void cancelOrder(final String reason) {
        final boolean sent = OrderSyncManager.isConfigured(this) && order.isSynced() && order.getRemoteId() != null;
        if (!sent) {
            // Never reached the shop (or offline-only mode): a local cancel is enough.
            boolean ok = repository.cancel(orderId, session.getUserId(), reason);
            if (ok) UiUtils.snack(this, R.string.order_cancelled);
            else UiUtils.snack(this, R.string.error_cancel_failed);
            load();
            return;
        }

        // The shop already has it, so the shop decides whether it can still be cancelled.
        final String remoteId = order.getRemoteId();
        final android.content.Context app = getApplicationContext();
        Async.io(() -> {
            final OrderSyncManager.CancelResult result = OrderSyncManager.cancelRemote(app, remoteId, reason);
            Async.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                switch (result) {
                    case OK:
                        repository.cancel(orderId, session.getUserId(), reason);
                        UiUtils.snack(this, R.string.order_cancelled);
                        break;
                    case NOT_CANCELLABLE:
                        UiUtils.snack(this, R.string.error_cancel_shop_started);
                        OrderSyncManager.syncAsync(this, session.getUserId(), true, this::refreshAfterSync);
                        break;
                    case OFFLINE:
                        UiUtils.snack(this, R.string.error_cancel_offline);
                        break;
                    default:
                        UiUtils.snack(this, R.string.error_generic);
                        break;
                }
                load();
            });
        });
    }

    private void reorder() {
        CartRepository cart = new CartRepository(this, session.getUserId());
        int added = 0;
        int skipped = 0;
        for (OrderItem item : order.getItems()) {
            Product p = ProductCatalog.findById(item.getProductId());
            if (p != null && FileUtils.exists(item.getFilePath())) {
                cart.addItem(p, item.getQuantity(), item.getFilePath(), item.getNotes());
                added++;
            } else {
                skipped++;
            }
        }
        if (added == 0) {
            UiUtils.snack(this, R.string.reorder_none);
            return;
        }
        if (skipped > 0) {
            android.widget.Toast.makeText(this, R.string.reorder_partial, android.widget.Toast.LENGTH_LONG).show();
        }
        startActivity(new Intent(this, CartActivity.class));
    }
}
