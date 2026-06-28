package com.example.polynation.data.local;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;

public class LocalImageStore {

    private static final String DIR = "visit_images";

    private final File baseDir;

    public LocalImageStore(Context context) {
        baseDir = new File(context.getApplicationContext().getFilesDir(), DIR);
        if (!baseDir.exists()) baseDir.mkdirs();
    }

    public File imageFile(int visitPointId, int imageId) {
        return new File(baseDir, "vp" + visitPointId + "_img" + imageId + ".img");
    }

    public boolean hasImage(int visitPointId, int imageId) {
        File f = imageFile(visitPointId, imageId);
        return f.exists() && f.length() > 0;
    }

    public void saveImage(int visitPointId, int imageId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        try (FileOutputStream out = new FileOutputStream(imageFile(visitPointId, imageId))) {
            out.write(bytes);
        } catch (Exception ignored) {
        }
    }

    public void deleteImage(int visitPointId, int imageId) {
        File f = imageFile(visitPointId, imageId);
        if (f.exists()) {
            f.delete();
        }
    }
}
