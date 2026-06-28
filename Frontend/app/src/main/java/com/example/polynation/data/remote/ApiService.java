package com.example.polynation.data.remote;

import com.example.polynation.data.remote.dto.AchievementBatchRequest;
import com.example.polynation.data.remote.dto.AchievementIdsResponse;
import com.example.polynation.data.remote.dto.AchievementsResponse;
import com.example.polynation.data.remote.dto.AuthResponse;
import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.data.remote.dto.CountryDetailsResponse;
import com.example.polynation.data.remote.dto.LoginRequest;
import com.example.polynation.data.remote.dto.QuizDetailResponse;
import com.example.polynation.data.remote.dto.QuizzesResponse;
import com.example.polynation.data.remote.dto.RatingRequest;
import com.example.polynation.data.remote.dto.RatingResponse;
import com.example.polynation.data.remote.dto.RegisterRequest;
import com.example.polynation.data.remote.dto.UpdateUserRequest;
import com.example.polynation.data.remote.dto.UserProfileResponse;
import com.example.polynation.data.remote.dto.UsersResponse;
import com.example.polynation.data.remote.dto.VisitImageResponse;
import com.example.polynation.data.remote.dto.VisitImagesResponse;
import com.example.polynation.data.remote.dto.VisitPointRequest;
import com.example.polynation.data.remote.dto.VisitPointResponse;
import com.example.polynation.data.remote.dto.VisitPointsResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
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

    @GET("/api/visit-points/{id}/images")
    Call<VisitImagesResponse> getVisitImages(@Path("id") int visitPointId);

    @Multipart
    @POST("/api/visit-points/{id}/images")
    Call<VisitImageResponse> uploadVisitImage(@Path("id") int visitPointId,
                                              @Part MultipartBody.Part file);

    @DELETE("/api/visit-points/{id}/images/{imageId}")
    Call<VisitImageResponse> deleteVisitImage(@Path("id") int visitPointId,
                                              @Path("imageId") int imageId);

    @GET("/api/users/{userId}/achievements/ids")
    Call<AchievementIdsResponse> getAchievementIds(@Path("userId") int userId);

    @POST("/api/users/{userId}/achievements/batch")
    Call<AchievementsResponse> addAchievementsBatch(
            @Path("userId") int userId, @Body AchievementBatchRequest request);
}
