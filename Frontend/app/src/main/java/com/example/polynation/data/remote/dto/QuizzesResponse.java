package com.example.polynation.data.remote.dto;

import java.util.List;

public class QuizzesResponse {
    private boolean success;
    private String message;
    private List<QuizItem> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<QuizItem> getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(List<QuizItem> data) { this.data = data; }

    public static class QuizItem {
        private int id;
        private int countryId;
        private String countryName;
        private String title;
        private String type;
        private int questionCount;

        public int getId() { return id; }
        public int getCountryId() { return countryId; }
        public String getCountryName() { return countryName; }
        public String getTitle() { return title; }
        public String getType() { return type; }
        public int getQuestionCount() { return questionCount; }

        public void setId(int id) { this.id = id; }
        public void setCountryId(int countryId) { this.countryId = countryId; }
        public void setCountryName(String countryName) { this.countryName = countryName; }
        public void setTitle(String title) { this.title = title; }
        public void setType(String type) { this.type = type; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    }
}
