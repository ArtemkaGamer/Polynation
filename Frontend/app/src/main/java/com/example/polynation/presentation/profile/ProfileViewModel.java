package com.example.polynation.presentation.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.polynation.data.remote.dto.UserProfile;
import com.example.polynation.data.remote.dto.VisitImage;
import com.example.polynation.data.remote.dto.VisitPoint;
import com.example.polynation.data.repository.AchievementRepository;
import com.example.polynation.data.repository.AchievementSyncResult;
import com.example.polynation.data.repository.QuizRepository;
import com.example.polynation.data.repository.UserRepository;
import com.example.polynation.data.repository.VisitImageRepository;
import com.example.polynation.data.repository.VisitPointRepository;
import com.example.polynation.domain.model.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final VisitPointRepository visitPointRepository;
    private final VisitImageRepository visitImageRepository;
    private final QuizRepository quizRepository;
    private final AchievementRepository achievementRepository;

    private final MutableLiveData<Resource<UserProfile>> profile = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<VisitPoint>>> visited = new MutableLiveData<>();
    private final MutableLiveData<Resource<UserProfile>> usernameUpdate = new MutableLiveData<>();
    private final MutableLiveData<Resource<VisitPoint>> deleteResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<AchievementSyncResult>> achievements = new MutableLiveData<>();
    private final MutableLiveData<List<GalleryEntry>> gallery = new MutableLiveData<>();

    public static class GalleryEntry {
        public final VisitPoint point;
        public final List<VisitImage> images;
        public GalleryEntry(VisitPoint point, List<VisitImage> images) {
            this.point = point;
            this.images = images;
        }
    }

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        visitPointRepository = new VisitPointRepository(application);
        visitImageRepository = new VisitImageRepository(application);
        quizRepository = new QuizRepository(application);
        achievementRepository = new AchievementRepository(application);
    }

    public LiveData<Resource<UserProfile>> getProfile() { return profile; }
    public LiveData<Resource<List<VisitPoint>>> getVisited() { return visited; }
    public LiveData<Resource<UserProfile>> getUsernameUpdate() { return usernameUpdate; }
    public LiveData<Resource<VisitPoint>> getDeleteResult() { return deleteResult; }
    public LiveData<Resource<AchievementSyncResult>> getAchievements() { return achievements; }
    public LiveData<List<GalleryEntry>> getGallery() { return gallery; }

    public void loadGallery(List<VisitPoint> points) {
        if (points == null || points.isEmpty()) {
            gallery.setValue(new ArrayList<>());
            return;
        }
        final List<VisitPoint> snapshot = new ArrayList<>(points);
        final int total = snapshot.size();
        final Map<Integer, List<VisitImage>> collected = new HashMap<>();
        final int[] done = {0};

        for (VisitPoint p : snapshot) {
            final int pointId = p.getId();
            visitImageRepository.getImages(pointId, result -> {
                if (result.isSuccess() && result.data != null && !result.data.isEmpty()) {
                    collected.put(pointId, result.data);
                }
                done[0]++;
                if (done[0] >= total) {
                    List<GalleryEntry> entries = new ArrayList<>();
                    for (VisitPoint vp : snapshot) {
                        List<VisitImage> imgs = collected.get(vp.getId());
                        if (imgs != null && !imgs.isEmpty()) {
                            entries.add(new GalleryEntry(vp, imgs));
                        }
                    }
                    gallery.setValue(entries);
                }
            });
        }
    }

    public List<Long> getCachedAchievementIds(int userId) {
        return achievementRepository.getCachedIds(userId);
    }

    public void syncAchievements(int userId, int quizzes, int visitedCount, int rating) {
        achievementRepository.syncAndGet(userId, quizzes, visitedCount, rating, achievements::setValue);
    }

    public int getSolvedCount(int userId) {
        return quizRepository.getSolvedCount(userId);
    }

    public void loadProfile(int userId) {
        userRepository.getUserProfile(userId, profile::setValue);
    }

    public void loadVisited(int userId) {
        visitPointRepository.getVisitPoints(userId, visited::setValue);
    }

    public void saveUsername(int userId, String newName) {
        userRepository.updateUsername(userId, newName, usernameUpdate::setValue);
    }

    public void deleteVisit(VisitPoint point) {
        visitPointRepository.deleteVisitPoint(point.getId(), result -> {
            if (result.isSuccess()) {
                deleteResult.setValue(Resource.success(point));
            } else {
                deleteResult.setValue(Resource.error(result.message));
            }
        });
    }

    public void persistVisited(int userId, List<VisitPoint> points) {
        visitPointRepository.saveLocal(userId, points);
    }
}
