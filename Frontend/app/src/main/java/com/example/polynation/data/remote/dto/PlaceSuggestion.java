package com.example.polynation.data.remote.dto;

public class PlaceSuggestion {
    public String code;
    public String name;
    public String type;
    public String country_name;
    public Coordinates coordinates;

    public static class Coordinates {
        public double lon;
        public double lat;
    }
}
