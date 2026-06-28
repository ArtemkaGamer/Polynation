package com.example.polynation;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("/api/users/{id}")
    Call<UserProfileResponse> getUserProfile(@Path("id") int id);

    @PATCH("/api/users/{id}")
    Call<UserProfileResponse> updateUser(@Path("id") int id, @Body UpdateUserRequest request);

    @GET("/api/users")
    Call<UsersResponse> getAllUsers();

    @GET("/api/quizzes")
    Call<QuizzesResponse> getAllQuizzes();

    @GET("/api/quizzes/{id}")
    Call<QuizDetailResponse> getQuizById(@Path("id") int quizId);

    @POST("/api/users/{id}/rating")
    Call<RatingResponse> addRating(@Path("id") int userId, @Body RatingRequest request);

    @GET("/api/countries")
    Call<CountriesResponse> getAllCountries();

    @GET("/api/countries/details/{name}")
    Call<CountryDetailsResponse> getCountryDetails(
            @Path("name") String name,
            @Query("nameForExternal") String nameForExternal
    );

    @POST("/api/visit-points")
    Call<VisitPointResponse> addVisitPoint(@Body VisitPointRequest request);

    @GET("/api/visit-points/user/{userId}")
    Call<VisitPointsResponse> getVisitPoints(@Path("userId") int userId);

    @DELETE("/api/visit-points/{id}")
    Call<VisitPointResponse> deleteVisitPoint(@Path("id") int id);
}
