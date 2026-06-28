package com.example.polynation.presentation.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.QuizzesResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.common.BaseNavigationActivity;
import com.example.polynation.util.AppToast;

import java.util.List;

public class QuizzesActivity extends BaseNavigationActivity {
    private LinearLayout containerQuizzesList;
    private QuizzesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quizzes);

        currentUserId = getIntent().getIntExtra("userId", -1);
        currentUsername = getIntent().getStringExtra("username");

        containerQuizzesList = findViewById(R.id.container_quizzes_list);
        viewModel = new ViewModelProvider(this).get(QuizzesViewModel.class);
        viewModel.getQuizzes().observe(this, this::renderQuizzes);
        viewModel.loadQuizzes();

        setupBottomNavigation();
        setActiveNavItem("quizzes");

        notifyAssistantScreenShown("quizzes");
    }

    private void renderQuizzes(Resource<List<QuizzesResponse.QuizItem>> result) {
        if (result == null || result.isLoading()) return;
        if (!result.isSuccess() || result.data == null) {
            AppToast.show(this, result.message);
            return;
        }
        displayQuizzes(result.data);
    }

    private void displayQuizzes(List<QuizzesResponse.QuizItem> quizzes) {
        containerQuizzesList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (QuizzesResponse.QuizItem quiz : quizzes) {
            View itemView = inflater.inflate(R.layout.item_quiz, containerQuizzesList, false);

            TextView tvTitle = itemView.findViewById(R.id.tv_quiz_title);
            TextView tvCountry = itemView.findViewById(R.id.tv_quiz_country);
            TextView tvQuestionCount = itemView.findViewById(R.id.tv_question_count);

            tvTitle.setText(quiz.getTitle());
            tvCountry.setText(quiz.getCountryName());
            tvQuestionCount.setText(quiz.getQuestionCount() + " вопросов");

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(this, QuizQuestionActivity.class);
                intent.putExtra("quizId", quiz.getId());
                intent.putExtra("quizTitle", quiz.getTitle());
                intent.putExtra("userId", currentUserId);
                intent.putExtra("username", currentUsername);
                startActivity(intent);
            });

            containerQuizzesList.addView(itemView);
        }
    }
}
