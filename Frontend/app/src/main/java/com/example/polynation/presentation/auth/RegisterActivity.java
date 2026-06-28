package com.example.polynation.presentation.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.lifecycle.ViewModelProvider;

import com.example.polynation.R;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.common.BaseActivity;
import com.example.polynation.util.AppToast;

public class RegisterActivity extends BaseActivity {
    private EditText etUsername, etEmail, etPassword;
    private ImageView ivTogglePassword;
    private Button btnRegister;
    private boolean passwordVisible = false;

    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        Button btnBack = findViewById(R.id.btn_back);
        btnRegister = findViewById(R.id.btn_register);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());

        viewModel.getRegisterResult().observe(this, this::renderRegisterResult);
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AppToast.show(this, "Заполните все поля");
            return;
        }
        if (password.length() < 4) {
            AppToast.show(this, "Пароль должен быть не менее 4 символов");
            return;
        }
        viewModel.register(username, email, password);
    }

    private void renderRegisterResult(Resource<Boolean> result) {
        if (result == null) return;
        if (result.isLoading()) {
            btnRegister.setEnabled(false);
            btnRegister.setText("Регистрация...");
            return;
        }

        btnRegister.setEnabled(true);
        btnRegister.setText("Зарегистрироваться");

        if (result.isSuccess()) {
            onRegisterSuccess();
        } else {
            AppToast.show(this, result.message);
        }
    }

    private void onRegisterSuccess() {
        getSharedPreferences("assistant_prefs", MODE_PRIVATE).edit()
                .putBoolean("onboarding_done", false)
                .putBoolean("tour_active", false)
                .putInt("tour_step", 0)
                .apply();

        AppToast.showLong(this, "Регистрация успешна! Теперь войдите в аккаунт");

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
