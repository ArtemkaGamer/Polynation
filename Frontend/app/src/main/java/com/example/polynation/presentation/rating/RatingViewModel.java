package com.example.polynation.presentation.rating;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.UserProfile;
import com.example.polynation.data.repository.UserRepository;
import com.example.polynation.domain.model.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RatingViewModel extends AndroidViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final MutableLiveData<Resource<List<UserProfile>>> users = new MutableLiveData<>();

    public RatingViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Resource<List<UserProfile>>> getUsers() {
        return users;
    }

    public void loadUsers() {
        users.setValue(Resource.loading());
        userRepository.getAllUsers(result -> {
            if (result.isSuccess() && result.data != null) {
                List<UserProfile> sorted = new ArrayList<>(result.data);
                Collections.sort(sorted, (u1, u2) -> Integer.compare(u2.getRating(), u1.getRating()));
                users.setValue(Resource.success(sorted));
            } else {
                users.setValue(result);
            }
        });
    }
}
