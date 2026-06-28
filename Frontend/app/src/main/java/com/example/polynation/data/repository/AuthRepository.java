package com.example.polynation.data.repository;

import androidx.annotation.NonNull;

import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.AuthResponse;
import com.example.polynation.data.remote.dto.LoginRequest;
import com.example.polynation.data.remote.dto.RegisterRequest;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService api = ApiClient.getApiService();

    public void login(String email, String password, ResultCallback<AuthResponse.User> callback) {
        api.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getUser() != null) {
                    callback.onResult(Resource.success(response.body().getUser()));
                } else {
                    callback.onResult(Resource.error(loginErrorMessage(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public void register(String username, String email, String password, ResultCallback<Boolean> callback) {
        api.register(new RegisterRequest(username, email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onResult(Resource.success(Boolean.TRUE));
                } else {
                    callback.onResult(Resource.error(registerErrorMessage(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    private static String loginErrorMessage(Response<AuthResponse> response) {
        String message = extractMessage(response);
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

    private static String registerErrorMessage(Response<AuthResponse> response) {
        String message = extractMessage(response);
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

    private static String extractMessage(Response<AuthResponse> response) {
        if (response.body() != null && response.body().getMessage() != null) {
            return response.body().getMessage();
        }
        if (response.errorBody() != null) {
            try {
                String raw = response.errorBody().string();
                AuthResponse parsed = new Gson().fromJson(raw, AuthResponse.class);
                return (parsed != null && parsed.getMessage() != null) ? parsed.getMessage() : raw;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
