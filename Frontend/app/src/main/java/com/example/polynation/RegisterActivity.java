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

public class RegisterActivity extends BaseActivity {
    private EditText etUsername, etEmail, etPassword;
    private ImageView ivTogglePassword;
    private Button btnBack, btnRegister;
    private ApiService apiService;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.getApiService();

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnBack = findViewById(R.id.btn_back);
        btnRegister = findViewById(R.id.btn_register);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                AppToast.show(RegisterActivity.this, "Заполните все поля");
                return;
            }

            if (password.length() < 4) {
                AppToast.show(RegisterActivity.this, "Пароль должен быть не менее 4 символов");
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Регистрация...");

            RegisterRequest request = new RegisterRequest(username, email, password);
            Call<AuthResponse> call = apiService.register(request);

            call.enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Зарегистрироваться");

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        AppToast.showLong(RegisterActivity.this,
                                "Регистрация успешна! Теперь войдите в аккаунт");

                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        AppToast.show(RegisterActivity.this, registerErrorMessage(response));
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Зарегистрироваться");
                    AppToast.show(RegisterActivity.this, AppToast.ERR_NETWORK);
                }
            });
        });
    }

    private String registerErrorMessage(Response<AuthResponse> response) {
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
        boolean emailHit = low.contains("email") || low.contains("e-mail") || low.contains("mail") || low.contains("почт");
        boolean nameHit = low.contains("username") || low.contains("имя") || low.contains("логин")
                || low.contains("никнейм") || low.contains("nickname");
        boolean taken = low.contains("занят") || low.contains("уже") || low.contains("exist") || low.contains("taken")
                || low.contains("registered") || low.contains("зарегистр") || low.contains("duplicate")
                || low.contains("already") || low.contains("in use");

        if (emailHit && !nameHit) return "Пользователь с таким email уже существует";
        if (nameHit && !emailHit) return "Это имя пользователя уже занято";
        if (taken || emailHit || nameHit) return "Пользователь с таким email или именем уже существует";

        if (response.code() >= 500) return AppToast.ERR_SERVER;
        return "Не удалось зарегистрироваться, попробуйте позже";
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
