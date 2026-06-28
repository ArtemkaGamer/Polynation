package com.example.polynation.presentation.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.repository.AuthRepository;
import com.example.polynation.domain.model.Resource;

public class RegisterViewModel extends AndroidViewModel {

    private final AuthRepository authRepository = new AuthRepository();
    private final MutableLiveData<Resource<Boolean>> registerResult = new MutableLiveData<>();

    public RegisterViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Resource<Boolean>> getRegisterResult() {
        return registerResult;
    }

    public void register(String username, String email, String password) {
        registerResult.setValue(Resource.loading());
        authRepository.register(username, email, password, registerResult::setValue);
    }
}
