package com.example.polynation.presentation.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.CountriesResponse;
import com.example.polynation.data.remote.dto.CountryDetailsResponse;
import com.example.polynation.data.remote.dto.VisitPoint;
import com.example.polynation.data.repository.CountryRepository;
import com.example.polynation.data.repository.VisitImageRepository;
import com.example.polynation.data.repository.VisitPointRepository;
import com.example.polynation.domain.model.Resource;

import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private final CountryRepository countryRepository;
    private final VisitPointRepository visitPointRepository;
    private final VisitImageRepository visitImageRepository;

    private final MutableLiveData<Resource<List<CountriesResponse.Country>>> countries = new MutableLiveData<>();
    private final MutableLiveData<Resource<CountryDetailsResponse.CountryDetails>> details = new MutableLiveData<>();
    private final MutableLiveData<List<VisitPoint>> visitPoints = new MutableLiveData<>();
    private final MutableLiveData<Resource<VisitPoint>> addResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> removeResult = new MutableLiveData<>();
    private final MutableLiveData<PhotoStatus> photoStatus = new MutableLiveData<>();

    public static class PhotoStatus {
        public final int visitPointId;
        public final int count;
        public PhotoStatus(int visitPointId, int count) {
            this.visitPointId = visitPointId;
            this.count = count;
        }
    }

    public MapViewModel(@NonNull Application application) {
        super(application);
        countryRepository = new CountryRepository(application);
        visitPointRepository = new VisitPointRepository(application);
        visitImageRepository = new VisitImageRepository(application);
    }

    public LiveData<Resource<List<CountriesResponse.Country>>> getCountries() { return countries; }
    public LiveData<Resource<CountryDetailsResponse.CountryDetails>> getDetails() { return details; }
    public LiveData<List<VisitPoint>> getVisitPoints() { return visitPoints; }
    public LiveData<Resource<VisitPoint>> getAddResult() { return addResult; }
    public LiveData<Resource<String>> getRemoveResult() { return removeResult; }
    public LiveData<PhotoStatus> getPhotoStatus() { return photoStatus; }

    public void loadPhotoStatus(int visitPointId) {
        if (visitPointId <= 0) return;
        visitImageRepository.getImages(visitPointId, result -> {
            int count = (result.isSuccess() && result.data != null) ? result.data.size() : -1;
            photoStatus.setValue(new PhotoStatus(visitPointId, count));
        });
    }

    public void loadCountries() {
        countryRepository.getCountries(countries::setValue);
    }

    public void loadVisitPoints(int userId) {
        if (userId == -1) return;
        visitPointRepository.getVisitPoints(userId, result -> {
            if (result.isSuccess()) {
                visitPoints.setValue(result.data);
            }
        });
    }

    public void loadDetails(String russianName, String englishName) {
        countryRepository.getCountryDetails(russianName, englishName, details::setValue);
    }

    public void addVisit(int userId, double lat, double lon, String label) {
        visitPointRepository.addVisitPoint(userId, lat, lon, label, addResult::setValue);
    }

    public void removeVisit(VisitPoint point) {
        final String label = point.getLabel();
        visitPointRepository.deleteVisitPoint(point.getId(), result -> {
            if (result.isSuccess()) {
                removeResult.setValue(Resource.success(label));
            } else {
                removeResult.setValue(Resource.error(result.message));
            }
        });
    }

    public void persistVisited(int userId, List<VisitPoint> points) {
        visitPointRepository.saveLocal(userId, points);
    }
}
