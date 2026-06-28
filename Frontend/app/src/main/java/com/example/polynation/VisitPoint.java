package com.example.polynation;

public class VisitPoint {
    private int id;
    private int userId;
    private double lat;
    private double lon;
    private String label;
    private String createdAt;

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getLabel() { return label; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLon(double lon) { this.lon = lon; }
    public void setLabel(String label) { this.label = label; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
