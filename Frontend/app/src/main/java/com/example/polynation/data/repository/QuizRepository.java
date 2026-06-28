package com.example.polynation.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.QuizDetailResponse;
import com.example.polynation.data.remote.dto.QuizzesResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizRepository {

    private final Context appContext;
    private final ApiService api = ApiClient.getApiService();

    public QuizRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void getQuizzes(ResultCallback<List<QuizzesResponse.QuizItem>> callback) {
        List<QuizzesResponse.QuizItem> cached = LocalDataCache.getQuizzesList(appContext);
        if (cached != null && !cached.isEmpty()) {
            callback.onResult(Resource.success(cached));
            return;
        }
        api.getAllQuizzes().enqueue(new Callback<QuizzesResponse>() {
            @Override
            public void onResponse(@NonNull Call<QuizzesResponse> call, @NonNull Response<QuizzesResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    List<QuizzesResponse.QuizItem> quizzes = response.body().getData();
                    LocalDataCache.saveQuizzesList(appContext, quizzes);
                    callback.onResult(Resource.success(quizzes));
                } else {
                    callback.onResult(Resource.error(AppToast.ERR_SERVER));
                }
            }

            @Override
            public void onFailure(@NonNull Call<QuizzesResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public List<QuizzesResponse.QuizItem> getCachedQuizzes() {
        return LocalDataCache.getQuizzesList(appContext);
    }

    public void getQuizById(int quizId, ResultCallback<QuizDetailResponse.QuizData> callback) {
        api.getQuizById(quizId).enqueue(new Callback<QuizDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<QuizDetailResponse> call, @NonNull Response<QuizDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onResult(Resource.success(response.body().getData()));
                } else {
                    callback.onResult(Resource.error(AppToast.ERR_SERVER));
                }
            }

            @Override
            public void onFailure(@NonNull Call<QuizDetailResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public int getSolvedCount(int userId) {
        return LocalDataCache.getQuizzesSolved(appContext, userId);
    }

    public int incrementSolved(int userId) {
        return LocalDataCache.incrementQuizzesSolved(appContext, userId);
    }
}
