package com.example.polynation.data.remote.dto;

import java.util.List;

public class FlightPricesResponse {
    public boolean success;
    public String currency;
    public String error;
    public List<Flight> data;

    public static class Flight {
        public String origin;
        public String destination;
        public String origin_airport;
        public String destination_airport;
        public double price;
        public String airline;
        public String flight_number;
        public String departure_at;
        public String return_at;
        public int transfers;
        public String link;
    }
}
