package com.printxpress.printxpress.database;

import android.provider.BaseColumns;

public final class DBContract {

    private DBContract() {}

    public static final class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_FULL_NAME = "full_name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PHONE = "phone_number";
        public static final String COLUMN_PASSWORD = "password_hash";
        public static final String COLUMN_DOB = "date_of_birth";
        public static final String COLUMN_GENDER = "gender";
        public static final String COLUMN_ADDRESS = "address";
    }

    public static final class OrderEntry implements BaseColumns {
        public static final String TABLE_NAME = "orders";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_ORDER_DATE = "order_date";
        public static final String COLUMN_TOTAL_AMOUNT = "total_amount";
        public static final String COLUMN_STATUS = "order_status";
        public static final String COLUMN_DELIVERY_METHOD = "delivery_method";
        public static final String COLUMN_SHIPPING_ADDRESS = "shipping_address";
        public static final String COLUMN_PAYMENT_METHOD = "payment_method";
        public static final String COLUMN_SUBTOTAL = "subtotal";
        public static final String COLUMN_DISCOUNT = "discount_amount";
        public static final String COLUMN_PROMO_CODE = "promo_code";
        public static final String COLUMN_DELIVERY_FEE = "delivery_fee";
        public static final String COLUMN_DELIVERY_DATE = "delivery_date";
        public static final String COLUMN_CANCEL_REASON = "cancel_reason";
        public static final String COLUMN_REMOTE_ID = "remote_id";
        public static final String COLUMN_SYNCED = "synced";

        public static final String DELIVERY_HOME = "Home Delivery";
        public static final String DELIVERY_PICKUP = "Pickup";
        public static final String PAYMENT_COD = "Cash on Delivery";
        public static final String PAYMENT_CARD = "Card on Delivery";
        public static final String PAYMENT_BANK = "Bank Transfer";

        public static final String STATUS_PENDING = "Pending";
        public static final String STATUS_PROCESSING = "Processing";
        public static final String STATUS_PRINTING = "Printing";
        public static final String STATUS_COMPLETED = "Completed";
        public static final String STATUS_CANCELLED = "Cancelled";
    }

    public static final class OrderDetailEntry implements BaseColumns {
        public static final String TABLE_NAME = "order_details";
        public static final String COLUMN_ORDER_ID = "order_id";
        public static final String COLUMN_PRODUCT_ID = "product_id";
        public static final String COLUMN_PRODUCT_NAME = "product_name";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_UNIT_PRICE = "unit_price";
        public static final String COLUMN_PRINT_SPEC = "print_specification";
        public static final String COLUMN_FILE_PATH = "local_file_path";
        public static final String COLUMN_NOTES = "notes";
        public static final String COLUMN_REMOTE_FILE_PATH = "remote_file_path";
    }

    public static final class CartEntry implements BaseColumns {
        public static final String TABLE_NAME = "cart_items";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_PRODUCT_ID = "product_id";
        public static final String COLUMN_PRODUCT_NAME = "product_name";
        public static final String COLUMN_SPEC = "spec";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_UNIT_PRICE = "unit_price";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_FILE_PATH = "file_path";
        public static final String COLUMN_NOTES = "notes";
    }
}
