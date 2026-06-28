package com.example.polynation.data.remote.dto;

import java.util.List;

public class QuizDetailResponse {
    private boolean success;
    private String message;
    private QuizData data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public QuizData getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(QuizData data) { this.data = data; }

    public static class QuizData {
        private int id;
        private int countryId;
        private String countryName;
        private String title;
        private String type;
        private int questionCount;
        private List<Question> questions;

        public int getId() { return id; }
        public int getCountryId() { return countryId; }
        public String getCountryName() { return countryName; }
        public String getTitle() { return title; }
        public String getType() { return type; }
        public int getQuestionCount() { return questionCount; }
        public List<Question> getQuestions() { return questions; }

        public void setId(int id) { this.id = id; }
        public void setCountryId(int countryId) { this.countryId = countryId; }
        public void setCountryName(String countryName) { this.countryName = countryName; }
        public void setTitle(String title) { this.title = title; }
        public void setType(String type) { this.type = type; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
        public void setQuestions(List<Question> questions) { this.questions = questions; }
    }

    public static class Question {
        private int id;
        private String questionText;
        private List<String> answerOptions;
        private int orderIndex;

        public int getId() { return id; }
        public String getQuestionText() { return questionText; }
        public List<String> getAnswerOptions() { return answerOptions; }
        public int getOrderIndex() { return orderIndex; }

        public void setId(int id) { this.id = id; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public void setAnswerOptions(List<String> answerOptions) { this.answerOptions = answerOptions; }
        public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    }
}
