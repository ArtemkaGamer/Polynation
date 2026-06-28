package com.example.polynation;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizzesActivity extends BaseNavigationActivity {
    private LinearLayout containerQuizzesList;
    private ApiService apiService;
    private List<QuizzesResponse.QuizItem> quizzesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quizzes);

        currentUserId = getIntent().getIntExtra("userId", -1);
        currentUsername = getIntent().getStringExtra("username");

        apiService = ApiClient.getApiService();
        containerQuizzesList = findViewById(R.id.container_quizzes_list);

        loadQuizzes();

        setupBottomNavigation();
        setActiveNavItem("quizzes");
    }

    private void loadQuizzes() {
        List<QuizzesResponse.QuizItem> cachedQuizzes = DataCache.getQuizzesList(this);
        if (cachedQuizzes != null && !cachedQuizzes.isEmpty()) {
            quizzesList = cachedQuizzes;
            displayQuizzes();
            return;
        }

        Call<QuizzesResponse> call = apiService.getAllQuizzes();

        call.enqueue(new Callback<QuizzesResponse>() {
            @Override
            public void onResponse(Call<QuizzesResponse> call, Response<QuizzesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    QuizzesResponse quizzesResponse = response.body();
                    if (quizzesResponse.isSuccess() && quizzesResponse.getData() != null) {
                        quizzesList = quizzesResponse.getData();
                        DataCache.saveQuizzesList(QuizzesActivity.this, quizzesList);
                        displayQuizzes();
                    } else {
                        AppToast.show(QuizzesActivity.this, AppToast.ERR_SERVER);
                    }
                } else {
                    AppToast.show(QuizzesActivity.this, AppToast.ERR_SERVER);
                }
            }

            @Override
            public void onFailure(Call<QuizzesResponse> call, Throwable t) {
                AppToast.show(QuizzesActivity.this, AppToast.ERR_NETWORK);
            }
        });
    }

    private void displayQuizzes() {
        containerQuizzesList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (QuizzesResponse.QuizItem quiz : quizzesList) {
            View itemView = inflater.inflate(R.layout.item_quiz, containerQuizzesList, false);

            TextView tvTitle = itemView.findViewById(R.id.tv_quiz_title);
            TextView tvCountry = itemView.findViewById(R.id.tv_quiz_country);
            TextView tvQuestionCount = itemView.findViewById(R.id.tv_question_count);

            tvTitle.setText(quiz.getTitle());
            tvCountry.setText(quiz.getCountryName());
            tvQuestionCount.setText(quiz.getQuestionCount() + " вопросов");

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(QuizzesActivity.this, QuizQuestionActivity.class);
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
