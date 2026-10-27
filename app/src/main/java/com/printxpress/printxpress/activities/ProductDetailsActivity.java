package com.printxpress.printxpress.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.printxpress.printxpress.R;
import com.printxpress.printxpress.database.CartRepository;
import com.printxpress.printxpress.models.Product;
import com.printxpress.printxpress.models.ProductCatalog;
import com.printxpress.printxpress.utils.FileUtils;
import com.printxpress.printxpress.utils.Money;
import com.printxpress.printxpress.utils.UiUtils;

import java.io.File;
import java.util.Locale;

public class ProductDetailsActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "PRODUCT_ID";

    private static final String STATE_PATH = "design_path";
    private static final String STATE_NAME = "design_name";
    private static final String STATE_SIZE = "design_size";
    private static final String STATE_USED = "design_used";

    private Product product;
    private TextInputEditText etQuantity;
    private TextInputEditText etNotes;
    private TextView tvTotal;
    private View uploadPrompt;
    private View uploadDone;
    private TextView tvFileName;
    private TextView tvFileSize;
    private LinearProgressIndicator uploadProgress;
    private MaterialButton btnAdd;

    private String designPath;
    private String designName;
    private long designSize;
    /** True once the current file is referenced by a cart line (so it must not be deleted). */
    private boolean designUsed;

    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importDesign(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        product = ProductCatalog.findById(getIntent().getStringExtra(EXTRA_PRODUCT_ID));
        if (product == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_product_details);
        setupToolbar(R.string.product_details_title);

        ImageView image = findViewById(R.id.iv_details_image);
        image.setImageResource(product.getImageResource());
        setText(R.id.tv_details_title, product.getName());
        setText(R.id.tv_details_specs, product.getSpecDetails());
        setText(R.id.tv_details_rating, String.format(Locale.US, "%.1f", product.getRating()));
        setText(R.id.tv_details_unit_price, getString(R.string.price_each, Money.format(product.getPrice())));

        etQuantity = findViewById(R.id.et_quantity);
        etNotes = findViewById(R.id.et_notes);
        tvTotal = findViewById(R.id.tv_details_price_total);
        uploadPrompt = findViewById(R.id.btn_upload_file_trigger);
        uploadDone = findViewById(R.id.card_upload_done);
        tvFileName = findViewById(R.id.tv_upload_file_name);
        tvFileSize = findViewById(R.id.tv_upload_file_size);
        uploadProgress = findViewById(R.id.progress_upload);
        btnAdd = findViewById(R.id.btn_add_to_cart_submit);

        if (savedInstanceState != null) {
            designPath = savedInstanceState.getString(STATE_PATH);
            designName = savedInstanceState.getString(STATE_NAME);
            designSize = savedInstanceState.getLong(STATE_SIZE);
            designUsed = savedInstanceState.getBoolean(STATE_USED);
            if (!FileUtils.exists(designPath)) designPath = null;
        }

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { updateTotal(); }
        });
        etQuantity.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) etQuantity.setText(String.valueOf(readQuantity()));
        });
        findViewById(R.id.btn_qty_minus).setOnClickListener(v -> setQuantity(readQuantity() - 1));
        findViewById(R.id.btn_qty_plus).setOnClickListener(v -> setQuantity(readQuantity() + 1));

        uploadPrompt.setOnClickListener(v -> picker.launch(FileUtils.PICKER_MIME_TYPES));
        findViewById(R.id.btn_upload_replace).setOnClickListener(v -> picker.launch(FileUtils.PICKER_MIME_TYPES));
        findViewById(R.id.btn_upload_remove).setOnClickListener(v -> {
            discardDesignIfUnused();
            designPath = null;
            designUsed = false;
            renderUpload();
        });
        btnAdd.setOnClickListener(v -> addToCart());

        etQuantity.setText("1");
        renderUpload();
        updateTotal();
    }

    private int readQuantity() {
        return UiUtils.parseQuantity(etQuantity.getText() == null ? "" : etQuantity.getText().toString(), 1);
    }

    private void setQuantity(int value) {
        int clamped = Math.max(1, Math.min(CartRepository.MAX_QUANTITY, value));
        etQuantity.setText(String.valueOf(clamped));
        etQuantity.setSelection(etQuantity.getText().length());
    }

    private void updateTotal() {
        tvTotal.setText(Money.format(product.getPrice() * readQuantity()));
    }

    private void importDesign(Uri uri) {
        uploadProgress.setVisibility(View.VISIBLE);
        uploadPrompt.setEnabled(false);
        FileUtils.importDesign(this, uri, new FileUtils.ImportCallback() {
            @Override
            public void onSuccess(String absolutePath, String displayName, long sizeBytes) {
                if (isFinishing() || isDestroyed()) return;
                uploadProgress.setVisibility(View.GONE);
                uploadPrompt.setEnabled(true);
                discardDesignIfUnused();
                designPath = absolutePath;
                designName = displayName;
                designSize = sizeBytes;
                designUsed = false;
                renderUpload();
            }

            @Override
            public void onError(int messageRes) {
                if (isFinishing() || isDestroyed()) return;
                uploadProgress.setVisibility(View.GONE);
                uploadPrompt.setEnabled(true);
                UiUtils.snack(ProductDetailsActivity.this, messageRes);
            }
        });
    }

    private void renderUpload() {
        boolean has = designPath != null;
        uploadPrompt.setVisibility(has ? View.GONE : View.VISIBLE);
        uploadDone.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) {
            tvFileName.setText(designName != null ? designName : FileUtils.displayName(designPath));
            tvFileSize.setText(FileUtils.formatSize(designSize));
        }
    }

    private void addToCart() {
        if (designPath == null) {
            UiUtils.snack(this, R.string.error_design_required);
            findViewById(R.id.btn_upload_file_trigger).requestFocus();
            return;
        }
        UiUtils.hideKeyboard(this);
        String notes = etNotes.getText() == null ? "" : etNotes.getText().toString().trim();
        new CartRepository(this, session.getUserId()).addItem(product, readQuantity(), designPath, notes);
        designUsed = true;

        Snackbar bar = UiUtils.makeSnack(this, getString(R.string.added_to_cart));
        bar.setAction(R.string.view_cart, v -> startActivity(new Intent(this, CartActivity.class)));
        bar.show();
    }

    private void discardDesignIfUnused() {
        if (designPath != null && !designUsed) {
            //noinspection ResultOfMethodCallIgnored
            new File(designPath).delete();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PATH, designPath);
        outState.putString(STATE_NAME, designName);
        outState.putLong(STATE_SIZE, designSize);
        outState.putBoolean(STATE_USED, designUsed);
    }

    @Override
    protected void onDestroy() {
        // Only clean up when leaving for good, not on rotation.
        if (isFinishing()) discardDesignIfUnused();
        super.onDestroy();
    }
}
