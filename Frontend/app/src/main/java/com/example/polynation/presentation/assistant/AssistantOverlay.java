package com.example.polynation.presentation.assistant;

import com.example.polynation.R;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AssistantOverlay extends FrameLayout {

    public static final String TAG = "assistant_overlay";

    public interface CoachListener {
        void onNext();
        void onBack();
        void onSkip();
    }

    private boolean coachMode = false;

    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF spotlight = new RectF();
    private boolean hasSpotlight = false;
    private float spotlightCorner = 0f;
    private float ringPhase = 0f;
    private float spotlightReveal = 1f;

    private ValueAnimator ringAnimator;
    private ValueAnimator bobAnimator;
    private ValueAnimator dotAnimator;
    private ValueAnimator revealAnimator;

    private int coachGeneration = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoDismiss;

    public AssistantOverlay(Context context) {
        super(context);
        setWillNotDraw(false);
        setClipChildren(false);

        scrimPaint.setColor(Color.parseColor("#CC0A1F2D"));
        scrimPaint.setStyle(Paint.Style.FILL);

        clearPaint.setColor(Color.TRANSPARENT);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#FF7A45"));
        ringPaint.setStrokeWidth(dp(2.5f));
    }

    public static AssistantOverlay find(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return null;
        View v = content.findViewWithTag(TAG);
        return v instanceof AssistantOverlay ? (AssistantOverlay) v : null;
    }

    public static AssistantOverlay obtain(Activity activity) {
        AssistantOverlay existing = find(activity);
        if (existing != null) return existing;
        ViewGroup content = activity.findViewById(android.R.id.content);
        AssistantOverlay overlay = new AssistantOverlay(activity);
        overlay.setTag(TAG);
        content.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return overlay;
    }

    public boolean isCoach() {
        return coachMode;
    }

    public void showCoach(String message, final View target, int stepIndex, int stepCount,
                          boolean isLast, final CoachListener listener) {
        cancelAuto();
        coachMode = true;
        setAlpha(1f);
        setClickable(true);
        removeAllViews();

        final View card = LayoutInflater.from(getContext())
                .inflate(R.layout.view_assistant_coach, this, false);
        addView(card);

        TextView tvMessage = card.findViewById(R.id.coach_message);
        Button btnNext = card.findViewById(R.id.coach_next);
        View btnBack = card.findViewById(R.id.coach_back);
        TextView tvSkip = card.findViewById(R.id.coach_skip);
        LinearLayout dots = card.findViewById(R.id.coach_dots);
        final View avatar = card.findViewById(R.id.coach_avatar);

        tvMessage.setText(message);
        btnNext.setText(isLast ? "Завершить" : "Далее");
        buildDots(dots, stepIndex, stepCount);

        btnNext.setOnClickListener(v -> { if (listener != null) listener.onNext(); });
        btnBack.setOnClickListener(v -> { if (listener != null) listener.onBack(); });
        tvSkip.setOnClickListener(v -> { if (listener != null) listener.onSkip(); });
        btnBack.setVisibility(stepIndex > 0 ? VISIBLE : INVISIBLE);
        tvSkip.setVisibility(isLast ? GONE : VISIBLE);

        card.setVisibility(INVISIBLE);

        final int gen = ++coachGeneration;
        post(() -> {
            measureTarget(target);
            placeCard(card);
            animateCardIn(card, tvMessage);
            startBob(avatar);
            startRingPulse();
            startRevealPulse();
            if (target != null && !hasSpotlight) {
                retryMeasure(card, target, gen, 1);
            }
        });
    }

    private void retryMeasure(final View card, final View target, final int gen, final int attempt) {
        if (attempt > 10) return;
        handler.postDelayed(() -> {
            if (!coachMode || gen != coachGeneration) return;
            if (target.getWidth() > 0 && target.getHeight() > 0) {
                measureTarget(target);
                placeCard(card);
                startRevealPulse();
            } else {
                retryMeasure(card, target, gen, attempt + 1);
            }
        }, 220);
    }

    private void buildDots(LinearLayout dots, int active, int count) {
        stopDotPulse();
        dots.removeAllViews();
        int size = (int) dp(7f);
        int margin = (int) dp(4f);
        View activeDot = null;
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(margin);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == active
                    ? R.drawable.bg_tour_dot_active : R.drawable.bg_tour_dot);
            dots.addView(dot);
            if (i == active) activeDot = dot;
        }
        startDotPulse(activeDot);
    }

    private void measureTarget(final View target) {
        if (target == null || target.getWidth() == 0 || target.getHeight() == 0) {
            hasSpotlight = false;
            invalidate();
            return;
        }
        int[] t = new int[2];
        int[] self = new int[2];
        target.getLocationInWindow(t);
        getLocationInWindow(self);

        float pad = dp(8f);
        float left = t[0] - self[0] - pad;
        float top = t[1] - self[1] - pad;
        float right = left + target.getWidth() + pad * 2;
        float bottom = top + target.getHeight() + pad * 2;

        View nav = getRootView().findViewById(R.id.bottom_navigation_layout);
        if (nav != null && nav != target && nav.getVisibility() == VISIBLE
                && nav.getWidth() > 0 && nav.getHeight() > 0) {
            int[] n = new int[2];
            nav.getLocationInWindow(n);
            float navTop = n[1] - self[1];
            if (navTop > top) bottom = Math.min(bottom, navTop - dp(4f));
        }

        float inset = dp(12f);
        left = Math.max(inset, left);
        top = Math.max(inset, top);
        right = Math.min(getWidth() - inset, right);
        bottom = Math.min(getHeight() - inset, bottom);

        if (right <= left || bottom <= top) {
            hasSpotlight = false;
            invalidate();
            return;
        }

        spotlight.set(left, top, right, bottom);
        spotlightCorner = Math.min(dp(18f), Math.min(spotlight.width(), spotlight.height()) / 2f);
        hasSpotlight = true;
        invalidate();
    }

    private void placeCard(View card) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) card.getLayoutParams();

        float topMargin = dp(56f);
        float bottomMargin = dp(94f);
        float gap = dp(16f);

        int availW = Math.max(1, getWidth() - (int) dp(36f));
        card.measure(
                View.MeasureSpec.makeMeasureSpec(availW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.AT_MOST));
        float cardH = card.getMeasuredHeight();

        boolean top;
        if (!hasSpotlight) {
            top = false;
        } else {
            boolean bigTarget = spotlight.height() > getHeight() * 0.5f;
            boolean canTop = topMargin + cardH + gap <= spotlight.top;
            boolean canBottom = getHeight() - bottomMargin - cardH - gap >= spotlight.bottom;
            boolean targetLower = spotlight.centerY() > getHeight() * 0.5f;

            if (bigTarget) {
                top = false;
            } else if (targetLower) {
                top = canTop || !canBottom;
            } else {
                top = canTop && !canBottom;
            }
        }

        if (top) {
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.topMargin = (int) topMargin;
            lp.bottomMargin = 0;
        } else {
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            lp.topMargin = 0;
            lp.bottomMargin = (int) bottomMargin;
        }
        card.setLayoutParams(lp);
    }

    private void animateCardIn(final View card, final View message) {
        card.setVisibility(VISIBLE);
        card.setAlpha(0f);
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.setTranslationY(dp(36f));
        card.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(380).setInterpolator(new DecelerateInterpolator(1.4f)).start();

        message.setAlpha(0f);
        message.setTranslationY(dp(6f));
        message.animate().alpha(1f).translationY(0f)
                .setStartDelay(120).setDuration(320)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    public void showPeek(String message, long autoDismissMs) {
        cancelAuto();
        coachMode = false;
        setAlpha(1f);
        setClickable(false);
        hasSpotlight = false;
        stopRingPulse();
        stopDotPulse();
        removeAllViews();
        invalidate();

        final View root = LayoutInflater.from(getContext())
                .inflate(R.layout.view_assistant_peek, this, false);
        addView(root);

        TextView tvText = root.findViewById(R.id.peek_text);
        View avatar = root.findViewById(R.id.peek_avatar);
        tvText.setText(message);

        root.setOnClickListener(v -> dismiss(true, null));

        root.setAlpha(0f);
        root.post(() -> {
            float from = -(root.getWidth() + dp(16f));
            root.setTranslationX(from);
            root.setAlpha(1f);
            root.animate().translationX(0f)
                    .setDuration(420).setInterpolator(new OvershootInterpolator(1.1f))
                    .withEndAction(() -> startBob(avatar)).start();
        });

        if (autoDismissMs > 0) {
            autoDismiss = () -> dismiss(true, null);
            handler.postDelayed(autoDismiss, autoDismissMs);
        }
    }

    public void dismiss(boolean animate, final Runnable after) {
        cancelAuto();
        stopBob();
        stopDotPulse();
        stopRingPulse();
        stopRevealPulse();

        final Runnable remove = () -> {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) parent.removeView(this);
            if (after != null) after.run();
        };

        if (!animate || getChildCount() == 0) {
            remove.run();
            return;
        }

        View child = getChildAt(0);
        child.animate().alpha(0f).translationY(dp(20f))
                .setDuration(220).setInterpolator(new AccelerateInterpolator())
                .withEndAction(remove).start();
        if (coachMode) {
            ObjectAnimator fade = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f);
            fade.setDuration(220);
            fade.start();
        }
    }

    private void startBob(final View v) {
        if (v == null) return;
        stopBob();
        bobAnimator = ObjectAnimator.ofFloat(v, "translationY", 0f, -dp(5f), 0f);
        bobAnimator.setDuration(1600);
        bobAnimator.setRepeatCount(ValueAnimator.INFINITE);
        bobAnimator.start();
    }

    private void stopBob() {
        if (bobAnimator != null) {
            bobAnimator.cancel();
            bobAnimator = null;
        }
    }

    private void startDotPulse(final View dot) {
        stopDotPulse();
        if (dot == null) return;
        dotAnimator = ValueAnimator.ofFloat(1f, 1.22f, 1f);
        dotAnimator.setDuration(1100);
        dotAnimator.setRepeatCount(ValueAnimator.INFINITE);
        dotAnimator.addUpdateListener(a -> {
            float s = (float) a.getAnimatedValue();
            dot.setScaleX(s);
            dot.setScaleY(s);
        });
        dotAnimator.start();
    }

    private void stopDotPulse() {
        if (dotAnimator != null) {
            dotAnimator.cancel();
            dotAnimator = null;
        }
    }

    private void startRingPulse() {
        stopRingPulse();
        ringAnimator = ValueAnimator.ofFloat(0f, 1f);
        ringAnimator.setDuration(1500);
        ringAnimator.setRepeatCount(ValueAnimator.INFINITE);
        ringAnimator.setInterpolator(new DecelerateInterpolator());
        ringAnimator.addUpdateListener(a -> {
            ringPhase = (float) a.getAnimatedValue();
            if (hasSpotlight) invalidate();
        });
        ringAnimator.start();
    }

    private void stopRingPulse() {
        if (ringAnimator != null) {
            ringAnimator.cancel();
            ringAnimator = null;
        }
    }

    private void startRevealPulse() {
        stopRevealPulse();
        if (!hasSpotlight) { spotlightReveal = 1f; return; }
        revealAnimator = ValueAnimator.ofFloat(0f, 1f);
        revealAnimator.setDuration(420);
        revealAnimator.setInterpolator(new OvershootInterpolator(1.6f));
        revealAnimator.addUpdateListener(a -> {
            spotlightReveal = (float) a.getAnimatedValue();
            invalidate();
        });
        revealAnimator.start();
    }

    private void stopRevealPulse() {
        if (revealAnimator != null) {
            revealAnimator.cancel();
            revealAnimator = null;
        }
        spotlightReveal = 1f;
    }

    private void cancelAuto() {
        if (autoDismiss != null) {
            handler.removeCallbacks(autoDismiss);
            autoDismiss = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!coachMode) return;

        int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);

        if (hasSpotlight) {
            RectF hole = revealRect();
            float corner = spotlightCorner * lerp(0.85f, 1f, spotlightReveal);
            canvas.drawRoundRect(hole, corner, corner, clearPaint);
        }
        canvas.restoreToCount(save);

        if (hasSpotlight && spotlightReveal > 0.6f) {
            RectF hole = revealRect();
            float grow = dp(5f) * ringPhase;
            int alpha = (int) (200 * (1f - ringPhase) * spotlightReveal);
            ringPaint.setAlpha(Math.max(0, alpha));
            RectF r = new RectF(
                    hole.left - grow, hole.top - grow,
                    hole.right + grow, hole.bottom + grow);
            float corner = spotlightCorner + grow;
            canvas.drawRoundRect(r, corner, corner, ringPaint);
        }
    }

    private RectF revealRect() {
        if (spotlightReveal >= 1f) return spotlight;
        float shrink = dp(10f) * (1f - spotlightReveal);
        return new RectF(
                spotlight.left + shrink, spotlight.top + shrink,
                spotlight.right - shrink, spotlight.bottom - shrink);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return coachMode;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAuto();
        stopBob();
        stopDotPulse();
        stopRingPulse();
        stopRevealPulse();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
