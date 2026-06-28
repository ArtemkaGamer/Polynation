package com.example.polynation.presentation.quiz;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.QuizDetailResponse;
import com.example.polynation.data.repository.QuizRepository;
import com.example.polynation.data.repository.RatingRepository;
import com.example.polynation.domain.model.Resource;

public class QuizViewModel extends AndroidViewModel {

    private final QuizRepository quizRepository;
    private final RatingRepository ratingRepository = new RatingRepository();

    private final MutableLiveData<Resource<QuizDetailResponse.QuizData>> quiz = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> ratingSaved = new MutableLiveData<>();

    public QuizViewModel(@NonNull Application application) {
        super(application);
        quizRepository = new QuizRepository(application);
    }

    public LiveData<Resource<QuizDetailResponse.QuizData>> getQuiz() {
        return quiz;
    }

    public LiveData<Resource<Boolean>> getRatingSaved() {
        return ratingSaved;
    }

    public void loadQuiz(int quizId) {
        quiz.setValue(Resource.loading());
        quizRepository.getQuizById(quizId, quiz::setValue);
    }

    public void finishQuiz(int userId, int earnedScore) {
        quizRepository.incrementSolved(userId);
        ratingRepository.addRating(userId, earnedScore, ratingSaved::setValue);
    }
}
