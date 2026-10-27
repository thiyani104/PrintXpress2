package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.TaskStackBuilder;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.OrderRepository;
import com.printxpress.printxpress.models.Order;
import com.printxpress.printxpress.utils.Money;

public class OrderSuccessActivity extends BaseActivity {

    public static final String EXTRA_ORDER_ID = "ORDER_ID";

    private long orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        Order order = new OrderRepository(this).getOrder(orderId, session.getUserId());
        if (order != null) {
            setText(R.id.tv_success_order_id, order.getDisplayId());
            setText(R.id.tv_success_total, Money.format(order.getTotal()));
            setText(R.id.tv_success_date, order.getFormattedDeliveryDate());
            setText(R.id.tv_success_method, order.getDeliveryMethod());
        }

        findViewById(R.id.btn_track_order).setOnClickListener(v -> {
            Intent details = new Intent(this, OrderDetailsActivity.class);
            details.putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, orderId);
            TaskStackBuilder.create(this)
                    .addNextIntent(new Intent(this, MainActivity.class))
                    .addNextIntent(details)
                    .startActivities();
            finish();
        });
        findViewById(R.id.btn_continue_shopping).setOnClickListener(v -> goHome());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
