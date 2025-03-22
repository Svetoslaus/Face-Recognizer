package eu.example.realtimefr.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;


public class ImageTransformer {
    private static final int MAX_COLOR_VALUE = 262143;

    public static int computeYUVSize(final int width, final int height) {
        int yPlane = width * height;
        int uvPlane = ((width + 1) / 2) * ((height + 1) / 2) * 2;
        return yPlane + uvPlane;
    }

    public static void storeBitmap(final Bitmap image, final String filename) {
        final String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "image_processing";
        final File dir = new File(storagePath);
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }

        final File file = new File(dir, filename);
        if (file.exists()) {
            file.delete();
        }

        try (FileOutputStream outStream = new FileOutputStream(file)) {
            image.compress(Bitmap.CompressFormat.PNG, 99, outStream);
            outStream.flush();
        } catch (Exception ignored) {
        }
    }

    public static void convertYUVToRGB(byte[] yuvData, int width, int height, int[] rgbOutput) {
        final int frameArea = width * height;
        for (int row = 0, pixelIndex = 0; row < height; row++) {
            int uvIndex = frameArea + (row >> 1) * width;
            int u = 0, v = 0;
            for (int col = 0; col < width; col++, pixelIndex++) {
                int y = 0xff & yuvData[pixelIndex];
                if ((col & 1) == 0) {
                    v = 0xff & yuvData[uvIndex++];
                    u = 0xff & yuvData[uvIndex++];
                }
                rgbOutput[pixelIndex] = yuvToRgb(y, u, v);
            }
        }
    }

    private static int yuvToRgb(int y, int u, int v) {
        y = Math.max(y - 16, 0);
        u -= 128;
        v -= 128;

        int yScaled = 1192 * y;
        int r = yScaled + 1634 * v;
        int g = yScaled - 833 * v - 400 * u;
        int b = yScaled + 2066 * u;

        r = Math.min(MAX_COLOR_VALUE, Math.max(0, r));
        g = Math.min(MAX_COLOR_VALUE, Math.max(0, g));
        b = Math.min(MAX_COLOR_VALUE, Math.max(0, b));

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    public static Matrix generateTransformationMatrix(
            int sourceWidth, int sourceHeight, int targetWidth, int targetHeight, int rotationAngle, boolean preserveAspectRatio) {
        Matrix transformation = new Matrix();
        if (rotationAngle != 0) {
            transformation.postTranslate(-sourceWidth / 2.0f, -sourceHeight / 2.0f);
            transformation.postRotate(rotationAngle);
        }

        boolean rotated = (Math.abs(rotationAngle) + 90) % 180 == 0;
        int adjustedWidth = rotated ? sourceHeight : sourceWidth;
        int adjustedHeight = rotated ? sourceWidth : sourceHeight;

        if (adjustedWidth != targetWidth || adjustedHeight != targetHeight) {
            float scaleX = targetWidth / (float) adjustedWidth;
            float scaleY = targetHeight / (float) adjustedHeight;

            if (preserveAspectRatio) {
                float scale = Math.max(scaleX, scaleY);
                transformation.postScale(scale, scale);
            } else {
                transformation.postScale(scaleX, scaleY);
            }
        }

        if (rotationAngle != 0) {
            transformation.postTranslate(targetWidth / 2.0f, targetHeight / 2.0f);
        }
        return transformation;
    }
}
