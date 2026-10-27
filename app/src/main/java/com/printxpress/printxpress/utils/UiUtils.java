package com.printxpress.printxpress.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.StringRes;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.CartRepository;

public final class UiUtils {

    private UiUtils() {}

    public interface IntCallback {
        void onValue(int value);
    }

    public static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static void hideKeyboard(Activity activity) {
        View focus = activity.getCurrentFocus();
        if (focus == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    public static void copyToClipboard(Context context, String label, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    /** Builds a snackbar that sits above the bottom bar / bottom navigation when one is visible. */
    public static Snackbar makeSnack(Activity activity, CharSequence message) {
        View root = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
        View anchor = activity.findViewById(R.id.bottom_bar);
        if (anchor == null || anchor.getVisibility() != View.VISIBLE) {
            anchor = activity.findViewById(R.id.bottom_nav);
        }
        if (anchor != null && anchor.getVisibility() == View.VISIBLE) snackbar.setAnchorView(anchor);
        return snackbar;
    }

    public static void snack(Activity activity, CharSequence message) {
        makeSnack(activity, message).show();
    }

    public static void snack(Activity activity, @StringRes int message) {
        makeSnack(activity, activity.getString(message)).show();
    }

    /** Makes a set of checkable cards behave like a radio group. */
    public static void bindSingleChoice(final MaterialCardView[] cards, int initialIndex, final IntCallback callback) {
        for (int i = 0; i < cards.length; i++) {
            final int index = i;
            cards[i].setCheckable(true);
            cards[i].setChecked(i == initialIndex);
            cards[i].setOnClickListener(v -> {
                for (int j = 0; j < cards.length; j++) cards[j].setChecked(j == index);
                callback.onValue(index);
            });
        }
    }

    /** Lets the user type an exact quantity (useful for 500 flyers, where +/- is impractical). */
    public static void promptQuantity(Context context, int current, IntCallback callback) {
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelection(input.getText().length());
        input.setHint(R.string.hint_quantity);

        FrameLayout container = new FrameLayout(context);
        int pad = dp(context, 24);
        container.setPadding(pad, dp(context, 8), pad, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_quantity_title)
                .setView(container)
                .setPositiveButton(R.string.action_update, (d, w) ->
                        callback.onValue(parseQuantity(input.getText().toString(), current)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    public static int parseQuantity(String text, int fallback) {
        try {
            int v = Integer.parseInt(text.trim());
            return Math.max(1, Math.min(CartRepository.MAX_QUANTITY, v));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
