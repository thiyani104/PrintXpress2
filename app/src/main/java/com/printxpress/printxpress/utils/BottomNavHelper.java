package com.printxpress.printxpress.utils;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.activities.MainActivity;
import com.printxpress.printxpress.activities.OrdersActivity;
import com.printxpress.printxpress.activities.ProfileActivity;
import com.printxpress.printxpress.activities.PromotionsActivity;

/** Wires the shared bottom navigation bar used by the four top-level screens. */
public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void attach(Activity activity, int selectedItemId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        if (nav == null) return;
        nav.setSelectedItemId(selectedItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) return true;

            Class<?> target;
            if (id == R.id.nav_home) target = MainActivity.class;
            else if (id == R.id.nav_orders) target = OrdersActivity.class;
            else if (id == R.id.nav_offers) target = PromotionsActivity.class;
            else target = ProfileActivity.class;

            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
            // Home stays at the bottom of the stack; other tabs replace each other.
            if (!(activity instanceof MainActivity)) activity.finish();
            return true;
        });
    }
}
