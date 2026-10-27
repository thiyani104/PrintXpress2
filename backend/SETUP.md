# Connecting PrintXpress to a real print-shop backend (Supabase)

Time needed: about 15 minutes. Cost: free (Supabase free plan).

What you get:

```
 Customer's phone (PrintXpress app)                         Print shop (shop-dashboard.html)
   place order + design file  ───────►  Supabase  ◄───────  sign in, see orders, download designs,
   see status updates         ◄───────  (database + files)   change status
```

## 1. Create the project
1. Go to <https://supabase.com>, sign up, and click **New project**.
2. Choose a name, a strong database password (save it somewhere), and the region closest to your customers.
3. Wait until the project finishes setting up.

## 2. Create the tables, security rules and file storage
1. In the left menu open **SQL Editor** → **New query**.
2. Open `backend/supabase_setup.sql`, copy everything, paste it in, click **Run**.
3. You should see "Success. No rows returned". It is safe to run again.

## 3. Lock down sign-ups (important)
Shop staff are Supabase users. If anyone could register, anyone could read your orders.
1. Go to **Authentication** → **Sign In / Providers** (older dashboards: **Authentication → Settings**).
2. Turn **OFF** "Allow new users to sign up".

## 4. Create a login for the shop
1. **Authentication** → **Users** → **Add user** → **Create new user**.
2. Enter an email and password and tick **Auto Confirm User**.
3. Repeat for every staff member who should see orders.

## 5. Put the project keys in the app
1. **Project Settings** → **API** (or **API Keys**). Copy:
   - **Project URL** (looks like `https://abcdxyz.supabase.co`)
   - the public key: either the legacy **anon** key (`eyJ...`) or the new **publishable** key (`sb_publishable_...`)
2. Open `app/src/main/res/values/backend_config.xml` and replace the two placeholders.
3. Rebuild and run the app.

> Never use the **service_role** / **secret** key anywhere in the app or the dashboard. Only the public anon/publishable key is safe to ship.

Leave the placeholders as they are and the app simply keeps working offline, as before.

## 6. Open the shop dashboard
1. Double-click `backend/shop-dashboard.html` (it opens in your browser).
2. First time only: paste the same Project URL and public key.
3. Sign in with the staff account from step 4.

To let staff use it from any computer or phone, host the single file for free (GitHub Pages, Netlify drag-and-drop, Cloudflare Pages). It is protected by the staff login, but keep the address private anyway.

## 7. Test the whole chain
1. In the app (phone needs internet): register, add a product with a design file, check out.
2. On the order screen you should see **"Received by the print shop"** within a few seconds.
3. The order appears in the dashboard (new orders pop a notification there).
4. Click it → **Download design** → set the status to **Processing** → **Save changes**.
5. In the app open **Orders** → the status badge updates (the app checks when the screen opens).
6. Try cancelling a Pending order from the app, and try again after the shop set it to Processing: the second attempt is refused.

## Troubleshooting
| Symptom | Check |
|---|---|
| Order stays "Waiting to send…" | Internet on the phone; URL starts with `https://`; keys pasted fully; SQL from step 2 ran without errors. Tap **Retry now**. |
| Quick server test | `curl -X POST "<URL>/rest/v1/rpc/get_order_statuses" -H "apikey: <KEY>" -H "Authorization: Bearer <KEY>" -H "Content-Type: application/json" -d '{"ids":[]}'` should print `[]` (for a publishable `sb_...` key leave out the Authorization header). |
| Dashboard login fails | The user must exist in Authentication → Users and be confirmed. |
| Dashboard shows no orders | Sign in with a staff account (not just the anon key); re-run the SQL file. |
| Design file missing in an order | File was over the storage plan limit (50 MB on the free plan). The order still arrives with a note telling you to contact the customer. |

## Known limits (be upfront about these)
- **Customer accounts are still stored on the phone.** Orders reach the shop, but a customer can't log in from a second device, and reinstalling the app loses their history. Moving accounts to Supabase Auth is the natural next step.
- **Prices are calculated in the app.** The server checks that the totals add up but doesn't know your price list, so a modified app could send a cheaper price. Review totals before printing, or add a `products` table and recompute on the server.
- **Anyone with the app key can submit orders** (that's how a public ordering app works). Add rate limiting or CAPTCHA before a real launch.
- **No push notifications:** status changes appear when the customer opens the app or the Orders screen.
- **Free-plan projects pause after about a week without activity** (you can resume them with one click).
- **Free-plan file limit is 50 MB**, while the app currently accepts up to 100 MB.
- Update your privacy policy: the app now sends names, phone numbers, addresses and design files to your Supabase project.
