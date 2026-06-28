package com.polynation.server.dto.response;

import com.polynation.server.model.Country;
import lombok.Data;
import java.util.List;

@Data
public class CountryResponse {
    private Long id;
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

    public static CountryResponse from(Country c) {
        CountryResponse r = new CountryResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.capital = c.getCapital();
        if (c.getCapitalLat() != null && c.getCapitalLng() != null) {
            r.capitalInfoLatlng = List.of(c.getCapitalLat(), c.getCapitalLng());
        }
        r.historyInfo = c.getHistoryInfo();
        r.cultureInfo = c.getCultureInfo();
        r.musicInfo = c.getMusicInfo();
        r.moviesInfo = c.getMoviesInfo();
        r.sportsInfo = c.getSportsInfo();
        return r;
    }
}