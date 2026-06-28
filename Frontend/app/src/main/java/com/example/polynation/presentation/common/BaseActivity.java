package com.example.polynation.presentation.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.polynation.util.NetworkUtils;
import com.example.polynation.util.NoInternetDialog;

public class BaseActivity extends AppCompatActivity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private NoInternetDialog noInternetDialog;
    private boolean networkWatchActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startNetworkWatch();
    }

    @Override
    protected void onPause() {
        stopNetworkWatch();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    protected void hideSystemBars() {
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);
    }

    private void startNetworkWatch() {
        if (networkWatchActive) return;
        networkWatchActive = true;

        checkConnection();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                checkConnection();
            }

            @Override
            public void onLost(@NonNull Network network) {
                checkConnection();
            }
        };
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception e) {
            connectivityManager = null;
            networkCallback = null;
        }
    }

    private void stopNetworkWatch() {
        if (!networkWatchActive) return;
        networkWatchActive = false;

        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
        connectivityManager = null;
        networkCallback = null;
        mainHandler.removeCallbacksAndMessages(null);
        dismissNoInternetDialog();
    }

    private void checkConnection() {
        if (!networkWatchActive) return;
        NetworkUtils.hasInternetAccess(this, online -> mainHandler.post(() -> applyConnectionState(online)));
    }

    private void applyConnectionState(boolean online) {
        if (isFinishing() || isDestroyed()) return;
        if (online) {
            dismissNoInternetDialog();
        } else {
            showNoInternetDialog();
        }
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;
        noInternetDialog = new NoInternetDialog(this, new NoInternetDialog.Callback() {
            @Override
            public void onRetry() {
                retryConnectionCheck();
            }

            @Override
            public void onExit() {
                finishAffinity();
            }
        });
        noInternetDialog.show();
    }

    private void retryConnectionCheck() {
        if (noInternetDialog != null) noInternetDialog.setCheckingConnection(true);
        NetworkUtils.hasInternetAccess(this, online -> mainHandler.post(() -> {
            if (noInternetDialog != null) noInternetDialog.setCheckingConnection(false);
            applyConnectionState(online);
        }));
    }

    private void dismissNoInternetDialog() {
        if (noInternetDialog == null) return;
        try {
            if (noInternetDialog.isShowing()) noInternetDialog.dismiss();
        } catch (Exception ignored) {
        }
        noInternetDialog = null;
    }
}
