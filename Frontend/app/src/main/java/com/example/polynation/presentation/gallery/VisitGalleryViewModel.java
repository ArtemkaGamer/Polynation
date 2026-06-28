package com.example.polynation.presentation.gallery;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.VisitImage;
import com.example.polynation.data.repository.VisitImageRepository;
import com.example.polynation.domain.model.Resource;

import java.util.List;

public class VisitGalleryViewModel extends AndroidViewModel {

    private final VisitImageRepository repository;

    private final MutableLiveData<Resource<List<VisitImage>>> images = new MutableLiveData<>();
    private final MutableLiveData<Resource<VisitImage>> uploadResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Integer>> deleteResult = new MutableLiveData<>();

    public VisitGalleryViewModel(@NonNull Application application) {
        super(application);
        repository = new VisitImageRepository(application);
    }

    public LiveData<Resource<List<VisitImage>>> getImages() { return images; }
    public LiveData<Resource<VisitImage>> getUploadResult() { return uploadResult; }
    public LiveData<Resource<Integer>> getDeleteResult() { return deleteResult; }

    public void loadImages(int visitPointId) {
        images.setValue(Resource.loading());
        repository.getImages(visitPointId, images::setValue);
    }

    public void upload(int visitPointId, Uri uri) {
        uploadResult.setValue(Resource.loading());
        repository.uploadImage(visitPointId, uri, uploadResult::setValue);
    }

    public void delete(int visitPointId, int imageId) {
        repository.deleteImage(visitPointId, imageId, result -> {
            if (result.isSuccess()) {
                deleteResult.setValue(Resource.success(imageId));
            } else {
                deleteResult.setValue(Resource.error(result.message));
            }
        });
    }
}
