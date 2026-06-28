package com.example.polynation.data.repository;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.polynation.BuildConfig;
import com.example.polynation.data.remote.TravelApiClient;
import com.example.polynation.data.remote.dto.FlightPricesResponse;
import com.example.polynation.data.remote.dto.PlaceSuggestion;
import com.example.polynation.data.remote.dto.WhereAmIResponse;
import com.example.polynation.util.FlightFormat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Response;

public class FlightTicketsRepository {
    private static final String TAG = "FlightTickets";
    private static final int LIMIT = 8;
    private static final int ANYWHERE_LIMIT = 5;

    public interface Callback {
        void onLoading();
        void onResult(String originLabel, String destLabel, List<FlightPricesResponse.Flight> flights);
        void onResultFromAnywhere(String destLabel, List<FlightPricesResponse.Flight> flights);
        void onEmpty(String originLabel, String destLabel);
        void onUnavailable(String message);
    }

    private final Context appContext;
    private final Handler main = new Handler(Looper.getMainLooper());

    private static final Map<String, String> iataCache = new ConcurrentHashMap<>();

    public FlightTicketsRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void load(Double originLat, Double originLng,
                     double destLat, double destLng,
                     String capitalName, Callback cb) {
        cb.onLoading();
        new Thread(() -> {
            try {
                String token = BuildConfig.TRAVELPAYOUTS_TOKEN;
                if (token == null || token.trim().isEmpty()) {
                    post(() -> cb.onUnavailable("Сервис билетов скоро будет доступен"));
                    return;
                }
                token = token.trim();

                final String ftoken = token;
                String destIata = resolveDestinationIata(capitalName, destLat, destLng);
                String[] origin = resolveOrigin(originLat, originLng);
                String originIata = origin[0];
                String originLabel = origin[1];

                if (destIata == null) {
                    post(() -> cb.onUnavailable("Не удалось определить аэропорт назначения"));
                    return;
                }
                final String dIata = destIata;
                if (originIata == null) {
                    fallbackToAnywhere(dIata, capitalName, ftoken, cb,
                            () -> cb.onUnavailable("Включите геопозицию, чтобы найти билеты"));
                    return;
                }
                if (originIata.equalsIgnoreCase(destIata)) {
                    post(() -> cb.onUnavailable("Вы уже здесь"));
                    return;
                }

                Response<FlightPricesResponse> resp = TravelApiClient.get()
                        .pricesForDates(originIata, destIata, FlightFormat.CURRENCY, true, "price", LIMIT, token)
                        .execute();
                FlightPricesResponse body = resp.body();

                if (!resp.isSuccessful() || body == null || !body.success || body.data == null) {
                    post(() -> cb.onUnavailable("Не удалось загрузить цены на билеты"));
                    return;
                }
                final String oLabel = originLabel;
                if (body.data.isEmpty()) {
                    fallbackToAnywhere(dIata, capitalName, ftoken, cb,
                            () -> cb.onEmpty(oLabel, capitalName));
                } else {
                    post(() -> cb.onResult(oLabel, capitalName, body.data));
                }
            } catch (Exception e) {
                Log.e(TAG, "tickets error: " + e.getMessage());
                post(() -> cb.onUnavailable("Нет связи с сервисом билетов"));
            }
        }).start();
    }

    private void post(Runnable r) {
        main.post(r);
    }

    private void fallbackToAnywhere(String destIata, String destLabel, String token,
                                    Callback cb, Runnable onNothing) {
        try {
            Response<FlightPricesResponse> resp = TravelApiClient.get()
                    .pricesForDates(null, destIata, FlightFormat.CURRENCY, true, "price", 30, token)
                    .execute();
            FlightPricesResponse body = resp.body();
            if (resp.isSuccessful() && body != null && body.success
                    && body.data != null && !body.data.isEmpty()) {
                List<FlightPricesResponse.Flight> top = topUniqueByOrigin(body.data, ANYWHERE_LIMIT);
                post(() -> cb.onResultFromAnywhere(destLabel, top));
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "anywhere fallback error: " + e.getMessage());
        }
        post(onNothing);
    }

    private static List<FlightPricesResponse.Flight> topUniqueByOrigin(
            List<FlightPricesResponse.Flight> flights, int limit) {
        List<FlightPricesResponse.Flight> sorted = new java.util.ArrayList<>(flights);
        java.util.Collections.sort(sorted, (a, b) -> Double.compare(a.price, b.price));
        java.util.LinkedHashMap<String, FlightPricesResponse.Flight> byOrigin =
                new java.util.LinkedHashMap<>();
        for (FlightPricesResponse.Flight f : sorted) {
            String key = (f.origin != null) ? f.origin : String.valueOf(System.identityHashCode(f));
            if (!byOrigin.containsKey(key)) {
                byOrigin.put(key, f);
                if (byOrigin.size() >= limit) break;
            }
        }
        return new java.util.ArrayList<>(byOrigin.values());
    }

    private String resolveDestinationIata(String capitalName, double lat, double lng) {
        if (capitalName == null || capitalName.isEmpty()) return null;
        String key = "dst:" + capitalName.toLowerCase();
        String cached = iataCache.get(key);
        if (cached != null) return cached;
        String iata = bestIataByName(capitalName, lat, lng);
        if (iata != null) iataCache.put(key, iata);
        return iata;
    }

    private String[] resolveOrigin(Double lat, Double lng) {
        if (lat != null && lng != null) {
            String city = reverseCity(lat, lng);
            if (city != null && !city.isEmpty()) {
                String iata = bestIataByName(city, lat, lng);
                if (iata != null) return new String[]{iata, city};
            }
        }

        try {
            Response<WhereAmIResponse> r = TravelApiClient.get().whereAmI("ru").execute();
            if (r.isSuccessful() && r.body() != null && r.body().iata != null && !r.body().iata.isEmpty()) {
                String label = (r.body().name != null && !r.body().name.isEmpty()) ? r.body().name : r.body().iata;
                return new String[]{r.body().iata, label};
            }
        } catch (Exception ignored) {}
        return new String[]{null, null};
    }

    private String reverseCity(double lat, double lng) {
        try {
            if (!Geocoder.isPresent()) return null;
            Geocoder geocoder = new Geocoder(appContext, new Locale("ru"));
            List<Address> list = geocoder.getFromLocation(lat, lng, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                String city = a.getLocality();
                if (city == null) city = a.getSubAdminArea();
                if (city == null) city = a.getAdminArea();
                return city;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String bestIataByName(String term, double lat, double lng) {
        try {
            Response<List<PlaceSuggestion>> r = TravelApiClient.get()
                    .autocomplete(term, "ru", "city").execute();
            if (!r.isSuccessful() || r.body() == null || r.body().isEmpty()) return null;

            PlaceSuggestion best = null;
            double bestDist = Double.MAX_VALUE;
            for (PlaceSuggestion p : r.body()) {
                if (p.code == null || p.code.isEmpty()) continue;
                double d = (p.coordinates == null)
                        ? Double.MAX_VALUE
                        : squaredDist(lat, lng, p.coordinates.lat, p.coordinates.lon);
                if (d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
            return best != null ? best.code : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static double squaredDist(double la1, double lo1, double la2, double lo2) {
        double dx = la1 - la2, dy = lo1 - lo2;
        return dx * dx + dy * dy;
    }
}
