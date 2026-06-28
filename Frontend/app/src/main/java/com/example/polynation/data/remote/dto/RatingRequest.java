package com.example.polynation.data.remote.dto;

public class RatingRequest {
    private int points;

    public RatingRequest(int points) {
        this.points = points;
    }

    public int getPoints() { return points; }
}
