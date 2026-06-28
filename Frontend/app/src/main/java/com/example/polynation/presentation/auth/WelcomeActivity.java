package com.example.polynation.presentation.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.example.polynation.R;
import com.example.polynation.presentation.common.BaseActivity;
import com.example.polynation.presentation.map.MapActivity;

public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        int userId = prefs.getInt("userId", -1);
        String username = prefs.getString("username", "");

        if (isLoggedIn && userId != -1 && !username.isEmpty()) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("userId", userId);
            intent.putExtra("from_auth", true);
            startActivity(intent);
            finish();
            return;
        }

        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
}
