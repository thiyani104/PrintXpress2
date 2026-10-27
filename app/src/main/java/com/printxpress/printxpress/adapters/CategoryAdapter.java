package com.printxpress.printxpress.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.activities.CategoryActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<String> names = new ArrayList<>();
    private final List<Integer> covers = new ArrayList<>();

    public CategoryAdapter(Map<String, Integer> categories) {
        for (Map.Entry<String, Integer> e : categories.entrySet()) {
            names.add(e.getKey());
            covers.add(e.getValue());
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        final String name = names.get(position);
        holder.label.setText(name);
        holder.image.setImageResource(covers.get(position));
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME, name);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView label;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_category_image);
            label = itemView.findViewById(R.id.tv_category_label);
        }
    }
}
