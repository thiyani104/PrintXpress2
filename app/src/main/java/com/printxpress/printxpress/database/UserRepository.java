package com.printxpress.printxpress.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.printxpress.printxpress.database.DBContract.UserEntry;
import com.printxpress.printxpress.models.User;
import com.printxpress.printxpress.utils.PasswordHasher;
import com.printxpress.printxpress.utils.Validation;

public class UserRepository {

    private static final String[] COLUMNS = {
            UserEntry._ID, UserEntry.COLUMN_FULL_NAME, UserEntry.COLUMN_EMAIL,
            UserEntry.COLUMN_PHONE, UserEntry.COLUMN_DOB, UserEntry.COLUMN_ADDRESS
    };

    private final DatabaseHelper helper;

    public UserRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    /** @return the new user id, or -1 if the email is already registered. */
    public long register(String name, String email, String phone, String password) {
        ContentValues values = new ContentValues();
        values.put(UserEntry.COLUMN_FULL_NAME, name.trim());
        values.put(UserEntry.COLUMN_EMAIL, Validation.normalizeEmail(email));
        values.put(UserEntry.COLUMN_PHONE, Validation.normalizePhone(phone));
        values.put(UserEntry.COLUMN_PASSWORD, PasswordHasher.hash(password));
        return helper.getWritableDatabase().insert(UserEntry.TABLE_NAME, null, values);
    }

    public boolean emailExists(String email) {
        try (Cursor c = helper.getReadableDatabase().query(UserEntry.TABLE_NAME,
                new String[]{UserEntry._ID}, UserEntry.COLUMN_EMAIL + " = ?",
                new String[]{Validation.normalizeEmail(email)}, null, null, null)) {
            return c.moveToFirst();
        }
    }

    /** @return the user when the credentials are valid, otherwise null. */
    public User authenticate(String email, String password) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id;
        String stored;
        try (Cursor c = db.query(UserEntry.TABLE_NAME,
                new String[]{UserEntry._ID, UserEntry.COLUMN_PASSWORD},
                UserEntry.COLUMN_EMAIL + " = ?",
                new String[]{Validation.normalizeEmail(email)}, null, null, null)) {
            if (!c.moveToFirst()) return null;
            id = c.getLong(0);
            stored = c.getString(1);
        }

        if (!PasswordHasher.verify(password, stored)) return null;

        // Accounts created by the first app version stored the password in plain text.
        // Upgrade them to a salted hash the first time they sign in.
        if (!PasswordHasher.isHashed(stored)) {
            ContentValues v = new ContentValues();
            v.put(UserEntry.COLUMN_PASSWORD, PasswordHasher.hash(password));
            db.update(UserEntry.TABLE_NAME, v, UserEntry._ID + " = ?", new String[]{String.valueOf(id)});
        }
        return getById(id);
    }

    public User getById(long id) {
        try (Cursor c = helper.getReadableDatabase().query(UserEntry.TABLE_NAME, COLUMNS,
                UserEntry._ID + " = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (!c.moveToFirst()) return null;
            return new User(c.getLong(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getString(5));
        }
    }

    public boolean updateProfile(long id, String name, String phone, String dob, String address) {
        ContentValues v = new ContentValues();
        v.put(UserEntry.COLUMN_FULL_NAME, name.trim());
        v.put(UserEntry.COLUMN_PHONE, Validation.normalizePhone(phone));
        v.put(UserEntry.COLUMN_DOB, dob);
        v.put(UserEntry.COLUMN_ADDRESS, address == null ? null : address.trim());
        return helper.getWritableDatabase().update(UserEntry.TABLE_NAME, v,
                UserEntry._ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean updateAddress(long id, String address) {
        ContentValues v = new ContentValues();
        v.put(UserEntry.COLUMN_ADDRESS, address.trim());
        return helper.getWritableDatabase().update(UserEntry.TABLE_NAME, v,
                UserEntry._ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * Offline password reset: the app has no mail server, so identity is confirmed by
     * matching the email together with the phone number used at registration.
     */
    public boolean resetPassword(String email, String phone, String newPassword) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id;
        String storedPhone;
        try (Cursor c = db.query(UserEntry.TABLE_NAME,
                new String[]{UserEntry._ID, UserEntry.COLUMN_PHONE},
                UserEntry.COLUMN_EMAIL + " = ?",
                new String[]{Validation.normalizeEmail(email)}, null, null, null)) {
            if (!c.moveToFirst()) return false;
            id = c.getLong(0);
            storedPhone = c.getString(1);
        }
        if (!Validation.normalizePhone(storedPhone).equals(Validation.normalizePhone(phone))) return false;

        ContentValues v = new ContentValues();
        v.put(UserEntry.COLUMN_PASSWORD, PasswordHasher.hash(newPassword));
        return db.update(UserEntry.TABLE_NAME, v, UserEntry._ID + " = ?", new String[]{String.valueOf(id)}) > 0;
    }
}
