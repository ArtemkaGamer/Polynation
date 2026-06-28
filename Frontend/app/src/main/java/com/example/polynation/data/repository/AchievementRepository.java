package com.example.polynation.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.AchievementBatchRequest;
import com.example.polynation.data.remote.dto.AchievementIdsResponse;
import com.example.polynation.data.remote.dto.AchievementsResponse;
import com.example.polynation.domain.achievement.Achievement;
import com.example.polynation.domain.achievement.AchievementCatalog;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AchievementRepository {

    private final Context appContext;
    private final ApiService api = ApiClient.getApiService();

    public AchievementRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void syncAndGet(int userId, int quizzes, int visited, int rating,
                           ResultCallback<AchievementSyncResult> callback) {
        final Set<Long> earned = AchievementCatalog.evaluateEarned(quizzes, visited, rating);
        final Set<Long> previouslyKnown = new HashSet<>(safeCached(userId));

        api.getAchievementIds(userId).enqueue(new Callback<AchievementIdsResponse>() {
            @Override
            public void onResponse(@NonNull Call<AchievementIdsResponse> call,
                                   @NonNull Response<AchievementIdsResponse> response) {
                Set<Long> serverIds = new HashSet<>();
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    serverIds.addAll(response.body().getData());
                }

                final Set<Long> union = new HashSet<>(serverIds);
                union.addAll(earned);

                List<Long> toAdd = new ArrayList<>();
                for (Long id : earned) {
                    if (!serverIds.contains(id)) toAdd.add(id);
                }

                if (toAdd.isEmpty()) {
                    finish(union, previouslyKnown, callback, userId);
                } else {
                    pushBatch(userId, toAdd, () -> finish(union, previouslyKnown, callback, userId));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AchievementIdsResponse> call, @NonNull Throwable t) {
                Set<Long> union = new HashSet<>(previouslyKnown);
                union.addAll(earned);
                finish(union, previouslyKnown, callback, userId);
            }
        });
    }

    private void pushBatch(int userId, List<Long> ids, Runnable onDone) {
        api.addAchievementsBatch(userId, new AchievementBatchRequest(ids))
                .enqueue(new Callback<AchievementsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AchievementsResponse> call,
                                           @NonNull Response<AchievementsResponse> response) {
                        onDone.run();
                    }

                    @Override
                    public void onFailure(@NonNull Call<AchievementsResponse> call, @NonNull Throwable t) {
                        onDone.run();
                    }
                });
    }

    private void finish(Set<Long> union, Set<Long> previouslyKnown,
                        ResultCallback<AchievementSyncResult> callback, int userId) {
        List<Long> all = new ArrayList<>(union);
        Collections.sort(all);
        LocalDataCache.saveAchievementIds(appContext, userId, all);

        List<Long> newly = new ArrayList<>();
        if (!previouslyKnown.isEmpty()) {
            for (Long id : all) {
                if (previouslyKnown.contains(id)) continue;
                Achievement a = AchievementCatalog.byId(id);
                if (a != null && a.metric == Achievement.Metric.NONE) continue;
                newly.add(id);
            }
        }
        callback.onResult(Resource.success(new AchievementSyncResult(all, newly)));
    }

    public void getEarnedIds(int userId, ResultCallback<List<Long>> callback) {
        api.getAchievementIds(userId).enqueue(new Callback<AchievementIdsResponse>() {
            @Override
            public void onResponse(@NonNull Call<AchievementIdsResponse> call,
                                   @NonNull Response<AchievementIdsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onResult(Resource.success(response.body().getData()));
                } else {
                    callback.onResult(Resource.error(AppToast.ERR_SERVER));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AchievementIdsResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public List<Long> getCachedIds(int userId) {
        return safeCached(userId);
    }

    private List<Long> safeCached(int userId) {
        List<Long> cached = LocalDataCache.getAchievementIds(appContext, userId);
        return cached != null ? cached : new ArrayList<>();
    }
}
