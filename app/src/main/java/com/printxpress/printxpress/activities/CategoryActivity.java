package com.printxpress.printxpress.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.adapters.ProductAdapter;
import com.printxpress.printxpress.models.Product;
import com.printxpress.printxpress.models.ProductCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shows one category, or search results when started with a search query. */
public class CategoryActivity extends BaseActivity {

    public static final String EXTRA_CATEGORY_NAME = "CATEGORY_NAME";
    public static final String EXTRA_SEARCH_QUERY = "SEARCH_QUERY";

    private static final int SORT_RECOMMENDED = 0;
    private static final int SORT_PRICE_LOW = 1;
    private static final int SORT_PRICE_HIGH = 2;
    private static final int SORT_RATING = 3;

    private ProductAdapter adapter;
    private View emptyState;
    private TextView tvCount;
    private List<Product> master = new ArrayList<>();
    private String selectedTag = null;
    private int sortMode = SORT_RECOMMENDED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        String category = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String query = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);

        View chipScroll = findViewById(R.id.scroll_filter_chips);
        ChipGroup chipGroup = findViewById(R.id.container_filter_chips);
        emptyState = findViewById(R.id.empty_state);
        tvCount = findViewById(R.id.tv_result_count);

        if (query != null) {
            setupToolbar(getString(R.string.search_results_title, query), true);
            master = ProductCatalog.search(query);
            chipScroll.setVisibility(View.GONE);
        } else {
            String name = category == null ? "Business Cards" : category;
            setupToolbar(name, true);
            master = ProductCatalog.getByCategory(name);
            buildChips(chipGroup);
        }

        RecyclerView rv = findViewById(R.id.rv_products_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(false);
        rv.setAdapter(adapter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_category);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sort) {
                showSortDialog();
                return true;
            }
            return false;
        });

        refresh();
    }

    private void buildChips(ChipGroup group) {
        group.removeAllViews();
        List<String> tags = new ArrayList<>();
        for (Product p : master) {
            if (p.getSubTag() != null && !tags.contains(p.getSubTag())) tags.add(p.getSubTag());
        }
        if (tags.size() < 2) {
            findViewById(R.id.scroll_filter_chips).setVisibility(View.GONE);
            return;
        }
        addChip(group, getString(R.string.filter_all), null, true);
        for (String tag : tags) addChip(group, tag, tag, false);

        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = g.findViewById(checkedIds.get(0));
            selectedTag = chip == null ? null : (String) chip.getTag();
            refresh();
        });
    }

    private void addChip(ChipGroup group, String label, String tag, boolean checked) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(tag);
        chip.setChecked(checked);
        group.addView(chip);
    }

    private void showSortDialog() {
        String[] labels = {
                getString(R.string.sort_recommended), getString(R.string.sort_price_low),
                getString(R.string.sort_price_high), getString(R.string.sort_rating)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort)
                .setSingleChoiceItems(labels, sortMode, (dialog, which) -> {
                    sortMode = which;
                    refresh();
                    dialog.dismiss();
                })
                .show();
    }

    private void refresh() {
        List<Product> out = new ArrayList<>();
        for (Product p : master) {
            if (selectedTag == null || selectedTag.equalsIgnoreCase(p.getSubTag())) out.add(p);
        }
        switch (sortMode) {
            case SORT_PRICE_LOW:
                Collections.sort(out, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                break;
            case SORT_PRICE_HIGH:
                Collections.sort(out, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            case SORT_RATING:
                Collections.sort(out, (a, b) -> Double.compare(b.getRating(), a.getRating()));
                break;
            default:
                break;
        }
        adapter.submitList(out);
        emptyState.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        tvCount.setText(getResources().getQuantityString(R.plurals.product_count, out.size(), out.size()));
    }
}
