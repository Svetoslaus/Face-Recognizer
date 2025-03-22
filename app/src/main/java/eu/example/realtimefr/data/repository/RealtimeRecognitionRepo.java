package eu.example.realtimefr.data.repository;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import eu.example.realtimefr.data.FaceDatabase;
import eu.example.realtimefr.viewmodel.FaceRecognizer;

public class RealtimeRecognitionRepo implements FaceRecognizer {

    private static final int EMBEDDING_SIZE = 512;
    private static final float NORMALIZATION_MEAN = 128.0f;
    private static final float NORMALIZATION_STD = 128.0f;

    private boolean isQuantized;
    private int inputSize;
    private int[] pixelBuffer;
    private float[][] faceEmbeddings;
    private ByteBuffer imageByteBuffer;
    private Interpreter faceModel;

    private final HashMap<String, RecognitionData> knownFaces = new HashMap<>();
    private final FaceDatabase database;

    public void addFace(String name, RecognitionData recognition) {
        database.addFace(name, recognition.getFeatureVector());
        knownFaces.put(name, recognition);
    }

    private RealtimeRecognitionRepo(Context context) {
        database = new FaceDatabase(context);
        knownFaces.putAll(database.getAllFaces());
    }

    private static MappedByteBuffer loadModel(AssetManager assets, String modelPath) throws IOException {
        AssetFileDescriptor descriptor = assets.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, descriptor.getStartOffset(), descriptor.getDeclaredLength());
    }

    public static FaceRecognizer initialize(
            final AssetManager assetManager,
            final String modelPath,
            final int inputSize,
            final boolean isQuantized,
            Context context) throws IOException {
        final RealtimeRecognitionRepo recognizer = new RealtimeRecognitionRepo(context);
        recognizer.inputSize = inputSize;

        try {
            recognizer.faceModel = new Interpreter(loadModel(assetManager, modelPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        recognizer.isQuantized = isQuantized;
        int bytesPerChannel = isQuantized ? 1 : 4;
        recognizer.imageByteBuffer = ByteBuffer.allocateDirect(1 * recognizer.inputSize * recognizer.inputSize * 3 * bytesPerChannel);
        recognizer.imageByteBuffer.order(ByteOrder.nativeOrder());
        recognizer.pixelBuffer = new int[recognizer.inputSize * recognizer.inputSize];
        return recognizer;
    }

    private Pair<String, Float> findClosestMatch(float[] embedding) {
        Pair<String, Float> bestMatch = null;
        float bestDistance = Float.MAX_VALUE; // Track the closest match

        Log.d("FaceRecognition", "Starting face matching. Comparing with " + knownFaces.size() + " stored faces.");

        for (Map.Entry<String, RecognitionData> entry : knownFaces.entrySet()) {
            final String name = entry.getKey();
            final float[] storedEmbedding = ((float[][]) entry.getValue().getFeatureVector())[0];

            float distance = 0;
            for (int i = 0; i < embedding.length; i++) {
                float diff = embedding[i] - storedEmbedding[i];
                distance += diff * diff;
            }
            distance = (float) Math.sqrt(distance); // Compute Euclidean distance

            Log.d("FaceRecognition", "Comparing with " + name + " -> Distance: " + distance);

            if (bestMatch == null || distance < bestDistance) {
                bestMatch = new Pair<>(name, distance);
                bestDistance = distance;
            }
        }

        if (bestMatch != null) {
            Log.d("FaceRecognition", "Best match: " + bestMatch.first + " with distance: " + bestMatch.second);
        } else {
            Log.d("FaceRecognition", "No match found!");
        }

        return bestMatch;
    }


    @Override
    public void addIdentity(String name, RecognitionData recognition) {
        if (recognition.getFeatureVector() == null) {
            Log.e("FaceRegistration", "ERROR: Cannot register " + name + ". Feature vector is NULL!");
            return;
        }


        database.addFace(name, recognition.getFeatureVector());
        knownFaces.clear();

        knownFaces.put(name, recognition);

        Log.d("FaceRegistration", "Face registered: " + name + " with embedding: " + recognition.getFeatureVector());
    }


    @Override
    public RecognitionData processImage(final Bitmap bitmap, boolean storeEmbedding) {
        bitmap.getPixels(pixelBuffer, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imageByteBuffer.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = pixelBuffer[i * inputSize + j];
                if (isQuantized) {
                    imageByteBuffer.put((byte) ((pixelValue >> 16) & 0xFF));
                    imageByteBuffer.put((byte) ((pixelValue >> 8) & 0xFF));
                    imageByteBuffer.put((byte) (pixelValue & 0xFF));
                } else {
                    imageByteBuffer.putFloat((((pixelValue >> 16) & 0xFF) - NORMALIZATION_MEAN) / NORMALIZATION_STD);
                    imageByteBuffer.putFloat((((pixelValue >> 8) & 0xFF) - NORMALIZATION_MEAN) / NORMALIZATION_STD);
                    imageByteBuffer.putFloat(((pixelValue & 0xFF) - NORMALIZATION_MEAN) / NORMALIZATION_STD);
                }
            }
        }
        Object[] inputArray = {imageByteBuffer};
        Map<Integer, Object> outputMap = new HashMap<>();

        faceEmbeddings = new float[1][EMBEDDING_SIZE];
        outputMap.put(0, faceEmbeddings);
        faceModel.runForMultipleInputsOutputs(inputArray, outputMap);

        float distance = Float.MAX_VALUE;
        String id = "0";
        String label = "Unknown";

        if (!knownFaces.isEmpty()) {
            final Pair<String, Float> closestMatch = findClosestMatch(faceEmbeddings[0]);
            if (closestMatch != null) {
                label = closestMatch.first;
                distance = closestMatch.second;
            }
        }

        RecognitionData result = new RecognitionData(id, label, distance, new RectF());
        if (storeEmbedding) {
            result.setFeatureVector(faceEmbeddings);
        }
        return result;
    }
}