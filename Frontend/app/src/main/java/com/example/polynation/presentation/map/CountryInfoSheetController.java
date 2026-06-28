package com.example.polynation.presentation.map;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.polynation.R;
import com.example.polynation.data.remote.dto.CountryDetailsResponse;
import com.example.polynation.domain.CountryNameMapper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.imageview.ShapeableImageView;

public class CountryInfoSheetController {

    public interface Listener {
        void onTakeQuiz(int quizId);
        void onToggleVisited();
        void onManageVisitPhotos();
        void onCloseRequested();
        void onSheetHidden();
        void onSheetShown();
    }

    private final Activity activity;
    private final Listener listener;

    private final LinearLayout countryTitleBadge;
    private final TextView tvCountryBadgeName;
    private final TextView tvCountryBadgeCapital;
    private final LinearLayout bottomSheetInfo;
    private final TextView tvSheetCountryName;
    private final TextView tvInfoCapital;
    private final TextView tvInfoHistory;
    private final TextView tvInfoCulture;
    private final TextView tvInfoMusic;
    private final TextView tvInfoMovies;
    private final TextView tvInfoSports;
    private final ShapeableImageView ivInfoFlag;
    private final LinearLayout galleryContainer;
    private final Button btnTakeQuiz;
    private final Button btnToggleVisited;
    private final Button btnVisitPhotos;

    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    private int pendingQuizId = -1;

    public CountryInfoSheetController(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;

        countryTitleBadge = activity.findViewById(R.id.country_title_badge);
        tvCountryBadgeName = activity.findViewById(R.id.tv_country_badge_name);
        tvCountryBadgeCapital = activity.findViewById(R.id.tv_country_badge_capital);
        bottomSheetInfo = activity.findViewById(R.id.bottom_sheet_info);
        tvSheetCountryName = activity.findViewById(R.id.tv_sheet_country_name);
        tvInfoCapital = activity.findViewById(R.id.tv_info_capital);
        tvInfoHistory = activity.findViewById(R.id.tv_info_history);
        tvInfoCulture = activity.findViewById(R.id.tv_info_culture);
        tvInfoMusic = activity.findViewById(R.id.tv_info_music);
        tvInfoMovies = activity.findViewById(R.id.tv_info_movies);
        tvInfoSports = activity.findViewById(R.id.tv_info_sports);
        ivInfoFlag = activity.findViewById(R.id.iv_info_flag);
        galleryContainer = activity.findViewById(R.id.gallery_container);
        btnTakeQuiz = activity.findViewById(R.id.btn_take_quiz);
        btnToggleVisited = activity.findViewById(R.id.btn_toggle_visited);
        btnVisitPhotos = activity.findViewById(R.id.btn_visit_photos);

        ImageView btnCloseSheet = activity.findViewById(R.id.btn_close_sheet);
        btnCloseSheet.setOnClickListener(v -> listener.onCloseRequested());
        btnToggleVisited.setOnClickListener(v -> listener.onToggleVisited());
        btnVisitPhotos.setOnClickListener(v -> listener.onManageVisitPhotos());
        btnTakeQuiz.setOnClickListener(v -> listener.onTakeQuiz(pendingQuizId));
    }

    public void setup() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetInfo);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        final View bottomNavigationBar = activity.findViewById(R.id.bottom_navigation_layout);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    animateHideBadge();
                    if (bottomNavigationBar != null) bottomNavigationBar.setTranslationY(0);
                    if (galleryContainer != null) galleryContainer.removeAllViews();
                    listener.onSheetHidden();
                } else {
                    listener.onSheetShown();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (bottomNavigationBar != null) {
                    if (slideOffset > 0) {
                        bottomNavigationBar.setTranslationY(bottomNavigationBar.getHeight() * slideOffset);
                        countryTitleBadge.setAlpha(1f - slideOffset);
                    } else {
                        bottomNavigationBar.setTranslationY(0);
                        countryTitleBadge.setAlpha(1.0f);
                    }
                }
            }
        });
    }

    public void resetBeforeLoad(String capital) {
        if (tvInfoCapital != null) tvInfoCapital.setText(capital);
        if (tvInfoHistory != null) tvInfoHistory.setText("Загрузка истории...");
        if (tvInfoCulture != null) tvInfoCulture.setText("Загрузка культуры...");
        if (tvInfoMusic != null) tvInfoMusic.setText("Загрузка...");
        if (tvInfoMovies != null) tvInfoMovies.setText("Загрузка...");
        if (tvInfoSports != null) tvInfoSports.setText("Загрузка...");
        if (ivInfoFlag != null) ivInfoFlag.setImageResource(android.R.drawable.ic_menu_gallery);
        if (galleryContainer != null) galleryContainer.removeAllViews();
        if (btnVisitPhotos != null) btnVisitPhotos.setVisibility(View.GONE);
    }

    public void showCountry(String countryName, String capitalDescription, int quizId) {
        this.pendingQuizId = quizId;

        tvCountryBadgeName.setText(countryName);
        tvCountryBadgeCapital.setText(capitalDescription);
        if (tvSheetCountryName != null) tvSheetCountryName.setText(countryName);

        float density = activity.getResources().getDisplayMetrics().density;

        countryTitleBadge.setVisibility(View.VISIBLE);
        countryTitleBadge.setAlpha(0f);
        countryTitleBadge.setTranslationY(-80 * density);
        countryTitleBadge.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.0f))
                .start();

        final View navBar = activity.findViewById(R.id.bottom_navigation_layout);
        int barHeight = navBar != null ? navBar.getHeight() : 0;
        int peekHeightPx = (int) (300 * density) + barHeight;

        bottomSheetBehavior.setPeekHeight(peekHeightPx);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void hide() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        animateHideBadge();
    }

    private void animateHideBadge() {
        if (countryTitleBadge.getVisibility() == View.VISIBLE) {
            float density = activity.getResources().getDisplayMetrics().density;
            countryTitleBadge.animate()
                    .alpha(0f)
                    .translationY(-80 * density)
                    .setDuration(350)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .withEndAction(() -> countryTitleBadge.setVisibility(View.GONE))
                    .start();
        }
    }

    public void bindDetails(CountryDetailsResponse.CountryDetails details) {
        if (tvInfoHistory != null) tvInfoHistory.setText(details.getHistoryInfo());
        if (tvInfoCulture != null) tvInfoCulture.setText(details.getCultureInfo());
        if (tvInfoMusic != null) tvInfoMusic.setText(details.getMusicInfo());
        if (tvInfoMovies != null) tvInfoMovies.setText(details.getMoviesInfo());
        if (tvInfoSports != null) tvInfoSports.setText(details.getSportsInfo());
        if (tvInfoCapital != null) tvInfoCapital.setText(details.getCapital());

        if (ivInfoFlag != null) {
            String flagUrl = CountryNameMapper.getFlagUrlByRussianName(details.getName());
            if (flagUrl == null || flagUrl.isEmpty()) {
                flagUrl = details.getFlagUrl();
            }
            if (flagUrl != null && !flagUrl.isEmpty()) {
                Glide.with(activity)
                        .load(flagUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(ivInfoFlag);
            } else {
                ivInfoFlag.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        if (galleryContainer != null) {
            galleryContainer.removeAllViews();
            if (details.getPhotos() != null && !details.getPhotos().isEmpty()) {
                for (String photoUrl : details.getPhotos()) {
                    if (photoUrl == null || photoUrl.isEmpty()) continue;
                    addPhotoToGallery(photoUrl);
                }
            } else {
                showNoPhotosMessage();
            }
        }
    }

    public void updateVisitedButton(boolean visited) {
        if (btnToggleVisited == null) return;
        btnToggleVisited.setEnabled(true);
        if (visited) {
            btnToggleVisited.setText("Вы здесь были — убрать отметку");
            btnToggleVisited.setBackgroundResource(R.drawable.btn_outline_orange_dark);
        } else {
            btnToggleVisited.setText("Отметить, что я здесь был");
            btnToggleVisited.setBackgroundResource(R.drawable.btn_primary_orange);
        }
        btnToggleVisited.setTextColor(Color.parseColor("#FEF9E7"));
    }

    public void setVisitedButtonEnabled(boolean enabled) {
        if (btnToggleVisited != null) btnToggleVisited.setEnabled(enabled);
    }

    public void showVisitPhotos(boolean visible, int count) {
        if (btnVisitPhotos == null) return;
        if (!visible) {
            btnVisitPhotos.setVisibility(View.GONE);
            return;
        }
        btnVisitPhotos.setVisibility(View.VISIBLE);
        if (count < 0) {
            btnVisitPhotos.setText("Фотографии места");
        } else if (count == 0) {
            btnVisitPhotos.setText("Добавить фотки места");
        } else {
            btnVisitPhotos.setText("Фотки добавлены: " + count + " — открыть");
        }
    }

    private void addPhotoToGallery(String imageUrl) {
        float density = activity.getResources().getDisplayMetrics().density;

        ShapeableImageView ivPhoto = new ShapeableImageView(activity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (200 * density));
        lp.setMargins(0, 0, 0, (int) (14 * density));
        ivPhoto.setLayoutParams(lp);
        ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivPhoto.setShapeAppearanceModel(ivPhoto.getShapeAppearanceModel().toBuilder()
                .setAllCornerSizes(18 * density)
                .build());
        ivPhoto.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#22BA5C32")));
        ivPhoto.setStrokeWidth(density);
        ivPhoto.setBackgroundColor(Color.parseColor("#F2E8DA"));
        galleryContainer.addView(ivPhoto);

        Glide.with(activity)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivPhoto);
    }

    private void showNoPhotosMessage() {
        TextView tv = new TextView(activity);
        tv.setText("Фотографии скоро будут добавлены");
        tv.setTextColor(Color.parseColor("#BA5C32"));
        tv.setPadding(0, 20, 0, 20);
        tv.setTextSize(14);
        galleryContainer.addView(tv);
    }
}
