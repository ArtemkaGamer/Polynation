package com.example.polynation;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public final class AppToast {
    public static final String ERR_NETWORK = "Нет соединения с интернетом";

    public static final String ERR_SERVER = "Проблема с сервером, попробуйте позже";

    private AppToast() {}

    public static void show(Context context, CharSequence message) {
        make(context, message, Toast.LENGTH_SHORT);
    }

    public static void showLong(Context context, CharSequence message) {
        make(context, message, Toast.LENGTH_LONG);
    }

    private static void make(Context context, CharSequence message, int duration) {
        if (context == null || message == null || message.length() == 0) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.toast_app, null);
        TextView tv = view.findViewById(R.id.toast_text);
        tv.setText(message);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setView(view);
        toast.setDuration(duration);

        int yOffset = Math.round(96 * context.getResources().getDisplayMetrics().density);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset);
        toast.show();
    }
}
