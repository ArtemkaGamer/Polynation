package com.example.polynation.presentation.assistant;

import android.content.Context;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.data.remote.dto.CountryDetailsResponse;
import com.example.polynation.domain.CountryNameMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class PolyFacts {

    private static final Random RND = new Random();
    private static final int MAX_TEXT_LEN = 320;

    private PolyFacts() {}

    public static final class Fact {
        public final String country;
        public final String flagUrl;
        public final String label;
        public final String text;
        public final String photoUrl;

        Fact(String country, String flagUrl, String label, String text, String photoUrl) {
            this.country = country;
            this.flagUrl = flagUrl;
            this.label = label;
            this.text = text;
            this.photoUrl = photoUrl;
        }
    }

    public static Fact pickRandom(Context context) {
        List<CountriesResponse.Country> countries = LocalDataCache.getCountriesList(context);
        if (countries == null || countries.isEmpty()) return null;

        List<CountriesResponse.Country> shuffled = new ArrayList<>(countries);
        Collections.shuffle(shuffled, RND);

        for (CountriesResponse.Country c : shuffled) {
            if (c == null || c.getName() == null) continue;
            CountryDetailsResponse.CountryDetails d = LocalDataCache.getCountryDetails(context, c.getName());
            if (d == null) continue;

            List<String[]> facts = new ArrayList<>();
            addFact(facts, "История", d.getHistoryInfo());
            addFact(facts, "Культура", d.getCultureInfo());
            addFact(facts, "Музыка", d.getMusicInfo());
            addFact(facts, "Кино", d.getMoviesInfo());
            addFact(facts, "Спорт", d.getSportsInfo());
            if (facts.isEmpty()) continue;

            String[] chosen = facts.get(RND.nextInt(facts.size()));

            String name = notEmpty(d.getName()) ? d.getName() : c.getName();

            String flag = CountryNameMapper.getFlagUrlByRussianName(c.getName());
            if (!notEmpty(flag)) flag = d.getFlagUrl();

            String photo = null;
            if (d.getPhotos() != null) {
                for (String p : d.getPhotos()) {
                    if (notEmpty(p)) { photo = p; break; }
                }
            }

            return new Fact(name, flag, chosen[0], trim(chosen[1]), photo);
        }
        return null;
    }

    private static void addFact(List<String[]> out, String label, String text) {
        if (notEmpty(text)) out.add(new String[]{label, text.trim()});
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String trim(String text) {
        if (text.length() <= MAX_TEXT_LEN) return text;
        String cut = text.substring(0, MAX_TEXT_LEN);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > MAX_TEXT_LEN - 60) cut = cut.substring(0, lastSpace);
        return cut.trim() + "…";
    }
}
