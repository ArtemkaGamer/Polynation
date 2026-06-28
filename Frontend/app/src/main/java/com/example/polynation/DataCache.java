package com.example.polynation;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class DataCache {
    private static final String PREF_NAME = "polynation_cache";
    private static final String KEY_COUNTRIES_LIST = "countries_list";

    private static final String KEY_COUNTRY_DETAILS_PREFIX = "country_details_v2_";
    private static final String KEY_COUNTRY_DETAILS_TS_PREFIX = "country_details_ts_v2_";
    private static final String KEY_QUIZZES_LIST = "quizzes_list";
    private static final String KEY_VISIT_POINTS_PREFIX = "visit_points_";

    private static final String KEY_QUIZZES_SOLVED_PREFIX = "quizzes_solved_";

    private static final long DETAILS_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveCountriesList(Context context, List<CountriesResponse.Country> countries) {
        Gson gson = new Gson();
        String json = gson.toJson(countries);
        getPrefs(context).edit().putString(KEY_COUNTRIES_LIST, json).apply();
    }

    public static List<CountriesResponse.Country> getCountriesList(Context context) {
        String json = getPrefs(context).getString(KEY_COUNTRIES_LIST, null);
        if (json == null) return null;
        Gson gson = new Gson();
        Type type = new TypeToken<List<CountriesResponse.Country>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public static void saveCountryDetails(Context context, String countryName, CountryDetailsResponse.CountryDetails details) {
        Gson gson = new Gson();
        String json = gson.toJson(details);
        getPrefs(context).edit()
                .putString(KEY_COUNTRY_DETAILS_PREFIX + countryName, json)
                .putLong(KEY_COUNTRY_DETAILS_TS_PREFIX + countryName, System.currentTimeMillis())
                .apply();
    }

    public static CountryDetailsResponse.CountryDetails getCountryDetails(Context context, String countryName) {
        String key = KEY_COUNTRY_DETAILS_PREFIX + countryName;
        String json = getPrefs(context).getString(key, null);
        if (json == null) return null;
        Gson gson = new Gson();
        return gson.fromJson(json, CountryDetailsResponse.CountryDetails.class);
    }

    public static boolean isCountryDetailsStale(Context context, String countryName) {
        long ts = getPrefs(context).getLong(KEY_COUNTRY_DETAILS_TS_PREFIX + countryName, 0L);
        return System.currentTimeMillis() - ts > DETAILS_TTL_MILLIS;
    }

    public static boolean hasCountriesCache(Context context) {
        return getPrefs(context).contains(KEY_COUNTRIES_LIST);
    }

    public static void saveQuizzesList(Context context, List<QuizzesResponse.QuizItem> quizzes) {
        Gson gson = new Gson();
        String json = gson.toJson(quizzes);
        getPrefs(context).edit().putString(KEY_QUIZZES_LIST, json).apply();
    }

    public static List<QuizzesResponse.QuizItem> getQuizzesList(Context context) {
        String json = getPrefs(context).getString(KEY_QUIZZES_LIST, null);
        if (json == null) return null;
        Gson gson = new Gson();
        Type type = new TypeToken<List<QuizzesResponse.QuizItem>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public static void saveVisitPoints(Context context, int userId, List<VisitPoint> points) {
        Gson gson = new Gson();
        String json = gson.toJson(points);
        getPrefs(context).edit().putString(KEY_VISIT_POINTS_PREFIX + userId, json).apply();
    }

    public static List<VisitPoint> getVisitPoints(Context context, int userId) {
        String json = getPrefs(context).getString(KEY_VISIT_POINTS_PREFIX + userId, null);
        if (json == null) return null;
        Gson gson = new Gson();
        Type type = new TypeToken<List<VisitPoint>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public static int getQuizzesSolved(Context context, int userId) {
        return getPrefs(context).getInt(KEY_QUIZZES_SOLVED_PREFIX + userId, 0);
    }

    public static void setQuizzesSolved(Context context, int userId, int count) {
        getPrefs(context).edit().putInt(KEY_QUIZZES_SOLVED_PREFIX + userId, count).apply();
    }

    public static int incrementQuizzesSolved(Context context, int userId) {
        int next = getQuizzesSolved(context, userId) + 1;
        setQuizzesSolved(context, userId, next);
        return next;
    }
}
