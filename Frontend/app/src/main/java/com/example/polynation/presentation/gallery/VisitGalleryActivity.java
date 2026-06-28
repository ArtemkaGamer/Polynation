package com.example.polynation.presentation.gallery;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.polynation.R;
import com.example.polynation.data.local.LocalImageStore;
import com.example.polynation.data.remote.dto.VisitImage;
import com.example.polynation.data.repository.VisitImageRepository;
import com.example.polynation.presentation.common.BaseActivity;
import com.example.polynation.util.AppToast;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class VisitGalleryActivity extends BaseActivity {

    public static final String EXTRA_VISIT_POINT_ID = "visitPointId";
    public static final String EXTRA_LABEL = "label";

    private static final int MAX_PHOTOS = 3;

    private int visitPointId = -1;
    private String label;

    private VisitGalleryViewModel viewModel;
    private LocalImageStore imageStore;

    private TextView tvSubtitle;
    private LinearLayout gridContainer;
    private View emptyState;
    private ProgressBar progress;
    private Button btnAdd;

    private Dialog uploadDialog;
    private TextView tvUploadProgress;

    private final List<VisitImage> images = new ArrayList<>();
    private final Deque<Uri> uploadQueue = new ArrayDeque<>();
    private int uploadOk = 0;
    private int uploadFail = 0;
    private int uploadTotal = 0;

    private ActivityResultLauncher<PickVisualMediaRequest> picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_gallery);

        visitPointId = getIntent().getIntExtra(EXTRA_VISIT_POINT_ID, -1);
        label = getIntent().getStringExtra(EXTRA_LABEL);

        ImageButton btnBack = findViewById(R.id.btn_gallery_back);
        TextView tvTitle = findViewById(R.id.tv_gallery_title);
        tvSubtitle = findViewById(R.id.tv_gallery_subtitle);
        gridContainer = findViewById(R.id.grid_container);
        emptyState = findViewById(R.id.empty_state);
        progress = findViewById(R.id.progress_gallery);
        btnAdd = findViewById(R.id.btn_add_photo);

        tvTitle.setText("Мои фотографии");
        tvSubtitle.setText(label != null && !label.isEmpty() ? label : "Посещённое место");

        picker = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS), this::onImagesPicked);

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> launchPicker());

        imageStore = new LocalImageStore(this);

        viewModel = new ViewModelProvider(this).get(VisitGalleryViewModel.class);
        observeViewModel();

        if (visitPointId == -1) {
            AppToast.show(this, "Не удалось открыть галерею");
            finish();
            return;
        }
        viewModel.loadImages(visitPointId);
    }

    @Override
    protected void onDestroy() {
        if (uploadDialog != null && uploadDialog.isShowing()) {
            uploadDialog.dismiss();
        }
        uploadDialog = null;
        super.onDestroy();
    }

    private void observeViewModel() {
        viewModel.getImages().observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                progress.setVisibility(View.VISIBLE);
                return;
            }
            progress.setVisibility(View.GONE);
            if (result.isSuccess()) {
                images.clear();
                if (result.data != null) images.addAll(result.data);
                renderGrid();
            } else {
                AppToast.show(this, result.message);
                renderGrid();
            }
        });

        viewModel.getUploadResult().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            if (result.isSuccess()) {
                uploadOk++;
            } else {
                uploadFail++;
            }
            uploadNext();
        });

        viewModel.getDeleteResult().observe(this, result -> {
            if (result == null) return;
            if (result.isSuccess() && result.data != null) {
                removeImageById(result.data);
                AppToast.show(this, "Фото удалено");
            } else if (result.isError()) {
                AppToast.show(this, result.message);
                renderGrid();
            }
        });
    }

    private void launchPicker() {
        if (images.size() >= MAX_PHOTOS) {
            AppToast.show(this, "Можно добавить не больше " + MAX_PHOTOS + " фотографий");
            return;
        }
        picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void onImagesPicked(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        int remaining = MAX_PHOTOS - images.size();
        if (remaining <= 0) {
            AppToast.show(this, "Можно добавить не больше " + MAX_PHOTOS + " фотографий");
            return;
        }
        List<Uri> toUpload = uris;
        boolean trimmed = false;
        if (uris.size() > remaining) {
            toUpload = new ArrayList<>(uris.subList(0, remaining));
            trimmed = true;
        }
        uploadQueue.addAll(toUpload);
        uploadOk = 0;
        uploadFail = 0;
        uploadTotal = toUpload.size();
        if (trimmed) {
            AppToast.show(this, "Можно добавить не больше " + MAX_PHOTOS + " фотографий");
        }
        showUploadDialog();
        uploadNext();
    }

    private void uploadNext() {
        Uri next = uploadQueue.poll();
        if (next == null) {
            finishUploads();
            return;
        }
        updateUploadProgress();
        viewModel.upload(visitPointId, next);
    }

    private void finishUploads() {
        dismissUploadDialog();
        if (uploadOk > 0) {
            AppToast.show(this, uploadOk == 1 ? "Фото добавлено" : "Добавлено фото: " + uploadOk);
            viewModel.loadImages(visitPointId);
        }
        boolean hadFailures = uploadFail > 0;
        uploadOk = 0;
        uploadFail = 0;
        if (hadFailures) {
            showPhotoErrorDialog();
        }
    }

    private void showPhotoErrorDialog() {
        if (isFinishing()) return;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_too_large, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        view.findViewById(R.id.btn_photo_choose_another).setOnClickListener(v -> {
            dialog.dismiss();
            launchPicker();
        });
        view.findViewById(R.id.btn_photo_dismiss).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showUploadDialog() {
        btnAdd.setEnabled(false);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_upload_progress, null);
        tvUploadProgress = view.findViewById(R.id.tv_upload_progress);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        uploadDialog = dialog;
        dialog.show();
    }

    private void updateUploadProgress() {
        if (tvUploadProgress == null) return;
        int current = uploadOk + uploadFail + 1;
        tvUploadProgress.setText(uploadTotal > 1
                ? "Фото " + current + " из " + uploadTotal
                : "Подождите немного");
    }

    private void dismissUploadDialog() {
        btnAdd.setEnabled(true);
        if (uploadDialog != null && uploadDialog.isShowing() && !isFinishing()) {
            uploadDialog.dismiss();
        }
        uploadDialog = null;
        tvUploadProgress = null;
    }

    private void deleteImage(VisitImage image, View badge) {
        if (image == null) return;
        badge.setEnabled(false);
        viewModel.delete(visitPointId, image.getId());
    }

    private void removeImageById(int imageId) {
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getId() == imageId) {
                images.remove(i);
                break;
            }
        }
        renderGrid();
    }

    private void renderGrid() {
        gridContainer.removeAllViews();
        boolean empty = images.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;

        float density = getResources().getDisplayMetrics().density;
        int gap = (int) (12 * density);
        int horizontalPadding = (int) (32 * density);
        int totalWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = (totalWidth - horizontalPadding - gap) / 2;

        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout row = null;

        for (int i = 0; i < images.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = gap;
                row.setLayoutParams(rowLp);
                gridContainer.addView(row);
            }

            final VisitImage image = images.get(i);
            View cell = inflater.inflate(R.layout.item_visit_photo, row, false);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(cellSize, cellSize);
            if (i % 2 == 0) cellLp.rightMargin = gap;
            cell.setLayoutParams(cellLp);

            ShapeableImageView iv = cell.findViewById(R.id.iv_photo);
            ImageView btnDelete = cell.findViewById(R.id.btn_delete_photo);
            btnDelete.setVisibility(View.VISIBLE);

            loadPhoto(iv, image);

            iv.setOnClickListener(v -> showFullScreen(image));
            btnDelete.setOnClickListener(v -> deleteImage(image, v));

            row.addView(cell);
        }
    }

    private void loadPhoto(ImageView target, VisitImage image) {
        String url = VisitImageRepository.resolveUrl(visitPointId, image);
        File local = imageStore.imageFile(visitPointId, image.getId());

        RequestBuilder<Drawable> fallback = Glide.with(this)
                .load(local.exists() ? local : null)
                .placeholder(R.drawable.bg_photo_placeholder)
                .error(R.drawable.bg_photo_error);

        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.bg_photo_placeholder)
                .error(fallback)
                .into(target);
    }

    private void showFullScreen(VisitImage image) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(Color.parseColor("#E60A1F2D"));
        iv.setOnClickListener(v -> dialog.dismiss());

        loadPhoto(iv, image);

        dialog.setContentView(iv);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }
}
