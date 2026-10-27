package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.adapters.OrderAdapter;
import com.printxpress.printxpress.backend.OrderSyncManager;
import com.printxpress.printxpress.database.DBContract.OrderEntry;
import com.printxpress.printxpress.database.OrderRepository;
import com.printxpress.printxpress.models.Order;
import com.printxpress.printxpress.utils.BottomNavHelper;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends BaseActivity {

    private OrderAdapter adapter;
    private OrderRepository repository;
    private ChipGroup chipGroup;
    private View emptyState;
    private TextView tvEmptyTitle, tvEmptyBody;
    private View btnStartShopping;
    private List<Order> all = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        setupToolbar(getString(R.string.orders_title), false);
        BottomNavHelper.attach(this, R.id.nav_orders);

        repository = new OrderRepository(this);
        chipGroup = findViewById(R.id.chip_group_orders);
        emptyState = findViewById(R.id.empty_state);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyBody = findViewById(R.id.tv_empty_body);
        btnStartShopping = findViewById(R.id.btn_start_shopping);

        RecyclerView rv = findViewById(R.id.rv_orders);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter();
        rv.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((g, ids) -> applyFilter());
        btnStartShopping.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        all = repository.getOrders(session.getUserId());
        applyFilter();

        // Ask the print shop for status updates, then refresh the list.
        OrderSyncManager.syncAsync(this, session.getUserId(), false, () -> {
            if (isFinishing() || isDestroyed()) return;
            all = repository.getOrders(session.getUserId());
            applyFilter();
        });
    }

    private void applyFilter() {
        int checked = chipGroup.getCheckedChipId();
        List<Order> out = new ArrayList<>();
        for (Order o : all) {
            boolean keep;
            if (checked == R.id.chip_filter_active) keep = o.isActive();
            else if (checked == R.id.chip_filter_completed) keep = OrderEntry.STATUS_COMPLETED.equals(o.getStatus());
            else if (checked == R.id.chip_filter_cancelled) keep = o.isCancelled();
            else keep = true;
            if (keep) out.add(o);
        }
        adapter.submitList(out);

        boolean empty = out.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean never = all.isEmpty();
            tvEmptyTitle.setText(never ? R.string.orders_empty_title : R.string.orders_filter_empty_title);
            tvEmptyBody.setText(never ? R.string.orders_empty_body : R.string.orders_filter_empty_body);
            btnStartShopping.setVisibility(never ? View.VISIBLE : View.GONE);
        }
    }
}
