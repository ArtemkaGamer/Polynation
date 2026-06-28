package com.example.polynation.presentation.assistant;

import com.example.polynation.R;
import com.example.polynation.presentation.common.BaseNavigationActivity;
import com.example.polynation.presentation.map.MapActivity;
import com.example.polynation.presentation.profile.ProfileActivity;
import com.example.polynation.presentation.quiz.QuizzesActivity;
import com.example.polynation.presentation.rating.RatingActivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import java.util.Random;

public final class AssistantManager {

    private static final String PREFS = "assistant_prefs";
    private static final String K_DONE = "onboarding_done";
    private static final String K_ACTIVE = "tour_active";
    private static final String K_STEP = "tour_step";
    private static final String K_LAST_PEEK = "last_peek_time";

    private static final long REACT_COOLDOWN_MS = 20_000L;
    private static final long ANTI_DOUBLE_MS = 3_500L;

    private static final long PEEK_DURATION_MS = 4_200L;

    private static final Random RND = new Random();

    private AssistantManager() {}

    private static final String[] RETURN_GREETINGS = {
            "С возвращением, %s!",
            "Рад снова тебя видеть, %s.",
            "А вот и ты, %s! Готов к новым открытиям?",
            "Снова в путь, %s? Мир уже ждёт.",
            "Привет, %s! Куда отправимся сегодня?"
    };

    private static final String[] COUNTRY_OPEN = {
            "Вау, %s — интересная страна!",
            "О, %s! Отличный выбор.",
            "%s — здесь точно есть на что посмотреть.",
            "Любопытно... про %s я знаю немало.",
            "%s? Прекрасное направление."
    };

    private static final String[] VISITED = {
            "Завидую — ты побывал в %s!",
            "%s теперь в твоей коллекции. Так держать!",
            "Круто! Ещё одна страна отмечена.",
            "Здорово, что ты добрался до %s.",
            "%s — отличное приключение!"
    };

    private static final class Step {
        final String screen;
        final int targetId;
        final String message;
        final boolean last;

        Step(String screen, int targetId, String message, boolean last) {
            this.screen = screen;
            this.targetId = targetId;
            this.message = message;
            this.last = last;
        }
    }

    private static Step[] steps() {
        return new Step[]{
                new Step("map", 0,
                        "Привет, %s! Я Поли — твой проводник по миру PolyNation. Прокачусь по экранам и за минуту покажу всё самое важное.",
                        false),
                new Step("map", R.id.mapView,
                        "Это карта мира. Приближай и тапай по любой стране — откроется её карточка: флаг и столица, история, культура, музыка, кино и спорт, фотографии, а ещё кнопка квиза по стране и актуальные цены на авиабилеты.",
                        false),
                new Step("map", R.id.fab_search,
                        "Лупа — быстрый поиск по названию. Введи страну или столицу, и карта сама перелетит к ней.",
                        false),
                new Step("map", R.id.fab_my_location,
                        "Эта кнопка — твоя геопозиция. Нажми, чтобы найти себя на карте и посмотреть, что рядом.",
                        false),
                new Step("map", R.id.fab_visited_filter,
                        "А здесь живут страны, где ты был. Отмечай их в карточке — на карте они станут зелёными, а этой кнопкой можно показать только их.",
                        false),
                new Step("map", R.id.bottom_navigation_layout,
                        "Внизу — меню разделов: карта, квизы, рейтинг и профиль. Поехали в «Квизы».",
                        false),
                new Step("quizzes", R.id.container_quizzes_list,
                        "Квизы — проверяй знания о странах, отвечай на вопросы и зарабатывай очки за правильные ответы.",
                        false),
                new Step("rating", R.id.container_rating_list,
                        "Рейтинг — здесь все игроки по очкам. Проходи квизы и поднимайся в топ.",
                        false),
                new Step("profile", R.id.profile_stats_row,
                        "А это твой профиль. Тут наглядно: заработанные очки, число пройденных квизов и сколько стран ты уже посетил.",
                        false),
                new Step("profile", R.id.btn_edit,
                        "Кнопкой «Редактировать» можно сменить имя и убрать страну из списка посещённых. Ниже — список этих стран, а в самом низу кнопка выхода из аккаунта.",
                        false),
                new Step("profile", 0,
                        "Вот и всё! Если что — я всегда рядом и подскажу. Удачных открытий, %s!",
                        true)
        };
    }

    public static void onMapEntered(BaseNavigationActivity activity, String username, boolean fromAuth) {
        SharedPreferences p = prefs(activity);
        if (!p.getBoolean(K_DONE, false)) {
            if (p.getBoolean(K_ACTIVE, false)) {
                int idx = findStep("map", p.getInt(K_STEP, 0));
                if (idx < 0) idx = 0;
                p.edit().putInt(K_STEP, idx).apply();
                showStep(activity, "map", idx);
            } else {
                p.edit().putBoolean(K_ACTIVE, true).putInt(K_STEP, 0).apply();
                showStep(activity, "map", 0);
            }
        } else {
            if (fromAuth) {
                peek(activity, formatOne(pick(RETURN_GREETINGS), safeName(username)));
            }
            attachRoamer(activity);
        }
    }

    public static void onScreenShown(BaseNavigationActivity activity, String screenKey) {
        SharedPreferences p = prefs(activity);
        if (p.getBoolean(K_ACTIVE, false)) {
            int idx = findStep(screenKey, p.getInt(K_STEP, 0));
            if (idx >= 0) {
                p.edit().putInt(K_STEP, idx).apply();
                showStep(activity, screenKey, idx);
            }
        }
    }

    public static void reactToCountryOpened(BaseNavigationActivity activity, String country) {
        SharedPreferences p = prefs(activity);
        if (p.getBoolean(K_ACTIVE, false)) return;
        if (System.currentTimeMillis() - p.getLong(K_LAST_PEEK, 0) < REACT_COOLDOWN_MS) return;
        if (RND.nextFloat() > 0.5f) return;
        peek(activity, formatOne(pick(COUNTRY_OPEN), safeCountry(country)));
    }

    public static void reactToVisited(BaseNavigationActivity activity, String country) {
        SharedPreferences p = prefs(activity);
        if (p.getBoolean(K_ACTIVE, false)) return;
        if (System.currentTimeMillis() - p.getLong(K_LAST_PEEK, 0) < ANTI_DOUBLE_MS) return;
        peek(activity, formatOne(pick(VISITED), safeCountry(country)));
    }

    public static void skip(BaseNavigationActivity activity) {
        endTour(activity);
    }

    public static boolean isTourActive(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).getBoolean(K_ACTIVE, false);
    }

    private static void showStep(final BaseNavigationActivity activity, final String screenKey, final int idx) {
        final Step s = steps()[idx];
        final String message = formatOne(s.message, safeName(activity.getCurrentUsername()));
        final AssistantOverlay overlay = AssistantOverlay.obtain(activity);

        View content = activity.findViewById(android.R.id.content);
        content.post(() -> {
            View target = s.targetId != 0 ? activity.findViewById(s.targetId) : null;
            overlay.showCoach(message, target, idx, steps().length, s.last,
                    new AssistantOverlay.CoachListener() {
                        @Override
                        public void onNext() {
                            advance(activity, screenKey, idx);
                        }

                        @Override
                        public void onBack() {
                            back(activity, screenKey, idx);
                        }

                        @Override
                        public void onSkip() {
                            endTour(activity);
                        }
                    });
        });
    }

    private static void advance(BaseNavigationActivity activity, String screenKey, int idx) {
        Step[] all = steps();
        int next = idx + 1;
        if (next >= all.length) {
            endTour(activity);
            activity.tourNavigateTo(MapActivity.class);
            return;
        }
        prefs(activity).edit().putInt(K_STEP, next).apply();

        Step ns = all[next];
        if (ns.screen.equals(screenKey)) {
            showStep(activity, screenKey, next);
        } else {
            activity.tourNavigateTo(classFor(ns.screen));
        }
    }

    private static void back(BaseNavigationActivity activity, String screenKey, int idx) {
        int prev = idx - 1;
        if (prev < 0) return;
        prefs(activity).edit().putInt(K_STEP, prev).apply();

        Step ps = steps()[prev];
        if (ps.screen.equals(screenKey)) {
            showStep(activity, screenKey, prev);
        } else {
            activity.tourNavigateTo(classFor(ps.screen));
        }
    }

    private static void endTour(BaseNavigationActivity activity) {
        prefs(activity).edit()
                .putBoolean(K_ACTIVE, false)
                .putBoolean(K_DONE, true)
                .putInt(K_STEP, 0)
                .apply();
        AssistantOverlay overlay = AssistantOverlay.find(activity);
        if (overlay != null) overlay.dismiss(true, null);
    }

    private static int findStep(String screenKey, int fromStep) {
        Step[] all = steps();
        if (fromStep < 0) fromStep = 0;
        for (int i = fromStep; i < all.length; i++) {
            if (all[i].screen.equals(screenKey)) return i;
        }
        return -1;
    }

    private static Class<?> classFor(String screenKey) {
        switch (screenKey) {
            case "quizzes": return QuizzesActivity.class;
            case "rating": return RatingActivity.class;
            case "profile": return ProfileActivity.class;
            default: return MapActivity.class;
        }
    }

    private static void attachRoamer(final BaseNavigationActivity activity) {
        SharedPreferences p = prefs(activity);
        if (p.getBoolean(K_ACTIVE, false)) return;
        if (!p.getBoolean(K_DONE, false)) return;
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        content.post(() -> {
            PolyRoamerOverlay roamer = PolyRoamerOverlay.obtain(activity);
            if (roamer != null) roamer.start();
        });
    }

    private static void peek(final BaseNavigationActivity activity, final String text) {
        prefs(activity).edit().putLong(K_LAST_PEEK, System.currentTimeMillis()).apply();
        final AssistantOverlay overlay = AssistantOverlay.obtain(activity);
        View content = activity.findViewById(android.R.id.content);
        content.post(() -> overlay.showPeek(text, PEEK_DURATION_MS));
    }

    private static SharedPreferences prefs(Activity activity) {
        return activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
    }

    private static String pick(String[] pool) {
        return pool[RND.nextInt(pool.length)];
    }

    private static String formatOne(String template, String value) {
        return template.contains("%s") ? template.replace("%s", value) : template;
    }

    private static String safeName(String name) {
        return (name == null || name.trim().isEmpty()) ? "путешественник" : name.trim();
    }

    private static String safeCountry(String country) {
        return (country == null || country.trim().isEmpty()) ? "эта страна" : country.trim();
    }
}
