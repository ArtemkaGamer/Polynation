package com.example.polynation.util;

import com.example.polynation.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

public class NoInternetDialog extends Dialog {
    public interface Callback {
        void onRetry();
        void onExit();
    }

    private final Callback callback;
    private Button btnRetry;

    public NoInternetDialog(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_no_internet);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnRetry = findViewById(R.id.btn_retry_connection);
        Button btnExit = findViewById(R.id.btn_exit_app);

        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                if (callback != null) callback.onRetry();
            });
        }
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                if (callback != null) callback.onExit();
            });
        }
    }

    public void setCheckingConnection(boolean checking) {
        if (btnRetry == null) return;
        btnRetry.setEnabled(!checking);
        btnRetry.setText(checking ? "Проверка..." : "Повторить");
    }
}
