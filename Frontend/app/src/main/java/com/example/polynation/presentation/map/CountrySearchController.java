package com.example.polynation.presentation.map;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.polynation.R;
import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.util.AppToast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CountrySearchController {

    public interface OnCountryPicked {
        void onPicked(CountriesResponse.Country country);
    }

    private final Activity activity;
    private final org.osmdroid.views.MapView mapView;
    private final OnCountryPicked listener;

    private final FloatingActionButton fabSearch;
    private final LinearLayout searchBar;
    private final EditText etSearch;
    private final ImageButton btnSearchClear;

    private List<CountriesResponse.Country> countries;

    public CountrySearchController(Activity activity, org.osmdroid.views.MapView mapView, OnCountryPicked listener) {
        this.activity = activity;
        this.mapView = mapView;
        this.listener = listener;
        this.fabSearch = activity.findViewById(R.id.fab_search);
        this.searchBar = activity.findViewById(R.id.search_bar);
        this.etSearch = activity.findViewById(R.id.et_search);
        this.btnSearchClear = activity.findViewById(R.id.btn_search_clear);
    }

    public void setup() {
        fabSearch.setOnClickListener(v -> toggleSearchBar());
        btnSearchClear.setOnClickListener(v -> {
            if (etSearch.getText().length() > 0) {
                etSearch.setText("");
            } else {
                hideSearchBar();
            }
        });
        etSearch.setOnEditorActionListener((tv, actionId, event) -> {
            performSearch(etSearch.getText().toString());
            return true;
        });
    }

    public void setCountries(List<CountriesResponse.Country> countries) {
        this.countries = countries;
    }

    private void toggleSearchBar() {
        if (searchBar.getVisibility() == View.VISIBLE) {
            hideSearchBar();
        } else {
            showSearchBar();
        }
    }

    private void showSearchBar() {
        if (!mapView.isEnabled()) return;
        searchBar.setVisibility(View.VISIBLE);
        searchBar.setAlpha(0f);
        searchBar.setTranslationY(-30f);
        searchBar.animate().alpha(1f).translationY(0f).setDuration(220).start();
        etSearch.requestFocus();
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    public void hideSearchBar() {
        hideKeyboard();
        searchBar.animate().alpha(0f).translationY(-30f).setDuration(180)
                .withEndAction(() -> searchBar.setVisibility(View.GONE)).start();
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void performSearch(String rawQuery) {
        if (rawQuery == null) return;
        String query = rawQuery.trim();
        if (query.isEmpty()) return;

        if (countries == null || countries.isEmpty()) {
            AppToast.show(activity, "Данные ещё загружаются, попробуйте позже");
            return;
        }

        CountriesResponse.Country match = findCountryByQuery(query);
        if (match == null) {
            AppToast.show(activity, "Страна или столица не найдена");
            return;
        }
        hideSearchBar();
        listener.onPicked(match);
    }

    private CountriesResponse.Country findCountryByQuery(String query) {
        String q = query.toLowerCase().trim();

        CountriesResponse.Country startsWith = null;
        CountriesResponse.Country contains = null;

        for (CountriesResponse.Country c : countries) {
            String name = c.getName() != null ? c.getName().toLowerCase().trim() : "";
            String capital = c.getCapital() != null ? c.getCapital().toLowerCase().trim() : "";
            String engName = c.getEnglishName() != null ? c.getEnglishName().toLowerCase().trim() : "";

            if (name.equals(q) || capital.equals(q) || engName.equals(q)) {
                return c;
            }
            if (startsWith == null
                    && (name.startsWith(q) || capital.startsWith(q) || engName.startsWith(q))) {
                startsWith = c;
            }
            if (contains == null
                    && (name.contains(q) || capital.contains(q) || engName.contains(q))) {
                contains = c;
            }
        }
        return startsWith != null ? startsWith : contains;
    }
}
