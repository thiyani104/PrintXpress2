package com.printxpress.printxpress.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import androidx.annotation.StringRes;

import com.printxpress.printxpress.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Imports a picked design file into app-private storage so it survives after the picker closes. */
public final class FileUtils {

    public static final long MAX_BYTES = 100L * 1024 * 1024; // matches the Design Guidelines (100MB)
    public static final String[] PICKER_MIME_TYPES = {
            "application/pdf", "image/*", "application/postscript",
            "image/vnd.adobe.photoshop", "application/octet-stream"
    };

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "ai", "psd", "jpg", "jpeg", "png", "tif", "tiff"));

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private FileUtils() {}

    public interface ImportCallback {
        void onSuccess(String absolutePath, String displayName, long sizeBytes);
        void onError(@StringRes int messageRes);
    }

    public static void importDesign(Context context, Uri uri, ImportCallback callback) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> {
            String name = queryName(app, uri);
            String ext = extensionOf(name);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                MAIN.post(() -> callback.onError(R.string.error_file_type));
                return;
            }
            long declared = querySize(app, uri);
            if (declared > MAX_BYTES) {
                MAIN.post(() -> callback.onError(R.string.error_file_too_large));
                return;
            }

            File dir = new File(app.getFilesDir(), "designs");
            if (!dir.exists() && !dir.mkdirs()) {
                MAIN.post(() -> callback.onError(R.string.error_file_copy));
                return;
            }
            File out = new File(dir, System.currentTimeMillis() + "_" + sanitize(name));

            try (InputStream in = app.getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(out)) {
                if (in == null) throw new IllegalStateException("No stream");
                byte[] buf = new byte[16 * 1024];
                long total = 0;
                int read;
                while ((read = in.read(buf)) != -1) {
                    total += read;
                    if (total > MAX_BYTES) {
                        os.close();
                        //noinspection ResultOfMethodCallIgnored
                        out.delete();
                        MAIN.post(() -> callback.onError(R.string.error_file_too_large));
                        return;
                    }
                    os.write(buf, 0, read);
                }
                final long size = total;
                final String shown = name;
                MAIN.post(() -> callback.onSuccess(out.getAbsolutePath(), shown, size));
            } catch (Exception e) {
                //noinspection ResultOfMethodCallIgnored
                out.delete();
                MAIN.post(() -> callback.onError(R.string.error_file_copy));
            }
        });
    }

    /** "1730000000_logo.pdf" becomes "logo.pdf". */
    public static String displayName(String path) {
        if (path == null || path.isEmpty()) return "";
        String name = new File(path).getName();
        return name.replaceFirst("^\\d+_", "");
    }

    public static boolean exists(String path) {
        return path != null && !path.isEmpty() && new File(path).isFile();
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.0f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static long folderSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        if (dir.isFile()) return dir.length();
        long total = 0;
        File[] children = dir.listFiles();
        if (children != null) for (File f : children) total += folderSize(f);
        return total;
    }

    public static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File f : children) deleteRecursively(f);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static String queryName(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && !c.isNull(idx)) return c.getString(idx);
            }
        } catch (Exception ignored) {
            // fall through
        }
        String last = uri.getLastPathSegment();
        return last == null ? "design" : last;
    }

    private static long querySize(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0 && !c.isNull(idx)) return c.getLong(idx);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return -1;
    }

    private static String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String name) {
        return name == null ? "design" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
