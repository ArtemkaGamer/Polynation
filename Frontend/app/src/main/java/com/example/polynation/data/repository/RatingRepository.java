package com.example.polynation.data.repository;

import androidx.annotation.NonNull;

import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.RatingRequest;
import com.example.polynation.data.remote.dto.RatingResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RatingRepository {

    private final ApiService api = ApiClient.getApiService();

    public void addRating(int userId, int points, ResultCallback<Boolean> callback) {
        api.addRating(userId, new RatingRequest(points)).enqueue(new Callback<RatingResponse>() {
            @Override
            public void onResponse(@NonNull Call<RatingResponse> call, @NonNull Response<RatingResponse> response) {
                boolean ok = response.isSuccessful() && response.body() != null && response.body().isSuccess();
                if (ok) {
                    callback.onResult(Resource.success(Boolean.TRUE));
                } else {
                    callback.onResult(Resource.error("Не удалось сохранить очки"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RatingResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error("Не удалось сохранить очки"));
            }
        });
    }
}
