package com.example.polynation.presentation.splash;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.domain.BackgroundCacheLoader;
import com.example.polynation.util.NetworkUtils;

public class SplashViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> connectivity = new MutableLiveData<>();

    public SplashViewModel(@NonNull Application application) {
        super(application);
    }

    public void startBackgroundCache() {
        BackgroundCacheLoader.getInstance(getApplication()).start();
    }

    public boolean hasCountriesCache() {
        return LocalDataCache.hasCountriesCache(getApplication());
    }

    public LiveData<Boolean> getConnectivity() {
        return connectivity;
    }

    public void checkConnectivity() {
        NetworkUtils.hasInternetAccess(getApplication(), connectivity::postValue);
    }
}
