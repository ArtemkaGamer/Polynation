package com.example.polynation.data.remote.dto;

public class VisitImage {
    private int id;
    private int visitPointId;
    private String url;
    private String fileName;
    private String contentType;
    private String createdAt;

    public int getId() { return id; }
    public int getVisitPointId() { return visitPointId; }
    public String getUrl() { return url; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setVisitPointId(int visitPointId) { this.visitPointId = visitPointId; }
    public void setUrl(String url) { this.url = url; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
