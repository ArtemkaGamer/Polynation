package com.example.polynation.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.VisitPoint;
import com.example.polynation.data.remote.dto.VisitPointRequest;
import com.example.polynation.data.remote.dto.VisitPointResponse;
import com.example.polynation.data.remote.dto.VisitPointsResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisitPointRepository {

    private final Context appContext;
    private final ApiService api = ApiClient.getApiService();

    public VisitPointRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public List<VisitPoint> getCached(int userId) {
        return LocalDataCache.getVisitPoints(appContext, userId);
    }

    public void saveLocal(int userId, List<VisitPoint> points) {
        LocalDataCache.saveVisitPoints(appContext, userId, points);
    }

    public void getVisitPoints(int userId, ResultCallback<List<VisitPoint>> callback) {
        final List<VisitPoint> cached = getCached(userId);
        if (cached != null) {
            callback.onResult(Resource.success(cached));
        }
        api.getVisitPoints(userId).enqueue(new Callback<VisitPointsResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitPointsResponse> call,
                                   @NonNull Response<VisitPointsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<VisitPoint> points = response.body().getData();
                    saveLocal(userId, points);
                    callback.onResult(Resource.success(points));
                } else if (cached == null) {
                    callback.onResult(Resource.error("Не удалось загрузить список"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitPointsResponse> call, @NonNull Throwable t) {
                if (cached == null) {
                    callback.onResult(Resource.error(AppToast.ERR_NETWORK));
                }
            }
        });
    }

    public void addVisitPoint(int userId, double lat, double lon, String label,
                              ResultCallback<VisitPoint> callback) {
        VisitPointRequest request = new VisitPointRequest(userId, lat, lon, label);
        api.addVisitPoint(request).enqueue(new Callback<VisitPointResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitPointResponse> call,
                                   @NonNull Response<VisitPointResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    callback.onResult(Resource.success(response.body().getData()));
                } else {
                    callback.onResult(Resource.error("Не удалось сохранить отметку"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitPointResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public void deleteVisitPoint(int pointId, ResultCallback<Boolean> callback) {
        api.deleteVisitPoint(pointId).enqueue(new Callback<VisitPointResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitPointResponse> call,
                                   @NonNull Response<VisitPointResponse> response) {
                boolean ok = response.isSuccessful() && response.body() != null && response.body().isSuccess();
                if (ok) {
                    callback.onResult(Resource.success(Boolean.TRUE));
                } else {
                    callback.onResult(Resource.error("Не удалось убрать отметку"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitPointResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }
}
