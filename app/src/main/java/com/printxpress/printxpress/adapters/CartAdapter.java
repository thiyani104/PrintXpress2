package com.printxpress.printxpress.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.models.Product;
import com.printxpress.printxpress.models.ProductCatalog;
import com.printxpress.printxpress.utils.FileUtils;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.UiUtils;

public class CartAdapter extends ListAdapter<CartItem, CartAdapter.CartViewHolder> {

    public interface Listener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemove(CartItem item);
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF = new DiffUtil.ItemCallback<CartItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
            return a.getId() == b.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
            return a.getQuantity() == b.getQuantity() && a.getUnitPrice() == b.getUnitPrice();
        }
    };

    private final Listener listener;

    public CartAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder h, int position) {
        final CartItem item = getItem(position);
        Product product = ProductCatalog.findById(item.getProductId());
        if (product != null) h.image.setImageResource(product.getImageResource());

        h.name.setText(item.getProductName());
        h.spec.setText(item.getSpec());
        h.file.setText(FileUtils.displayName(item.getFilePath()));
        h.unitPrice.setText(h.itemView.getContext().getString(R.string.price_each, Money.format(item.getUnitPrice())));
        h.quantity.setText(String.valueOf(item.getQuantity()));
        h.lineTotal.setText(Money.format(item.getLineTotal()));

        h.minus.setEnabled(item.getQuantity() > 1);
        h.minus.setOnClickListener(v -> listener.onQuantityChanged(item, item.getQuantity() - 1));
        h.plus.setOnClickListener(v -> listener.onQuantityChanged(item, item.getQuantity() + 1));
        h.quantity.setOnClickListener(v -> UiUtils.promptQuantity(v.getContext(), item.getQuantity(),
                value -> listener.onQuantityChanged(item, value)));
        h.remove.setOnClickListener(v -> listener.onRemove(item));
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name, spec, file, unitPrice, quantity, lineTotal;
        final View minus, plus, remove;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_cart_image);
            name = itemView.findViewById(R.id.tv_cart_name);
            spec = itemView.findViewById(R.id.tv_cart_spec);
            file = itemView.findViewById(R.id.tv_cart_file);
            unitPrice = itemView.findViewById(R.id.tv_cart_unit_price);
            quantity = itemView.findViewById(R.id.tv_cart_quantity);
            lineTotal = itemView.findViewById(R.id.tv_cart_line_total);
            minus = itemView.findViewById(R.id.btn_cart_minus);
            plus = itemView.findViewById(R.id.btn_cart_plus);
            remove = itemView.findViewById(R.id.btn_cart_remove);
        }
    }
}
