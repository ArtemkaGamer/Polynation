package com.example.polynation.data.remote.dto;

public class VisitPointRequest {
    private int userId;
    private double lat;
    private double lon;
    private String label;

    public VisitPointRequest(int userId, double lat, double lon, String label) {
        this.userId = userId;
        this.lat = lat;
        this.lon = lon;
        this.label = label;
    }

    public int getUserId() { return userId; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getLabel() { return label; }
}
