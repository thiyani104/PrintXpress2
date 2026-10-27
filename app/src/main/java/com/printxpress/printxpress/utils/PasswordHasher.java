package com.printxpress.printxpress.utils;

import android.os.Build;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 password hashing.
 * Stored format: v1$&lt;algorithm&gt;$&lt;iterations&gt;$&lt;salt&gt;$&lt;hash&gt;
 */
public final class PasswordHasher {

    private static final String PREFIX = "v1$";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordHasher() {}

    public static String hash(String password) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);
            String algorithm = preferredAlgorithm();
            byte[] hash = derive(password, salt, ITERATIONS, algorithm);
            return PREFIX + algorithm + "$" + ITERATIONS + "$"
                    + Base64.encodeToString(salt, Base64.NO_WRAP) + "$"
                    + Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        }
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        if (!isHashed(stored)) {
            // Legacy plain-text record from the first app version.
            return MessageDigest.isEqual(password.getBytes(), stored.getBytes());
        }
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 5) return false;
            String algorithm = parts[1];
            int iterations = Integer.parseInt(parts[2]);
            byte[] salt = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[4], Base64.NO_WRAP);
            byte[] actual = derive(password, salt, iterations, algorithm);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static String preferredAlgorithm() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? "PBKDF2WithHmacSHA256" : "PBKDF2WithHmacSHA1";
    }

    private static byte[] derive(String password, byte[] salt, int iterations, String algorithm) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
    }
}
