package com.example.polynation.presentation.rating;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.UserProfile;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.common.BaseNavigationActivity;
import com.example.polynation.util.AppToast;

import java.util.List;

public class RatingActivity extends BaseNavigationActivity {
    private LinearLayout containerRatingList;
    private RatingViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        currentUserId = getIntent().getIntExtra("userId", -1);
        currentUsername = getIntent().getStringExtra("username");

        containerRatingList = findViewById(R.id.container_rating_list);
        viewModel = new ViewModelProvider(this).get(RatingViewModel.class);
        viewModel.getUsers().observe(this, this::renderUsers);
        viewModel.loadUsers();

        setupBottomNavigation();
        setActiveNavItem("rating");

        notifyAssistantScreenShown("rating");
    }

    private void renderUsers(Resource<List<UserProfile>> result) {
        if (result == null || result.isLoading()) return;
        if (!result.isSuccess() || result.data == null) {
            AppToast.show(this, result.message);
            return;
        }
        displayUsers(result.data);
    }

    private void displayUsers(List<UserProfile> users) {
        containerRatingList.removeAllViews();
        for (int i = 0; i < users.size(); i++) {
            UserProfile user = users.get(i);
            int position = i + 1;
            boolean isCurrentUser = (user.getId() == currentUserId);
            addUserToRatingList(user, position, isCurrentUser);
        }
    }

    private void addUserToRatingList(UserProfile user, int position, boolean isCurrentUser) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_rating, containerRatingList, false);

        TextView tvPosition = itemView.findViewById(R.id.tv_position);
        TextView tvUsername = itemView.findViewById(R.id.tv_username);
        TextView tvEmail = itemView.findViewById(R.id.tv_email);
        TextView tvRating = itemView.findViewById(R.id.tv_rating);

        tvPosition.setText(String.valueOf(position));
        tvRating.setText(String.valueOf(user.getRating()));
        tvEmail.setText(user.getEmail());

        int badgeColor, badgeTextColor;
        if (position == 1) {
            badgeColor = 0xFFE0A82E;
            badgeTextColor = 0xFFFFFFFF;
        } else if (position == 2) {
            badgeColor = 0xFFB9C0C7;
            badgeTextColor = 0xFF2E3338;
        } else if (position == 3) {
            badgeColor = 0xFFCB8150;
            badgeTextColor = 0xFFFFFFFF;
        } else if (isCurrentUser) {
            badgeColor = 0x40FFFFFF;
            badgeTextColor = 0xFFFFFFFF;
        } else {
            badgeColor = 0xFFF1E7DB;
            badgeTextColor = 0xFFBA5C32;
        }
        tvPosition.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
        tvPosition.setTextColor(badgeTextColor);

        if (isCurrentUser) {
            tvUsername.setText(user.getUsername() + " (Вы)");
            itemView.setBackground(ContextCompat.getDrawable(this, R.drawable.card_rating_orange));
            tvUsername.setTextColor(0xFFFFFFFF);
            tvEmail.setTextColor(0xFFFFFFFF);
            tvEmail.setAlpha(0.75f);
        } else {
            tvUsername.setText(user.getUsername());
            itemView.setBackground(ContextCompat.getDrawable(this, R.drawable.card_rating_white));
            tvUsername.setTextColor(0xFF1B261F);
            tvEmail.setTextColor(0xFF1B261F);
            tvEmail.setAlpha(0.55f);
        }

        itemView.setOnClickListener(v -> UserInfoDialog.show(this, user));

        containerRatingList.addView(itemView);
    }
}
