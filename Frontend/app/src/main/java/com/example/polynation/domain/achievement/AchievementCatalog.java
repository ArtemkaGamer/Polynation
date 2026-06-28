package com.example.polynation.domain.achievement;

import com.example.polynation.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.polynation.domain.achievement.Achievement.Metric;

public final class AchievementCatalog {

    private AchievementCatalog() {}

    private static final List<Achievement> ALL = new ArrayList<>();
    private static final Map<Long, Achievement> BY_ID = new LinkedHashMap<>();

    static {
        add(new Achievement(1, "Первооткрыватель",
                "Добро пожаловать в PolyNation! Ваше путешествие началось.",
                R.drawable.ic_ach_compass, AchievementTier.SPECIAL, Metric.NONE, 0));

        add(new Achievement(2, "Любознательный",
                "Решите свой первый квиз.",
                R.drawable.ic_ach_lightbulb, AchievementTier.BRONZE, Metric.QUIZZES, 1));
        add(new Achievement(3, "Знаток",
                "Решите 5 квизов.",
                R.drawable.ic_ach_book, AchievementTier.SILVER, Metric.QUIZZES, 5));
        add(new Achievement(4, "Эрудит",
                "Решите 15 квизов.",
                R.drawable.ic_ach_books, AchievementTier.GOLD, Metric.QUIZZES, 15));
        add(new Achievement(5, "Магистр викторин",
                "Решите 30 квизов.",
                R.drawable.ic_ach_graduation, AchievementTier.PLATINUM, Metric.QUIZZES, 30));

        add(new Achievement(6, "Первый штамп",
                "Отметьте первую страну, где вы побывали.",
                R.drawable.ic_ach_pin, AchievementTier.BRONZE, Metric.VISITED, 1));
        add(new Achievement(7, "Путешественник",
                "Отметьте 5 посещённых стран.",
                R.drawable.ic_ach_plane, AchievementTier.SILVER, Metric.VISITED, 5));
        add(new Achievement(8, "Бывалый странник",
                "Отметьте 15 посещённых стран.",
                R.drawable.ic_ach_suitcase, AchievementTier.GOLD, Metric.VISITED, 15));
        add(new Achievement(9, "Гражданин мира",
                "Отметьте 30 посещённых стран.",
                R.drawable.ic_ach_globe, AchievementTier.PLATINUM, Metric.VISITED, 30));

        add(new Achievement(10, "Новичок арены",
                "Наберите 100 очков рейтинга.",
                R.drawable.ic_ach_star, AchievementTier.BRONZE, Metric.RATING, 100));
        add(new Achievement(11, "Восходящая звезда",
                "Наберите 500 очков рейтинга.",
                R.drawable.ic_ach_star_shine, AchievementTier.SILVER, Metric.RATING, 500));
        add(new Achievement(12, "Чемпион",
                "Наберите 1500 очков рейтинга.",
                R.drawable.ic_ach_medal, AchievementTier.GOLD, Metric.RATING, 1500));
        add(new Achievement(13, "Легенда PolyNation",
                "Наберите 3000 очков рейтинга.",
                R.drawable.ic_ach_crown, AchievementTier.PLATINUM, Metric.RATING, 3000));
    }

    private static void add(Achievement a) {
        ALL.add(a);
        BY_ID.put(a.id, a);
    }

    public static List<Achievement> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static int totalCount() {
        return ALL.size();
    }

    public static Achievement byId(long id) {
        return BY_ID.get(id);
    }

    public static List<Achievement> resolve(List<Long> ids) {
        List<Achievement> result = new ArrayList<>();
        if (ids == null) return result;
        for (Long id : ids) {
            Achievement a = BY_ID.get(id);
            if (a != null) result.add(a);
        }
        return result;
    }

    public static Set<Long> evaluateEarned(int quizzes, int visited, int rating) {
        Set<Long> earned = new HashSet<>();
        for (Achievement a : ALL) {
            if (a.isEarned(quizzes, visited, rating)) {
                earned.add(a.id);
            }
        }
        return earned;
    }
}
