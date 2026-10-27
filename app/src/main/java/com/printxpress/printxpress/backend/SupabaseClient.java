package com.printxpress.printxpress.backend;

import android.content.Context;

import com.printxpress.printxpress.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Tiny Supabase REST client built on HttpURLConnection (no extra dependencies).
 * Only the public "anon" key ever lives in the app; the database is protected by
 * row-level security and a few SECURITY DEFINER functions (see backend/supabase_setup.sql).
 */
public final class SupabaseClient {

    /** The server answered with an error status (4xx / 5xx). */
    public static class BackendException extends Exception {
        public final int code;
        public final String body;

        public BackendException(int code, String body) {
            super("HTTP " + code + ": " + body);
            this.code = code;
            this.body = body;
        }
    }

    private final String baseUrl;
    private final String apiKey;

    private SupabaseClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /** @return a client, or null when no backend is configured in res/values/backend_config.xml */
    public static SupabaseClient create(Context context) {
        String url = context.getString(R.string.supabase_url).trim();
        String key = context.getString(R.string.supabase_anon_key).trim();
        if (url.isEmpty() || key.isEmpty() || url.contains("YOUR_") || key.contains("YOUR_")) return null;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (!url.startsWith("https://")) return null; // never send customer data over plain http
        return new SupabaseClient(url, key);
    }

    /** Calls a Postgres function: POST /rest/v1/rpc/{function}. Returns the raw JSON response. */
    public String rpc(String function, String jsonBody) throws IOException, BackendException {
        HttpURLConnection c = open(baseUrl + "/rest/v1/rpc/" + function);
        try {
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            byte[] data = jsonBody.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(data.length);
            try (OutputStream os = c.getOutputStream()) {
                os.write(data);
            }
            return readResponse(c);
        } finally {
            c.disconnect();
        }
    }

    /** Streams a file into a private Storage bucket. An already-uploaded object counts as success. */
    public void uploadObject(String bucket, String path, File file, String contentType)
            throws IOException, BackendException {
        HttpURLConnection c = open(baseUrl + "/storage/v1/object/" + bucket + "/" + path);
        try {
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", contentType);
            c.setReadTimeout(120_000);
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(file.length());
            try (InputStream in = new FileInputStream(file); OutputStream os = c.getOutputStream()) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
            }
            readResponse(c);
        } catch (BackendException e) {
            // A previous attempt may have succeeded before the app recorded it.
            boolean duplicate = e.code == 409 || (e.body != null && e.body.contains("Duplicate"));
            if (!duplicate) throw e;
        } finally {
            c.disconnect();
        }
    }

    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15_000);
        c.setReadTimeout(30_000);
        c.setRequestProperty("apikey", apiKey);
        // Legacy anon keys are JWTs and go in the Authorization header too.
        // New-style publishable keys (sb_publishable_...) are not JWTs and must only use "apikey".
        if (!apiKey.startsWith("sb_")) c.setRequestProperty("Authorization", "Bearer " + apiKey);
        c.setRequestProperty("Accept", "application/json");
        return c;
    }

    private static String readResponse(HttpURLConnection c) throws IOException, BackendException {
        int code = c.getResponseCode();
        InputStream stream = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String body = stream == null ? "" : readAll(stream);
        if (code >= 400) throw new BackendException(code, body);
        return body;
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
}
