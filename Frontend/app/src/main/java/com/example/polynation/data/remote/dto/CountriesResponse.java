package com.example.polynation.data.remote.dto;

import java.util.List;

public class CountriesResponse {
    private boolean success;
    private String message;
    private List<Country> data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Country> getData() { return data; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setData(List<Country> data) { this.data = data; }

    public static class Country {
        private int id;
        private String name;
        private String capital;
        private List<Double> capitalInfoLatlng;
        private String englishName;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCapital() { return capital; }
        public List<Double> getCapitalInfoLatlng() { return capitalInfoLatlng; }
        public String getEnglishName() { return englishName; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setCapital(String capital) { this.capital = capital; }
        public void setCapitalInfoLatlng(List<Double> capitalInfoLatlng) { this.capitalInfoLatlng = capitalInfoLatlng; }
        public void setEnglishName(String englishName) { this.englishName = englishName; }
    }
}
