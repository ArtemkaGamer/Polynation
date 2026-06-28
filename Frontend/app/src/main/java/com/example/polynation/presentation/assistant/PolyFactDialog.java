package com.example.polynation.presentation.assistant;

import com.example.polynation.R;

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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;

public final class PolyFactDialog {

    private PolyFactDialog() {}

    public static void show(Activity activity, PolyFacts.Fact fact, Runnable onClosed) {
        if (activity == null || activity.isFinishing() || fact == null) {
            if (onClosed != null) onClosed.run();
            return;
        }

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_poly_fact);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvCountry = dialog.findViewById(R.id.fact_country);
        TextView tvLabel = dialog.findViewById(R.id.fact_label);
        TextView tvText = dialog.findViewById(R.id.fact_text);
        ShapeableImageView ivFlag = dialog.findViewById(R.id.fact_flag);
        ShapeableImageView ivPhoto = dialog.findViewById(R.id.fact_photo);
        ImageButton btnClose = dialog.findViewById(R.id.fact_close);
        Button btnOk = dialog.findViewById(R.id.fact_ok);
        final View card = dialog.findViewById(R.id.fact_card);

        tvCountry.setText(fact.country);
        tvLabel.setText(fact.label);
        tvText.setText(fact.text);

        if (fact.flagUrl != null && !fact.flagUrl.isEmpty()) {
            Glide.with(activity).load(fact.flagUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivFlag);
        } else {
            ivFlag.setVisibility(View.GONE);
        }

        if (fact.photoUrl != null && !fact.photoUrl.isEmpty()) {
            ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(activity).load(fact.photoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivPhoto);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> { if (onClosed != null) onClosed.run(); });

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
