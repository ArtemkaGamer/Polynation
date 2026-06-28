package com.example.polynation.presentation.quiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.QuizDetailResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.presentation.common.BaseActivity;
import com.example.polynation.util.AppToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizQuestionActivity extends BaseActivity {
    private TextView tvProgress, tvScore, tvQuestion;
    private ProgressBar progressQuiz;
    private LinearLayout containerOptions, layoutResult;
    private TextView tvResultMessage;
    private Button btnNext, btnFinish;

    private QuizViewModel viewModel;
    private int quizId;
    private int userId;
    private String username;
    private List<QuizDetailResponse.Question> questions;
    private int currentQuestionIndex = 0;
    private int currentScore = 0;
    private int totalEarnedScore = 0;
    private boolean isAnswered = false;
    private boolean quizFinished = false;
    private String correctAnswerText = "";
    private final List<OptionItem> currentOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_question);

        quizId = getIntent().getIntExtra("quizId", -1);
        userId = getIntent().getIntExtra("userId", -1);
        username = getIntent().getStringExtra("username");

        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        tvProgress = findViewById(R.id.tv_progress);
        tvScore = findViewById(R.id.tv_score);
        tvQuestion = findViewById(R.id.tv_question);
        progressQuiz = findViewById(R.id.progress_quiz);
        containerOptions = findViewById(R.id.container_options);
        layoutResult = findViewById(R.id.layout_result);
        tvResultMessage = findViewById(R.id.tv_result_message);
        btnNext = findViewById(R.id.btn_next);
        btnFinish = findViewById(R.id.btn_finish);

        btnNext.setOnClickListener(v -> goToNextQuestion());
        btnFinish.setOnClickListener(v -> finishQuiz());

        viewModel.getQuiz().observe(this, this::onQuizLoaded);
        viewModel.getRatingSaved().observe(this, result -> {
            if (result != null && result.isError()) {
                AppToast.show(this, result.message);
            }
        });
        viewModel.loadQuiz(quizId);
    }

    private void onQuizLoaded(Resource<QuizDetailResponse.QuizData> result) {
        if (result == null || result.isLoading()) return;
        if (result.isSuccess() && result.data != null) {
            questions = result.data.getQuestions();
            showQuestion();
        } else {
            AppToast.show(this, result.message);
            finish();
        }
    }

    private void showQuestion() {
        isAnswered = false;
        layoutResult.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        btnFinish.setVisibility(View.GONE);

        if (questions == null || currentQuestionIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        QuizDetailResponse.Question question = questions.get(currentQuestionIndex);
        tvQuestion.setText(question.getQuestionText());

        tvProgress.setText("Вопрос " + (currentQuestionIndex + 1) + " из " + questions.size());
        tvScore.setText(String.valueOf(currentScore));
        if (progressQuiz != null) {
            progressQuiz.setMax(questions.size());
            progressQuiz.setProgress(currentQuestionIndex + 1);
        }

        containerOptions.removeAllViews();

        List<String> options = question.getAnswerOptions();
        if (options == null || options.isEmpty()) {
            goToNextQuestion();
            return;
        }
        String correctAnswer = options.get(0);
        correctAnswerText = correctAnswer;

        currentOptions.clear();
        for (int i = 0; i < options.size(); i++) {
            currentOptions.add(new OptionItem(options.get(i), options.get(i).equals(correctAnswer)));
        }

        Collections.shuffle(currentOptions);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < currentOptions.size(); i++) {
            OptionItem option = currentOptions.get(i);
            View optionView = inflater.inflate(R.layout.item_quiz_option, containerOptions, false);
            TextView tvOptionText = optionView.findViewById(R.id.tv_option_text);
            TextView tvOptionLetter = optionView.findViewById(R.id.tv_option_letter);

            tvOptionText.setText(option.getText());
            tvOptionLetter.setText(optionLetter(i));

            final int selectedIndex = i;
            optionView.setOnClickListener(v -> checkAnswer(selectedIndex, optionView));

            containerOptions.addView(optionView);
        }
    }

    private void checkAnswer(int selectedIndex, View selectedView) {
        if (isAnswered) return;
        isAnswered = true;

        OptionItem selectedOption = currentOptions.get(selectedIndex);
        boolean isCorrect = selectedOption.isCorrect();

        for (int i = 0; i < containerOptions.getChildCount(); i++) {
            View child = containerOptions.getChildAt(i);
            TextView tvText = child.findViewById(R.id.tv_option_text);
            TextView tvLetter = child.findViewById(R.id.tv_option_letter);
            ImageView ivCheck = child.findViewById(R.id.iv_check);
            OptionItem option = currentOptions.get(i);

            if (option.isCorrect()) {
                child.setBackground(ContextCompat.getDrawable(this, R.drawable.quiz_option_correct_bg));
                tvText.setTextColor(0xFFFFFFFF);
                styleOptionBadge(tvLetter, 0xFFFFFFFF, 0xFF16A34A);
                ivCheck.setVisibility(View.VISIBLE);
                ivCheck.setImageResource(R.drawable.ic_check_circle);
                ivCheck.setColorFilter(0xFFFFFFFF);
            } else if (i == selectedIndex && !isCorrect) {
                child.setBackground(ContextCompat.getDrawable(this, R.drawable.quiz_option_wrong_bg));
                tvText.setTextColor(0xFFFFFFFF);
                styleOptionBadge(tvLetter, 0xFFFFFFFF, 0xFFDC2626);
                ivCheck.setVisibility(View.VISIBLE);
                ivCheck.setImageResource(R.drawable.ic_close_circle);
                ivCheck.setColorFilter(0xFFFFFFFF);
            } else {
                child.setBackground(ContextCompat.getDrawable(this, R.drawable.quiz_option_bg));
                tvText.setTextColor(0xFF1B261F);
                styleOptionBadge(tvLetter, 0xFFF1E7DB, 0xFFBA5C32);
                ivCheck.setVisibility(View.GONE);
                ivCheck.setColorFilter(null);
            }

            child.setClickable(false);
        }

        if (isCorrect) {
            currentScore += 10;
            totalEarnedScore += 10;
            layoutResult.setBackground(ContextCompat.getDrawable(this, R.drawable.result_message_bg));
            tvResultMessage.setText("Правильно! +10 очков");
            tvResultMessage.setTextColor(0xFF16A34A);
        } else {
            layoutResult.setBackground(ContextCompat.getDrawable(this, R.drawable.result_message_wrong_bg));
            tvResultMessage.setText("Неправильно. Правильный ответ: " + correctAnswerText);
            tvResultMessage.setTextColor(0xFFDC2626);
        }

        layoutResult.setVisibility(View.VISIBLE);
        tvScore.setText(String.valueOf(currentScore));

        if (currentQuestionIndex + 1 >= questions.size()) {
            btnFinish.setVisibility(View.VISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
        }
    }

    private void goToNextQuestion() {
        currentQuestionIndex++;
        showQuestion();
    }

    private String optionLetter(int index) {
        String[] letters = {"А", "Б", "В", "Г", "Д", "Е"};
        return index < letters.length ? letters[index] : String.valueOf(index + 1);
    }

    private void styleOptionBadge(TextView badge, int bgColor, int textColor) {
        badge.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        badge.setTextColor(textColor);
    }

    private void finishQuiz() {
        if (quizFinished) return;
        quizFinished = true;

        viewModel.finishQuiz(userId, totalEarnedScore);

        AppToast.showLong(this, "Квиз завершён! +" + totalEarnedScore + " очков");
        returnToQuizzes();
    }

    private void returnToQuizzes() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("userId", userId);
        editor.putString("username", username);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        Intent intent = new Intent(this, QuizzesActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("username", username);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private static class OptionItem {
        private final String text;
        private final boolean isCorrect;

        OptionItem(String text, boolean isCorrect) {
            this.text = text;
            this.isCorrect = isCorrect;
        }

        public String getText() { return text; }
        public boolean isCorrect() { return isCorrect; }
    }
}
