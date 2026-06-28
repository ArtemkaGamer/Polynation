package com.example.polynation.data.repository;

import androidx.annotation.NonNull;

import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.UpdateUserRequest;
import com.example.polynation.data.remote.dto.UserProfile;
import com.example.polynation.data.remote.dto.UserProfileResponse;
import com.example.polynation.data.remote.dto.UsersResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final ApiService api = ApiClient.getApiService();

    public void getUserProfile(int userId, ResultCallback<UserProfile> callback) {
        api.getUserProfile(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onResult(Resource.success(response.body().getData()));
                } else {
                    callback.onResult(Resource.error(AppToast.ERR_SERVER));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public void updateUsername(int userId, String newName, ResultCallback<UserProfile> callback) {
        api.updateUser(userId, new UpdateUserRequest(newName)).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onResult(Resource.success(response.body().getData()));
                } else {
                    callback.onResult(Resource.error(nameErrorMessage(response.body())));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public void getAllUsers(ResultCallback<List<UserProfile>> callback) {
        api.getAllUsers().enqueue(new Callback<UsersResponse>() {
            @Override
            public void onResponse(@NonNull Call<UsersResponse> call, @NonNull Response<UsersResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onResult(Resource.success(response.body().getData()));
                } else {
                    callback.onResult(Resource.error(AppToast.ERR_SERVER));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UsersResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    private static String nameErrorMessage(UserProfileResponse body) {
        if (body != null && body.getMessage() != null
                && body.getMessage().toLowerCase().contains("занят")) {
            return "Это имя уже занято";
        }
        return "Не удалось изменить имя";
    }
}
