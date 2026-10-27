package com.printxpress.printxpress.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.printxpress.printxpress.database.DBContract.CartEntry;
import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.models.Product;

import java.util.ArrayList;
import java.util.List;

public class CartRepository {

    public static final int MAX_QUANTITY = 9999;

    private final DatabaseHelper helper;
    private final long userId;

    public CartRepository(Context context, long userId) {
        this.helper = DatabaseHelper.getInstance(context);
        this.userId = userId;
    }

    /** Adds a line. The same product with the same design file is merged into one line. */
    public void addItem(Product product, int quantity, String filePath, String notes) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String fileValue = filePath == null ? "" : filePath;

        try (Cursor c = db.query(CartEntry.TABLE_NAME,
                new String[]{CartEntry._ID, CartEntry.COLUMN_QUANTITY},
                CartEntry.COLUMN_USER_ID + " = ? AND " + CartEntry.COLUMN_PRODUCT_ID + " = ? AND "
                        + "IFNULL(" + CartEntry.COLUMN_FILE_PATH + ", '') = ?",
                new String[]{String.valueOf(userId), product.getId(), fileValue},
                null, null, null)) {
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                int merged = Math.min(MAX_QUANTITY, c.getInt(1) + quantity);
                setQuantity(id, merged);
                return;
            }
        }

        ContentValues v = new ContentValues();
        v.put(CartEntry.COLUMN_USER_ID, userId);
        v.put(CartEntry.COLUMN_PRODUCT_ID, product.getId());
        v.put(CartEntry.COLUMN_PRODUCT_NAME, product.getName());
        v.put(CartEntry.COLUMN_SPEC, product.getSpecDetails());
        v.put(CartEntry.COLUMN_CATEGORY, product.getCategory());
        v.put(CartEntry.COLUMN_UNIT_PRICE, product.getPrice());
        v.put(CartEntry.COLUMN_QUANTITY, quantity);
        v.put(CartEntry.COLUMN_FILE_PATH, fileValue);
        v.put(CartEntry.COLUMN_NOTES, notes);
        db.insert(CartEntry.TABLE_NAME, null, v);
    }

    public List<CartItem> getItems() {
        List<CartItem> items = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(CartEntry.TABLE_NAME, null,
                CartEntry.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, CartEntry._ID + " ASC")) {
            while (c.moveToNext()) {
                items.add(new CartItem(
                        c.getLong(c.getColumnIndexOrThrow(CartEntry._ID)),
                        c.getString(c.getColumnIndexOrThrow(CartEntry.COLUMN_PRODUCT_ID)),
                        c.getString(c.getColumnIndexOrThrow(CartEntry.COLUMN_PRODUCT_NAME)),
                        c.getString(c.getColumnIndexOrThrow(CartEntry.COLUMN_SPEC)),
                        c.getString(c.getColumnIndexOrThrow(CartEntry.COLUMN_CATEGORY)),
                        c.getDouble(c.getColumnIndexOrThrow(CartEntry.COLUMN_UNIT_PRICE)),
                        c.getInt(c.getColumnIndexOrThrow(CartEntry.COLUMN_QUANTITY)),
                        c.getString(c.getColumnIndexOrThrow(CartEntry.COLUMN_FILE_PATH)),
                        c.getString(c.getColumnIndexOrThrow(CartEntry.COLUMN_NOTES))));
            }
        }
        return items;
    }

    public int getLineCount() {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + CartEntry.TABLE_NAME + " WHERE " + CartEntry.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void setQuantity(long itemId, int quantity) {
        ContentValues v = new ContentValues();
        v.put(CartEntry.COLUMN_QUANTITY, Math.max(1, Math.min(MAX_QUANTITY, quantity)));
        helper.getWritableDatabase().update(CartEntry.TABLE_NAME, v,
                CartEntry._ID + " = ? AND " + CartEntry.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(userId)});
    }

    public void remove(long itemId) {
        helper.getWritableDatabase().delete(CartEntry.TABLE_NAME,
                CartEntry._ID + " = ? AND " + CartEntry.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(userId)});
    }

    public void clear() {
        helper.getWritableDatabase().delete(CartEntry.TABLE_NAME,
                CartEntry.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }
}
