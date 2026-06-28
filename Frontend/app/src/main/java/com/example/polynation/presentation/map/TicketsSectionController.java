package com.example.polynation.presentation.map;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.FlightPricesResponse;
import com.example.polynation.data.repository.FlightTicketsRepository;
import com.example.polynation.util.AppToast;
import com.example.polynation.util.FlightFormat;

import java.util.List;

public class TicketsSectionController {

    private final Activity activity;
    private final FlightTicketsRepository repository;

    private final LinearLayout ticketsCard;
    private final LinearLayout ticketsContainer;
    private final LinearLayout ticketsStatusPanel;
    private final ImageView ivTicketsStatusIcon;
    private final TextView tvTicketsSubtitle;
    private final TextView tvTicketsStatus;

    private int requestId = 0;

    public TicketsSectionController(Activity activity, View root) {
        this.activity = activity;
        this.repository = new FlightTicketsRepository(activity);
        this.ticketsCard = root.findViewById(R.id.tickets_card);
        this.ticketsContainer = root.findViewById(R.id.tickets_container);
        this.ticketsStatusPanel = root.findViewById(R.id.tickets_status_panel);
        this.ivTicketsStatusIcon = root.findViewById(R.id.iv_tickets_status_icon);
        this.tvTicketsSubtitle = root.findViewById(R.id.tv_tickets_subtitle);
        this.tvTicketsStatus = root.findViewById(R.id.tv_tickets_status);
    }

    public void reset() {
        if (ticketsContainer != null) ticketsContainer.removeAllViews();
        hideSubtitle();
        showStatus("Ищем лучшие цены на билеты…");
    }

    public void load(Double originLat, Double originLng,
                     double destLat, double destLng, String capitalName) {
        if (ticketsCard == null) return;
        ticketsCard.setVisibility(View.VISIBLE);

        final int reqId = ++requestId;

        repository.load(originLat, originLng, destLat, destLng, capitalName,
                new FlightTicketsRepository.Callback() {
                    @Override
                    public void onLoading() {
                        if (reqId != requestId) return;
                        hideSubtitle();
                        if (ticketsContainer != null) ticketsContainer.removeAllViews();
                        showStatus("Ищем лучшие цены на билеты…");
                    }

                    @Override
                    public void onResult(String originLabel, String destLabel,
                                         List<FlightPricesResponse.Flight> flights) {
                        if (reqId != requestId) return;
                        setSubtitle(originLabel + "  →  " + destLabel);
                        hideStatus();
                        renderTickets(flights);
                    }

                    @Override
                    public void onResultFromAnywhere(String destLabel,
                                                     List<FlightPricesResponse.Flight> flights) {
                        if (reqId != requestId) return;
                        setSubtitle("Из вашего города прямых билетов нет — "
                                + "лучшие предложения в " + destLabel + " из других городов мира");
                        hideStatus();
                        renderTickets(flights);
                    }

                    @Override
                    public void onEmpty(String originLabel, String destLabel) {
                        if (reqId != requestId) return;
                        hideSubtitle();
                        if (ticketsContainer != null) ticketsContainer.removeAllViews();
                        showStatus("Пока не нашли билетов в " + destLabel
                                + " на ближайшие даты", R.drawable.ic_error_image);
                    }

                    @Override
                    public void onUnavailable(String message) {
                        if (reqId != requestId) return;
                        hideSubtitle();
                        if (ticketsContainer != null) ticketsContainer.removeAllViews();
                        showStatus(message, R.drawable.ic_error_image);
                    }
                });
    }

    public void hide() {
        if (ticketsCard != null) ticketsCard.setVisibility(View.GONE);
        if (ticketsContainer != null) ticketsContainer.removeAllViews();
    }

    private void renderTickets(List<FlightPricesResponse.Flight> flights) {
        if (ticketsContainer == null) return;
        ticketsContainer.removeAllViews();
        for (FlightPricesResponse.Flight f : flights) {
            addTicketCard(f);
        }
    }

    private void setSubtitle(String text) {
        if (tvTicketsSubtitle != null) {
            tvTicketsSubtitle.setText(text);
            tvTicketsSubtitle.setVisibility(View.VISIBLE);
        }
    }

    private void hideSubtitle() {
        if (tvTicketsSubtitle != null) {
            tvTicketsSubtitle.setText("");
            tvTicketsSubtitle.setVisibility(View.GONE);
        }
    }

    private void showStatus(String text) {
        showStatus(text, R.drawable.ic_search);
    }

    private void showStatus(String text, int iconRes) {
        if (ticketsStatusPanel != null) ticketsStatusPanel.setVisibility(View.VISIBLE);
        if (ivTicketsStatusIcon != null) ivTicketsStatusIcon.setImageResource(iconRes);
        if (tvTicketsStatus != null) tvTicketsStatus.setText(text);
    }

    private void hideStatus() {
        if (ticketsStatusPanel != null) ticketsStatusPanel.setVisibility(View.GONE);
    }

    private void addTicketCard(FlightPricesResponse.Flight flight) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View card = inflater.inflate(R.layout.item_flight_ticket, ticketsContainer, false);

        TextView tvRoute = card.findViewById(R.id.tv_route);
        TextView tvMeta = card.findViewById(R.id.tv_meta);
        TextView tvPrice = card.findViewById(R.id.tv_price);
        Button btnBuy = card.findViewById(R.id.btn_buy);

        tvRoute.setText(flight.origin + " → " + flight.destination);

        String date = FlightFormat.formatDate(flight.departure_at);
        String transfers = FlightFormat.transfersText(flight.transfers);
        tvMeta.setText(date.isEmpty() ? transfers : (date + " · " + transfers));

        tvPrice.setText(FlightFormat.formatPrice(flight.price));

        btnBuy.setOnClickListener(v -> openBuyLink(flight));

        ticketsContainer.addView(card);
    }

    private void openBuyLink(FlightPricesResponse.Flight flight) {
        try {
            String url = FlightFormat.buildBuyUrl(flight);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            AppToast.show(activity, "Не удалось открыть страницу покупки");
        }
    }
}
