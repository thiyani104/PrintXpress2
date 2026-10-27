package com.printxpress.printxpress.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.printxpress.printxpress.database.DBContract.CartEntry;
import com.printxpress.printxpress.database.DBContract.OrderDetailEntry;
import com.printxpress.printxpress.database.DBContract.OrderEntry;
import com.printxpress.printxpress.models.CartItem;
import com.printxpress.printxpress.models.Order;
import com.printxpress.printxpress.models.OrderItem;
import com.printxpress.printxpress.utils.Pricing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class OrderRepository {

    private final DatabaseHelper helper;

    public OrderRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    /**
     * Saves the order and its items, then empties the user's cart, all in one transaction
     * so a failure can never leave a half-created order behind.
     *
     * @return the new order id, or -1 on failure
     */
    public long placeOrder(long userId, List<CartItem> items, Pricing.Quote quote,
                           String deliveryMethod, String address, String deliveryDate,
                           String paymentMethod) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues o = new ContentValues();
            o.put(OrderEntry.COLUMN_USER_ID, userId);
            o.put(OrderEntry.COLUMN_ORDER_DATE,
                    new SimpleDateFormat(Order.DB_DATE_FORMAT, Locale.US).format(new Date()));
            o.put(OrderEntry.COLUMN_STATUS, OrderEntry.STATUS_PENDING);
            o.put(OrderEntry.COLUMN_SUBTOTAL, quote.subtotal);
            o.put(OrderEntry.COLUMN_DISCOUNT, quote.discount);
            o.put(OrderEntry.COLUMN_PROMO_CODE, quote.appliedCode);
            o.put(OrderEntry.COLUMN_DELIVERY_FEE, quote.deliveryFee);
            o.put(OrderEntry.COLUMN_TOTAL_AMOUNT, quote.total);
            o.put(OrderEntry.COLUMN_DELIVERY_METHOD, deliveryMethod);
            o.put(OrderEntry.COLUMN_SHIPPING_ADDRESS, address);
            o.put(OrderEntry.COLUMN_DELIVERY_DATE, deliveryDate);
            o.put(OrderEntry.COLUMN_PAYMENT_METHOD, paymentMethod);
            o.put(OrderEntry.COLUMN_REMOTE_ID, UUID.randomUUID().toString());
            o.put(OrderEntry.COLUMN_SYNCED, 0);
            long orderId = db.insert(OrderEntry.TABLE_NAME, null, o);
            if (orderId == -1) return -1;

            for (CartItem item : items) {
                ContentValues d = new ContentValues();
                d.put(OrderDetailEntry.COLUMN_ORDER_ID, orderId);
                d.put(OrderDetailEntry.COLUMN_PRODUCT_ID, item.getProductId());
                d.put(OrderDetailEntry.COLUMN_PRODUCT_NAME, item.getProductName());
                d.put(OrderDetailEntry.COLUMN_PRINT_SPEC, item.getSpec());
                d.put(OrderDetailEntry.COLUMN_QUANTITY, item.getQuantity());
                d.put(OrderDetailEntry.COLUMN_UNIT_PRICE, item.getUnitPrice());
                d.put(OrderDetailEntry.COLUMN_FILE_PATH, item.getFilePath());
                d.put(OrderDetailEntry.COLUMN_NOTES, item.getNotes());
                if (db.insert(OrderDetailEntry.TABLE_NAME, null, d) == -1) return -1;
            }

            db.delete(CartEntry.TABLE_NAME, CartEntry.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
            return orderId;
        } finally {
            db.endTransaction();
        }
    }

    public List<Order> getOrders(long userId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, "
                + "(SELECT d." + OrderDetailEntry.COLUMN_PRODUCT_NAME + " FROM " + OrderDetailEntry.TABLE_NAME
                + " d WHERE d." + OrderDetailEntry.COLUMN_ORDER_ID + " = o." + OrderEntry._ID
                + " ORDER BY d." + OrderDetailEntry._ID + " LIMIT 1) AS first_item, "
                + "(SELECT COUNT(*) FROM " + OrderDetailEntry.TABLE_NAME
                + " d WHERE d." + OrderDetailEntry.COLUMN_ORDER_ID + " = o." + OrderEntry._ID + ") AS item_count, "
                + "(SELECT IFNULL(SUM(d." + OrderDetailEntry.COLUMN_QUANTITY + "), 0) FROM " + OrderDetailEntry.TABLE_NAME
                + " d WHERE d." + OrderDetailEntry.COLUMN_ORDER_ID + " = o." + OrderEntry._ID + ") AS total_qty "
                + "FROM " + OrderEntry.TABLE_NAME + " o WHERE o." + OrderEntry.COLUMN_USER_ID + " = ? "
                + "ORDER BY o." + OrderEntry._ID + " DESC";
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) {
                Order order = readOrder(c);
                String first = c.getString(c.getColumnIndexOrThrow("first_item"));
                order.setFirstItemName(first == null ? "" : first);
                order.setItemCount(c.getInt(c.getColumnIndexOrThrow("item_count")));
                order.setTotalQuantity(c.getInt(c.getColumnIndexOrThrow("total_qty")));
                orders.add(order);
            }
        }
        return orders;
    }

    /** @return the order with its items, or null if it does not exist or belongs to someone else. */
    public Order getOrder(long orderId, long userId) {
        Order order = null;
        try (Cursor c = helper.getReadableDatabase().query(OrderEntry.TABLE_NAME, null,
                OrderEntry._ID + " = ? AND " + OrderEntry.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(orderId), String.valueOf(userId)}, null, null, null)) {
            if (c.moveToFirst()) order = readOrder(c);
        }
        if (order == null) return null;

        List<OrderItem> items = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(OrderDetailEntry.TABLE_NAME, null,
                OrderDetailEntry.COLUMN_ORDER_ID + " = ?", new String[]{String.valueOf(orderId)},
                null, null, OrderDetailEntry._ID + " ASC")) {
            while (c.moveToNext()) {
                items.add(new OrderItem(
                        c.getLong(c.getColumnIndexOrThrow(OrderDetailEntry._ID)),
                        c.getString(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_PRODUCT_ID)),
                        c.getString(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_PRODUCT_NAME)),
                        c.getString(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_PRINT_SPEC)),
                        c.getInt(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_QUANTITY)),
                        c.getDouble(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_UNIT_PRICE)),
                        c.getString(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_FILE_PATH)),
                        c.getString(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_REMOTE_FILE_PATH)),
                        c.getString(c.getColumnIndexOrThrow(OrderDetailEntry.COLUMN_NOTES))));
            }
        }
        order.setItems(items);
        return order;
    }

    /** Only pending orders can be cancelled. */
    public boolean cancel(long orderId, long userId, String reason) {
        ContentValues v = new ContentValues();
        v.put(OrderEntry.COLUMN_STATUS, OrderEntry.STATUS_CANCELLED);
        v.put(OrderEntry.COLUMN_CANCEL_REASON, reason);
        return helper.getWritableDatabase().update(OrderEntry.TABLE_NAME, v,
                OrderEntry._ID + " = ? AND " + OrderEntry.COLUMN_USER_ID + " = ? AND "
                        + OrderEntry.COLUMN_STATUS + " = ?",
                new String[]{String.valueOf(orderId), String.valueOf(userId), OrderEntry.STATUS_PENDING}) > 0;
    }

    /** Moves an order one step along Pending, Processing, Printing, Completed (demo/testing aid). */
    public void advanceStatus(long orderId, long userId, String currentStatus) {
        String next;
        if (OrderEntry.STATUS_PENDING.equals(currentStatus)) next = OrderEntry.STATUS_PROCESSING;
        else if (OrderEntry.STATUS_PROCESSING.equals(currentStatus)) next = OrderEntry.STATUS_PRINTING;
        else if (OrderEntry.STATUS_PRINTING.equals(currentStatus)) next = OrderEntry.STATUS_COMPLETED;
        else return;
        ContentValues v = new ContentValues();
        v.put(OrderEntry.COLUMN_STATUS, next);
        helper.getWritableDatabase().update(OrderEntry.TABLE_NAME, v,
                OrderEntry._ID + " = ? AND " + OrderEntry.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(orderId), String.valueOf(userId)});
    }

    private Order readOrder(Cursor c) {
        Order o = new Order();
        o.setId(c.getLong(c.getColumnIndexOrThrow(OrderEntry._ID)));
        o.setCreatedAt(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_ORDER_DATE)));
        o.setStatus(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_STATUS)));
        o.setSubtotal(c.getDouble(c.getColumnIndexOrThrow(OrderEntry.COLUMN_SUBTOTAL)));
        o.setDiscount(c.getDouble(c.getColumnIndexOrThrow(OrderEntry.COLUMN_DISCOUNT)));
        o.setPromoCode(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_PROMO_CODE)));
        o.setDeliveryFee(c.getDouble(c.getColumnIndexOrThrow(OrderEntry.COLUMN_DELIVERY_FEE)));
        o.setTotal(c.getDouble(c.getColumnIndexOrThrow(OrderEntry.COLUMN_TOTAL_AMOUNT)));
        o.setDeliveryMethod(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_DELIVERY_METHOD)));
        o.setAddress(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_SHIPPING_ADDRESS)));
        o.setDeliveryDate(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_DELIVERY_DATE)));
        o.setPaymentMethod(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_PAYMENT_METHOD)));
        o.setCancelReason(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_CANCEL_REASON)));
        o.setRemoteId(c.getString(c.getColumnIndexOrThrow(OrderEntry.COLUMN_REMOTE_ID)));
        o.setSynced(c.getInt(c.getColumnIndexOrThrow(OrderEntry.COLUMN_SYNCED)) == 1);
        return o;
    }

    // ---------------------------------------------------------------- print-shop sync

    /** Local ids of orders that still have to be sent (cancelled-before-sending orders are skipped). */
    public List<Long> getUnsyncedOrderIds(long userId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(OrderEntry.TABLE_NAME,
                new String[]{OrderEntry._ID},
                OrderEntry.COLUMN_USER_ID + " = ? AND " + OrderEntry.COLUMN_SYNCED + " = 0 AND "
                        + OrderEntry.COLUMN_STATUS + " != ? AND " + OrderEntry.COLUMN_REMOTE_ID + " IS NOT NULL",
                new String[]{String.valueOf(userId), OrderEntry.STATUS_CANCELLED},
                null, null, OrderEntry._ID + " ASC")) {
            while (c.moveToNext()) ids.add(c.getLong(0));
        }
        return ids;
    }

    public void markSynced(long orderId) {
        ContentValues v = new ContentValues();
        v.put(OrderEntry.COLUMN_SYNCED, 1);
        helper.getWritableDatabase().update(OrderEntry.TABLE_NAME, v,
                OrderEntry._ID + " = ?", new String[]{String.valueOf(orderId)});
    }

    public void setItemRemotePath(long itemId, String remotePath) {
        ContentValues v = new ContentValues();
        v.put(OrderDetailEntry.COLUMN_REMOTE_FILE_PATH, remotePath);
        helper.getWritableDatabase().update(OrderDetailEntry.TABLE_NAME, v,
                OrderDetailEntry._ID + " = ?", new String[]{String.valueOf(itemId)});
    }

    /** Remote ids of sent orders whose status can still change. */
    public List<String> getOpenRemoteIds(long userId) {
        List<String> ids = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(OrderEntry.TABLE_NAME,
                new String[]{OrderEntry.COLUMN_REMOTE_ID},
                OrderEntry.COLUMN_USER_ID + " = ? AND " + OrderEntry.COLUMN_SYNCED + " = 1 AND "
                        + OrderEntry.COLUMN_REMOTE_ID + " IS NOT NULL AND "
                        + OrderEntry.COLUMN_STATUS + " NOT IN (?, ?)",
                new String[]{String.valueOf(userId), OrderEntry.STATUS_COMPLETED, OrderEntry.STATUS_CANCELLED},
                null, null, null)) {
            while (c.moveToNext()) ids.add(c.getString(0));
        }
        return ids;
    }

    /** Applies a status change made by the print shop. Unknown statuses are ignored. */
    public void applyRemoteStatus(String remoteId, String status, String cancelReason) {
        if (!OrderEntry.STATUS_PENDING.equals(status) && !OrderEntry.STATUS_PROCESSING.equals(status)
                && !OrderEntry.STATUS_PRINTING.equals(status) && !OrderEntry.STATUS_COMPLETED.equals(status)
                && !OrderEntry.STATUS_CANCELLED.equals(status)) return;
        ContentValues v = new ContentValues();
        v.put(OrderEntry.COLUMN_STATUS, status);
        if (cancelReason != null) v.put(OrderEntry.COLUMN_CANCEL_REASON, cancelReason);
        helper.getWritableDatabase().update(OrderEntry.TABLE_NAME, v,
                OrderEntry.COLUMN_REMOTE_ID + " = ? AND " + OrderEntry.COLUMN_STATUS + " != ?",
                new String[]{remoteId, status});
    }
}
