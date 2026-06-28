package com.example.polynation;

public class AuthResponse {
    private boolean success;
    private String message;
    private User user;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public User getUser() { return user; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setUser(User user) { this.user = user; }

    public static class User {
        private int id;
        private String username;
        private String email;
        private String role;
        private String createdAt;
        private int rating;
        private int quizzesSolved;

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getCreatedAt() { return createdAt; }
        public int getRating() { return rating; }
        public int getQuizzesSolved() { return quizzesSolved; }

        public void setId(int id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
        public void setEmail(String email) { this.email = email; }
        public void setRole(String role) { this.role = role; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public void setRating(int rating) { this.rating = rating; }
        public void setQuizzesSolved(int quizzesSolved) { this.quizzesSolved = quizzesSolved; }
    }
}
