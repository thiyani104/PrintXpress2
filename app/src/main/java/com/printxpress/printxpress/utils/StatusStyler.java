package com.printxpress.printxpress.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.DBContract.OrderEntry;

public final class StatusStyler {

    private StatusStyler() {}

    /** Colours a status badge using day/night-aware colour resources. */
    public static void apply(TextView badge, String status) {
        Context ctx = badge.getContext();
        int bg;
        int fg;
        if (OrderEntry.STATUS_PROCESSING.equals(status)) {
            bg = R.color.status_processing_bg; fg = R.color.status_processing_text;
        } else if (OrderEntry.STATUS_PRINTING.equals(status)) {
            bg = R.color.status_printing_bg; fg = R.color.status_printing_text;
        } else if (OrderEntry.STATUS_COMPLETED.equals(status)) {
            bg = R.color.status_completed_bg; fg = R.color.status_completed_text;
        } else if (OrderEntry.STATUS_CANCELLED.equals(status)) {
            bg = R.color.status_cancelled_bg; fg = R.color.status_cancelled_text;
        } else {
            bg = R.color.status_pending_bg; fg = R.color.status_pending_text;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(ctx.getResources().getDisplayMetrics().density * 100f);
        shape.setColor(ContextCompat.getColor(ctx, bg));
        badge.setBackground(shape);
        badge.setTextColor(ContextCompat.getColor(ctx, fg));
        badge.setText(status);
    }
}
