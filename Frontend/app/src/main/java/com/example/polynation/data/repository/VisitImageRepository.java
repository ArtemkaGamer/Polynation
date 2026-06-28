package com.example.polynation.data.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.example.polynation.data.local.LocalDataCache;
import com.example.polynation.data.local.LocalImageStore;
import com.example.polynation.data.remote.ApiClient;
import com.example.polynation.data.remote.ApiService;
import com.example.polynation.data.remote.dto.VisitImage;
import com.example.polynation.data.remote.dto.VisitImageResponse;
import com.example.polynation.data.remote.dto.VisitImagesResponse;
import com.example.polynation.domain.model.Resource;
import com.example.polynation.domain.model.ResultCallback;
import com.example.polynation.util.AppToast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisitImageRepository {

    private static final long MAX_BYTES = 12L * 1024 * 1024;

    private final Context appContext;
    private final LocalImageStore imageStore;
    private final ApiService api = ApiClient.getApiService();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public VisitImageRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.imageStore = new LocalImageStore(appContext);
    }

    public void getImages(int visitPointId, ResultCallback<List<VisitImage>> callback) {
        api.getVisitImages(visitPointId).enqueue(new Callback<VisitImagesResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitImagesResponse> call,
                                   @NonNull Response<VisitImagesResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<VisitImage> data = response.body().getData();
                    if (data == null) data = new ArrayList<>();
                    final List<VisitImage> fresh = data;
                    io.execute(() -> LocalDataCache.saveVisitImages(appContext, visitPointId, fresh));
                    callback.onResult(Resource.success(fresh));
                } else {
                    fallbackToLocal(visitPointId, "Не удалось загрузить фотографии", callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitImagesResponse> call, @NonNull Throwable t) {
                fallbackToLocal(visitPointId, AppToast.ERR_NETWORK, callback);
            }
        });
    }

    private void fallbackToLocal(int visitPointId, String errorMessage,
                                 ResultCallback<List<VisitImage>> callback) {
        io.execute(() -> {
            List<VisitImage> cached = LocalDataCache.getVisitImages(appContext, visitPointId);
            List<VisitImage> available = new ArrayList<>();
            if (cached != null) {
                for (VisitImage img : cached) {
                    if (img != null && imageStore.hasImage(visitPointId, img.getId())) {
                        available.add(img);
                    }
                }
            }
            main.post(() -> {
                if (!available.isEmpty()) {
                    callback.onResult(Resource.success(available));
                } else {
                    callback.onResult(Resource.error(errorMessage));
                }
            });
        });
    }

    public void uploadImage(int visitPointId, Uri uri, ResultCallback<VisitImage> callback) {
        io.execute(() -> {
            try {
                ContentResolver resolver = appContext.getContentResolver();
                byte[] bytes = readBytes(resolver, uri);
                if (bytes == null || bytes.length == 0) {
                    main.post(() -> callback.onResult(Resource.error("Не удалось прочитать файл")));
                    return;
                }
                if (bytes.length > MAX_BYTES) {
                    main.post(() -> callback.onResult(Resource.error("Файл слишком большой (до 12 МБ)")));
                    return;
                }

                String type = resolver.getType(uri);
                if (type == null || type.isEmpty()) type = "image/jpeg";
                String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                String name = "photo_" + System.currentTimeMillis() + (ext != null ? "." + ext : ".jpg");

                RequestBody body = RequestBody.create(MediaType.parse(type), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", name, body);

                final byte[] savedBytes = bytes;
                api.uploadVisitImage(visitPointId, part).enqueue(new Callback<VisitImageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VisitImageResponse> call,
                                           @NonNull Response<VisitImageResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            VisitImage saved = response.body().getData();
                            io.execute(() -> imageStore.saveImage(visitPointId, saved.getId(), savedBytes));
                            callback.onResult(Resource.success(saved));
                        } else {
                            callback.onResult(Resource.error("Не удалось загрузить фото"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VisitImageResponse> call, @NonNull Throwable t) {
                        callback.onResult(Resource.error(AppToast.ERR_NETWORK));
                    }
                });
            } catch (Exception e) {
                main.post(() -> callback.onResult(Resource.error("Не удалось загрузить фото")));
            }
        });
    }

    public void deleteImage(int visitPointId, int imageId, ResultCallback<Boolean> callback) {
        api.deleteVisitImage(visitPointId, imageId).enqueue(new Callback<VisitImageResponse>() {
            @Override
            public void onResponse(@NonNull Call<VisitImageResponse> call,
                                   @NonNull Response<VisitImageResponse> response) {
                boolean ok = response.isSuccessful() && response.body() != null && response.body().isSuccess();
                if (ok) {
                    io.execute(() -> imageStore.deleteImage(visitPointId, imageId));
                    callback.onResult(Resource.success(Boolean.TRUE));
                } else {
                    callback.onResult(Resource.error("Не удалось удалить фото"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<VisitImageResponse> call, @NonNull Throwable t) {
                callback.onResult(Resource.error(AppToast.ERR_NETWORK));
            }
        });
    }

    public static String resolveUrl(int visitPointId, VisitImage image) {
        if (image == null) return null;
        String base = ApiClient.getBaseUrl();
        String url = image.getUrl();
        if (url != null) {
            String u = url.trim();
            if (!u.isEmpty()) {
                if (u.startsWith("http://") || u.startsWith("https://")) {
                    String path = pathOf(u);
                    if (path != null) return base + path;
                    return u;
                }
                if (u.startsWith("/")) return base + u;
            }
        }
        return base + "/api/visit-points/" + visitPointId + "/images/" + image.getId();
    }

    private static String pathOf(String absoluteUrl) {
        int schemeEnd = absoluteUrl.indexOf("://");
        if (schemeEnd < 0) return null;
        int pathStart = absoluteUrl.indexOf('/', schemeEnd + 3);
        if (pathStart < 0) return null;
        return absoluteUrl.substring(pathStart);
    }

    private static byte[] readBytes(ContentResolver resolver, Uri uri) throws Exception {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }
}
