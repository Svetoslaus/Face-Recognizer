package eu.example.realtimefr.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import eu.example.realtimefr.utils.ImageProcessor;
import eu.example.realtimefr.viewmodel.FaceRecognizer;


public class FaceDetectionTracker {
    private static final float TEXT_SCALE = 18;
    private static final float MIN_DETECTION_SIZE = 16.0f;
    private static final int[] COLOR_PALETTE = {
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN,
            Color.MAGENTA, Color.WHITE, Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"), Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"), Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };
    private final List<Pair<Float, RectF>> detectedObjects = new LinkedList<>();
    private final Queue<Integer> colorQueue = new LinkedList<>();
    private final List<TrackedFace> trackedFaces = new LinkedList<>();
    private final Paint boundingBoxPaint = new Paint();
    private final float textSizePx;
    private final TextStyler textRenderer;
    private Matrix transformMatrix;
    private int imageWidth;
    private int imageHeight;
    private int imageOrientation;

    public FaceDetectionTracker(final Context context) {
        for (final int color : COLOR_PALETTE) {
            colorQueue.add(color);
        }

        boundingBoxPaint.setColor(Color.RED);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(10.0f);
        boundingBoxPaint.setStrokeCap(Paint.Cap.ROUND);
        boundingBoxPaint.setStrokeJoin(Paint.Join.ROUND);
        boundingBoxPaint.setStrokeMiter(100);

        textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SCALE, context.getResources().getDisplayMetrics());
        textRenderer = new TextStyler(textSizePx);
    }

    public synchronized void configureFrame(final int width, final int height, final int orientation) {
        imageWidth = width;
        imageHeight = height;
        this.imageOrientation = orientation;
    }

    public synchronized void renderDebug(Canvas canvas) {
        Paint debugTextPaint = new Paint();
        debugTextPaint.setColor(Color.WHITE);
        debugTextPaint.setTextSize(60.0f);

        Paint debugBoxPaint = new Paint();
        debugBoxPaint.setColor(Color.RED);
        debugBoxPaint.setAlpha(200);
        debugBoxPaint.setStyle(Paint.Style.STROKE);

        for (final Pair<Float, RectF> detection : detectedObjects) {
            final RectF rect = detection.second;
            canvas.drawRect(rect, debugBoxPaint);
            canvas.drawText("" + detection.first, rect.left, rect.top, debugTextPaint);
            textRenderer.renderText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
        }
    }

    public synchronized void updateTrackedObjects(final List<FaceRecognizer.RecognitionData> results, final long timestamp) {
        processDetections(results);
    }

    @SuppressLint("SuspiciousIndentation")
    public synchronized void draw(Canvas canvas) {
        final boolean isRotated = imageOrientation % 180 == 90;
        final float scale = Math.min(
                canvas.getHeight() / (float) (isRotated ? imageWidth : imageHeight),
                canvas.getWidth() / (float) (isRotated ? imageHeight : imageWidth));

        transformMatrix = ImageProcessor.createTransformationMatrix(
                imageWidth, imageHeight,
                (int) (scale * (isRotated ? imageHeight : imageWidth)),
                (int) (scale * (isRotated ? imageWidth : imageHeight)),
                imageOrientation, false);

        for (final TrackedFace face : trackedFaces) {
            final RectF adjustedRect = new RectF(face.position);
            transformMatrix.mapRect(adjustedRect);
            boundingBoxPaint.setColor(face.color);

            float cornerRadius = Math.min(adjustedRect.width(), adjustedRect.height()) / 8.0f;
            canvas.drawRoundRect(adjustedRect, cornerRadius, cornerRadius, boundingBoxPaint);

            final String label = !TextUtils.isEmpty(face.label)
                    ? String.format("%s %.2f", face.label, face.confidence)
                    : String.format("%.2f", face.confidence);
            textRenderer.renderTextWithBackground(canvas, adjustedRect.left + cornerRadius, adjustedRect.top, label, boundingBoxPaint);
        }
    }

    private void processDetections(final List<FaceRecognizer.RecognitionData> results) {
        final List<Pair<Float, FaceRecognizer.RecognitionData>> candidates = new LinkedList<>();
        detectedObjects.clear();
        final Matrix transformToScreen = new Matrix(transformMatrix);

        for (final FaceRecognizer.RecognitionData face : results) {
            if (face.getBoundingBox() == null) {
                continue;
            }
            final RectF detectionBox = new RectF(face.getBoundingBox());
            final RectF screenBox = new RectF();
            transformToScreen.mapRect(screenBox, detectionBox);

            detectedObjects.add(new Pair<>(face.getConfidenceScore(), screenBox));

            if (detectionBox.width() < MIN_DETECTION_SIZE || detectionBox.height() < MIN_DETECTION_SIZE) {
                continue;
            }

            candidates.add(new Pair<>(face.getConfidenceScore(), face));
        }

        trackedFaces.clear();
        if (candidates.isEmpty()) {
            return;
        }

        for (final Pair<Float, FaceRecognizer.RecognitionData> candidate : candidates) {
            final TrackedFace trackedFace = new TrackedFace();
            trackedFace.confidence = candidate.first;
            trackedFace.position = new RectF(candidate.second.getBoundingBox());
            trackedFace.label = candidate.second.getLabel();
            trackedFace.color = COLOR_PALETTE[trackedFaces.size()];
            trackedFaces.add(trackedFace);

            if (trackedFaces.size() >= COLOR_PALETTE.length) {
                break;
            }
        }
    }

    private static class TrackedFace {
        RectF position;
        float confidence;
        int color;
        String label;
    }
}
