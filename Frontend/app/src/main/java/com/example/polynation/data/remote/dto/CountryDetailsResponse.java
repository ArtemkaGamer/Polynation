package com.example.polynation.data.remote.dto;

import java.util.List;

public class CountryDetailsResponse {
    private boolean success;
    private String message;
    private CountryDetails data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public CountryDetails getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(CountryDetails data) { this.data = data; }

    public static class CountryDetails {
        private int id;
        private String name;
        private String capital;
        private List<Double> capitalInfoLatlng;
        private String historyInfo;
        private String cultureInfo;
        private String musicInfo;
        private String moviesInfo;
        private String sportsInfo;
        private String flagUrl;
        private List<String> photos;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCapital() { return capital; }
        public List<Double> getCapitalInfoLatlng() { return capitalInfoLatlng; }
        public String getHistoryInfo() { return historyInfo; }
        public String getCultureInfo() { return cultureInfo; }
        public String getMusicInfo() { return musicInfo; }
        public String getMoviesInfo() { return moviesInfo; }
        public String getSportsInfo() { return sportsInfo; }
        public String getFlagUrl() { return flagUrl; }
        public List<String> getPhotos() { return photos; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setCapital(String capital) { this.capital = capital; }
        public void setCapitalInfoLatlng(List<Double> capitalInfoLatlng) { this.capitalInfoLatlng = capitalInfoLatlng; }
        public void setHistoryInfo(String historyInfo) { this.historyInfo = historyInfo; }
        public void setCultureInfo(String cultureInfo) { this.cultureInfo = cultureInfo; }
        public void setMusicInfo(String musicInfo) { this.musicInfo = musicInfo; }
        public void setMoviesInfo(String moviesInfo) { this.moviesInfo = moviesInfo; }
        public void setSportsInfo(String sportsInfo) { this.sportsInfo = sportsInfo; }
        public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }
        public void setPhotos(List<String> photos) { this.photos = photos; }
    }
}
