package com.example.polynation.presentation.map;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.CountriesResponse;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CountryMarkerManager {

    public interface OnCountrySelected {
        void onSelected(CountriesResponse.Country country);
    }

    private final MapView mapView;
    private final Context context;
    private final OnCountrySelected listener;

    private final List<Marker> countryMarkers = new ArrayList<>();
    private final Map<String, Marker> markerByName = new HashMap<>();
    private Set<String> visitedKeys = new HashSet<>();
    private boolean showOnlyVisited = false;

    public CountryMarkerManager(MapView mapView, Context context, OnCountrySelected listener) {
        this.mapView = mapView;
        this.context = context;
        this.listener = listener;
    }

    public static String visitKey(String russianName) {
        return russianName == null ? "" : russianName.toLowerCase().trim();
    }

    public boolean isShowOnlyVisited() {
        return showOnlyVisited;
    }

    public void setCountries(List<CountriesResponse.Country> countries) {
        if (countries == null) return;

        for (CountriesResponse.Country country : countries) {
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
            marker.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_custom_marker));

            marker.setOnMarkerClickListener((m, view) -> {
                if (!mapView.isEnabled()) return false;
                listener.onSelected(country);
                return true;
            });

            mapView.getOverlays().add(marker);
            countryMarkers.add(marker);
            if (russianName != null) markerByName.put(visitKey(russianName), marker);
        }

        refreshAllIcons();
        if (showOnlyVisited) applyFilter();
        mapView.invalidate();
    }

    public void updateVisited(Set<String> keys) {
        visitedKeys = (keys != null) ? keys : new HashSet<>();
        refreshAllIcons();
        if (showOnlyVisited) applyFilter();
    }

    public void refreshAllIcons() {
        for (Map.Entry<String, Marker> e : markerByName.entrySet()) {
            boolean visited = visitedKeys.contains(e.getKey());
            e.getValue().setIcon(ContextCompat.getDrawable(context,
                    visited ? R.drawable.ic_marker_visited : R.drawable.ic_custom_marker));
        }
        mapView.invalidate();
    }

    public void setShowOnlyVisited(boolean value) {
        showOnlyVisited = value;
        applyFilter();
    }

    public void applyFilter() {
        for (Marker m : countryMarkers) {
            boolean visited = visitedKeys.contains(visitKey(m.getTitle()));
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
}
