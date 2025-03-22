package eu.example.realtimefr.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;


public class ImageProcessor {
    private static final int MAX_COLOR_VALUE = 262143;

    public static int computeYUVSize(final int width, final int height) {
        final int yPlane = width * height;
        final int uvPlane = ((width + 1) / 2) * ((height + 1) / 2) * 2;
        return yPlane + uvPlane;
    }

    public static void storeBitmap(final Bitmap image) {
        storeBitmap(image, "image_preview.png");
    }

    public static void storeBitmap(final Bitmap image, final String filename) {
        final String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "image_storage";
        final File dir = new File(directoryPath);
        if (!dir.mkdirs()) {
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
        final int frameSize = width * height;
        for (int j = 0, index = 0; j < height; j++) {
            int uvIndex = frameSize + (j >> 1) * width;
            int u = 0, v = 0;

            for (int i = 0; i < width; i++, index++) {
                int y = 0xff & yuvData[index];
                if ((i & 1) == 0) {
                    v = 0xff & yuvData[uvIndex++];
                    u = 0xff & yuvData[uvIndex++];
                }
                rgbOutput[index] = convertYUVToRGB(y, u, v);
            }
        }
    }

    public static void transformYUV420ToRGB(
            byte[] luminance,
            byte[] chromaU,
            byte[] chromaV,
            int imageWidth,
            int imageHeight,
            int luminanceStride,
            int chromaStride,
            int chromaPixelStride,
            int[] rgbOutput) {
        int pixelIndex = 0;
        for (int row = 0; row < imageHeight; row++) {
            int lumRowOffset = luminanceStride * row;
            int chromaRowOffset = chromaStride * (row >> 1);

            for (int col = 0; col < imageWidth; col++) {
                int chromaOffset = chromaRowOffset + (col >> 1) * chromaPixelStride;
                rgbOutput[pixelIndex++] = convertYUVToRGB(
                        0xff & luminance[lumRowOffset + col],
                        0xff & chromaU[chromaOffset],
                        0xff & chromaV[chromaOffset]
                );
            }
        }
    }

    private static int convertYUVToRGB(int y, int u, int v) {
        y = Math.max(y - 16, 0);
        u -= 128;
        v -= 128;

        int yScale = 1192 * y;
        int r = yScale + 1634 * v;
        int g = yScale - 833 * v - 400 * u;
        int b = yScale + 2066 * u;

        r = Math.min(MAX_COLOR_VALUE, Math.max(0, r));
        g = Math.min(MAX_COLOR_VALUE, Math.max(0, g));
        b = Math.min(MAX_COLOR_VALUE, Math.max(0, b));

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    public static Matrix createTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int rotationAngle,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (rotationAngle != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(rotationAngle);
        }

        final boolean isTransposed = (Math.abs(rotationAngle) + 90) % 180 == 0;
        final int adjustedWidth = isTransposed ? srcHeight : srcWidth;
        final int adjustedHeight = isTransposed ? srcWidth : srcHeight;

        if (adjustedWidth != dstWidth || adjustedHeight != dstHeight) {
            final float scaleX = dstWidth / (float) adjustedWidth;
            final float scaleY = dstHeight / (float) adjustedHeight;

            if (maintainAspectRatio) {
                final float scaleFactor = Math.max(scaleX, scaleY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                matrix.postScale(scaleX, scaleY);
            }
        }

        if (rotationAngle != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
}
