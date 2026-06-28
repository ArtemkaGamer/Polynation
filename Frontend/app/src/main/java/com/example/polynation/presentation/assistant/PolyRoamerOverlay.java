package com.example.polynation.presentation.assistant;

import com.example.polynation.R;
import com.example.polynation.util.AppToast;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class PolyRoamerOverlay extends FrameLayout {

    public static final String TAG = "poly_roamer_overlay";

    private static final String PREFS = "assistant_prefs";
    private static final String K_BADGE = "poly_badge_pending";
    private static final String K_POS = "poly_pos_frac";

    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Activity activity;
    private FrameLayout holder;
    private ImageView car;
    private TextView badge;

    private boolean started = false;
    private boolean driving = false;
    private boolean badgeShown = false;
    private PolyFacts.Fact pendingFact;

    private ValueAnimator driveAnim;
    private float curS = 0f;

    private float rimX0, rimY0, rimX1, rimY1, rimR;
    private float segBottom, segCorner, segSide, rimP = 1f;

    private final Runnable driveRunnable = this::driveStep;
    private final Runnable badgeRunnable = this::tryShowBadge;

    public PolyRoamerOverlay(Activity activity) {
        super(activity);
        this.activity = activity;
        setClipChildren(false);
        build();
    }

    public static PolyRoamerOverlay find(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return null;
        View v = content.findViewWithTag(TAG);
        return v instanceof PolyRoamerOverlay ? (PolyRoamerOverlay) v : null;
    }

    public static PolyRoamerOverlay obtain(Activity activity) {
        PolyRoamerOverlay existing = find(activity);
        if (existing != null) return existing;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return null;
        PolyRoamerOverlay overlay = new PolyRoamerOverlay(activity);
        overlay.setTag(TAG);
        content.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return overlay;
    }

    public static void hideOn(Activity activity) {
        PolyRoamerOverlay o = find(activity);
        if (o != null) o.hide();
    }

    public static void showOn(Activity activity) {
        PolyRoamerOverlay o = find(activity);
        if (o != null) o.show();
    }

    private void build() {
        int carW = (int) dp(48f);
        int carH = (int) dp(36f);
        int pad = (int) dp(20f);

        holder = new FrameLayout(getContext());
        holder.setClipChildren(false);
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(carW + pad, carH + pad);
        hlp.gravity = Gravity.TOP | Gravity.START;
        holder.setLayoutParams(hlp);

        car = new ImageView(getContext());
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(carW, carH);
        clp.gravity = Gravity.CENTER;
        car.setLayoutParams(clp);
        car.setImageResource(R.drawable.ic_poly_car);
        holder.addView(car);

        badge = new TextView(getContext());
        int b = (int) dp(20f);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(b, b);
        blp.gravity = Gravity.TOP | Gravity.END;
        badge.setLayoutParams(blp);
        badge.setText("!");
        badge.setTextColor(Color.parseColor("#FEF9E7"));
        badge.setTextSize(12f);
        badge.setGravity(Gravity.CENTER);
        badge.getPaint().setFakeBoldText(true);
        badge.setBackgroundResource(R.drawable.bg_fact_badge);
        badge.setVisibility(GONE);
        holder.addView(badge);

        holder.setOnClickListener(v -> onCarTapped());

        addView(holder);
    }

    public void start() {
        if (started) return;
        started = true;
        holder.setAlpha(0f);
        holder.post(() -> {
            setupRim();
            curS = norm(prefs().getFloat(K_POS, 0f) * rimP);
            applyRim(curS);
            holder.setAlpha(1f);
            holder.setScaleX(0f);
            holder.setScaleY(0f);
            holder.animate().scaleX(1f).scaleY(1f).setDuration(520)
                    .setInterpolator(new OvershootInterpolator(1.4f)).start();
            startEngineBob();
            driving = true;
            handler.postDelayed(driveRunnable, 700);

            if (isBadgePending()) {
                handler.postDelayed(this::showBadge, 900);
            } else {
                scheduleBadge();
            }
        });
    }

    private void setupRim() {
        float hw = holder.getWidth();
        float hh = holder.getHeight();
        rimX0 = dp(6f);
        rimY0 = dp(46f);
        rimX1 = getWidth() - dp(6f) - hw;
        rimY1 = getHeight() - dp(78f) - hh;
        if (rimX1 < rimX0) rimX1 = rimX0;
        if (rimY1 < rimY0) rimY1 = rimY0;

        float w = rimX1 - rimX0;
        float h = rimY1 - rimY0;
        rimR = Math.min(dp(22f), Math.min(w, h) / 2f);
        if (rimR < 0) rimR = 0;

        segBottom = w - 2 * rimR;
        segSide = h - 2 * rimR;
        segCorner = (float) (Math.PI / 2 * rimR);
        rimP = 2 * segBottom + 2 * segSide + 4 * segCorner;
        if (rimP <= 0) rimP = 1;
    }

    private float norm(float s) {
        return ((s % rimP) + rimP) % rimP;
    }

    private void applyRim(float s) {
        s = norm(s);
        float x, y, rot;

        float a = segBottom;
        float b = a + segCorner;
        float c = b + segSide;
        float d = c + segCorner;
        float e = d + segBottom;
        float f = e + segCorner;
        float g = f + segSide;

        if (s < a) {
            x = rimX0 + rimR + s; y = rimY1; rot = 0f;
        } else if (s < b) {
            float u = (s - a) / segCorner;
            float[] p = arc(rimX1 - rimR, rimY1 - rimR, 90f - 90f * u);
            x = p[0]; y = p[1]; rot = -90f * u;
        } else if (s < c) {
            x = rimX1; y = rimY1 - rimR - (s - b); rot = -90f;
        } else if (s < d) {
            float u = (s - c) / segCorner;
            float[] p = arc(rimX1 - rimR, rimY0 + rimR, -90f * u);
            x = p[0]; y = p[1]; rot = -90f - 90f * u;
        } else if (s < e) {
            x = rimX1 - rimR - (s - d); y = rimY0; rot = -180f;
        } else if (s < f) {
            float u = (s - e) / segCorner;
            float[] p = arc(rimX0 + rimR, rimY0 + rimR, -90f - 90f * u);
            x = p[0]; y = p[1]; rot = -180f - 90f * u;
        } else if (s < g) {
            x = rimX0; y = rimY0 + rimR + (s - f); rot = -270f;
        } else {
            float u = (s - g) / segCorner;
            float[] p = arc(rimX0 + rimR, rimY1 - rimR, -180f - 90f * u);
            x = p[0]; y = p[1]; rot = -270f - 90f * u;
        }

        holder.setTranslationX(x);
        holder.setTranslationY(y);
        car.setRotation(rot);
    }

    private float[] arc(float cx, float cy, float deg) {
        double r = Math.toRadians(deg);
        return new float[]{cx + rimR * (float) Math.cos(r), cy + rimR * (float) Math.sin(r)};
    }

    private void driveStep() {
        if (!driving) return;
        if (rimP <= 1) setupRim();

        float dir = rnd.nextBoolean() ? 1f : -1f;
        float frac = 0.10f + rnd.nextFloat() * 0.32f;
        final float delta = dir * rimP * frac;
        final float from = curS;
        final float to = curS + delta;

        long dur = (long) (700 + Math.abs(delta) * 3.0f + rnd.nextFloat() * 350);

        if (driveAnim != null) driveAnim.cancel();
        driveAnim = ValueAnimator.ofFloat(from, to);
        driveAnim.setDuration(dur);
        driveAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        driveAnim.addUpdateListener(av -> {
            curS = (float) av.getAnimatedValue();
            applyRim(curS);
        });
        driveAnim.addListener(new AnimatorListenerAdapter() {
            boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator a) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator a) {
                if (cancelled || !driving) return;
                curS = norm(curS);
                long pause = (long) (250 + rnd.nextFloat() * 1100);
                handler.postDelayed(driveRunnable, pause);
            }
        });
        driveAnim.start();
    }

    private void startEngineBob() {
        bobLoop();
    }

    private void bobLoop() {
        if (!started) return;
        car.animate().scaleX(1.04f).scaleY(0.96f).setDuration(540)
                .withEndAction(() -> {
                    if (!started) return;
                    car.animate().scaleX(1f).scaleY(1f).setDuration(540)
                            .withEndAction(this::bobLoop).start();
                }).start();
    }

    private void hide() {
        driving = false;
        if (driveAnim != null) driveAnim.cancel();
        handler.removeCallbacks(driveRunnable);
        handler.removeCallbacks(badgeRunnable);
        if (holder != null) {
            holder.animate().cancel();
            holder.setVisibility(GONE);
        }
    }

    private void show() {
        if (!started || holder == null) return;
        holder.setVisibility(VISIBLE);
        if (!driving) {
            driving = true;
            handler.postDelayed(driveRunnable, 400);
        }
        if (!badgeShown) scheduleBadge();
    }

    private void scheduleBadge() {
        handler.removeCallbacks(badgeRunnable);
        long delay = 7000 + (long) (rnd.nextFloat() * 9000);
        handler.postDelayed(badgeRunnable, delay);
    }

    private void tryShowBadge() {
        if (badgeShown) return;
        pendingFact = PolyFacts.pickRandom(activity);
        if (pendingFact == null) {
            handler.postDelayed(badgeRunnable, 6000);
            return;
        }
        showBadge();
    }

    private void showBadge() {
        if (badgeShown) return;
        badgeShown = true;
        setBadgePending(true);
        badge.setVisibility(VISIBLE);
        badge.setScaleX(0f);
        badge.setScaleY(0f);
        badge.animate().scaleX(1f).scaleY(1f).setDuration(360)
                .setInterpolator(new OvershootInterpolator(3f)).start();
        attentionWiggle();
    }

    private void attentionWiggle() {
        if (!badgeShown) return;
        badge.animate().scaleX(1.18f).scaleY(1.18f).setDuration(420)
                .withEndAction(() -> badge.animate().scaleX(1f).scaleY(1f).setDuration(420)
                        .withEndAction(() -> { if (badgeShown) handler.postDelayed(this::attentionWiggle, 1600); })
                        .start())
                .start();
    }

    private void hideBadge() {
        badgeShown = false;
        setBadgePending(false);
        badge.animate().scaleX(0f).scaleY(0f).setDuration(200)
                .withEndAction(() -> badge.setVisibility(GONE)).start();
    }

    private void onCarTapped() {
        if (!badgeShown) {
            AppToast.show(activity, "У Поли пока нет новой информации для вас");
            bounce();
            return;
        }
        if (pendingFact == null) {
            pendingFact = PolyFacts.pickRandom(activity);
        }
        if (pendingFact == null) {
            AppToast.show(activity, "У Поли пока нет новой информации для вас");
            hideBadge();
            bounce();
            scheduleBadge();
            return;
        }
        final PolyFacts.Fact fact = pendingFact;
        pendingFact = null;
        final boolean wasDriving = driving;
        driving = false;
        if (driveAnim != null) driveAnim.cancel();
        handler.removeCallbacks(driveRunnable);
        hideBadge();

        PolyFactDialog.show(activity, fact, () -> {
            driving = wasDriving;
            if (driving) handler.postDelayed(driveRunnable, 500);
            scheduleBadge();
        });
    }

    private void bounce() {
        car.animate().translationYBy(-dp(8f)).setDuration(160)
                .withEndAction(() -> car.animate().translationYBy(dp(8f)).setDuration(180).start())
                .start();
    }

    private SharedPreferences prefs() {
        return activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private boolean isBadgePending() {
        return prefs().getBoolean(K_BADGE, false);
    }

    private void setBadgePending(boolean value) {
        prefs().edit().putBoolean(K_BADGE, value).apply();
    }

    private void savePosition() {
        if (started && rimP > 1) {
            prefs().edit().putFloat(K_POS, norm(curS) / rimP).apply();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        savePosition();
        started = false;
        driving = false;
        if (driveAnim != null) driveAnim.cancel();
        handler.removeCallbacks(driveRunnable);
        handler.removeCallbacks(badgeRunnable);
        if (car != null) car.animate().cancel();
        if (holder != null) holder.animate().cancel();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
