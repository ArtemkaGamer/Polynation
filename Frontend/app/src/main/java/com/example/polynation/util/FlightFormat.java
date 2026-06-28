package com.example.polynation.util;

import com.example.polynation.BuildConfig;
import com.example.polynation.data.remote.dto.FlightPricesResponse;

import java.util.Locale;

public final class FlightFormat {

    public static final String CURRENCY = "rub";

    private FlightFormat() {}

    public static String buildBuyUrl(FlightPricesResponse.Flight f) {
        String dd = "", mm = "";
        if (f.departure_at != null && f.departure_at.length() >= 10) {
            mm = f.departure_at.substring(5, 7);
            dd = f.departure_at.substring(8, 10);
        }

        String path = f.origin + dd + mm + f.destination + "1";
        StringBuilder url = new StringBuilder("https://www.aviasales.ru/search/")
                .append(path).append("?currency=").append(CURRENCY);
        String marker = BuildConfig.TRAVELPAYOUTS_MARKER;
        if (marker != null && !marker.trim().isEmpty()) {
            url.append("&marker=").append(marker.trim());
        }
        return url.toString();
    }

    public static String formatPrice(double price) {
        long rub = Math.round(price);
        return String.format(Locale.US, "%,d", rub).replace(',', ' ') + " ₽";
    }

    public static String transfersText(int transfers) {
        if (transfers <= 0) return "Прямой рейс";
        int n = transfers % 100;
        int n1 = transfers % 10;
        String word;
        if (n > 10 && n < 20) word = "пересадок";
        else if (n1 > 1 && n1 < 5) word = "пересадки";
        else if (n1 == 1) word = "пересадка";
        else word = "пересадок";
        return transfers + " " + word;
    }

    public static String formatDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return "";
        try {
            String[] months = {"янв", "фев", "мар", "апр", "мая", "июн",
                    "июл", "авг", "сен", "окт", "ноя", "дек"};
            int month = Integer.parseInt(isoDate.substring(5, 7));
            int day = Integer.parseInt(isoDate.substring(8, 10));
            return day + " " + months[month - 1];
        } catch (Exception e) {
            return "";
        }
    }
}
