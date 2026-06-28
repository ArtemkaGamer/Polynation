package com.example.polynation.presentation.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.polynation.R;
import com.example.polynation.data.remote.dto.UserProfile;
import com.example.polynation.data.remote.dto.VisitImage;
import com.example.polynation.data.remote.dto.VisitPoint;
import com.example.polynation.data.repository.AchievementSyncResult;
import com.example.polynation.data.repository.VisitImageRepository;
import com.example.polynation.domain.achievement.Achievement;
import com.example.polynation.domain.achievement.AchievementCatalog;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.achievement.AchievementViews;
import com.example.polynation.presentation.assistant.AssistantManager;
import com.example.polynation.presentation.auth.WelcomeActivity;
import com.example.polynation.presentation.common.BaseNavigationActivity;
import com.example.polynation.presentation.gallery.VisitGalleryActivity;
import com.example.polynation.util.AppToast;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileActivity extends BaseNavigationActivity {
    private TextView tvUsername, tvEmail, tvRating, tvQuizzesSolved, tvJoinedDate;
    private TextView tvVisitedCount, tvVisitedEmpty, tvVisitedHint;
    private TextView tvAchievementsCount, tvGalleryEmpty;
    private ProgressBar progressAchievements;
    private LinearLayout cardEditName, containerVisitedList, containerGallery;
    private LinearLayout wreathLeft, wreathRight, containerAchievements;
    private EditText etUsername;
    private Button btnEdit, btnSaveName;

    private ProfileViewModel viewModel;
    private final List<VisitPoint> visitedPoints = new ArrayList<>();
    private List<Integer> lastGalleryIds = null;
    private boolean editMode = false;

    private int ratingVal = 0, quizzesVal = 0, visitedVal = 0;
    private boolean hasProfile = false, hasVisited = false, achievementsSynced = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        tvUsername = findViewById(R.id.tv_username);
        tvEmail = findViewById(R.id.tv_email);
        tvRating = findViewById(R.id.tv_rating);
        tvQuizzesSolved = findViewById(R.id.tv_quizzes_solved);
        tvJoinedDate = findViewById(R.id.tv_joined_date);
        tvVisitedCount = findViewById(R.id.tv_visited_count);
        tvVisitedEmpty = findViewById(R.id.tv_visited_empty);
        tvVisitedHint = findViewById(R.id.tv_visited_hint);
        tvAchievementsCount = findViewById(R.id.tv_achievements_count);
        progressAchievements = findViewById(R.id.progress_achievements);
        wreathLeft = findViewById(R.id.container_wreath_left);
        wreathRight = findViewById(R.id.container_wreath_right);
        containerAchievements = findViewById(R.id.container_achievements);
        cardEditName = findViewById(R.id.card_edit_name);
        containerVisitedList = findViewById(R.id.container_visited_list);
        containerGallery = findViewById(R.id.container_gallery);
        tvGalleryEmpty = findViewById(R.id.tv_gallery_empty);
        etUsername = findViewById(R.id.et_username);
        btnEdit = findViewById(R.id.btn_edit);
        btnSaveName = findViewById(R.id.btn_save_name);
        Button btnLogout = findViewById(R.id.btn_logout);

        currentUserId = getIntent().getIntExtra("userId", -1);
        currentUsername = getIntent().getStringExtra("username");

        observeViewModel();

        if (currentUserId != -1) {
            tvQuizzesSolved.setText(String.valueOf(viewModel.getSolvedCount(currentUserId)));
            renderAchievements(new HashSet<>(viewModel.getCachedAchievementIds(currentUserId)), null);
            viewModel.loadProfile(currentUserId);
            viewModel.loadVisited(currentUserId);
        } else {
            AppToast.show(this, "Не удалось открыть профиль");
        }

        btnEdit.setOnClickListener(v -> setEditMode(!editMode));
        btnSaveName.setOnClickListener(v -> saveUsername());
        btnLogout.setOnClickListener(v -> logout());

        setupBottomNavigation();
        setActiveNavItem("profile");

        notifyAssistantScreenShown("profile");
    }

    private void observeViewModel() {
        viewModel.getProfile().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            if (result.isSuccess() && result.data != null) {
                displayUserProfile(result.data);
            } else {
                AppToast.show(this, result.message);
            }
        });

        viewModel.getVisited().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            if (result.isSuccess()) {
                displayVisitedCountries(result.data);
            } else {
                showVisitedMessage(result.message);
            }
        });

        viewModel.getUsernameUpdate().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            btnSaveName.setEnabled(true);
            if (result.isSuccess() && result.data != null) {
                applyNewUsername(result.data.getUsername());
                setEditMode(false);
                AppToast.show(this, "Имя обновлено");
            } else {
                AppToast.show(this, result.message);
            }
        });

        viewModel.getDeleteResult().observe(this, result -> {
            if (result == null) return;
            if (result.isSuccess() && result.data != null) {
                onVisitDeleted(result.data);
            } else if (result.isError()) {
                renderVisitedRows();
                AppToast.show(this, result.message);
            }
        });

        viewModel.getAchievements().observe(this, result -> {
            if (result == null || !result.isSuccess() || result.data == null) return;
            AchievementSyncResult data = result.data;
            renderAchievements(new HashSet<>(data.allIds), data.newlyUnlocked);
        });

        viewModel.getGallery().observe(this, this::renderGallery);
    }

    private void maybeSyncAchievements() {
        if (achievementsSynced || !hasProfile || !hasVisited || currentUserId == -1) return;
        if (AssistantManager.isTourActive(this)) return;
        achievementsSynced = true;
        viewModel.syncAchievements(currentUserId, quizzesVal, visitedVal, ratingVal);
    }

    private void renderAchievements(Set<Long> unlockedIds, List<Long> newlyUnlocked) {
        List<Achievement> all = AchievementCatalog.all();
        int total = all.size();
        int earned = unlockedIds.size();
        tvAchievementsCount.setText(earned + " / " + total);
        progressAchievements.setMax(total);
        progressAchievements.setProgress(earned);

        List<Achievement> featured = AchievementViews.pickFeatured(
                all, unlockedIds, quizzesVal, visitedVal, ratingVal, 6);
        AchievementViews.populateWreath(this, wreathLeft, wreathRight, featured, unlockedIds,
                this::openAchievement);

        AchievementViews.populateCategorizedGrid(this, containerAchievements, unlockedIds, 4, 60,
                this::openAchievement);

        if (newlyUnlocked != null && !newlyUnlocked.isEmpty()
                && !AssistantManager.isTourActive(this)) {
            AchievementViews.showUnlockedDialog(this, AchievementCatalog.resolve(newlyUnlocked));
        }
    }

    private void openAchievement(Achievement a, boolean unlocked) {
        AchievementViews.showDetailDialog(this, a, unlocked, quizzesVal, visitedVal, ratingVal);
    }

    private void setEditMode(boolean on) {
        editMode = on;
        cardEditName.setVisibility(on ? View.VISIBLE : View.GONE);
        btnEdit.setText(on ? "Готово" : "Редактировать");
        if (on) {
            etUsername.setText(currentUsername == null ? "" : currentUsername);
            etUsername.setSelection(etUsername.getText().length());
        }
        boolean hasVisited = !visitedPoints.isEmpty();
        tvVisitedHint.setVisibility(on && hasVisited ? View.VISIBLE : View.GONE);
        renderVisitedRows();
    }

    private void saveUsername() {
        String newName = etUsername.getText().toString().trim();

        if (newName.isEmpty()) {
            AppToast.show(this, "Введите имя пользователя");
            return;
        }
        if (newName.length() < 3) {
            AppToast.show(this, "Имя слишком короткое (минимум 3 символа)");
            return;
        }
        if (newName.equals(currentUsername)) {
            AppToast.show(this, "Введите новое имя");
            return;
        }

        btnSaveName.setEnabled(false);
        viewModel.saveUsername(currentUserId, newName);
    }

    private void applyNewUsername(String newName) {
        currentUsername = newName;
        tvUsername.setText(newName);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        prefs.edit().putString("username", newName).apply();
    }

    private void logout() {
        getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply();

        AppToast.show(this, "Вы вышли из аккаунта");

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void displayVisitedCountries(List<VisitPoint> points) {
        visitedPoints.clear();
        if (points != null) {
            for (VisitPoint p : points) {
                if (p != null && p.getLabel() != null && !p.getLabel().isEmpty()) {
                    visitedPoints.add(p);
                }
            }
        }

        tvVisitedCount.setText(String.valueOf(visitedPoints.size()));

        if (visitedPoints.isEmpty()) {
            showVisitedMessage("Вы ещё не отметили страны на карте");
            tvVisitedHint.setVisibility(View.GONE);
        } else {
            tvVisitedEmpty.setVisibility(View.GONE);
            tvVisitedHint.setVisibility(editMode ? View.VISIBLE : View.GONE);
        }

        renderVisitedRows();
        maybeLoadGallery();

        visitedVal = visitedPoints.size();
        hasVisited = true;
        maybeSyncAchievements();
    }

    private void showVisitedMessage(String message) {
        tvVisitedEmpty.setText(message);
        tvVisitedEmpty.setVisibility(View.VISIBLE);
    }

    private void renderVisitedRows() {
        containerVisitedList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (VisitPoint point : visitedPoints) {
            View row = inflater.inflate(R.layout.item_visited_country, containerVisitedList, false);
            TextView tvLabel = row.findViewById(R.id.tv_country_label);
            ImageView btnDelete = row.findViewById(R.id.btn_delete_country);

            tvLabel.setText(point.getLabel());
            btnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);
            btnDelete.setOnClickListener(v -> {
                v.setEnabled(false);
                viewModel.deleteVisit(point);
            });

            containerVisitedList.addView(row);
        }
    }

    private void onVisitDeleted(VisitPoint point) {
        visitedPoints.remove(point);
        viewModel.persistVisited(currentUserId, new ArrayList<>(visitedPoints));

        tvVisitedCount.setText(String.valueOf(visitedPoints.size()));
        if (visitedPoints.isEmpty()) {
            showVisitedMessage("Вы ещё не отметили страны на карте");
            tvVisitedHint.setVisibility(View.GONE);
        }
        renderVisitedRows();
        maybeLoadGallery();
        AppToast.show(this, "Страна удалена: " + point.getLabel());
    }

    private void maybeLoadGallery() {
        List<Integer> ids = new ArrayList<>();
        for (VisitPoint p : visitedPoints) ids.add(p.getId());
        if (ids.equals(lastGalleryIds)) return;
        lastGalleryIds = ids;
        if (ids.isEmpty()) {
            renderGallery(new ArrayList<>());
        } else {
            viewModel.loadGallery(new ArrayList<>(visitedPoints));
        }
    }

    private void renderGallery(List<ProfileViewModel.GalleryEntry> entries) {
        containerGallery.removeAllViews();
        if (entries == null || entries.isEmpty()) {
            tvGalleryEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvGalleryEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;
        int thumbSize = (int) (84 * density);
        int gap = (int) (8 * density);
        int maxThumbs = 8;

        for (ProfileViewModel.GalleryEntry entry : entries) {
            View row = inflater.inflate(R.layout.item_gallery_country, containerGallery, false);
            TextView tvCountry = row.findViewById(R.id.tv_gallery_country);
            TextView tvCount = row.findViewById(R.id.tv_gallery_count);
            LinearLayout thumbs = row.findViewById(R.id.gallery_thumbs);

            tvCountry.setText(entry.point.getLabel());
            int n = entry.images.size();
            tvCount.setText(n == 1 ? "1 фото" : n + " фото");

            int shown = Math.min(n, maxThumbs);
            for (int i = 0; i < shown; i++) {
                VisitImage image = entry.images.get(i);
                ShapeableImageView iv = new ShapeableImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(thumbSize, thumbSize);
                if (i > 0) lp.setMarginStart(gap);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setShapeAppearanceModel(iv.getShapeAppearanceModel().toBuilder()
                        .setAllCornerSizes(14 * density)
                        .build());
                iv.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#22BA5C32")));
                iv.setStrokeWidth(density);
                iv.setBackgroundColor(Color.parseColor("#F2E8DA"));

                String url = VisitImageRepository.resolveUrl(entry.point.getId(), image);
                Glide.with(this)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(R.drawable.ic_error_image)
                        .into(iv);

                iv.setOnClickListener(v -> openGallery(entry.point));
                thumbs.addView(iv);
            }

            row.setOnClickListener(v -> openGallery(entry.point));
            containerGallery.addView(row);
        }
    }

    private void openGallery(VisitPoint point) {
        Intent intent = new Intent(this, VisitGalleryActivity.class);
        intent.putExtra(VisitGalleryActivity.EXTRA_VISIT_POINT_ID, point.getId());
        intent.putExtra(VisitGalleryActivity.EXTRA_LABEL, point.getLabel());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!visitedPoints.isEmpty()) {
            lastGalleryIds = null;
            maybeLoadGallery();
        }
    }

    private void displayUserProfile(UserProfile user) {
        tvUsername.setText(user.getUsername());
        tvEmail.setText(user.getEmail());
        tvRating.setText(String.valueOf(user.getRating()));

        int solved = Math.max(user.getQuizzesSolved(), viewModel.getSolvedCount(currentUserId));
        tvQuizzesSolved.setText(String.valueOf(solved));
        tvJoinedDate.setText(formatDate(user.getCreatedAt()));

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            currentUsername = user.getUsername();
        }

        ratingVal = user.getRating();
        quizzesVal = solved;
        hasProfile = true;
        maybeSyncAchievements();
    }

    private String formatDate(String isoDate) {
        try {
            String[] parts = isoDate.split("T");
            String datePart = parts[0];
            String[] dateParts = datePart.split("-");
            return dateParts[2] + "." + dateParts[1] + "." + dateParts[0];
        } catch (Exception e) {
            return isoDate;
        }
    }
}
