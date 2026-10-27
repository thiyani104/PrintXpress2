package com.printxpress.printxpress.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.utils.UiUtils;

public class ContactSupportActivity extends BaseActivity {

    private static final String PHONE = "0111224567";
    private static final String EMAIL = "support@printxpress.lk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_support);
        setupToolbar(R.string.help_support);

        String name = session.getUserName().trim();
        int space = name.indexOf(' ');
        String first = space > 0 ? name.substring(0, space) : name;
        setText(R.id.tv_contact_greeting, first.isEmpty()
                ? getString(R.string.contact_greeting_guest)
                : getString(R.string.contact_greeting, first));

        findViewById(R.id.btn_contact_call).setOnClickListener(v ->
                open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + PHONE))));
        findViewById(R.id.btn_contact_email).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
            open(intent);
        });

        LinearLayout faq = findViewById(R.id.ll_faq);
        String[] questions = getResources().getStringArray(R.array.faq_questions);
        String[] answers = getResources().getStringArray(R.array.faq_answers);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < questions.length && i < answers.length; i++) {
            final String q = questions[i];
            final String a = answers[i];
            View row = inflater.inflate(R.layout.item_menu_row, faq, false);
            ((ImageView) row.findViewById(R.id.iv_row_icon)).setImageResource(R.drawable.ic_help);
            ((TextView) row.findViewById(R.id.tv_row_title)).setText(q);
            row.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setTitle(q)
                    .setMessage(a)
                    .setPositiveButton(R.string.action_close, null)
                    .show());
            faq.addView(row);
        }
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.snack(this, R.string.error_no_app);
        }
    }
}
