package com.printxpress.printxpress.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.activities.OrderDetailsActivity;
import com.printxpress.printxpress.models.Order;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.StatusStyler;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.OrderViewHolder> {

    private static final DiffUtil.ItemCallback<Order> DIFF = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order a, @NonNull Order b) {
            return a.getId() == b.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order a, @NonNull Order b) {
            return a.getStatus().equals(b.getStatus()) && a.getTotal() == b.getTotal();
        }
    };

    public OrderAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false);
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder h, int position) {
        final Order order = getItem(position);
        Context ctx = h.itemView.getContext();

        h.id.setText(order.getDisplayId());
        h.date.setText(order.getFormattedDate());
        StatusStyler.apply(h.status, order.getStatus());

        String name = order.getFirstItemName();
        if (order.getItemCount() > 1) {
            name = ctx.getString(R.string.order_item_and_more, name, order.getItemCount() - 1);
        }
        h.item.setText(name);
        h.qty.setText(ctx.getString(R.string.order_qty_total, order.getTotalQuantity()));
        h.total.setText(Money.format(order.getTotal()));

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), OrderDetailsActivity.class);
            intent.putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, order.getId());
            v.getContext().startActivity(intent);
        });
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        final TextView id, date, status, item, qty, total;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.tv_card_order_id);
            date = itemView.findViewById(R.id.tv_card_order_date);
            status = itemView.findViewById(R.id.tv_card_status_badge);
            item = itemView.findViewById(R.id.tv_card_item_name);
            qty = itemView.findViewById(R.id.tv_card_item_qty);
            total = itemView.findViewById(R.id.tv_card_absolute_total);
        }
    }
}
