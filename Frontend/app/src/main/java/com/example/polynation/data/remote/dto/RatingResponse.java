package com.example.polynation.data.remote.dto;

public class RatingResponse {
    private boolean success;
    private String message;
    private RatingData data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public RatingData getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(RatingData data) { this.data = data; }

    public static class RatingData {
        private int id;
        private String username;
        private int rating;

        public int getId() { return id; }
        public String getUsername() { return username; }
        public int getRating() { return rating; }

        public void setId(int id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
        public void setRating(int rating) { this.rating = rating; }
    }
}
