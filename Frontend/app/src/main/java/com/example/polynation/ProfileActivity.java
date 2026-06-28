package com.example.polynation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseNavigationActivity {
    private TextView tvUsername, tvEmail, tvRating, tvQuizzesSolved, tvJoinedDate;
    private TextView tvVisitedCount, tvVisitedEmpty, tvVisitedHint;
    private LinearLayout cardEditName, containerVisitedList;
    private EditText etUsername;
    private Button btnEdit, btnSaveName, btnLogout;
    private ApiService apiService;

    private final List<VisitPoint> visitedPoints = new ArrayList<>();
    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        apiService = ApiClient.getApiService();

        tvUsername = findViewById(R.id.tv_username);
        tvEmail = findViewById(R.id.tv_email);
        tvRating = findViewById(R.id.tv_rating);
        tvQuizzesSolved = findViewById(R.id.tv_quizzes_solved);
        tvJoinedDate = findViewById(R.id.tv_joined_date);
        tvVisitedCount = findViewById(R.id.tv_visited_count);
        tvVisitedEmpty = findViewById(R.id.tv_visited_empty);
        tvVisitedHint = findViewById(R.id.tv_visited_hint);
        cardEditName = findViewById(R.id.card_edit_name);
        containerVisitedList = findViewById(R.id.container_visited_list);
        etUsername = findViewById(R.id.et_username);
        btnEdit = findViewById(R.id.btn_edit);
        btnSaveName = findViewById(R.id.btn_save_name);
        btnLogout = findViewById(R.id.btn_logout);

        currentUserId = getIntent().getIntExtra("userId", -1);
        currentUsername = getIntent().getStringExtra("username");

        if (currentUserId != -1) {
            tvQuizzesSolved.setText(String.valueOf(DataCache.getQuizzesSolved(this, currentUserId)));
            loadUserProfile(currentUserId);
            loadVisitedCountries(currentUserId);
        } else {
            AppToast.show(this, "Не удалось открыть профиль");
        }

        btnEdit.setOnClickListener(v -> setEditMode(!editMode));
        btnSaveName.setOnClickListener(v -> saveUsername());
        btnLogout.setOnClickListener(v -> logout());

        setupBottomNavigation();
        setActiveNavItem("profile");
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
        UpdateUserRequest request = new UpdateUserRequest(newName);

        apiService.updateUser(currentUserId, request).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                btnSaveName.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    applyNewUsername(response.body().getData().getUsername());
                    setEditMode(false);
                    AppToast.show(ProfileActivity.this, "Имя обновлено");
                } else {
                    AppToast.show(ProfileActivity.this, nameErrorMessage(response.body()));
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                btnSaveName.setEnabled(true);
                AppToast.show(ProfileActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private String nameErrorMessage(UserProfileResponse body) {
        if (body != null && body.getMessage() != null
                && body.getMessage().toLowerCase().contains("занят")) {
            return "Это имя уже занято";
        }
        return "Не удалось изменить имя";
    }

    private void applyNewUsername(String newName) {
        currentUsername = newName;
        tvUsername.setText(newName);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        prefs.edit().putString("username", newName).apply();
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();

        AppToast.show(ProfileActivity.this, "Вы вышли из аккаунта");

        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserProfile(int userId) {
        Call<UserProfileResponse> call = apiService.getUserProfile(userId);

        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    displayUserProfile(response.body().getData());
                } else {
                    AppToast.show(ProfileActivity.this, AppToast.ERR_SERVER);
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                AppToast.show(ProfileActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void loadVisitedCountries(int userId) {
        List<VisitPoint> cached = DataCache.getVisitPoints(this, userId);
        if (cached != null) {
            displayVisitedCountries(cached);
        }

        apiService.getVisitPoints(userId).enqueue(new Callback<VisitPointsResponse>() {
            @Override
            public void onResponse(Call<VisitPointsResponse> call, Response<VisitPointsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<VisitPoint> points = response.body().getData();
                    DataCache.saveVisitPoints(ProfileActivity.this, userId, points);
                    displayVisitedCountries(points);
                } else if (cached == null) {
                    showVisitedMessage("Не удалось загрузить список");
                }
            }

            @Override
            public void onFailure(Call<VisitPointsResponse> call, Throwable t) {
                if (cached == null) {
                    showVisitedMessage(AppToast.ERR_NETWORK);
                }
            }
        });
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
            btnDelete.setOnClickListener(v -> deleteVisit(point, btnDelete));

            containerVisitedList.addView(row);
        }
    }

    private void deleteVisit(VisitPoint point, View deleteButton) {
        deleteButton.setEnabled(false);

        apiService.deleteVisitPoint(point.getId()).enqueue(new Callback<VisitPointResponse>() {
            @Override
            public void onResponse(Call<VisitPointResponse> call, Response<VisitPointResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    visitedPoints.remove(point);
                    DataCache.saveVisitPoints(ProfileActivity.this, currentUserId,
                            new ArrayList<>(visitedPoints));

                    tvVisitedCount.setText(String.valueOf(visitedPoints.size()));
                    if (visitedPoints.isEmpty()) {
                        showVisitedMessage("Вы ещё не отметили страны на карте");
                        tvVisitedHint.setVisibility(View.GONE);
                    }
                    renderVisitedRows();
                    AppToast.show(ProfileActivity.this, "Страна удалена: " + point.getLabel());
                } else {
                    deleteButton.setEnabled(true);
                    AppToast.show(ProfileActivity.this, "Не удалось удалить страну");
                }
            }

            @Override
            public void onFailure(Call<VisitPointResponse> call, Throwable t) {
                deleteButton.setEnabled(true);
                AppToast.show(ProfileActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void displayUserProfile(UserProfile user) {
        tvUsername.setText(user.getUsername());
        tvEmail.setText(user.getEmail());
        tvRating.setText(String.valueOf(user.getRating()));

        int solved = Math.max(user.getQuizzesSolved(), DataCache.getQuizzesSolved(this, currentUserId));
        tvQuizzesSolved.setText(String.valueOf(solved));
        tvJoinedDate.setText(formatDate(user.getCreatedAt()));

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            currentUsername = user.getUsername();
        }
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
