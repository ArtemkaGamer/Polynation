package com.example.polynation;

public class UserProfile {
    private int id;
    private String username;
    private String email;
    private String role;
    private String createdAt;
    private int rating;
    private int quizzesSolved;

    public UserProfile(int id, String username, String email, String role, String createdAt, int rating, int quizzesSolved) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.rating = rating;
        this.quizzesSolved = quizzesSolved;
    }

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
