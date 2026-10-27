package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.adapters.CategoryAdapter;
import com.printxpress.printxpress.adapters.ProductAdapter;
import com.printxpress.printxpress.backend.OrderSyncManager;
import com.printxpress.printxpress.database.CartRepository;
import com.printxpress.printxpress.models.ProductCatalog;
import com.printxpress.printxpress.utils.BottomNavHelper;
import com.printxpress.printxpress.utils.UiUtils;

public class MainActivity extends BaseActivity {

    private TextView tvGreetingName;
    private TextView tvCartBadge;
    private TextInputEditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!session.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        BottomNavHelper.attach(this, R.id.nav_home);

        tvGreetingName = findViewById(R.id.tv_dashboard_welcome);
        tvCartBadge = findViewById(R.id.tv_cart_badge);
        etSearch = findViewById(R.id.et_search_bar);

        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 3));
        rvCategories.setAdapter(new CategoryAdapter(ProductCatalog.getCategoryCovers()));

        RecyclerView rvPopular = findViewById(R.id.rv_popular);
        rvPopular.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ProductAdapter popularAdapter = new ProductAdapter(true);
        rvPopular.setAdapter(popularAdapter);
        popularAdapter.submitList(ProductCatalog.getTopRated(6));

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_home_cart).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        findViewById(R.id.layout_promo_banner).setOnClickListener(v ->
                startActivity(new Intent(this, PromotionsActivity.class)));
        findViewById(R.id.btn_promo_order).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryActivity.class);
            intent.putExtra(CategoryActivity.EXTRA_CATEGORY_NAME, "Business Cards");
            startActivity(intent);
        });

        findViewById(R.id.btn_support_contact).setOnClickListener(v ->
                startActivity(new Intent(this, ContactSupportActivity.class)));
        findViewById(R.id.btn_support_guidelines).setOnClickListener(v ->
                startActivity(new Intent(this, DesignGuidelinesActivity.class)));
        findViewById(R.id.btn_support_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutUsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!session.isLoggedIn() || tvGreetingName == null) return;
        String name = session.getUserName().trim();
        int space = name.indexOf(' ');
        String first = space > 0 ? name.substring(0, space) : name;
        tvGreetingName.setText(first.isEmpty() ? getString(R.string.greeting_guest) : first);

        OrderSyncManager.syncAsync(this, session.getUserId(), false, null);

        int count = new CartRepository(this, session.getUserId()).getLineCount();
        tvCartBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        tvCartBadge.setText(String.valueOf(count));
    }

    private void runSearch() {
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        if (query.isEmpty()) return;
        UiUtils.hideKeyboard(this);
        Intent intent = new Intent(this, CategoryActivity.class);
        intent.putExtra(CategoryActivity.EXTRA_SEARCH_QUERY, query);
        startActivity(intent);
    }
}
