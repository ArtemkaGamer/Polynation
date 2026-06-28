package com.example.polynation.data.remote;

import com.example.polynation.data.remote.dto.FlightPricesResponse;
import com.example.polynation.data.remote.dto.PlaceSuggestion;
import com.example.polynation.data.remote.dto.WhereAmIResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TravelApiService {
    @GET("https://autocomplete.travelpayouts.com/places2")
    Call<List<PlaceSuggestion>> autocomplete(
            @Query("term") String term,
            @Query("locale") String locale,
            @Query(value = "types[]", encoded = true) String type
    );

    @GET("https://www.travelpayouts.com/whereami")
    Call<WhereAmIResponse> whereAmI(@Query("locale") String locale);

    @GET("https://api.travelpayouts.com/aviasales/v3/prices_for_dates")
    Call<FlightPricesResponse> pricesForDates(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("currency") String currency,
            @Query("one_way") boolean oneWay,
            @Query("sorting") String sorting,
            @Query("limit") int limit,
            @Query("token") String token
    );
}
