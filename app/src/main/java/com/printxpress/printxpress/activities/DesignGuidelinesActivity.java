package com.printxpress.printxpress.activities;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.utils.Async;
import com.printxpress.printxpress.utils.UiUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DesignGuidelinesActivity extends BaseActivity {

    private static final String ASSET_NAME = "print_design_guidelines.pdf";
    private static final String FILE_NAME = "PrintXpress_Design_Guidelines.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_design_guidelines);
        setupToolbar(R.string.design_guidelines_title);

        findViewById(R.id.btn_download_guidelines).setOnClickListener(v -> savePdf());
    }

    private void savePdf() {
        Async.io(() -> {
            Uri uri = null;
            boolean savedToDownloads = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (InputStream in = getAssets().open(ASSET_NAME);
                             OutputStream out = getContentResolver().openOutputStream(uri)) {
                            if (out == null) throw new java.io.IOException("No output stream");
                            copy(in, out);
                        }
                        savedToDownloads = true;
                    }
                }
                if (uri == null) {
                    // Android 9 and below (or a failed insert): share a private copy instead.
                    File dir = new File(getCacheDir(), "shared");
                    if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("mkdir failed");
                    File target = new File(dir, FILE_NAME);
                    try (InputStream in = getAssets().open(ASSET_NAME);
                         OutputStream out = new FileOutputStream(target)) {
                        copy(in, out);
                    }
                    uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", target);
                }
                final Uri result = uri;
                final boolean downloads = savedToDownloads;
                Async.main(() -> onSaved(result, downloads));
            } catch (Exception e) {
                Async.main(() -> {
                    if (!isFinishing() && !isDestroyed()) UiUtils.snack(this, R.string.error_download_failed);
                });
            }
        });
    }

    private void onSaved(Uri uri, boolean savedToDownloads) {
        if (isFinishing() || isDestroyed()) return;
        if (!savedToDownloads) {
            openPdf(uri);
            return;
        }
        Snackbar bar = UiUtils.makeSnack(this, getString(R.string.guidelines_saved));
        bar.setAction(R.string.action_open, v -> openPdf(uri));
        bar.show();
    }

    private void openPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            UiUtils.snack(this, R.string.error_no_pdf_viewer);
        }
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }
}
