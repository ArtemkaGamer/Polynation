package com.example.polynation.presentation.rating;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.UserProfile;
import com.example.polynation.data.remote.dto.VisitPoint;
import com.example.polynation.data.repository.AchievementRepository;
import com.example.polynation.data.repository.VisitPointRepository;
import com.example.polynation.domain.achievement.Achievement;
import com.example.polynation.domain.achievement.AchievementCatalog;
import com.example.polynation.presentation.achievement.AchievementViews;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UserInfoDialog {

    private UserInfoDialog() {}

    public static void show(Activity activity, UserProfile user) {
        if (activity == null || activity.isFinishing() || user == null) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_user_info);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView name = dialog.findViewById(R.id.user_name);
        TextView joined = dialog.findViewById(R.id.user_joined);
        TextView rating = dialog.findViewById(R.id.user_rating);
        TextView visitedCount = dialog.findViewById(R.id.user_visited_count);
        TextView achCount = dialog.findViewById(R.id.user_achievements_count);
        TextView achTotal = dialog.findViewById(R.id.user_achievements_total);
        ProgressBar progress = dialog.findViewById(R.id.user_progress_achievements);
        LinearLayout wreathLeft = dialog.findViewById(R.id.user_wreath_left);
        LinearLayout wreathRight = dialog.findViewById(R.id.user_wreath_right);
        LinearLayout grid = dialog.findViewById(R.id.user_achievements_grid);
        LinearLayout visitedList = dialog.findViewById(R.id.user_visited_list);
        TextView visitedEmpty = dialog.findViewById(R.id.user_visited_empty);
        ImageView close = dialog.findViewById(R.id.user_close);

        name.setText(user.getUsername());
        joined.setText(formatJoined(user.getCreatedAt()));
        rating.setText(String.valueOf(user.getRating()));
        visitedCount.setText("0");
        achCount.setText("0");
        achTotal.setText("0 / " + AchievementCatalog.totalCount());
        progress.setMax(AchievementCatalog.totalCount());

        close.setOnClickListener(v -> dialog.dismiss());

        final Set<Long> unlockedIds = new HashSet<>();
        final int[] visitedVal = {0};

        renderAchievements(activity, user, unlockedIds, visitedVal[0],
                wreathLeft, wreathRight, grid, achCount, achTotal, progress);

        new AchievementRepository(activity).getEarnedIds(user.getId(), result -> {
            if (activity.isFinishing() || !dialog.isShowing()) return;
            if (result.isSuccess() && result.data != null) {
                unlockedIds.clear();
                unlockedIds.addAll(result.data);
                renderAchievements(activity, user, unlockedIds, visitedVal[0],
                        wreathLeft, wreathRight, grid, achCount, achTotal, progress);
            }
        });

        new VisitPointRepository(activity).getVisitPoints(user.getId(), result -> {
            if (activity.isFinishing() || !dialog.isShowing()) return;
            if (result.isSuccess()) {
                List<VisitPoint> named = withLabels(result.data);
                visitedVal[0] = named.size();
                visitedCount.setText(String.valueOf(named.size()));
                populateVisited(activity, visitedList, visitedEmpty, named);
                renderAchievements(activity, user, unlockedIds, visitedVal[0],
                        wreathLeft, wreathRight, grid, achCount, achTotal, progress);
            } else {
                visitedEmpty.setText("Не удалось загрузить страны");
                visitedEmpty.setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }

    private static void renderAchievements(Activity activity, UserProfile user, Set<Long> unlockedIds,
                                           int visited, LinearLayout wreathLeft, LinearLayout wreathRight,
                                           LinearLayout grid, TextView achCount, TextView achTotal,
                                           ProgressBar progress) {
        int total = AchievementCatalog.totalCount();
        achCount.setText(String.valueOf(unlockedIds.size()));
        achTotal.setText(unlockedIds.size() + " / " + total);
        progress.setProgress(unlockedIds.size());

        List<Achievement> all = AchievementCatalog.all();

        AchievementViews.OnAchievementClick click = (a, unlk) ->
                AchievementViews.showDetailDialog(activity, a, unlk,
                        user.getQuizzesSolved(), visited, user.getRating());

        List<Achievement> featured = AchievementViews.pickFeatured(
                all, unlockedIds, user.getQuizzesSolved(), visited, user.getRating(), 6);
        AchievementViews.populateWreath(activity, wreathLeft, wreathRight, featured, unlockedIds, click);

        AchievementViews.populateCategorizedGrid(activity, grid, unlockedIds, 4, 56, click);
    }

    private static String formatJoined(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "Игрок PolyNation";
        try {
            String datePart = isoDate.split("T")[0];
            String[] d = datePart.split("-");
            return "В PolyNation с " + d[2] + "." + d[1] + "." + d[0];
        } catch (Exception e) {
            return "Игрок PolyNation";
        }
    }

    private static List<VisitPoint> withLabels(List<VisitPoint> points) {
        List<VisitPoint> named = new ArrayList<>();
        if (points != null) {
            for (VisitPoint p : points) {
                if (p != null && p.getLabel() != null && !p.getLabel().isEmpty()) {
                    named.add(p);
                }
            }
        }
        return named;
    }

    private static void populateVisited(Activity activity, LinearLayout container,
                                        TextView emptyView, List<VisitPoint> points) {
        container.removeAllViews();
        if (points.isEmpty()) {
            emptyView.setText("Игрок ещё не отметил страны");
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(activity);
        for (VisitPoint point : points) {
            View row = inflater.inflate(R.layout.item_visited_country, container, false);
            TextView label = row.findViewById(R.id.tv_country_label);
            label.setText(point.getLabel());
            container.addView(row);
        }
    }
}
