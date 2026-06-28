package com.example.polynation.presentation.map;

import android.graphics.Color;
import android.util.Log;

import com.example.polynation.domain.geo.GeoJsonBoundaryProvider;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class CountryBoundaryRenderer {
    private static final String TAG = "BoundaryRenderer";

    private final MapView mapView;
    private final GeoJsonBoundaryProvider provider;
    private final List<Polygon> currentPolygons = new ArrayList<>();

    private String pendingRussianName;
    private String pendingEnglishName;

    public CountryBoundaryRenderer(MapView mapView, GeoJsonBoundaryProvider provider) {
        this.mapView = mapView;
        this.provider = provider;
    }

    public void init() {
        provider.ensureLoaded(() -> {
            if (pendingRussianName != null) {
                String ru = pendingRussianName;
                String en = pendingEnglishName;
                pendingRussianName = null;
                pendingEnglishName = null;
                draw(ru, en);
            }
        });
    }

    public void draw(String russianName, String englishName) {
        clear();

        if (!provider.isReady()) {
            pendingRussianName = russianName;
            pendingEnglishName = englishName;
            mapView.invalidate();
            return;
        }

        List<List<GeoPoint>> rings = provider.findRings(russianName, englishName);
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

    public void clear() {
        for (Polygon oldPolygon : currentPolygons) {
            mapView.getOverlays().remove(oldPolygon);
        }
        currentPolygons.clear();
    }
}
