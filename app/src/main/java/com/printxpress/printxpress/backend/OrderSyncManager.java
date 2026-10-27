package com.printxpress.printxpress.backend;

import android.content.Context;
import android.os.SystemClock;

import com.printxpress.printxpress.backend.SupabaseClient.BackendException;
import com.printxpress.printxpress.database.OrderRepository;
import com.printxpress.printxpress.database.UserRepository;
import com.printxpress.printxpress.models.Order;
import com.printxpress.printxpress.models.OrderItem;
import com.printxpress.printxpress.models.User;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps the local order history and the print shop in step:
 *  1. pushes orders that have not been sent yet (design files first, then the order itself)
 *  2. pulls status changes made by the shop
 * Everything is best-effort and retried at the next trigger, so the app works offline.
 */
public final class OrderSyncManager {

    public enum CancelResult { OK, NOT_CANCELLABLE, OFFLINE, ERROR }

    private static final String BUCKET = "designs";
    private static final long MIN_INTERVAL_MS = 10_000;
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile long lastRunMs = -MIN_INTERVAL_MS;

    private OrderSyncManager() {}

    public static boolean isConfigured(Context context) {
        return SupabaseClient.create(context) != null;
    }

    /**
     * Runs a sync in the background.
     *
     * @param force      ignore the minimum interval between syncs
     * @param onFinished optional; called on the main thread when the attempt ends
     *                   (not called when no backend is configured)
     */
    public static void syncAsync(Context context, final long userId, boolean force, final Runnable onFinished) {
        final Context app = context.getApplicationContext();
        final SupabaseClient client = SupabaseClient.create(app);
        if (client == null) return;

        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastRunMs < MIN_INTERVAL_MS) return;
        if (!RUNNING.compareAndSet(false, true)) {
            if (onFinished != null) Async.main(onFinished);
            return;
        }
        lastRunMs = now;

        Async.io(() -> {
            try {
                syncNow(app, client, userId);
            } catch (RuntimeException ignored) {
                // never let a sync problem crash the app
            } finally {
                RUNNING.set(false);
            }
            if (onFinished != null) Async.main(onFinished);
        });
    }

    /** Cancels an order that was already sent. Blocking: call from a background thread. */
    public static CancelResult cancelRemote(Context context, String remoteId, String reason) {
        SupabaseClient client = SupabaseClient.create(context.getApplicationContext());
        if (client == null) return CancelResult.ERROR;
        try {
            JSONObject body = new JSONObject();
            body.put("p_id", remoteId);
            body.put("p_reason", reason);
            String response = client.rpc("cancel_order", body.toString());
            return "true".equals(response.trim()) ? CancelResult.OK : CancelResult.NOT_CANCELLABLE;
        } catch (IOException e) {
            return CancelResult.OFFLINE;
        } catch (BackendException | JSONException e) {
            return CancelResult.ERROR;
        }
    }

    // ------------------------------------------------------------------------------------

    private static void syncNow(Context app, SupabaseClient client, long userId) {
        OrderRepository repo = new OrderRepository(app);
        User user = new UserRepository(app).getById(userId);
        if (user == null) return;

        try {
            for (long orderId : repo.getUnsyncedOrderIds(userId)) {
                Order order = repo.getOrder(orderId, userId);
                if (order == null || order.getRemoteId() == null) continue;
                pushOrder(client, repo, user, order, userId);
            }
            pullStatuses(client, repo, userId);
        } catch (IOException e) {
            // Offline or timed out: try again at the next trigger.
        } catch (BackendException | JSONException e) {
            // Rejected by the server: leave the order pending so it can be retried.
        }
    }

    private static void pushOrder(SupabaseClient client, OrderRepository repo, User user, Order order, long userId)
            throws IOException, BackendException, JSONException {
        JSONArray items = new JSONArray();
        int index = 0;
        for (OrderItem item : order.getItems()) {
            index++;
            String remotePath = item.getRemoteFilePath();
            String notes = item.getNotes() == null ? "" : item.getNotes();

            if ((remotePath == null || remotePath.isEmpty()) && FileUtils.exists(item.getFilePath())) {
                File file = new File(item.getFilePath());
                String name = FileUtils.displayName(item.getFilePath());
                String path = order.getRemoteId() + "/" + index + "_" + name;
                try {
                    client.uploadObject(BUCKET, path, file, contentTypeFor(name));
                    repo.setItemRemotePath(item.getId(), path);
                    remotePath = path;
                } catch (BackendException e) {
                    if (e.code == 413 || (e.body != null && e.body.toLowerCase(Locale.ROOT).contains("maximum allowed size"))) {
                        // Too big for the storage plan: send the order anyway and tell the shop.
                        notes = (notes + " [Design file was too large to upload - please contact the customer.]").trim();
                    } else {
                        throw e;
                    }
                }
            }

            JSONObject o = new JSONObject();
            o.put("product_id", item.getProductId());
            o.put("product_name", item.getProductName());
            o.put("spec", item.getSpec() == null ? "" : item.getSpec());
            o.put("quantity", item.getQuantity());
            o.put("unit_price", item.getUnitPrice());
            o.put("file_path", remotePath == null || remotePath.isEmpty() ? JSONObject.NULL : remotePath);
            o.put("notes", notes);
            items.put(o);
        }

        JSONObject payload = new JSONObject();
        payload.put("id", order.getRemoteId());
        payload.put("customer_name", user.getFullName());
        payload.put("customer_email", user.getEmail());
        payload.put("customer_phone", user.getPhone());
        payload.put("delivery_method", order.getDeliveryMethod());
        payload.put("delivery_address", order.getAddress() == null ? "" : order.getAddress());
        payload.put("delivery_date", order.getDeliveryDate() == null ? "" : order.getDeliveryDate());
        payload.put("payment_method", order.getPaymentMethod());
        payload.put("subtotal", order.getSubtotal());
        payload.put("discount", order.getDiscount());
        payload.put("promo_code", order.getPromoCode() == null ? "" : order.getPromoCode());
        payload.put("delivery_fee", order.getDeliveryFee());
        payload.put("total", order.getTotal());
        payload.put("items", items);

        JSONObject body = new JSONObject();
        body.put("payload", payload);
        client.rpc("place_order", body.toString()); // idempotent on the order id

        // The customer may have cancelled while the order was being uploaded.
        Order fresh = repo.getOrder(order.getId(), userId);
        if (fresh != null && fresh.isCancelled()) {
            JSONObject cancel = new JSONObject();
            cancel.put("p_id", order.getRemoteId());
            cancel.put("p_reason", fresh.getCancelReason());
            client.rpc("cancel_order", cancel.toString());
        }
        repo.markSynced(order.getId());
    }

    private static void pullStatuses(SupabaseClient client, OrderRepository repo, long userId)
            throws IOException, BackendException, JSONException {
        List<String> ids = repo.getOpenRemoteIds(userId);
        if (ids.isEmpty()) return;

        JSONObject body = new JSONObject();
        body.put("ids", new JSONArray(ids));
        JSONArray rows = new JSONArray(client.rpc("get_order_statuses", body.toString()));
        for (int i = 0; i < rows.length(); i++) {
            JSONObject r = rows.getJSONObject(i);
            String reason = r.isNull("cancel_reason") ? null : r.getString("cancel_reason");
            repo.applyRemoteStatus(r.getString("id"), r.getString("status"), reason);
        }
    }

    private static String contentTypeFor(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".tif") || n.endsWith(".tiff")) return "image/tiff";
        if (n.endsWith(".psd")) return "image/vnd.adobe.photoshop";
        if (n.endsWith(".ai")) return "application/postscript";
        return "application/octet-stream";
    }
}
