# PrintXpress

An Android app for ordering custom print jobs: business cards, flyers, posters, banners, stickers, T-shirts and mugs. Customers browse products, upload their own design, check out, and track their orders.

> **Status:** portfolio / learning project. Orders flow from the app to a **Supabase** backend where the print shop sees them in a web dashboard, downloads the customer's design file and updates the status, which the customer sees in the app. Without a backend configured, the app still works fully offline. There is no payment gateway yet (see [Limitations](#limitations) and [Roadmap](#roadmap)).

```
 Customer app (Android)  ──orders + design files──►  Supabase (Postgres + Storage)  ◄──  Shop dashboard (web)
                         ◄──────status updates────                                      update status, download designs
```

<!-- Add screenshots or a demo GIF here. Suggested: Home, Product details, Cart, Checkout, Order details, Dark mode. -->
<!-- ![Home](docs/screenshots/home.png) -->

## Features

**Shopping**
- Browse 7 categories with 23 products, filter by finish/type, and sort by price or rating
- Search across all products
- Product details with quantity stepper, live total, and special instructions
- Design file upload (PDF, AI, PSD, JPG, PNG, TIFF, up to 100 MB) with validation; files are copied to private storage off the main thread

**Cart and checkout**
- Persistent multi-item cart (survives app restarts)
- Promo codes that apply real discounts (see table below)
- Home delivery or store pickup, address editing, date picker
- Payment method selection: cash on delivery, card on delivery, bank transfer
- Orders saved in a single database transaction

**Orders**
- Order history with Active / Completed / Cancelled filters
- Order details with status timeline, itemised totals and delivery info
- Cancel (while Pending) and reorder

**Print-shop backend (Supabase)**
- Orders are sent to the cloud right after checkout; design files are uploaded to a private storage bucket first
- Works offline-first: if there is no connection the order is kept and retried automatically
- The order screen shows "Received by the print shop" or "Waiting to send" with a retry button
- Status changes made by the shop (Processing, Printing, Completed) appear in the app
- Cancelling is checked with the shop: allowed only while the order is still Pending
- **Shop dashboard** (`backend/shop-dashboard.html`): staff login, live order list with filters and search, customer and delivery details, signed design-file downloads, status and internal notes
- Security: the app only holds the public key; tables are closed to the public by row-level security, and the app can only call three validated database functions. Order ids are random UUIDs. Customer-supplied text is rendered safely in the dashboard (no HTML injection)

**Account**
- Register, login with a persistent session, edit profile
- Passwords stored as salted PBKDF2 hashes
- Offline password reset (email + registered phone number)

**Polish**
- Material 3 theme with light and dark mode (system / light / dark setting)
- Edge-to-edge layouts that handle system bars and the keyboard
- Empty states, input validation, accessible content descriptions
- Offers screen, support screen with real call/email actions and FAQs, in-app Design Guidelines with PDF download

### Promo codes

| Code | Effect |
|---|---|
| `CARD20` | 20% off Business Cards in the cart |
| `SAVE15` | 15% off orders of Rs. 5,000 or more |
| `FREE3000` | Free home delivery on orders of Rs. 3,000 or more |

Offer validity dates live in `OfferCatalog.java`.

## Tech stack

- **Language:** Java 11
- **Platform:** Android, `minSdk 24`, `targetSdk 36`
- **UI:** Material Components 3 (`material 1.14`), ConstraintLayout, ViewPager2, RecyclerView with `ListAdapter` + `DiffUtil`
- **Storage:** SQLite (`SQLiteOpenHelper`) with versioned, non-destructive migrations; `SharedPreferences` for session and settings
- **Backend:** Supabase (Postgres, Row Level Security, `SECURITY DEFINER` functions, Storage) called over HTTPS with `HttpURLConnection` (no extra Android dependencies); the shop dashboard is a single HTML file using `supabase-js`
- **Build:** Gradle with version catalog, Android Gradle Plugin 9.x

## Project structure

```
app/src/main/java/com/printxpress/printxpress/
├── activities/     Screens (all extend BaseActivity)
├── adapters/       RecyclerView adapters
├── app/            Application class (applies saved theme at startup)
├── backend/        SupabaseClient (REST) and OrderSyncManager (push orders, pull statuses)
├── database/       DBContract, DatabaseHelper, User/Cart/Order repositories
├── models/         Product, Order, CartItem, Offer, catalogs
└── utils/          Pricing, Validation, PasswordHasher, SessionManager, FileUtils, ...

backend/
├── supabase_setup.sql    Tables, security rules, functions and storage (run once)
├── shop-dashboard.html   The print shop's web dashboard
└── SETUP.md              Step-by-step backend setup
```

Design notes:
- `Pricing` is the single source of truth for subtotal, discount, delivery fee and total, so the cart, checkout and order history always agree.
- `BaseActivity` centralises edge-to-edge insets and toolbar setup.
- Repositories keep SQL out of the activities.
- Slow work (password hashing, file import, order placement) runs off the main thread.

## Getting started

**Requirements:** a recent Android Studio (this project uses a very new Android Gradle Plugin) and an internet connection for the first Gradle sync.

1. Clone the repository.
2. In Android Studio choose **File > Open** and select the folder that contains `settings.gradle`.
3. Wait for Gradle sync to finish.
4. Run the `app` configuration on an emulator or device (Android 7.0+).

There is no seeded data. Register an account on first launch.

**Connecting the backend (optional):** follow [`backend/SETUP.md`](backend/SETUP.md), about 15 minutes on Supabase's free plan. Without it the app runs offline.

### Try this flow
1. Register and log in
2. Open **Business Cards**, pick a product, upload any PDF or image
3. Add to cart, apply `CARD20`
4. Choose delivery or pickup, pick a date, choose a payment method, place the order
5. Open **Orders** to see it. With the backend connected, open the shop dashboard, find the order, change its status, then reopen **Orders** in the app to see it update. Without a backend, a debug-build button on the order details screen advances the status so you can still see the timeline.

## Limitations

- **Customer accounts are local:** orders reach the shop, but accounts and order history live on the phone (no login from a second device).
- **Prices are calculated in the app;** the server checks that totals add up but does not hold a price list.
- **No push notifications:** status changes show up when the app or the Orders screen is opened.
- **Free-tier limits:** 50 MB per design file on Supabase's free plan; free projects pause after about a week of inactivity.
- **No online payments:** orders record the chosen method (cash, card on delivery, bank transfer). Nothing is charged in the app.
- **Password reset** confirms identity with the registered phone number because the app cannot send email.
- **Placeholder content:** the privacy policy and terms text are short placeholders, and promo validity dates should be reviewed.
- Product prices are treated as per-unit (price multiplied by quantity).

## Roadmap

- [ ] Move customer accounts to Supabase Auth (multi-device login, cloud order history)
- [ ] Server-side price list and promo validation
- [ ] Online payments (for example PayHere)
- [ ] Push notifications for order updates (Firebase Cloud Messaging)
- [ ] Migrate local storage to Room
- [ ] Rate limiting / CAPTCHA on order submission
- [ ] Automated unit and UI tests
- [ ] Sinhala and Tamil localisation

## Credits

Product and promotional images are placeholders for demonstration. Replace them with images you own or have licensed before any public release.

<!-- Add your name, LinkedIn and license here. -->
