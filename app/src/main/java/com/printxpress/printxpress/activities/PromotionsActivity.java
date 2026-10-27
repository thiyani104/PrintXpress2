package com.printxpress.printxpress.activities;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.adapters.OfferAdapter;
import com.printxpress.printxpress.models.OfferCatalog;
import com.printxpress.printxpress.utils.BottomNavHelper;
import com.printxpress.printxpress.utils.UiUtils;

public class PromotionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotions);
        setupToolbar(getString(R.string.offers_title), false);
        BottomNavHelper.attach(this, R.id.nav_offers);

        RecyclerView rv = findViewById(R.id.rv_offers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new OfferAdapter(OfferCatalog.getAll(), offer -> {
            UiUtils.copyToClipboard(this, "Promo code", offer.getCode());
            UiUtils.snack(this, getString(R.string.code_copied, offer.getCode()));
        }));
    }
}
