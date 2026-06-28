package com.example.polynation.domain;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.data.remote.dto.CountryDetailsResponse;
import com.example.polynation.data.remote.dto.QuizzesResponse;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;

public final class BackgroundCacheLoader {
    private static final String TAG = "BackgroundCache";

    private static final long IMAGE_PRELOAD_TIMEOUT_SECONDS = 20;

    private static volatile BackgroundCacheLoader instance;

    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object queueLock = new Object();
    private final LinkedList<CountriesResponse.Country> detailsQueue = new LinkedList<>();
    private boolean started = false;

    public static BackgroundCacheLoader getInstance(Context context) {
        if (instance == null) {
            synchronized (BackgroundCacheLoader.class) {
                if (instance == null) {
                    instance = new BackgroundCacheLoader(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private BackgroundCacheLoader(Context appContext) {
        this.appContext = appContext;
    }

    public void start() {
        synchronized (this) {
            if (started) return;
            started = true;
        }
        executor.execute(() -> {
            boolean success;
            try {
                success = loadCountriesThenRest();
            } catch (Exception e) {
                Log.e(TAG, "Неожиданная ошибка фоновой загрузки", e);
                success = false;
            }
            if (!success) {
                synchronized (BackgroundCacheLoader.this) {
                    started = false;
                }
                Log.d(TAG, "Загрузка не удалась — повторим при следующем обращении к приложению");
            }
        });
    }

    public void prioritize(String russianName) {
        if (russianName == null || russianName.isEmpty()) return;
        synchronized (queueLock) {
            Iterator<CountriesResponse.Country> it = detailsQueue.iterator();
            while (it.hasNext()) {
                CountriesResponse.Country c = it.next();
                if (russianName.equals(c.getName())) {
                    it.remove();
                    detailsQueue.addFirst(c);
                    Log.d(TAG, "Приоритет отдан стране: " + russianName);
                    return;
                }
            }
        }
    }

    private boolean loadCountriesThenRest() {
        List<CountriesResponse.Country> countries = fetchCountriesSync();
        if (countries == null || countries.isEmpty()) {
            Log.d(TAG, "Список стран недоступен — попробуем снова позже");
            return false;
        }
        LocalDataCache.saveCountriesList(appContext, countries);
        Log.d(TAG, "Список стран закэширован: " + countries.size());

        fetchQuizzesSync();

        synchronized (queueLock) {
            detailsQueue.clear();
            for (CountriesResponse.Country country : countries) {
                if (needsDetails(country)) detailsQueue.add(country);
            }
        }
        Log.d(TAG, "Стран требуют загрузки деталей: " + detailsQueue.size());

        processDetailsQueue();
        return true;
    }

    private boolean needsDetails(CountriesResponse.Country country) {
        String name = country.getName();
        return LocalDataCache.getCountryDetails(appContext, name) == null
                || LocalDataCache.isCountryDetailsStale(appContext, name);
    }

    private void processDetailsQueue() {
        while (true) {
            CountriesResponse.Country country;
            synchronized (queueLock) {
                country = detailsQueue.pollFirst();
            }
            if (country == null) break;

            if (!needsDetails(country)) continue;

            loadCountryDetailsSync(country);
        }
        Log.d(TAG, "Фоновая загрузка деталей стран завершена");
    }

    private List<CountriesResponse.Country> fetchCountriesSync() {
        try {
            Response<CountriesResponse> response = ApiClient.getApiService().getAllCountries().execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                return response.body().getData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки списка стран", e);
        }
        return null;
    }

    private void fetchQuizzesSync() {
        try {
            Response<QuizzesResponse> response = ApiClient.getApiService().getAllQuizzes().execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                List<QuizzesResponse.QuizItem> quizzes = response.body().getData();
                LocalDataCache.saveQuizzesList(appContext, quizzes);
                Log.d(TAG, "Квизы закэшированы: " + quizzes.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки квизов", e);
        }
    }

    private void loadCountryDetailsSync(CountriesResponse.Country country) {
        String russianName = country.getName();
        String englishName = (country.getEnglishName() != null && !country.getEnglishName().isEmpty())
                ? country.getEnglishName()
                : CountryNameMapper.getNameForExternal(russianName);
        try {
            Response<CountryDetailsResponse> response = ApiClient.getApiService()
                    .getCountryDetails(russianName, englishName)
                    .execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                CountryDetailsResponse.CountryDetails details = response.body().getData();
                if (details != null) {
                    LocalDataCache.saveCountryDetails(appContext, russianName, details);
                    preloadImages(details);
                    Log.d(TAG, "Закэширована страна: " + russianName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки деталей страны: " + russianName, e);
        }
    }

    private void preloadImages(CountryDetailsResponse.CountryDetails details) {
        if (details.getFlagUrl() != null && !details.getFlagUrl().isEmpty()) {
            preloadImage(details.getFlagUrl());
        }
        if (details.getPhotos() != null) {
            for (String photoUrl : details.getPhotos()) {
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    preloadImage(photoUrl);
                }
            }
        }
    }

    private void preloadImage(String urlStr) {
        try {
            Glide.with(appContext).download(urlStr).submit()
                    .get(IMAGE_PRELOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка предзагрузки изображения: " + urlStr, e);
        }
    }
}
