package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.printxpress.printxpress.R;

public class OnboardingActivity extends BaseActivity {

    private static final int[] IMAGES = {
            R.drawable.img_onboarding_1, R.drawable.img_onboarding_2, R.drawable.img_onboarding_3
    };
    private static final int[] TITLES = {
            R.string.onboarding_title_1, R.string.onboarding_title_2, R.string.onboarding_title_3
    };
    private static final int[] DESCRIPTIONS = {
            R.string.onboarding_desc_1, R.string.onboarding_desc_2, R.string.onboarding_desc_3
    };

    private ViewPager2 pager;
    private MaterialButton btnNext;
    private View tvSkip;
    private LinearLayout dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager = findViewById(R.id.pager_onboarding);
        btnNext = findViewById(R.id.btn_next);
        tvSkip = findViewById(R.id.tv_skip);
        dots = findViewById(R.id.dots_container);

        pager.setAdapter(new PagerAdapter());
        buildDots();
        updateForPage(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateForPage(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current < IMAGES.length - 1) pager.setCurrentItem(current + 1, true);
            else finishOnboarding();
        });
        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void buildDots() {
        dots.removeAllViews();
        for (int i = 0; i < IMAGES.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_selector);
            dots.addView(dot);
        }
    }

    private void updateForPage(int position) {
        for (int i = 0; i < dots.getChildCount(); i++) dots.getChildAt(i).setSelected(i == position);
        boolean last = position == IMAGES.length - 1;
        btnNext.setText(last ? R.string.onboarding_get_started : R.string.action_next);
        tvSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
    }

    private void finishOnboarding() {
        session.setOnboardingDone();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private static class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.PageHolder> {
        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
            return new PageHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder h, int position) {
            h.image.setImageResource(IMAGES[position]);
            h.title.setText(TITLES[position]);
            h.desc.setText(DESCRIPTIONS[position]);
        }

        @Override
        public int getItemCount() {
            return IMAGES.length;
        }

        static class PageHolder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView title, desc;

            PageHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.iv_onboarding_illustration);
                title = itemView.findViewById(R.id.tv_onboarding_title);
                desc = itemView.findViewById(R.id.tv_onboarding_description);
            }
        }
    }
}
