package com.example.polynation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class BaseNavigationActivity extends BaseActivity {
    protected LinearLayout navMap, navQuizzes, navRating, navProfile;
    protected View iconMap, iconQuizzes, iconRating, iconProfile;
    protected TextView textMap, textQuizzes, textRating, textProfile;
    protected String currentUsername;
    protected int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUsername = getIntent().getStringExtra("username");
        currentUserId = getIntent().getIntExtra("userId", -1);
    }

    protected void setupBottomNavigation() {
        navMap = findViewById(R.id.nav_map);
        navQuizzes = findViewById(R.id.nav_quizzes);
        navRating = findViewById(R.id.nav_rating);
        navProfile = findViewById(R.id.nav_profile);

        iconMap = findViewById(R.id.icon_map);
        iconQuizzes = findViewById(R.id.icon_quizzes);
        iconRating = findViewById(R.id.icon_rating);
        iconProfile = findViewById(R.id.icon_profile);

        textMap = findViewById(R.id.text_map);
        textQuizzes = findViewById(R.id.text_quizzes);
        textRating = findViewById(R.id.text_rating);
        textProfile = findViewById(R.id.text_profile);

        navMap.setOnClickListener(v -> navigateTo(MapActivity.class));
        navQuizzes.setOnClickListener(v -> navigateTo(QuizzesActivity.class));
        navRating.setOnClickListener(v -> navigateTo(RatingActivity.class));
        navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.putExtra("username", currentUsername);
        intent.putExtra("userId", currentUserId);
        startActivity(intent);
        finish();
    }

    protected void setActiveNavItem(String activeItem) {
        setNavItemStyle(iconMap, textMap, R.drawable.ic_map_inactive, "#FEF9E7");
        setNavItemStyle(iconQuizzes, textQuizzes, R.drawable.ic_quizzes_inactive, "#FEF9E7");
        setNavItemStyle(iconRating, textRating, R.drawable.ic_rating_inactive, "#FEF9E7");
        setNavItemStyle(iconProfile, textProfile, R.drawable.ic_profile_inactive, "#FEF9E7");

        switch (activeItem) {
            case "map":
                setNavItemStyle(iconMap, textMap, R.drawable.ic_map_active, "#BA5C32");
                break;
            case "quizzes":
                setNavItemStyle(iconQuizzes, textQuizzes, R.drawable.ic_quizzes_active, "#BA5C32");
                break;
            case "rating":
                setNavItemStyle(iconRating, textRating, R.drawable.ic_rating_active, "#BA5C32");
                break;
            case "profile":
                setNavItemStyle(iconProfile, textProfile, R.drawable.ic_profile_active, "#BA5C32");
                break;
        }
    }

    private void setNavItemStyle(View iconView, TextView textView, int iconRes, String colorHex) {
        if (iconView != null) {
            iconView.setBackgroundResource(iconRes);
        }
        if (textView != null) {
            textView.setTextColor(android.graphics.Color.parseColor(colorHex));
        }
    }
}
