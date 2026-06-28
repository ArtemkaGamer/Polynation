package com.example.polynation;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends BaseNavigationActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "MapActivity";

    private MapView mapView;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabSearch;
    private FloatingActionButton fabVisitedFilter;
    private LinearLayout searchBar;
    private android.widget.EditText etSearch;
    private ImageButton btnSearchClear;
    private LinearLayout tooltip;
    private Button btnCloseTooltip;
    private TextView tvWelcomeTooltip;
    private LinearLayout countryTitleBadge;
    private TextView tvCountryBadgeName;
    private TextView tvCountryBadgeCapital;
    private LinearLayout bottomSheetInfo;
    private TextView tvSheetCountryName;
    private ImageButton btnCloseSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    private TextView tvInfoCapital;
    private TextView tvInfoHistory;
    private TextView tvInfoCulture;
    private TextView tvInfoMusic;
    private TextView tvInfoMovies;
    private TextView tvInfoSports;
    private ShapeableImageView ivInfoFlag;
    private LinearLayout galleryContainer;
    private Button btnTakeQuiz;
    private Button btnToggleVisited;

    private LinearLayout ticketsCard;
    private LinearLayout ticketsContainer;
    private LinearLayout ticketsStatusPanel;
    private ImageView ivTicketsStatusIcon;
    private TextView tvTicketsSubtitle;
    private TextView tvTicketsStatus;
    private FlightTicketsManager flightTicketsManager;

    private int ticketsRequestId = 0;

    private MyLocationNewOverlay locationOverlay;

    private List<CountriesResponse.Country> loadedCountries;
    private final List<Polygon> currentPolygons = new ArrayList<>();
    private final Map<String, List<List<GeoPoint>>> boundaryCache = new ConcurrentHashMap<>();

    private final List<Marker> countryMarkers = new ArrayList<>();
    private final Map<String, Marker> markerByName = new HashMap<>();

    private final Map<String, VisitPoint> visitedByName = new HashMap<>();

    private boolean showOnlyVisited = false;

    private CountriesResponse.Country selectedCountry;

    private volatile boolean boundariesReady = false;
    private String pendingBoundaryRussianName;
    private String pendingBoundaryEnglishName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String username = getIntent().getStringExtra("username");
        int userId = getIntent().getIntExtra("userId", -1);
        currentUsername = username;
        currentUserId = userId;

        BackgroundCacheLoader.getInstance(this).start();

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        initViews();
        setupBottomSheet();
        setupTooltip(username);
        setupMap();
        checkLocationPermissions();
        setupBottomNavigation();
        setActiveNavItem("map");

        fabMyLocation.setOnClickListener(v -> onMyLocationClick());
        btnCloseSheet.setOnClickListener(v -> hideCountryUI());
        setupSearch();

        fabVisitedFilter.setOnClickListener(v -> toggleVisitedFilter());
        btnToggleVisited.setOnClickListener(v -> onToggleVisitedClick());
        loadVisitPoints();
    }

    private void setupSearch() {
        fabSearch.setOnClickListener(v -> toggleSearchBar());
        btnSearchClear.setOnClickListener(v -> {
            if (etSearch.getText().length() > 0) {
                etSearch.setText("");
            } else {
                hideSearchBar();
            }
        });
        etSearch.setOnEditorActionListener((tv, actionId, event) -> {
            performSearch(etSearch.getText().toString());
            return true;
        });
    }

    private void toggleSearchBar() {
        if (searchBar.getVisibility() == View.VISIBLE) {
            hideSearchBar();
        } else {
            showSearchBar();
        }
    }

    private void showSearchBar() {
        if (!mapView.isEnabled()) return;
        searchBar.setVisibility(View.VISIBLE);
        searchBar.setAlpha(0f);
        searchBar.setTranslationY(-30f);
        searchBar.animate().alpha(1f).translationY(0f).setDuration(220).start();
        etSearch.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearchBar() {
        hideKeyboard();
        searchBar.animate().alpha(0f).translationY(-30f).setDuration(180)
                .withEndAction(() -> searchBar.setVisibility(View.GONE)).start();
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void performSearch(String rawQuery) {
        if (rawQuery == null) return;
        String query = rawQuery.trim();
        if (query.isEmpty()) return;

        if (loadedCountries == null || loadedCountries.isEmpty()) {
            AppToast.show(this, "Данные ещё загружаются, попробуйте позже");
            return;
        }

        CountriesResponse.Country match = findCountryByQuery(query);
        if (match == null) {
            AppToast.show(this, "Страна или столица не найдена");
            return;
        }
        hideSearchBar();
        selectCountry(match);
    }

    private CountriesResponse.Country findCountryByQuery(String query) {
        String q = query.toLowerCase().trim();

        CountriesResponse.Country startsWith = null;
        CountriesResponse.Country contains = null;

        for (CountriesResponse.Country c : loadedCountries) {
            String name = c.getName() != null ? c.getName().toLowerCase().trim() : "";
            String capital = c.getCapital() != null ? c.getCapital().toLowerCase().trim() : "";
            String engName = c.getEnglishName() != null ? c.getEnglishName().toLowerCase().trim() : "";

            if (name.equals(q) || capital.equals(q) || engName.equals(q)) {
                return c;
            }
            if (startsWith == null
                    && (name.startsWith(q) || capital.startsWith(q) || engName.startsWith(q))) {
                startsWith = c;
            }
            if (contains == null
                    && (name.contains(q) || capital.contains(q) || engName.contains(q))) {
                contains = c;
            }
        }
        return startsWith != null ? startsWith : contains;
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
                : Countrynamemapper.getNameForExternal(russianName);

        GeoPoint offsetPoint = new GeoPoint(lat - 3.5, lng);
        mapView.getController().animateTo(offsetPoint, 5.5, 800L);

        resetBottomSheetBeforeLoad(russianName, capital);
        showCountryUI(russianName, "Столица: " + capital);
        updateVisitedButton(country);

        drawLocalCountryBoundaryFromCache(russianName, englishName);

        CountryDetailsResponse.CountryDetails cachedDetails =
                DataCache.getCountryDetails(MapActivity.this, russianName);
        boolean stale = DataCache.isCountryDetailsStale(MapActivity.this, russianName);
        if (cachedDetails != null) {
            updateBottomSheetWithFullDetails(cachedDetails);
            if (stale) {
                fetchDetailedCountryInfo(russianName, englishName, true);
            }
        } else {
            BackgroundCacheLoader.getInstance(this).prioritize(russianName);
            fetchDetailedCountryInfo(russianName, englishName);
        }

        loadTicketsForCountry(lat, lng, capital);
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fab_my_location);
        tooltip = findViewById(R.id.tooltip);
        btnCloseTooltip = findViewById(R.id.btn_close_tooltip);
        tvWelcomeTooltip = findViewById(R.id.tv_welcome_tooltip);
        countryTitleBadge = findViewById(R.id.country_title_badge);
        tvCountryBadgeName = findViewById(R.id.tv_country_badge_name);
        tvCountryBadgeCapital = findViewById(R.id.tv_country_badge_capital);
        bottomSheetInfo = findViewById(R.id.bottom_sheet_info);
        tvSheetCountryName = findViewById(R.id.tv_sheet_country_name);
        btnCloseSheet = findViewById(R.id.btn_close_sheet);

        tvInfoCapital = findViewById(R.id.tv_info_capital);
        tvInfoHistory = findViewById(R.id.tv_info_history);
        tvInfoCulture = findViewById(R.id.tv_info_culture);
        tvInfoMusic = findViewById(R.id.tv_info_music);
        tvInfoMovies = findViewById(R.id.tv_info_movies);
        tvInfoSports = findViewById(R.id.tv_info_sports);
        ivInfoFlag = findViewById(R.id.iv_info_flag);
        galleryContainer = findViewById(R.id.gallery_container);
        btnTakeQuiz = findViewById(R.id.btn_take_quiz);
        btnToggleVisited = findViewById(R.id.btn_toggle_visited);

        ticketsCard = findViewById(R.id.tickets_card);
        ticketsContainer = findViewById(R.id.tickets_container);
        ticketsStatusPanel = findViewById(R.id.tickets_status_panel);
        ivTicketsStatusIcon = findViewById(R.id.iv_tickets_status_icon);
        tvTicketsSubtitle = findViewById(R.id.tv_tickets_subtitle);
        tvTicketsStatus = findViewById(R.id.tv_tickets_status);
        flightTicketsManager = new FlightTicketsManager(this);

        fabSearch = findViewById(R.id.fab_search);
        fabVisitedFilter = findViewById(R.id.fab_visited_filter);
        searchBar = findViewById(R.id.search_bar);
        etSearch = findViewById(R.id.et_search);
        btnSearchClear = findViewById(R.id.btn_search_clear);
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetInfo);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        final View bottomNavigationBar = findViewById(R.id.bottom_navigation_layout);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    animateHideCountryBadge();
                    if (bottomNavigationBar != null) bottomNavigationBar.setTranslationY(0);
                    if (galleryContainer != null) galleryContainer.removeAllViews();
                    hideTicketsCard();
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

    private void setupTooltip(String username) {
        if (username != null && !username.isEmpty()) {
            tvWelcomeTooltip.setText("Добро пожаловать, " + username + "!");
        } else {
            tvWelcomeTooltip.setText("Добро пожаловать!");
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean tooltipShown = prefs.getBoolean("tooltip_shown", false);

        if (!tooltipShown) {
            tooltip.setVisibility(View.VISIBLE);
            mapView.setEnabled(false);
            mapView.setClickable(false);
            btnCloseTooltip.setOnClickListener(v -> {
                tooltip.setVisibility(View.GONE);
                mapView.setEnabled(true);
                mapView.setClickable(true);
                prefs.edit().putBoolean("tooltip_shown", true).apply();
            });
        } else {
            tooltip.setVisibility(View.GONE);
            mapView.setEnabled(true);
            mapView.setClickable(true);
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

        preloadGeoJsonBoundaries();

        if (!loadCountriesFromCache()) {
            loadCountriesFromServer();
        }

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(false);
        scaleBarOverlay.setScaleBarOffset(50, 50);
        mapView.getOverlays().add(scaleBarOverlay);
    }

    private void preloadGeoJsonBoundaries() {
        new Thread(() -> {
            try {
                InputStream is = getAssets().open("countries.geojson");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] chunk = new byte[16384];
                int read;
                while ((read = is.read(chunk)) != -1) {
                    baos.write(chunk, 0, read);
                }
                is.close();

                String jsonString = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                JSONObject geoJson = new JSONObject(jsonString);
                JSONArray features = geoJson.getJSONArray("features");

                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.getJSONObject("properties");

                    String iso3 = properties.optString("ISO3166-1-Alpha-3",
                            properties.optString("iso_a3",
                                    properties.optString("ISO_A3", ""))).toUpperCase().trim();

                    String nameKey = properties.optString("name",
                            properties.optString("NAME",
                                    properties.optString("admin", ""))).toLowerCase().trim();

                    JSONObject geometry = feature.getJSONObject("geometry");
                    String geometryType = geometry.getString("type");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    List<List<GeoPoint>> polygonsList = new ArrayList<>();
                    if (geometryType.equals("Polygon")) {
                        parseGeoJsonRing(coordinates, polygonsList);
                    } else if (geometryType.equals("MultiPolygon")) {
                        for (int j = 0; j < coordinates.length(); j++) {
                            parseGeoJsonRing(coordinates.getJSONArray(j), polygonsList);
                        }
                    }

                    if (!polygonsList.isEmpty()) {
                        if (!iso3.isEmpty() && !iso3.equals("-99") && !iso3.equals("-1")) {
                            boundaryCache.put(iso3, polygonsList);
                        }
                        if (!nameKey.isEmpty()) {
                            boundaryCache.put(nameKey, polygonsList);
                        }
                    }
                }
                Log.d(TAG, "GeoJSON загружен: " + boundaryCache.size() + " записей");

                runOnUiThread(() -> {
                    boundariesReady = true;

                    if (pendingBoundaryRussianName != null) {
                        drawLocalCountryBoundaryFromCache(pendingBoundaryRussianName, pendingBoundaryEnglishName);
                        pendingBoundaryRussianName = null;
                        pendingBoundaryEnglishName = null;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseGeoJsonRing(JSONArray polygonCoords, List<List<GeoPoint>> outList) throws Exception {
        JSONArray outerRingCoords = polygonCoords.getJSONArray(0);
        List<GeoPoint> points = new ArrayList<>();
        for (int k = 0; k < outerRingCoords.length(); k++) {
            JSONArray coord = outerRingCoords.getJSONArray(k);
            points.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
        }
        if (!points.isEmpty()) outList.add(points);
    }

    private boolean loadCountriesFromCache() {
        List<CountriesResponse.Country> cachedCountries = DataCache.getCountriesList(this);
        if (cachedCountries != null && !cachedCountries.isEmpty()) {
            addServerCountryMarkers(cachedCountries);
            return true;
        }
        return false;
    }

    private void loadCountriesFromServer() {
        ApiClient.getApiService().getAllCountries().enqueue(new Callback<CountriesResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountriesResponse> call, @NonNull Response<CountriesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CountriesResponse.Country> countries = response.body().getData();
                    DataCache.saveCountriesList(MapActivity.this, countries);
                    addServerCountryMarkers(countries);
                } else {
                    AppToast.show(MapActivity.this, "Не удалось загрузить маркеры стран");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CountriesResponse> call, @NonNull Throwable t) {
                AppToast.show(MapActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void addServerCountryMarkers(List<CountriesResponse.Country> countriesList) {
        if (countriesList == null) return;
        loadedCountries = countriesList;

        for (CountriesResponse.Country country : countriesList) {
            String russianName = country.getName();
            String capital = country.getCapital();
            List<Double> latlng = country.getCapitalInfoLatlng();

            if (latlng == null || latlng.size() < 2) continue;
            double lat = latlng.get(0);
            double lng = latlng.get(1);

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lng));
            marker.setTitle(russianName);
            marker.setSnippet("Столица: " + capital);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_custom_marker));

            marker.setOnMarkerClickListener((m, view) -> {
                if (!mapView.isEnabled()) return false;
                selectCountry(country);
                return true;
            });

            mapView.getOverlays().add(marker);
            countryMarkers.add(marker);
            if (russianName != null) markerByName.put(russianName.toLowerCase().trim(), marker);
        }

        refreshAllMarkerIcons();
        if (showOnlyVisited) applyMarkerFilter();
        mapView.invalidate();
    }

    private String visitKey(String russianName) {
        return russianName == null ? "" : russianName.toLowerCase().trim();
    }

    private boolean isVisited(String russianName) {
        return visitedByName.containsKey(visitKey(russianName));
    }

    private void loadVisitPoints() {
        if (currentUserId == -1) return;

        List<VisitPoint> cached = DataCache.getVisitPoints(this, currentUserId);
        if (cached != null) {
            applyVisitPoints(cached);
        }

        ApiClient.getApiService().getVisitPoints(currentUserId).enqueue(new Callback<VisitPointsResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitPointsResponse> call,
                                   @NonNull Response<VisitPointsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<VisitPoint> points = response.body().getData();
                    DataCache.saveVisitPoints(MapActivity.this, currentUserId, points);
                    applyVisitPoints(points);
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitPointsResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "Не удалось загрузить точки визита: " + t.getMessage());
            }
        });
    }

    private void applyVisitPoints(List<VisitPoint> points) {
        visitedByName.clear();
        if (points != null) {
            for (VisitPoint p : points) {
                if (p != null && p.getLabel() != null) {
                    visitedByName.put(visitKey(p.getLabel()), p);
                }
            }
        }
        refreshAllMarkerIcons();
        if (showOnlyVisited) applyMarkerFilter();
        if (selectedCountry != null) updateVisitedButton(selectedCountry);
    }

    private void refreshAllMarkerIcons() {
        for (Map.Entry<String, Marker> e : markerByName.entrySet()) {
            boolean visited = visitedByName.containsKey(e.getKey());
            e.getValue().setIcon(ContextCompat.getDrawable(this,
                    visited ? R.drawable.ic_marker_visited : R.drawable.ic_custom_marker));
        }
        mapView.invalidate();
    }

    private void refreshMarkerIcon(String russianName) {
        Marker m = markerByName.get(visitKey(russianName));
        if (m != null) {
            m.setIcon(ContextCompat.getDrawable(this,
                    isVisited(russianName) ? R.drawable.ic_marker_visited : R.drawable.ic_custom_marker));
            mapView.invalidate();
        }
    }

    private void applyMarkerFilter() {
        for (Marker m : countryMarkers) {
            boolean visited = visitedByName.containsKey(visitKey(m.getTitle()));
            boolean shouldShow = !showOnlyVisited || visited;
            boolean present = mapView.getOverlays().contains(m);
            if (shouldShow && !present) {
                mapView.getOverlays().add(m);
            } else if (!shouldShow && present) {
                mapView.getOverlays().remove(m);
            }
        }
        mapView.invalidate();
    }

    private void toggleVisitedFilter() {
        if (!mapView.isEnabled()) return;
        showOnlyVisited = !showOnlyVisited;

        fabVisitedFilter.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(showOnlyVisited ? "#16A34A" : "#FFFFFF")));
        fabVisitedFilter.setColorFilter(
                showOnlyVisited ? Color.WHITE : Color.parseColor("#FF7A45"));

        applyMarkerFilter();

        if (showOnlyVisited) {
            int n = visitedByName.size();
            AppToast.show(this, n == 0
                    ? "Вы ещё не отметили ни одной страны"
                    : "Показаны ваши страны: " + n);
        } else {
            AppToast.show(this, "Показаны все страны");
        }
    }

    private void updateVisitedButton(CountriesResponse.Country country) {
        if (btnToggleVisited == null || country == null) return;
        btnToggleVisited.setEnabled(true);
        if (isVisited(country.getName())) {
            btnToggleVisited.setText("Вы здесь были — убрать отметку");
            btnToggleVisited.setBackgroundResource(R.drawable.btn_outline_orange_dark);
            btnToggleVisited.setTextColor(Color.parseColor("#FEF9E7"));
        } else {
            btnToggleVisited.setText("Отметить, что я здесь был");
            btnToggleVisited.setBackgroundResource(R.drawable.btn_primary_orange);
            btnToggleVisited.setTextColor(Color.parseColor("#FEF9E7"));
        }
    }

    private void onToggleVisitedClick() {
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

    private void addVisit(CountriesResponse.Country country) {
        List<Double> latlng = country.getCapitalInfoLatlng();
        if (latlng == null || latlng.size() < 2) return;
        double lat = latlng.get(0);
        double lon = latlng.get(1);
        final String label = country.getName();

        btnToggleVisited.setEnabled(false);
        VisitPointRequest request = new VisitPointRequest(currentUserId, lat, lon, label);

        ApiClient.getApiService().addVisitPoint(request).enqueue(new Callback<VisitPointResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitPointResponse> call,
                                   @NonNull Response<VisitPointResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    visitedByName.put(visitKey(label), response.body().getData());
                    persistVisitedCache();
                    refreshMarkerIcon(label);
                    if (showOnlyVisited) applyMarkerFilter();
                    if (selectedCountry == country) updateVisitedButton(country);
                    AppToast.show(MapActivity.this, "Отмечено: вы были в " + label);
                } else {
                    btnToggleVisited.setEnabled(true);
                    AppToast.show(MapActivity.this, "Не удалось сохранить отметку");
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitPointResponse> call, @NonNull Throwable t) {
                btnToggleVisited.setEnabled(true);
                AppToast.show(MapActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void removeVisit(CountriesResponse.Country country) {
        final String label = country.getName();
        VisitPoint point = visitedByName.get(visitKey(label));
        if (point == null) return;

        btnToggleVisited.setEnabled(false);

        ApiClient.getApiService().deleteVisitPoint(point.getId()).enqueue(new Callback<VisitPointResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitPointResponse> call,
                                   @NonNull Response<VisitPointResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    visitedByName.remove(visitKey(label));
                    persistVisitedCache();
                    refreshMarkerIcon(label);
                    if (showOnlyVisited) applyMarkerFilter();
                    if (selectedCountry == country) updateVisitedButton(country);
                    AppToast.show(MapActivity.this, "Отметка убрана");
                } else {
                    btnToggleVisited.setEnabled(true);
                    AppToast.show(MapActivity.this, "Не удалось убрать отметку");
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitPointResponse> call, @NonNull Throwable t) {
                btnToggleVisited.setEnabled(true);
                AppToast.show(MapActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void persistVisitedCache() {
        DataCache.saveVisitPoints(this, currentUserId, new ArrayList<>(visitedByName.values()));
    }

    private void drawLocalCountryBoundaryFromCache(String russianName, String englishName) {
        for (Polygon oldPolygon : currentPolygons) {
            mapView.getOverlays().remove(oldPolygon);
        }
        currentPolygons.clear();

        if (!boundariesReady) {
            pendingBoundaryRussianName = russianName;
            pendingBoundaryEnglishName = englishName;
            mapView.invalidate();
            return;
        }

        List<List<GeoPoint>> rings = findBoundaryRings(russianName, englishName);

        if (rings != null) {
            for (List<GeoPoint> points : rings) {
                Polygon polygon = new Polygon(mapView);
                polygon.setPoints(points);
                polygon.getFillPaint().setColor(Color.parseColor("#1ABC5C32"));
                polygon.getOutlinePaint().setColor(Color.parseColor("#FF7A45"));
                polygon.getOutlinePaint().setStrokeWidth(5f);
                currentPolygons.add(polygon);
                mapView.getOverlays().add(0, polygon);
            }
            Log.d(TAG, "Граница найдена: " + russianName);
        } else {
            Log.w(TAG, "Граница не найдена: " + russianName + " / " + englishName);
        }
        mapView.invalidate();
    }

    private List<List<GeoPoint>> findBoundaryRings(String russianName, String englishName) {
        List<List<GeoPoint>> rings;

        String iso3 = Countrynamemapper.getIso3ByRussianName(russianName);
        if (iso3 != null) {
            rings = boundaryCache.get(iso3.toUpperCase());
            if (rings != null) return rings;
        }

        if (englishName == null || englishName.isEmpty()) return null;
        String searchKey = englishName.toLowerCase().trim();

        rings = boundaryCache.get(searchKey);
        if (rings != null) return rings;

        for (Map.Entry<String, List<List<GeoPoint>>> entry : boundaryCache.entrySet()) {
            String k = entry.getKey();
            if (k.length() <= 3) continue;
            if (k.contains(searchKey) || searchKey.contains(k)) {
                return entry.getValue();
            }
        }

        String[] words = searchKey.split("\\s+");
        if (words.length > 0 && words[0].length() >= 4) {
            String firstWord = words[0];
            for (Map.Entry<String, List<List<GeoPoint>>> entry : boundaryCache.entrySet()) {
                String k = entry.getKey();
                if (k.length() <= 3) continue;
                if (k.startsWith(firstWord)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private void resetBottomSheetBeforeLoad(String countryName, String capital) {
        if (tvInfoCapital != null) tvInfoCapital.setText(capital);
        if (tvInfoHistory != null) tvInfoHistory.setText("Загрузка истории...");
        if (tvInfoCulture != null) tvInfoCulture.setText("Загрузка культуры...");
        if (tvInfoMusic != null) tvInfoMusic.setText("Загрузка...");
        if (tvInfoMovies != null) tvInfoMovies.setText("Загрузка...");
        if (tvInfoSports != null) tvInfoSports.setText("Загрузка...");
        if (ivInfoFlag != null) ivInfoFlag.setImageResource(android.R.drawable.ic_menu_gallery);
        if (galleryContainer != null) galleryContainer.removeAllViews();
        if (ticketsContainer != null) ticketsContainer.removeAllViews();
        hideTicketsSubtitle();
        showTicketsStatus("Ищем лучшие цены на билеты…");
    }

    private void showCountryUI(String countryName, String capitalDescription) {
        tvCountryBadgeName.setText(countryName);
        tvCountryBadgeCapital.setText(capitalDescription);

        if (tvSheetCountryName != null) tvSheetCountryName.setText(countryName);

        float density = getResources().getDisplayMetrics().density;

        countryTitleBadge.setVisibility(View.VISIBLE);
        countryTitleBadge.setAlpha(0f);
        countryTitleBadge.setTranslationY(-80 * density);

        List<QuizzesResponse.QuizItem> quizzes = DataCache.getQuizzesList(this);
        int quizId = -1;

        if (quizzes != null) {
            for (QuizzesResponse.QuizItem quiz : quizzes) {
                if (quiz.getCountryName() != null && quiz.getCountryName().equalsIgnoreCase(countryName)) {
                    quizId = quiz.getId();
                    break;
                }
            }

            if (quizId == -1) {
                for (QuizzesResponse.QuizItem quiz : quizzes) {
                    if (quiz.getCountryName() == null && "MIXED".equals(quiz.getType())) {
                        quizId = quiz.getId();
                        break;
                    }
                }
            }
        }

        final int finalQuizId = quizId;
        btnTakeQuiz.setOnClickListener(v -> {
            if (finalQuizId != -1 && currentUserId != -1 && currentUsername != null) {
                Intent intent = new Intent(MapActivity.this, QuizQuestionActivity.class);
                intent.putExtra("quizId", finalQuizId);
                intent.putExtra("userId", currentUserId);
                intent.putExtra("username", currentUsername);
                startActivity(intent);
            } else {
                AppToast.show(MapActivity.this, "Квиз временно недоступен");
            }
        });

        countryTitleBadge.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.0f))
                .start();

        final View navBar = findViewById(R.id.bottom_navigation_layout);
        int barHeight = navBar != null ? navBar.getHeight() : 0;
        int peekHeightPx = (int) (300 * density) + barHeight;

        bottomSheetBehavior.setPeekHeight(peekHeightPx);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideCountryUI() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        animateHideCountryBadge();
        for (Polygon oldPolygon : currentPolygons) {
            mapView.getOverlays().remove(oldPolygon);
        }
        currentPolygons.clear();
        mapView.invalidate();
    }

    private void animateHideCountryBadge() {
        if (countryTitleBadge.getVisibility() == View.VISIBLE) {
            float density = getResources().getDisplayMetrics().density;
            countryTitleBadge.animate()
                    .alpha(0f)
                    .translationY(-80 * density)
                    .setDuration(350)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .withEndAction(() -> countryTitleBadge.setVisibility(View.GONE))
                    .start();
        }
    }

    private void fetchDetailedCountryInfo(String russianName, String englishName) {
        fetchDetailedCountryInfo(russianName, englishName, false);
    }

    private void fetchDetailedCountryInfo(String russianName, String englishName, boolean silentOnError) {
        ApiClient.getApiService().getCountryDetails(russianName, englishName)
                .enqueue(new Callback<CountryDetailsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CountryDetailsResponse> call,
                                           @NonNull Response<CountryDetailsResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            CountryDetailsResponse.CountryDetails details = response.body().getData();
                            if (details != null) {
                                DataCache.saveCountryDetails(MapActivity.this, russianName, details);
                                updateBottomSheetWithFullDetails(details);
                            }
                        } else if (!silentOnError) {
                            AppToast.show(MapActivity.this, "Данные о стране не найдены");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CountryDetailsResponse> call, @NonNull Throwable t) {
                        if (!silentOnError) {
                            AppToast.show(MapActivity.this, AppToast.ERR_NETWORK);
                        }
                    }
                });
    }

    private void updateBottomSheetWithFullDetails(CountryDetailsResponse.CountryDetails details) {
        if (tvInfoHistory != null) tvInfoHistory.setText(details.getHistoryInfo());
        if (tvInfoCulture != null) tvInfoCulture.setText(details.getCultureInfo());
        if (tvInfoMusic != null) tvInfoMusic.setText(details.getMusicInfo());
        if (tvInfoMovies != null) tvInfoMovies.setText(details.getMoviesInfo());
        if (tvInfoSports != null) tvInfoSports.setText(details.getSportsInfo());
        if (tvInfoCapital != null) tvInfoCapital.setText(details.getCapital());

        if (ivInfoFlag != null) {
            String flagUrl = Countrynamemapper.getFlagUrlByRussianName(details.getName());
            if (flagUrl == null || flagUrl.isEmpty()) {
                flagUrl = details.getFlagUrl();
            }
            if (flagUrl != null && !flagUrl.isEmpty()) {
                Glide.with(this)
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
                    addPhotoToGallery(photoUrl, galleryContainer);
                }
            } else {
                showNoPhotosMessage(galleryContainer);
            }
        }
    }

    private void loadTicketsForCountry(double capitalLat, double capitalLng, String capitalName) {
        if (ticketsCard == null) return;
        ticketsCard.setVisibility(View.VISIBLE);

        final int reqId = ++ticketsRequestId;

        GeoPoint myLocation = (locationOverlay != null) ? locationOverlay.getMyLocation() : null;
        Double originLat = (myLocation != null) ? myLocation.getLatitude() : null;
        Double originLng = (myLocation != null) ? myLocation.getLongitude() : null;

        flightTicketsManager.load(originLat, originLng, capitalLat, capitalLng, capitalName,
                new FlightTicketsManager.Callback() {
                    @Override
                    public void onLoading() {
                        if (reqId != ticketsRequestId) return;
                        hideTicketsSubtitle();
                        if (ticketsContainer != null) ticketsContainer.removeAllViews();
                        showTicketsStatus("Ищем лучшие цены на билеты…");
                    }

                    @Override
                    public void onResult(String originLabel, String destLabel,
                                         List<FlightPricesResponse.Flight> flights) {
                        if (reqId != ticketsRequestId) return;
                        setTicketsSubtitle(originLabel + "  →  " + destLabel);
                        hideTicketsStatus();
                        renderTickets(flights);
                    }

                    @Override
                    public void onResultFromAnywhere(String destLabel,
                                                     List<FlightPricesResponse.Flight> flights) {
                        if (reqId != ticketsRequestId) return;
                        setTicketsSubtitle("Из вашего города прямых билетов нет — "
                                + "лучшие предложения в " + destLabel + " из других городов мира");
                        hideTicketsStatus();
                        renderTickets(flights);
                    }

                    @Override
                    public void onEmpty(String originLabel, String destLabel) {
                        if (reqId != ticketsRequestId) return;
                        hideTicketsSubtitle();
                        if (ticketsContainer != null) ticketsContainer.removeAllViews();
                        showTicketsStatus("Пока не нашли билетов в " + destLabel
                                + " на ближайшие даты", R.drawable.ic_error_image);
                    }

                    @Override
                    public void onUnavailable(String message) {
                        if (reqId != ticketsRequestId) return;
                        hideTicketsSubtitle();
                        if (ticketsContainer != null) ticketsContainer.removeAllViews();
                        showTicketsStatus(message, R.drawable.ic_error_image);
                    }
                });
    }

    private void renderTickets(List<FlightPricesResponse.Flight> flights) {
        if (ticketsContainer == null) return;
        ticketsContainer.removeAllViews();
        for (FlightPricesResponse.Flight f : flights) {
            addTicketCard(f);
        }
    }

    private void setTicketsSubtitle(String text) {
        if (tvTicketsSubtitle != null) {
            tvTicketsSubtitle.setText(text);
            tvTicketsSubtitle.setVisibility(View.VISIBLE);
        }
    }

    private void hideTicketsSubtitle() {
        if (tvTicketsSubtitle != null) {
            tvTicketsSubtitle.setText("");
            tvTicketsSubtitle.setVisibility(View.GONE);
        }
    }

    private void showTicketsStatus(String text) {
        showTicketsStatus(text, R.drawable.ic_search);
    }

    private void showTicketsStatus(String text, int iconRes) {
        if (ticketsStatusPanel != null) ticketsStatusPanel.setVisibility(View.VISIBLE);
        if (ivTicketsStatusIcon != null) ivTicketsStatusIcon.setImageResource(iconRes);
        if (tvTicketsStatus != null) tvTicketsStatus.setText(text);
    }

    private void hideTicketsStatus() {
        if (ticketsStatusPanel != null) ticketsStatusPanel.setVisibility(View.GONE);
    }

    private void addTicketCard(FlightPricesResponse.Flight flight) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_flight_ticket, ticketsContainer, false);

        TextView tvRoute = card.findViewById(R.id.tv_route);
        TextView tvMeta = card.findViewById(R.id.tv_meta);
        TextView tvPrice = card.findViewById(R.id.tv_price);
        Button btnBuy = card.findViewById(R.id.btn_buy);

        tvRoute.setText(flight.origin + " → " + flight.destination);

        String date = FlightTicketsManager.formatDate(flight.departure_at);
        String transfers = FlightTicketsManager.transfersText(flight.transfers);
        tvMeta.setText(date.isEmpty() ? transfers : (date + " · " + transfers));

        tvPrice.setText(FlightTicketsManager.formatPrice(flight.price));

        btnBuy.setOnClickListener(v -> openBuyLink(flight));

        ticketsContainer.addView(card);
    }

    private void openBuyLink(FlightPricesResponse.Flight flight) {
        try {
            String url = FlightTicketsManager.buildBuyUrl(flight);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            AppToast.show(this, "Не удалось открыть страницу покупки");
        }
    }

    private void hideTicketsCard() {
        if (ticketsCard != null) ticketsCard.setVisibility(View.GONE);
        if (ticketsContainer != null) ticketsContainer.removeAllViews();
    }

    private void addPhotoToGallery(String imageUrl, LinearLayout container) {
        float density = getResources().getDisplayMetrics().density;

        ShapeableImageView ivPhoto = new ShapeableImageView(this);
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
        container.addView(ivPhoto);

        Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivPhoto);
    }

    private void showNoPhotosMessage(LinearLayout container) {
        runOnUiThread(() -> {
            TextView tv = new TextView(this);
            tv.setText("Фотографии скоро будут добавлены");
            tv.setTextColor(Color.parseColor("#BA5C32"));
            tv.setPadding(0, 20, 0, 20);
            tv.setTextSize(14);
            container.addView(tv);
        });
    }

    private void onMyLocationClick() {
        if (!mapView.isEnabled()) return;

        if (!hasLocationPermission()) {
            checkLocationPermissions();
            return;
        }

        if (!isLocationEnabled()) {
            showEnableLocationDialog();
            return;
        }

        if (locationOverlay == null || !locationOverlay.isMyLocationEnabled()) {
            enableMyLocation();
        }
        GeoPoint myLocation = (locationOverlay != null) ? locationOverlay.getMyLocation() : null;
        if (myLocation != null) {
            hideCountryUI();
            mapView.getController().animateTo(myLocation, 15.0, 1200L);
        } else {
            AppToast.show(this, "Определяем ваше местоположение...");
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        LocationManager lm = getSystemService(LocationManager.class);
        if (lm == null) return false;
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private void showEnableLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Геолокация выключена")
                .setMessage("Включите геолокацию, чтобы найти своё местоположение на карте.")
                .setPositiveButton("Включить", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception e) {
                        AppToast.show(this, "Не удалось открыть настройки");
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void checkLocationPermissions() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (isLocationEnabled()) {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        try {
            GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
            provider.setLocationUpdateMinDistance(2);
            provider.setLocationUpdateMinTime(1500);

            locationOverlay = new MyLocationNewOverlay(provider, mapView);
            locationOverlay.enableMyLocation();
            locationOverlay.disableFollowLocation();
            locationOverlay.setDrawAccuracyEnabled(true);

            int size = 40;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setColor(Color.WHITE);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
            paint.setColor(Color.parseColor("#FF7A45"));
            canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 4, paint);

            locationOverlay.setPersonIcon(bitmap);
            locationOverlay.setDirectionIcon(bitmap);
            locationOverlay.setPersonAnchor(0.5f, 0.5f);
            locationOverlay.setDirectionAnchor(0.5f, 0.5f);

            mapView.getOverlays().add(locationOverlay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled()) {
                enableMyLocation();
            } else {
                showEnableLocationDialog();
            }
        } else {
            AppToast.show(this, "Без доступа к геолокации определить вас не получится");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }
}
