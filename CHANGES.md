# PrintXpress – upgrade notes

## How to open
Open the folder in Android Studio, let Gradle sync, and run. The first launch upgrades the
existing database in place (no data loss). Nothing in `build.gradle` / `libs.versions.toml` changed.

## What changed

### Design system (res/)
- Real Material 3 theme in your brand green + gold, with a proper dark theme (`values-night`).
  Before, the theme never applied your colours, and the night theme was defined twice with conflicting parents.
- Shared styles for buttons, cards, text fields, toolbars, typography and shapes (`values/styles.xml`).
- 36 vector icons replace JPG/system icons (tintable, sharp at any size, dark-mode aware).
- Edge-to-edge handled in one place (`BaseActivity`). With `targetSdk 36` content previously drew under the status/navigation bars.
- Photos compressed (drawables 3.9 MB -> 1.5 MB); dead and duplicate assets removed.
- All user-facing text moved to `strings.xml` (original wording preserved).

### Functionality
| Area | Before | Now |
|---|---|---|
| Login | Asked every launch; plain-text passwords | Session persists; salted PBKDF2 hashes; old accounts upgraded silently on next login |
| Cart | One item passed through an Intent | Persistent multi-item cart (SQLite): quantity, remove, merge duplicates |
| Orders | Never saved (random ID on success screen) | Saved in one transaction; real order numbers; history, details, status timeline, cancel (Pending only), reorder |
| Promo codes | Text on a screen | CARD20 / SAVE15 / FREE3000 actually discount at checkout and are stored on the order |
| Delivery fee | Rs.250 in code, Rs.200 on screen | One source of truth (`Pricing`), Rs.250, waived by FREE3000 |
| Design upload | URI lost after the picker closed; no checks | Copied to private storage off the main thread; type + 100 MB validation; survives rotation |
| Search | Decorative | Works across all products; category chips; sort |
| Forgot password | Fake toast | Works offline (email + registered phone confirms identity) |
| Profile / Contact | Hardcoded "Sumethya Perera" | Real logged-in user |
| Settings | Dead toggles | Theme (system/light/dark, persisted) and cache clearing that really work |
| Design Guidelines PDF | Asset missing, download could never work | PDF included; saved to Downloads (or opened on Android 9-) |
| Database upgrade | Dropped all tables | Non-destructive migrations |
| Navigation | Index-based tab hack | Shared bottom navigation (Home / Orders / Offers / Profile) |

## Things YOU need to decide / configure
1. **Offer expiry dates** – `models/OfferCatalog.java`. The originals (May 2026) had already expired, so I set `2026-12-31` as a placeholder.
2. **Pricing units** – prices are treated as per-unit (price x quantity). Confirm what a "unit" is for each product (e.g. a box of 100 cards vs. a single card).
3. **Delivery fee / store pickup text / phone / email / hours** – `Pricing.java`, `strings.xml`, `ContactSupportActivity.java`.
4. **Privacy policy / Terms** – short placeholder text in `strings.xml`; replace with reviewed legal text.
5. **Payment** – there is no payment gateway. Orders record "Cash on Delivery", "Card on Delivery" or "Bank Transfer" (online card payment was removed rather than faked). For real online payments integrate a gateway such as PayHere.

## Print-shop backend (new)
Orders now reach the print shop through Supabase. Full steps: `backend/SETUP.md`.
- `backend/supabase_setup.sql` - tables, row-level security, validated functions, private storage bucket
- `backend/shop-dashboard.html` - the shop's web dashboard (staff login, orders, design downloads, status updates)
- App: `backend/SupabaseClient.java`, `backend/OrderSyncManager.java`; DB v3 adds sync tracking (existing orders are marked as history and never sent)
- Put your keys in `res/values/backend_config.xml`. Leave the placeholders and the app runs offline exactly as before.
- Remaining limits: customer accounts are still on the phone; prices are computed in the app; no push notifications; free-plan file limit is 50 MB.

In debug builds a "Debug: advance status" button still lets you demo the status timeline without a backend.

## Verification performed (no Android SDK / network in my environment)
- All business logic (pricing, promos, validation, catalog, order helpers, password hashing): compiled and run, 57 checks pass.
- All SQL, including the v1 -> v2 migration on a database built with your original schema: executed against SQLite, all checks pass.
- Every `R.*` / `@...` reference in Java + XML resolved; every `findViewById` matches its layout view type; all XML well-formed.
- Backend: the HTTP client was tested against a local mock Supabase (headers, streaming upload, duplicate/oversize/error/offline handling, 16 checks); the shop dashboard was run in headless Chromium against a mocked Supabase (login, filters, search, status update, signed download, XSS attempts, 18 checks).
- **Not done:** a Gradle build, running on a device/emulator, and running `supabase_setup.sql` and the sync against a real Supabase project (no network or Postgres here). Please do a Sync + Build, follow `backend/SETUP.md`, and click through the full flow once.
