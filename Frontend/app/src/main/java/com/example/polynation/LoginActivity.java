package com.example.polynation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {
    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private Button btnBack, btnLogin;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        apiService = ApiClient.getApiService();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnBack = findViewById(R.id.btn_back);
        btnLogin = findViewById(R.id.btn_login);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                AppToast.show(LoginActivity.this, "Заполните все поля");
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Вход...");

            LoginRequest request = new LoginRequest(email, password);
            Call<AuthResponse> call = apiService.login(request);

            call.enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Войти");

                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSuccess() && response.body().getUser() != null) {
                        AuthResponse.User user = response.body().getUser();
                        int userId = user.getId();
                        String username = user.getUsername();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("userId", userId);
                        editor.putString("username", username);
                        editor.putBoolean("isLoggedIn", true);
                        editor.apply();

                        AppToast.showLong(LoginActivity.this, "Добро пожаловать, " + username + "!");

                        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    } else {
                        AppToast.show(LoginActivity.this, loginErrorMessage(response));
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Войти");
                    AppToast.show(LoginActivity.this, AppToast.ERR_NETWORK);
                }
            });
        });
    }

    private String loginErrorMessage(Response<AuthResponse> response) {
        String message = null;
        if (response.body() != null && response.body().getMessage() != null) {
            message = response.body().getMessage();
        } else if (response.errorBody() != null) {
            try {
                String raw = response.errorBody().string();
                AuthResponse parsed = new com.google.gson.Gson().fromJson(raw, AuthResponse.class);
                message = (parsed != null && parsed.getMessage() != null) ? parsed.getMessage() : raw;
            } catch (Exception ignored) {
            }
        }

        String low = message == null ? "" : message.toLowerCase();

        boolean wrongPassword = low.contains("парол") || low.contains("password");
        boolean userNotFound = low.contains("не найден") || low.contains("not found")
                || low.contains("не существ") || low.contains("нет такого")
                || low.contains("не зарегистр") || low.contains("does not exist")
                || low.contains("no user") || low.contains("user not");

        if (wrongPassword && !userNotFound) {
            return "Неверный пароль";
        }
        if (userNotFound && !wrongPassword) {
            return "Пользователь с таким email не найден";
        }

        int code = response.code();
        if (response.isSuccessful() || (code >= 400 && code < 500)) {
            return "Неверный email или пароль";
        }
        return AppToast.ERR_SERVER;
    }

    private void togglePasswordVisibility() {
        Typeface typeface = etPassword.getTypeface();
        int selection = etPassword.getSelectionStart();
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_eye_off);
            ivTogglePassword.setContentDescription("Скрыть пароль");
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(R.drawable.ic_eye);
            ivTogglePassword.setContentDescription("Показать пароль");
        }

        etPassword.setTypeface(typeface);
        etPassword.setSelection(Math.min(selection, etPassword.getText().length()));
    }
}
