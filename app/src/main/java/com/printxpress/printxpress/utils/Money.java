package com.printxpress.printxpress.utils;

import java.util.Locale;

public final class Money {

    private Money() {}

    public static String format(double amount) {
        return String.format(Locale.US, "Rs. %,.2f", amount);
    }
}
