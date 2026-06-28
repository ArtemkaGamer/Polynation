package com.example.polynation.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.net.HttpURLConnection;
import java.net.URL;

public final class NetworkUtils {
    private NetworkUtils() {}

    private static final String PING_URL = "https://polynation-serv-backend.onrender.com/";
    private static final int PING_TIMEOUT_MS = 8000;

    public interface ConnectionCallback {
        void onResult(boolean online);
    }

    public static boolean isOnline(Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }

    public static void hasInternetAccess(Context context, ConnectionCallback callback) {
        if (!isOnline(context)) {
            callback.onResult(false);
            return;
        }
        new Thread(() -> callback.onResult(pingServer())).start();
    }

    private static boolean pingServer() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(PING_URL).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(PING_TIMEOUT_MS);
            connection.setReadTimeout(PING_TIMEOUT_MS);
            connection.connect();

            return connection.getResponseCode() > 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
