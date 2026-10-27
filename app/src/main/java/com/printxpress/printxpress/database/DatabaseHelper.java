package com.printxpress.printxpress.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.printxpress.printxpress.database.DBContract.CartEntry;
import com.printxpress.printxpress.database.DBContract.OrderDetailEntry;
import com.printxpress.printxpress.database.DBContract.OrderEntry;
import com.printxpress.printxpress.database.DBContract.UserEntry;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "printxpress.db";
    private static final int DATABASE_VERSION = 3;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + UserEntry.TABLE_NAME + " ("
            + UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + UserEntry.COLUMN_FULL_NAME + " TEXT NOT NULL, "
            + UserEntry.COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, "
            + UserEntry.COLUMN_PHONE + " TEXT NOT NULL, "
            + UserEntry.COLUMN_PASSWORD + " TEXT NOT NULL, "
            + UserEntry.COLUMN_DOB + " TEXT, "
            + UserEntry.COLUMN_GENDER + " TEXT, "
            + UserEntry.COLUMN_ADDRESS + " TEXT);";

    private static final String CREATE_ORDERS_TABLE = "CREATE TABLE " + OrderEntry.TABLE_NAME + " ("
            + OrderEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + OrderEntry.COLUMN_USER_ID + " INTEGER NOT NULL, "
            + OrderEntry.COLUMN_ORDER_DATE + " TEXT NOT NULL, "
            + OrderEntry.COLUMN_TOTAL_AMOUNT + " REAL NOT NULL, "
            + OrderEntry.COLUMN_STATUS + " TEXT NOT NULL, "
            + OrderEntry.COLUMN_DELIVERY_METHOD + " TEXT NOT NULL, "
            + OrderEntry.COLUMN_SHIPPING_ADDRESS + " TEXT, "
            + OrderEntry.COLUMN_PAYMENT_METHOD + " TEXT NOT NULL, "
            + OrderEntry.COLUMN_SUBTOTAL + " REAL NOT NULL DEFAULT 0, "
            + OrderEntry.COLUMN_DISCOUNT + " REAL NOT NULL DEFAULT 0, "
            + OrderEntry.COLUMN_PROMO_CODE + " TEXT, "
            + OrderEntry.COLUMN_DELIVERY_FEE + " REAL NOT NULL DEFAULT 0, "
            + OrderEntry.COLUMN_DELIVERY_DATE + " TEXT, "
            + OrderEntry.COLUMN_CANCEL_REASON + " TEXT, "
            + OrderEntry.COLUMN_REMOTE_ID + " TEXT, "
            + OrderEntry.COLUMN_SYNCED + " INTEGER NOT NULL DEFAULT 0, "
            + "FOREIGN KEY(" + OrderEntry.COLUMN_USER_ID + ") REFERENCES "
            + UserEntry.TABLE_NAME + "(" + UserEntry._ID + "));";

    private static final String CREATE_ORDER_DETAILS_TABLE = "CREATE TABLE " + OrderDetailEntry.TABLE_NAME + " ("
            + OrderDetailEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + OrderDetailEntry.COLUMN_ORDER_ID + " INTEGER NOT NULL, "
            + OrderDetailEntry.COLUMN_PRODUCT_ID + " TEXT, "
            + OrderDetailEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
            + OrderDetailEntry.COLUMN_QUANTITY + " INTEGER NOT NULL, "
            + OrderDetailEntry.COLUMN_UNIT_PRICE + " REAL NOT NULL DEFAULT 0, "
            + OrderDetailEntry.COLUMN_PRINT_SPEC + " TEXT, "
            + OrderDetailEntry.COLUMN_FILE_PATH + " TEXT, "
            + OrderDetailEntry.COLUMN_NOTES + " TEXT, "
            + OrderDetailEntry.COLUMN_REMOTE_FILE_PATH + " TEXT, "
            + "FOREIGN KEY(" + OrderDetailEntry.COLUMN_ORDER_ID + ") REFERENCES "
            + OrderEntry.TABLE_NAME + "(" + OrderEntry._ID + ") ON DELETE CASCADE);";

    private static final String CREATE_CART_TABLE = "CREATE TABLE " + CartEntry.TABLE_NAME + " ("
            + CartEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + CartEntry.COLUMN_USER_ID + " INTEGER NOT NULL, "
            + CartEntry.COLUMN_PRODUCT_ID + " TEXT NOT NULL, "
            + CartEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
            + CartEntry.COLUMN_SPEC + " TEXT, "
            + CartEntry.COLUMN_CATEGORY + " TEXT, "
            + CartEntry.COLUMN_UNIT_PRICE + " REAL NOT NULL, "
            + CartEntry.COLUMN_QUANTITY + " INTEGER NOT NULL, "
            + CartEntry.COLUMN_FILE_PATH + " TEXT, "
            + CartEntry.COLUMN_NOTES + " TEXT, "
            + "FOREIGN KEY(" + CartEntry.COLUMN_USER_ID + ") REFERENCES "
            + UserEntry.TABLE_NAME + "(" + UserEntry._ID + ") ON DELETE CASCADE);";

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_ORDERS_TABLE);
        db.execSQL(CREATE_ORDER_DETAILS_TABLE);
        db.execSQL(CREATE_CART_TABLE);
    }

    /**
     * Non-destructive migrations: existing users and data survive an app update.
     * (The previous version dropped every table on upgrade.)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + UserEntry.TABLE_NAME + " ADD COLUMN " + UserEntry.COLUMN_ADDRESS + " TEXT");

            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_SUBTOTAL + " REAL NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_DISCOUNT + " REAL NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_PROMO_CODE + " TEXT");
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_DELIVERY_FEE + " REAL NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_DELIVERY_DATE + " TEXT");
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_CANCEL_REASON + " TEXT");

            db.execSQL("ALTER TABLE " + OrderDetailEntry.TABLE_NAME + " ADD COLUMN " + OrderDetailEntry.COLUMN_PRODUCT_ID + " TEXT");
            db.execSQL("ALTER TABLE " + OrderDetailEntry.TABLE_NAME + " ADD COLUMN " + OrderDetailEntry.COLUMN_UNIT_PRICE + " REAL NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + OrderDetailEntry.TABLE_NAME + " ADD COLUMN " + OrderDetailEntry.COLUMN_NOTES + " TEXT");

            db.execSQL(CREATE_CART_TABLE);
        }
        if (oldVersion < 3) {
            // Print-shop sync: every new order gets a remote id and is pushed to the backend.
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_REMOTE_ID + " TEXT");
            db.execSQL("ALTER TABLE " + OrderEntry.TABLE_NAME + " ADD COLUMN " + OrderEntry.COLUMN_SYNCED + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + OrderDetailEntry.TABLE_NAME + " ADD COLUMN " + OrderDetailEntry.COLUMN_REMOTE_FILE_PATH + " TEXT");
            // Orders placed before the backend existed are history only and are never sent.
            db.execSQL("UPDATE " + OrderEntry.TABLE_NAME + " SET " + OrderEntry.COLUMN_SYNCED + " = 1");
        }
    }
}
