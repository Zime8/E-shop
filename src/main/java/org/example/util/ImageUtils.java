package org.example.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;

public final class ImageUtils {
    private ImageUtils() {}

    public static void setImage(ImageView view, byte[] bytes) {
        if (view == null) return;
        if (bytes == null || bytes.length == 0) { view.setImage(null); return; }
        view.setImage(new Image(new ByteArrayInputStream(bytes)));
    }
}
