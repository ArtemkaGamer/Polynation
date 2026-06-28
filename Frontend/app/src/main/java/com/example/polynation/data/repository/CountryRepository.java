package com.example.polynation.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.data.remote.dto.CountryDetailsResponse;
import com.example.polynation.domain.BackgroundCacheLoader;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CountryRepository {

    private final Context appContext;
    private final ApiService api = ApiClient.getApiService();

    public CountryRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void getCountries(ResultCallback<List<CountriesResponse.Country>> callback) {
        List<CountriesResponse.Country> cached = LocalDataCache.getCountriesList(appContext);
        if (cached != null && !cached.isEmpty()) {
            callback.onResult(Resource.success(cached));
            return;
        }
        api.getAllCountries().enqueue(new Callback<CountriesResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountriesResponse> call, @NonNull Response<CountriesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CountriesResponse.Country> countries = response.body().getData();
                    LocalDataCache.saveCountriesList(appContext, countries);
                    callback.onResult(Resource.success(countries));
                } else {
                    callback.onResult(Resource.error("Не удалось загрузить маркеры стран"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CountriesResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public void getCountryDetails(String russianName, String englishName,
                                  ResultCallback<CountryDetailsResponse.CountryDetails> callback) {
        CountryDetailsResponse.CountryDetails cached = LocalDataCache.getCountryDetails(appContext, russianName);
        boolean stale = LocalDataCache.isCountryDetailsStale(appContext, russianName);
        if (cached != null) {
            callback.onResult(Resource.success(cached));
            if (stale) {
                fetchDetails(russianName, englishName, true, callback);
            }
        } else {
            BackgroundCacheLoader.getInstance(appContext).prioritize(russianName);
            fetchDetails(russianName, englishName, false, callback);
        }
    }

    private void fetchDetails(String russianName, String englishName, boolean silentOnError,
                             ResultCallback<CountryDetailsResponse.CountryDetails> callback) {
        api.getCountryDetails(russianName, englishName).enqueue(new Callback<CountryDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountryDetailsResponse> call,
                                   @NonNull Response<CountryDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CountryDetailsResponse.CountryDetails details = response.body().getData();
                    if (details != null) {
                        LocalDataCache.saveCountryDetails(appContext, russianName, details);
                        callback.onResult(Resource.success(details));
                    }
                } else if (!silentOnError) {
                    callback.onResult(Resource.error("Данные о стране не найдены"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CountryDetailsResponse> call, @NonNull Throwable t) {
                if (!silentOnError) {
                    callback.onResult(Resource.error(AppToast.ERR_NETWORK));
                }
            }
        });
    }
}
