package com.printxpress.printxpress.adapters;

import android.content.Intent;
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
import com.printxpress.printxpress.activities.ProductDetailsActivity;
import com.printxpress.printxpress.models.Product;
import com.printxpress.printxpress.utils.Money;

import java.util.Locale;

/** Used for both the vertical product list and the compact horizontal "popular" strip. */
public class ProductAdapter extends ListAdapter<Product, ProductAdapter.ProductViewHolder> {

    private static final DiffUtil.ItemCallback<Product> DIFF = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product a, @NonNull Product b) {
            return a.getId().equals(b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product a, @NonNull Product b) {
            return a.getId().equals(b.getId());
        }
    };

    private final boolean compact;

    public ProductAdapter(boolean compact) {
        super(DIFF);
        this.compact = compact;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = compact ? R.layout.item_product_compact : R.layout.item_product_card;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = getItem(position);
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(Money.format(product.getPrice()));
        holder.tvRating.setText(String.format(Locale.US, "%.1f", product.getRating()));
        holder.ivImage.setImageResource(product.getImageResource());
        if (holder.tvSpecs != null) holder.tvSpecs.setText(product.getSpecDetails());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductDetailsActivity.class);
            intent.putExtra(ProductDetailsActivity.EXTRA_PRODUCT_ID, product.getId());
            v.getContext().startActivity(intent);
        });
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvSpecs;
        final TextView tvPrice;
        final TextView tvRating;
        final ImageView ivImage;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_title);
            tvSpecs = itemView.findViewById(R.id.tv_item_specs);
            tvPrice = itemView.findViewById(R.id.tv_item_price);
            tvRating = itemView.findViewById(R.id.tv_item_rating);
            ivImage = itemView.findViewById(R.id.iv_item_image);
        }
    }
}
