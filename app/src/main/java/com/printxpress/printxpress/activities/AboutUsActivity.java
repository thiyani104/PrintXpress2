package com.printxpress.printxpress.activities;

import android.os.Bundle;

import com.printxpress.printxpress.R;

public class AboutUsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        setupToolbar(R.string.about_title);
    }
}
