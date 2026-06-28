package com.example.polynation.domain.geo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.polynation.domain.CountryNameMapper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeoJsonBoundaryProvider {
    private static final String TAG = "GeoBoundary";
    private static final String ASSET = "countries.geojson";

    public interface ReadyListener {
        void onBoundariesReady();
    }

    private static volatile GeoJsonBoundaryProvider instance;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, List<List<GeoPoint>>> boundaryCache = new ConcurrentHashMap<>();

    private volatile boolean ready = false;
    private boolean loadStarted = false;

    private GeoJsonBoundaryProvider(Context appContext) {
        this.appContext = appContext;
    }

    public static GeoJsonBoundaryProvider getInstance(Context context) {
        if (instance == null) {
            synchronized (GeoJsonBoundaryProvider.class) {
                if (instance == null) {
                    instance = new GeoJsonBoundaryProvider(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public boolean isReady() {
        return ready;
    }

    public void ensureLoaded(final ReadyListener listener) {
        if (ready) {
            if (listener != null) mainHandler.post(listener::onBoundariesReady);
            return;
        }
        synchronized (this) {
            if (loadStarted) {
                return;
            }
            loadStarted = true;
        }
        new Thread(() -> {
            parseAsset();
            ready = true;
            Log.d(TAG, "GeoJSON загружен: " + boundaryCache.size() + " записей");
            if (listener != null) mainHandler.post(listener::onBoundariesReady);
        }).start();
    }

    private void parseAsset() {
        try {
            InputStream is = appContext.getAssets().open(ASSET);
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
                    parseRing(coordinates, polygonsList);
                } else if (geometryType.equals("MultiPolygon")) {
                    for (int j = 0; j < coordinates.length(); j++) {
                        parseRing(coordinates.getJSONArray(j), polygonsList);
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
        } catch (Exception e) {
            Log.e(TAG, "Ошибка разбора GeoJSON", e);
        }
    }

    private void parseRing(JSONArray polygonCoords, List<List<GeoPoint>> outList) throws Exception {
        JSONArray outerRingCoords = polygonCoords.getJSONArray(0);
        List<GeoPoint> points = new ArrayList<>();
        for (int k = 0; k < outerRingCoords.length(); k++) {
            JSONArray coord = outerRingCoords.getJSONArray(k);
            points.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
        }
        if (!points.isEmpty()) outList.add(points);
    }

    public List<List<GeoPoint>> findRings(String russianName, String englishName) {
        List<List<GeoPoint>> rings;

        String iso3 = CountryNameMapper.getIso3ByRussianName(russianName);
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
}
