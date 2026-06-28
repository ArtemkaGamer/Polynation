package com.example.polynation.presentation.quiz;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.QuizzesResponse;
import com.example.polynation.data.repository.QuizRepository;
import com.example.polynation.domain.model.Resource;

import java.util.List;

public class QuizzesViewModel extends AndroidViewModel {

    private final QuizRepository quizRepository;
    private final MutableLiveData<Resource<List<QuizzesResponse.QuizItem>>> quizzes = new MutableLiveData<>();

    public QuizzesViewModel(@NonNull Application application) {
        super(application);
        quizRepository = new QuizRepository(application);
    }

    public LiveData<Resource<List<QuizzesResponse.QuizItem>>> getQuizzes() {
        return quizzes;
    }

    public void loadQuizzes() {
        quizzes.setValue(Resource.loading());
        quizRepository.getQuizzes(quizzes::setValue);
    }
}
