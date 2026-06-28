package com.example.polynation.presentation.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.AuthResponse;
import com.example.polynation.data.repository.AuthRepository;
import com.example.polynation.domain.model.Resource;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository = new AuthRepository();
    private final MutableLiveData<Resource<AuthResponse.User>> loginResult = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Resource<AuthResponse.User>> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        loginResult.setValue(Resource.loading());
        authRepository.login(email, password, loginResult::setValue);
    }
}
