package com.example.polynation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION_MS = 700;

    private TextView tvLoadingStatus, tvPercent;
    private ProgressBar progressBar;
    private Button btnRetry;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ValueAnimator progressAnimator;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        hideSystemBars();

        tvLoadingStatus = findViewById(R.id.tv_loading_status);
        tvPercent = findViewById(R.id.tv_percent);
        progressBar = findViewById(R.id.progress_bar);
        btnRetry = findViewById(R.id.btn_retry);

        progressBar.setMax(100);
        btnRetry.setOnClickListener(v -> startSplash());

        startSplash();
    }

    private boolean isActive() {
        return !isFinishing() && !isDestroyed();
    }

    private void startSplash() {
        if (!isActive()) return;
        navigated = false;

        btnRetry.setVisibility(View.GONE);
        tvPercent.setVisibility(View.VISIBLE);
        setStatus("Подготовка...");
        applyProgress(0);

        BackgroundCacheLoader.getInstance(this).start();

        if (DataCache.hasCountriesCache(this)) {
            animateProgressTo(100, SPLASH_DURATION_MS, this::navigateToMain);
            return;
        }

        setStatus("Проверка соединения...");
        NetworkUtils.hasInternetAccess(this, online -> handler.post(() -> {
            if (!isActive()) return;
            if (online) {
                animateProgressTo(100, SPLASH_DURATION_MS, this::navigateToMain);
            } else {
                showOffline();
            }
        }));
    }

    private void showOffline() {
        if (!isActive()) return;
        applyProgress(0);
        tvPercent.setVisibility(View.GONE);
        setStatus("Нет подключения к интернету");
        btnRetry.setVisibility(View.VISIBLE);
    }

    private void setStatus(String text) {
        if (tvLoadingStatus != null) tvLoadingStatus.setText(text);
    }

    private void applyProgress(int value) {
        if (progressBar == null) return;
        cancelProgressAnimator();
        if (value < 0) value = 0;
        if (value > 100) value = 100;
        progressBar.setProgress(value, true);
        if (tvPercent != null) tvPercent.setText(value + "%");
    }

    private void animateProgressTo(int target, long duration, Runnable onEnd) {
        if (progressBar == null) {
            if (onEnd != null) onEnd.run();
            return;
        }
        cancelProgressAnimator();
        progressAnimator = ValueAnimator.ofInt(progressBar.getProgress(), target);
        progressAnimator.setDuration(duration);
        progressAnimator.addUpdateListener(animation -> {
            int v = (int) animation.getAnimatedValue();
            progressBar.setProgress(v);
            if (tvPercent != null) tvPercent.setText(v + "%");
        });
        if (onEnd != null) {
            progressAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!cancelled) onEnd.run();
                }
            });
        }
        progressAnimator.start();
    }

    private void cancelProgressAnimator() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
    }

    private void navigateToMain() {
        if (navigated || !isActive()) return;
        navigated = true;

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        int userId = prefs.getInt("userId", -1);
        String username = prefs.getString("username", "");

        Intent intent;
        if (isLoggedIn && userId != -1 && !username.isEmpty()) {
            intent = new Intent(SplashActivity.this, MapActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("userId", userId);
        } else {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        cancelProgressAnimator();
    }

    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }
}
