package com.example.polynation.presentation.achievement;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.polynation.R;
import com.example.polynation.domain.achievement.Achievement;
import com.example.polynation.domain.achievement.AchievementCatalog;
import com.example.polynation.domain.achievement.AchievementTier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class AchievementViews {

    private static final int LOCKED_ICON_TINT = 0xFFB8AE9C;

    private static final int CATEGORY_ACCENT = 0xFFBA5C32;

    private static final Achievement.Metric[] CATEGORY_ORDER = {
            Achievement.Metric.NONE, Achievement.Metric.QUIZZES,
            Achievement.Metric.VISITED, Achievement.Metric.RATING
    };

    public interface OnAchievementClick {
        void onClick(Achievement achievement, boolean unlocked);
    }

    private AchievementViews() {}

    private static int dp(Context ctx, float value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

    private static String categoryLabel(Achievement.Metric metric) {
        switch (metric) {
            case QUIZZES: return "Квизы";
            case VISITED: return "Путешествия";
            case RATING: return "Рейтинг";
            case NONE:
            default: return "Начало пути";
        }
    }

    private static int tierRank(AchievementTier tier) {
        switch (tier) {
            case SPECIAL:  return 5;
            case PLATINUM: return 4;
            case GOLD:     return 3;
            case SILVER:   return 2;
            case BRONZE:
            default:       return 1;
        }
    }

    private static float progressRatio(Achievement a, int quizzes, int visited, int rating) {
        if (a.metric == Achievement.Metric.NONE || a.threshold <= 0) return 0f;
        return Math.min(1f, a.currentValue(quizzes, visited, rating) / (float) a.threshold);
    }

    public static FrameLayout buildMedallion(Context ctx, Achievement a, int sizeDp, boolean unlocked) {
        FrameLayout holder = new FrameLayout(ctx);
        int size = dp(ctx, sizeDp);
        holder.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        holder.setBackgroundResource(unlocked ? a.tier.medallionRes : R.drawable.bg_ach_locked);

        ImageView icon = new ImageView(ctx);
        int iconSize = Math.round(size * 0.52f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(iconSize, iconSize);
        lp.gravity = Gravity.CENTER;
        icon.setLayoutParams(lp);
        icon.setImageResource(a.iconRes);
        if (unlocked) {
            icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        } else {
            icon.setColorFilter(LOCKED_ICON_TINT, PorterDuff.Mode.SRC_IN);
            icon.setAlpha(0.75f);
        }
        holder.addView(icon);
        return holder;
    }

    private static LinearLayout buildGridCell(Context ctx, Achievement a, boolean unlocked,
                                              int medallionDp, OnAchievementClick click) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(ctx, 4);
        cell.setPadding(pad, dp(ctx, 6), pad, dp(ctx, 6));

        FrameLayout medallion = buildMedallion(ctx, a, medallionDp, unlocked);
        cell.addView(medallion);

        TextView title = new TextView(ctx);
        title.setText(a.title);
        title.setTextSize(10.5f);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setTextColor(unlocked ? 0xFF1B261F : 0xFF9A8C7A);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(ctx, 6);
        title.setLayoutParams(tlp);
        cell.addView(title);

        cell.setClickable(true);
        cell.setOnClickListener(v -> {
            if (click != null) click.onClick(a, unlocked);
        });
        return cell;
    }

    public static void populateGrid(Context ctx, LinearLayout container, List<Achievement> items,
                                    Set<Long> unlockedIds, int columns, int medallionDp,
                                    OnAchievementClick click) {
        container.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                container.addView(row);
            }
            Achievement a = items.get(i);
            boolean unlocked = unlockedIds.contains(a.id);
            LinearLayout cell = buildGridCell(ctx, a, unlocked, medallionDp, click);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cell.setLayoutParams(clp);
            row.addView(cell);
        }
        if (row != null) {
            int remainder = items.size() % columns;
            if (remainder != 0) {
                for (int k = remainder; k < columns; k++) {
                    View filler = new View(ctx);
                    filler.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                    row.addView(filler);
                }
            }
        }
    }

    public static void populateCategorizedGrid(Context ctx, LinearLayout container,
                                               Set<Long> unlockedIds, int columns,
                                               int medallionDp, OnAchievementClick click) {
        container.removeAllViews();
        for (Achievement.Metric metric : CATEGORY_ORDER) {
            List<Achievement> group = new ArrayList<>();
            int earned = 0;
            for (Achievement a : AchievementCatalog.all()) {
                if (a.metric != metric) continue;
                group.add(a);
                if (unlockedIds.contains(a.id)) earned++;
            }
            if (group.isEmpty()) continue;

            container.addView(buildCategoryHeader(ctx, categoryLabel(metric),
                    CATEGORY_ACCENT, earned, group.size()));

            LinearLayout block = new LinearLayout(ctx);
            block.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.bottomMargin = dp(ctx, 4);
            block.setLayoutParams(blp);
            populateGrid(ctx, block, group, unlockedIds, columns, medallionDp, click);
            container.addView(block);
        }
    }

    private static LinearLayout buildCategoryHeader(Context ctx, String label, int color,
                                                    int earned, int total) {
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(ctx, 12);
        hlp.bottomMargin = dp(ctx, 2);
        header.setLayoutParams(hlp);

        View dot = new View(ctx);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(ctx, 7), dp(ctx, 7));
        dlp.rightMargin = dp(ctx, 8);
        dot.setLayoutParams(dlp);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(color);
        dot.setBackground(dotBg);
        header.addView(dot);

        TextView title = new TextView(ctx);
        title.setText(label);
        title.setTextSize(13f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF1B261F);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tlp);
        header.addView(title);

        TextView count = new TextView(ctx);
        count.setText(earned + " / " + total);
        count.setTextSize(12f);
        count.setTypeface(Typeface.DEFAULT_BOLD);
        count.setTextColor(color);
        header.addView(count);
        return header;
    }

    public static List<Achievement> pickFeatured(List<Achievement> all, Set<Long> unlockedIds,
                                                 int quizzes, int visited, int rating, int slots) {
        List<Achievement> unlocked = new ArrayList<>();
        List<Achievement> locked = new ArrayList<>();
        for (Achievement a : all) {
            (unlockedIds.contains(a.id) ? unlocked : locked).add(a);
        }
        Collections.sort(unlocked, (x, y) -> tierRank(y.tier) - tierRank(x.tier));
        Collections.sort(locked, (x, y) -> Float.compare(
                progressRatio(y, quizzes, visited, rating),
                progressRatio(x, quizzes, visited, rating)));

        List<Achievement> featured = new ArrayList<>(unlocked);
        for (Achievement a : locked) {
            if (featured.size() >= slots) break;
            featured.add(a);
        }
        if (featured.size() > slots) featured = new ArrayList<>(featured.subList(0, slots));
        return featured;
    }

    public static void populateWreath(Context ctx, LinearLayout left, LinearLayout right,
                                      List<Achievement> featured, Set<Long> unlockedIds,
                                      OnAchievementClick click) {
        left.removeAllViews();
        right.removeAllViews();
        int medallionDp = 38;
        for (int i = 0; i < featured.size() && i < 6; i++) {
            Achievement a = featured.get(i);
            boolean unlocked = unlockedIds.contains(a.id);
            FrameLayout medallion = buildMedallion(ctx, a, medallionDp, unlocked);

            boolean isLeft = i < 3;
            int indexInColumn = isLeft ? i : i - 3;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(ctx, medallionDp), dp(ctx, medallionDp));
            if (indexInColumn != 0) lp.topMargin = dp(ctx, 8);
            medallion.setLayoutParams(lp);

            float outward = (indexInColumn == 1) ? 11f : 2f;
            medallion.setTranslationX(dp(ctx, isLeft ? -outward : outward));

            medallion.setClickable(true);
            final Achievement fa = a;
            final boolean fUnlocked = unlocked;
            medallion.setOnClickListener(v -> {
                if (click != null) click.onClick(fa, fUnlocked);
            });

            (isLeft ? left : right).addView(medallion);
        }
    }

    public static void showDetailDialog(Activity activity, Achievement a, boolean unlocked,
                                        int quizzes, int visited, int rating) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = baseDialog(activity);
        dialog.setContentView(R.layout.dialog_achievement);

        FrameLayout holder = dialog.findViewById(R.id.ach_medallion_holder);
        TextView header = dialog.findViewById(R.id.ach_header);
        TextView title = dialog.findViewById(R.id.ach_title);
        TextView tier = dialog.findViewById(R.id.ach_tier);
        TextView description = dialog.findViewById(R.id.ach_description);
        ProgressBar progressBar = dialog.findViewById(R.id.ach_progress_bar);
        TextView progress = dialog.findViewById(R.id.ach_progress);
        Button ok = dialog.findViewById(R.id.ach_ok);

        header.setVisibility(View.GONE);
        addMedallion(holder, buildMedallion(activity, a, 104, unlocked));
        title.setText(a.title);
        tier.setText(a.tier.label);
        description.setText(a.description);

        if (unlocked) {
            progressBar.setVisibility(View.GONE);
            progress.setText("Получено");
            progress.setTextColor(0xFF16A34A);
            progress.setVisibility(View.VISIBLE);
        } else if (a.metric != Achievement.Metric.NONE) {
            int current = Math.min(a.currentValue(quizzes, visited, rating), a.threshold);
            progressBar.setMax(a.threshold);
            progressBar.setProgress(current);
            progressBar.setVisibility(View.VISIBLE);
            progress.setText("Прогресс: " + current + " / " + a.threshold);
            progress.setTextColor(0xFF9A8C7A);
            progress.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
        }

        ok.setOnClickListener(v -> dialog.dismiss());
        popIn(dialog.findViewById(R.id.ach_card));
        dialog.show();
    }

    public static void showUnlockedDialog(Activity activity, List<Achievement> newly) {
        if (activity == null || activity.isFinishing() || newly == null || newly.isEmpty()) return;

        Achievement hero = newly.get(0);
        Dialog dialog = baseDialog(activity);
        dialog.setContentView(R.layout.dialog_achievement);

        FrameLayout holder = dialog.findViewById(R.id.ach_medallion_holder);
        TextView header = dialog.findViewById(R.id.ach_header);
        TextView title = dialog.findViewById(R.id.ach_title);
        TextView tier = dialog.findViewById(R.id.ach_tier);
        TextView description = dialog.findViewById(R.id.ach_description);
        TextView progress = dialog.findViewById(R.id.ach_progress);
        Button ok = dialog.findViewById(R.id.ach_ok);

        header.setVisibility(View.VISIBLE);
        addMedallion(holder, buildMedallion(activity, hero, 104, true));
        title.setText(hero.title);
        tier.setText(hero.tier.label);
        description.setText(hero.description);

        if (newly.size() > 1) {
            progress.setText("И ещё " + (newly.size() - 1) + " — загляните ниже");
            progress.setTextColor(0xFFBA5C32);
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }

        ok.setOnClickListener(v -> dialog.dismiss());
        popIn(dialog.findViewById(R.id.ach_card));
        dialog.show();
    }

    private static void addMedallion(FrameLayout holder, FrameLayout medallion) {
        if (holder == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) medallion.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        medallion.setLayoutParams(lp);
        holder.removeAllViews();
        holder.addView(medallion);
    }

    private static Dialog baseDialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private static void popIn(View card) {
        if (card == null) return;
        card.setAlpha(0f);
        card.setScaleX(0.88f);
        card.setScaleY(0.88f);
        card.post(() -> {
            card.setPivotX(card.getWidth() / 2f);
            card.setPivotY(card.getHeight() * 0.4f);
            card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(380)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .start();
        });
    }
}
