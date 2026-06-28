package com.example.polynation.domain.achievement;

public class Achievement {

    public enum Metric { NONE, QUIZZES, VISITED, RATING }

    public final long id;
    public final String title;
    public final String description;
    public final int iconRes;
    public final AchievementTier tier;
    public final Metric metric;
    public final int threshold;

    public Achievement(long id, String title, String description, int iconRes,
                       AchievementTier tier, Metric metric, int threshold) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.tier = tier;
        this.metric = metric;
        this.threshold = threshold;
    }

    public boolean isEarned(int quizzes, int visited, int rating) {
        switch (metric) {
            case QUIZZES: return quizzes >= threshold;
            case VISITED: return visited >= threshold;
            case RATING: return rating >= threshold;
            case NONE:
            default: return true;
        }
    }

    public int currentValue(int quizzes, int visited, int rating) {
        switch (metric) {
            case QUIZZES: return quizzes;
            case VISITED: return visited;
            case RATING: return rating;
            case NONE:
            default: return 0;
        }
    }
}
