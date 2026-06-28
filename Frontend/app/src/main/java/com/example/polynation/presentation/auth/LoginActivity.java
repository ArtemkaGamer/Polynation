package com.example.polynation.presentation.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.lifecycle.ViewModelProvider;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.AuthResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.common.BaseActivity;
import com.example.polynation.presentation.map.MapActivity;
import com.example.polynation.util.AppToast;

public class LoginActivity extends BaseActivity {
    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private Button btnLogin;
    private SharedPreferences sharedPreferences;
    private boolean passwordVisible = false;

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        Button btnBack = findViewById(R.id.btn_back);
        btnLogin = findViewById(R.id.btn_login);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnBack.setOnClickListener(v -> finish());
        btnLogin.setOnClickListener(v -> attemptLogin());

        viewModel.getLoginResult().observe(this, this::renderLoginResult);
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            AppToast.show(this, "Заполните все поля");
            return;
        }
        viewModel.login(email, password);
    }

    private void renderLoginResult(Resource<AuthResponse.User> result) {
        if (result == null) return;
        if (result.isLoading()) {
            btnLogin.setEnabled(false);
            btnLogin.setText("Вход...");
            return;
        }

        btnLogin.setEnabled(true);
        btnLogin.setText("Войти");

        if (result.isSuccess() && result.data != null) {
            onLoginSuccess(result.data);
        } else {
            AppToast.show(this, result.message);
        }
    }

    private void onLoginSuccess(AuthResponse.User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("userId", user.getId());
        editor.putString("username", user.getUsername());
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("username", user.getUsername());
        intent.putExtra("userId", user.getId());
        intent.putExtra("from_auth", true);
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
