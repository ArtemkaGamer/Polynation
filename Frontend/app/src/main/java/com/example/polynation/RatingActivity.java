package com.example.polynation;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RatingActivity extends BaseNavigationActivity {
    private LinearLayout containerRatingList;
    private ApiService apiService;
    private List<UserProfile> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        apiService = ApiClient.getApiService();
        currentUserId = getIntent().getIntExtra("userId", -1);
        currentUsername = getIntent().getStringExtra("username");

        containerRatingList = findViewById(R.id.container_rating_list);

        loadAllUsers();

        setupBottomNavigation();
        setActiveNavItem("rating");
    }

    private void loadAllUsers() {
        Call<UsersResponse> call = apiService.getAllUsers();

        call.enqueue(new Callback<UsersResponse>() {
            @Override
            public void onResponse(Call<UsersResponse> call, Response<UsersResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UsersResponse usersResponse = response.body();
                    if (usersResponse.isSuccess() && usersResponse.getData() != null) {
                        allUsers = usersResponse.getData();
                        sortAndDisplayUsers();
                    } else {
                        AppToast.show(RatingActivity.this, AppToast.ERR_SERVER);
                    }
                } else {
                    AppToast.show(RatingActivity.this, AppToast.ERR_SERVER);
                }
            }

            @Override
            public void onFailure(Call<UsersResponse> call, Throwable t) {
                AppToast.show(RatingActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void sortAndDisplayUsers() {
        Collections.sort(allUsers, (u1, u2) -> Integer.compare(u2.getRating(), u1.getRating()));

        containerRatingList.removeAllViews();

        for (int i = 0; i < allUsers.size(); i++) {
            UserProfile user = allUsers.get(i);
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

        containerRatingList.addView(itemView);
    }
}
