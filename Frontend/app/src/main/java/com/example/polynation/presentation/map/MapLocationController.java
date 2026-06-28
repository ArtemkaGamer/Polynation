package com.example.polynation.presentation.map;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.LocationManager;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.polynation.util.AppToast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapLocationController {
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final Activity activity;
    private final MapView mapView;
    private final Runnable onBeforeLocate;

    private MyLocationNewOverlay locationOverlay;

    public MapLocationController(Activity activity, MapView mapView, Runnable onBeforeLocate) {
        this.activity = activity;
        this.mapView = mapView;
        this.onBeforeLocate = onBeforeLocate;
    }

    public GeoPoint getMyLocation() {
        return (locationOverlay != null) ? locationOverlay.getMyLocation() : null;
    }

    public void onMyLocationClick() {
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
        GeoPoint myLocation = getMyLocation();
        if (myLocation != null) {
            if (onBeforeLocate != null) onBeforeLocate.run();
            mapView.getController().animateTo(myLocation, 15.0, 1200L);
        } else {
            AppToast.show(activity, "Определяем ваше местоположение...");
        }
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationEnabled() {
        LocationManager lm = activity.getSystemService(LocationManager.class);
        if (lm == null) return false;
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private void showEnableLocationDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Геолокация выключена")
                .setMessage("Включите геолокацию, чтобы найти своё местоположение на карте.")
                .setPositiveButton("Включить", (dialog, which) -> {
                    try {
                        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception e) {
                        AppToast.show(activity, "Не удалось открыть настройки");
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void checkLocationPermissions() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (isLocationEnabled()) {
            enableMyLocation();
        }
    }

    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled()) {
                enableMyLocation();
            } else {
                showEnableLocationDialog();
            }
        } else {
            AppToast.show(activity, "Без доступа к геолокации определить вас не получится");
        }
    }

    private void enableMyLocation() {
        try {
            GpsMyLocationProvider provider = new GpsMyLocationProvider(activity);
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

    public void onResume() {
        if (locationOverlay != null) locationOverlay.enableMyLocation();
    }

    public void onPause() {
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }
}
