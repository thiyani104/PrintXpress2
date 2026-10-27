package com.printxpress.printxpress.utils;

import java.util.regex.Pattern;

public class Validation {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Sri Lankan mobile numbers: 0712345678, 712345678 or +94712345678
    private static final Pattern SRI_LANKAN_PHONE_PATTERN = Pattern.compile(
            "^(?:0|\\+94)?7[01245678]\\d{7}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && SRI_LANKAN_PHONE_PATTERN.matcher(stripSeparators(phone)).matches();
    }

    /** At least 8 characters, with a letter and a number. */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        return letter && digit;
    }

    public static boolean isFieldNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** Converts +94 71 123 4567 / 0711234567 / 711234567 into 0711234567. */
    public static String normalizePhone(String phone) {
        String p = stripSeparators(phone);
        if (p.startsWith("+94")) p = "0" + p.substring(3);
        else if (p.length() == 9) p = "0" + p;
        return p;
    }

    private static String stripSeparators(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s\\-()]", "");
    }
}
