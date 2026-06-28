package com.example.polynation.presentation.map;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.data.remote.dto.QuizzesResponse;
import com.example.polynation.data.remote.dto.VisitPoint;
import com.example.polynation.data.repository.QuizRepository;
import com.example.polynation.domain.BackgroundCacheLoader;
import com.example.polynation.domain.CountryNameMapper;
import com.example.polynation.domain.geo.GeoJsonBoundaryProvider;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.assistant.AssistantManager;
import com.example.polynation.presentation.assistant.PolyPromptDialog;
import com.example.polynation.presentation.assistant.PolyRoamerOverlay;
import com.example.polynation.presentation.common.BaseNavigationActivity;
import com.example.polynation.presentation.gallery.VisitGalleryActivity;
import com.example.polynation.presentation.quiz.QuizQuestionActivity;
import com.example.polynation.util.AppToast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends BaseNavigationActivity
        implements CountryInfoSheetController.Listener {

    private MapView mapView;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabVisitedFilter;

    private MapViewModel viewModel;
    private QuizRepository quizRepository;

    private CountryMarkerManager markerManager;
    private CountryBoundaryRenderer boundaryRenderer;
    private MapLocationController locationController;
    private CountryInfoSheetController infoSheet;
    private TicketsSectionController ticketsController;
    private CountrySearchController searchController;

    private final Map<String, VisitPoint> visitedByName = new HashMap<>();
    private CountriesResponse.Country selectedCountry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String username = getIntent().getStringExtra("username");
        boolean fromAuth = getIntent().getBooleanExtra("from_auth", false);

        BackgroundCacheLoader.getInstance(this).start();

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fab_my_location);
        fabVisitedFilter = findViewById(R.id.fab_visited_filter);

        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        quizRepository = new QuizRepository(this);

        infoSheet = new CountryInfoSheetController(this, this);
        infoSheet.setup();

        boundaryRenderer = new CountryBoundaryRenderer(mapView, GeoJsonBoundaryProvider.getInstance(this));
        boundaryRenderer.init();

        locationController = new MapLocationController(this, mapView, this::hideCountryUI);
        ticketsController = new TicketsSectionController(this, findViewById(android.R.id.content));
        markerManager = new CountryMarkerManager(mapView, this, this::selectCountry);
        searchController = new CountrySearchController(this, mapView, this::selectCountry);
        searchController.setup();

        setupMap();
        setupBottomNavigation();
        setActiveNavItem("map");

        fabMyLocation.setOnClickListener(v -> locationController.onMyLocationClick());
        fabVisitedFilter.setOnClickListener(v -> toggleVisitedFilter());

        observeViewModel();
        viewModel.loadCountries();
        viewModel.loadVisitPoints(currentUserId);

        locationController.checkLocationPermissions();

        AssistantManager.onMapEntered(this, username, fromAuth);
    }

    private void observeViewModel() {
        viewModel.getCountries().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            if (result.isSuccess() && result.data != null) {
                markerManager.setCountries(result.data);
                searchController.setCountries(result.data);
            } else {
                AppToast.show(this, result.message);
            }
        });

        viewModel.getDetails().observe(this, result -> {
            if (result == null || result.isLoading()) return;
            if (result.isSuccess() && result.data != null) {
                infoSheet.bindDetails(result.data);
            } else if (result.isError()) {
                AppToast.show(this, result.message);
            }
        });

        viewModel.getVisitPoints().observe(this, this::applyVisitPoints);
        viewModel.getAddResult().observe(this, this::onAddVisitResult);
        viewModel.getRemoveResult().observe(this, this::onRemoveVisitResult);
        viewModel.getPhotoStatus().observe(this, this::onPhotoStatus);
    }

    private void onPhotoStatus(MapViewModel.PhotoStatus status) {
        if (status == null || selectedCountry == null) return;
        VisitPoint point = visitedPointFor(selectedCountry.getName());
        if (point != null && point.getId() == status.visitPointId) {
            infoSheet.showVisitPhotos(true, status.count);
        }
    }

    private void setupMap() {
        try { mapView.getTileProvider().clearTileCache(); } catch (Exception ignored) {}

        XYTileSource mutedGrayTiles = new XYTileSource(
                "CartoMutedGray", 0, 18, 256, ".png",
                new String[]{
                        "https://a.basemaps.cartocdn.com/light_all/",
                        "https://b.basemaps.cartocdn.com/light_all/",
                        "https://c.basemaps.cartocdn.com/light_all/"
                },
                "© OpenStreetMap contributors"
        );

        mapView.setTileSource(mutedGrayTiles);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.setMinZoomLevel(4.0);
        mapView.setMaxZoomLevel(18.0);
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setVerticalMapRepetitionEnabled(false);

        try {
            mapView.setScrollableAreaLimitDouble(new BoundingBox(85.0, 180.0, -85.0, -180.0));
        } catch (Exception ignored) {}

        mapView.getController().setZoom(4.0);
        mapView.getController().setCenter(new GeoPoint(30.0, 10.0));

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(false);
        scaleBarOverlay.setScaleBarOffset(50, 50);
        mapView.getOverlays().add(scaleBarOverlay);
    }

    private void selectCountry(CountriesResponse.Country country) {
        if (country == null) return;
        List<Double> latlng = country.getCapitalInfoLatlng();
        if (latlng == null || latlng.size() < 2) {
            AppToast.show(this, "Нет координат для этой страны");
            return;
        }
        selectedCountry = country;
        double lat = latlng.get(0);
        double lng = latlng.get(1);
        String russianName = country.getName();
        String capital = country.getCapital();

        String englishName = (country.getEnglishName() != null && !country.getEnglishName().isEmpty())
                ? country.getEnglishName()
                : CountryNameMapper.getNameForExternal(russianName);

        GeoPoint offsetPoint = new GeoPoint(lat - 3.5, lng);
        mapView.getController().animateTo(offsetPoint, 5.5, 800L);

        infoSheet.resetBeforeLoad(capital);
        ticketsController.reset();
        infoSheet.showCountry(russianName, "Столица: " + capital, resolveQuizId(russianName));
        infoSheet.updateVisitedButton(isVisited(russianName));
        updatePhotoButtonFor(russianName);

        boundaryRenderer.draw(russianName, englishName);

        viewModel.loadDetails(russianName, englishName);

        GeoPoint myLocation = locationController.getMyLocation();
        Double originLat = (myLocation != null) ? myLocation.getLatitude() : null;
        Double originLng = (myLocation != null) ? myLocation.getLongitude() : null;
        ticketsController.load(originLat, originLng, lat, lng, capital);

        AssistantManager.reactToCountryOpened(this, russianName);
    }

    private int resolveQuizId(String countryName) {
        List<QuizzesResponse.QuizItem> quizzes = quizRepository.getCachedQuizzes();
        if (quizzes == null) return -1;

        for (QuizzesResponse.QuizItem quiz : quizzes) {
            if (quiz.getCountryName() != null && quiz.getCountryName().equalsIgnoreCase(countryName)) {
                return quiz.getId();
            }
        }
        for (QuizzesResponse.QuizItem quiz : quizzes) {
            if (quiz.getCountryName() == null && "MIXED".equals(quiz.getType())) {
                return quiz.getId();
            }
        }
        return -1;
    }

    private void hideCountryUI() {
        infoSheet.hide();
        boundaryRenderer.clear();
        mapView.invalidate();
    }

    @Override
    public void onTakeQuiz(int quizId) {
        if (quizId != -1 && currentUserId != -1 && currentUsername != null) {
            Intent intent = new Intent(this, QuizQuestionActivity.class);
            intent.putExtra("quizId", quizId);
            intent.putExtra("userId", currentUserId);
            intent.putExtra("username", currentUsername);
            startActivity(intent);
        } else {
            AppToast.show(this, "Квиз временно недоступен");
        }
    }

    @Override
    public void onToggleVisited() {
        if (selectedCountry == null) return;
        if (currentUserId == -1) {
            AppToast.show(this, "Войдите в аккаунт, чтобы отмечать страны");
            return;
        }
        if (isVisited(selectedCountry.getName())) {
            removeVisit(selectedCountry);
        } else {
            addVisit(selectedCountry);
        }
    }

    @Override
    public void onManageVisitPhotos() {
        if (selectedCountry == null) return;
        VisitPoint point = visitedPointFor(selectedCountry.getName());
        if (point == null) {
            AppToast.show(this, "Сначала отметьте, что вы здесь были");
            return;
        }
        openGallery(point);
    }

    @Override
    public void onCloseRequested() {
        hideCountryUI();
    }

    @Override
    public void onSheetHidden() {
        PolyRoamerOverlay.showOn(this);
        ticketsController.hide();
    }

    @Override
    public void onSheetShown() {
        PolyRoamerOverlay.hideOn(this);
    }

    private void applyVisitPoints(List<VisitPoint> points) {
        visitedByName.clear();
        if (points != null) {
            for (VisitPoint p : points) {
                if (p != null && p.getLabel() != null) {
                    visitedByName.put(CountryMarkerManager.visitKey(p.getLabel()), p);
                }
            }
        }
        markerManager.updateVisited(visitedKeys());
        if (selectedCountry != null) infoSheet.updateVisitedButton(isVisited(selectedCountry.getName()));
    }

    private void addVisit(CountriesResponse.Country country) {
        List<Double> latlng = country.getCapitalInfoLatlng();
        if (latlng == null || latlng.size() < 2) return;
        infoSheet.setVisitedButtonEnabled(false);
        viewModel.addVisit(currentUserId, latlng.get(0), latlng.get(1), country.getName());
    }

    private void removeVisit(CountriesResponse.Country country) {
        VisitPoint point = visitedByName.get(CountryMarkerManager.visitKey(country.getName()));
        if (point == null) return;
        infoSheet.setVisitedButtonEnabled(false);
        viewModel.removeVisit(point);
    }

    private void onAddVisitResult(Resource<VisitPoint> result) {
        if (result == null) return;
        if (result.isSuccess() && result.data != null) {
            VisitPoint added = result.data;
            visitedByName.put(CountryMarkerManager.visitKey(added.getLabel()), added);
            persistVisited();
            markerManager.updateVisited(visitedKeys());
            if (selectedCountry != null && isVisited(selectedCountry.getName())) {
                infoSheet.updateVisitedButton(true);
                infoSheet.showVisitPhotos(true, 0);
            }
            promptAddPhotos(added);
        } else if (result.isError()) {
            infoSheet.setVisitedButtonEnabled(true);
            AppToast.show(this, result.message);
        }
    }

    private void onRemoveVisitResult(Resource<String> result) {
        if (result == null) return;
        if (result.isSuccess() && result.data != null) {
            String label = result.data;
            visitedByName.remove(CountryMarkerManager.visitKey(label));
            persistVisited();
            markerManager.updateVisited(visitedKeys());
            if (selectedCountry != null) {
                infoSheet.updateVisitedButton(isVisited(selectedCountry.getName()));
                infoSheet.showVisitPhotos(false, 0);
            }
            AppToast.show(this, "Отметка убрана");
        } else if (result.isError()) {
            infoSheet.setVisitedButtonEnabled(true);
            AppToast.show(this, result.message);
        }
    }

    private void updatePhotoButtonFor(String russianName) {
        VisitPoint point = visitedPointFor(russianName);
        if (point != null) {
            infoSheet.showVisitPhotos(true, -1);
            viewModel.loadPhotoStatus(point.getId());
        } else {
            infoSheet.showVisitPhotos(false, 0);
        }
    }

    private void promptAddPhotos(VisitPoint point) {
        String label = point.getLabel();
        String safe = (label == null || label.trim().isEmpty()) ? "это место" : label.trim();
        PolyPromptDialog.show(this,
                "Ура, новое место!",
                "Здорово, что ты побывал в стране «" + safe + "»! Хочешь добавить фотографии этого места, "
                        + "чтобы сохранить воспоминания?",
                "Да, добавить фото",
                "Позже",
                () -> openGallery(point));
    }

    private void openGallery(VisitPoint point) {
        Intent intent = new Intent(this, VisitGalleryActivity.class);
        intent.putExtra(VisitGalleryActivity.EXTRA_VISIT_POINT_ID, point.getId());
        intent.putExtra(VisitGalleryActivity.EXTRA_LABEL, point.getLabel());
        startActivity(intent);
    }

    private VisitPoint visitedPointFor(String russianName) {
        return visitedByName.get(CountryMarkerManager.visitKey(russianName));
    }

    private void toggleVisitedFilter() {
        if (!mapView.isEnabled()) return;
        boolean now = !markerManager.isShowOnlyVisited();
        markerManager.setShowOnlyVisited(now);

        fabVisitedFilter.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(now ? "#16A34A" : "#FFFFFF")));
        fabVisitedFilter.setColorFilter(now ? Color.WHITE : Color.parseColor("#FF7A45"));

        if (now) {
            int n = visitedByName.size();
            AppToast.show(this, n == 0
                    ? "Вы ещё не отметили ни одной страны"
                    : "Показаны ваши страны: " + n);
        } else {
            AppToast.show(this, "Показаны все страны");
        }
    }

    private void persistVisited() {
        viewModel.persistVisited(currentUserId, new java.util.ArrayList<>(visitedByName.values()));
    }

    private boolean isVisited(String russianName) {
        return visitedByName.containsKey(CountryMarkerManager.visitKey(russianName));
    }

    private Set<String> visitedKeys() {
        return new HashSet<>(visitedByName.keySet());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationController.onRequestPermissionsResult(requestCode, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (locationController != null) locationController.onResume();
        if (selectedCountry != null) updatePhotoButtonFor(selectedCountry.getName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (locationController != null) locationController.onPause();
    }
}
