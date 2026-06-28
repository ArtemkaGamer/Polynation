package com.example.polynation.presentation.assistant;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.polynation.R;

public final class PolyPromptDialog {

    private PolyPromptDialog() {}

    public static void show(Activity activity, String title, String message,
                            String yesText, String laterText, Runnable onYes) {
        if (activity == null || activity.isFinishing()) return;

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_poly_prompt);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.prompt_title);
        TextView tvMessage = dialog.findViewById(R.id.prompt_message);
        Button btnYes = dialog.findViewById(R.id.prompt_yes);
        Button btnLater = dialog.findViewById(R.id.prompt_later);
        ImageButton btnClose = dialog.findViewById(R.id.prompt_close);
        final View card = dialog.findViewById(R.id.prompt_card);

        if (title != null) tvTitle.setText(title);
        tvMessage.setText(message);
        if (yesText != null) btnYes.setText(yesText);
        if (laterText != null) btnLater.setText(laterText);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            if (onYes != null) onYes.run();
        });
        btnLater.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (card != null) {
            card.setAlpha(0f);
            card.setScaleX(0.86f);
            card.setScaleY(0.86f);
            card.post(() -> {
                card.setPivotX(card.getWidth() / 2f);
                card.setPivotY(card.getHeight() * 0.7f);
                card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(420)
                        .setInterpolator(new OvershootInterpolator(1.5f))
                        .start();
            });
        }

        dialog.show();
    }
}
