package eu.example.realtimefr.viewmodel;

import android.graphics.Bitmap;
import android.graphics.RectF;


public interface FaceRecognizer {

    void addIdentity(String label, RecognitionData data);

    RecognitionData processImage(Bitmap image, boolean includeMetadata);

    class RecognitionData {
        private final String identifier;
        private final String label;
        private final Float confidenceScore;
        private Object featureVector;
        private RectF boundingBox;
        private Bitmap faceThumbnail;

        public RecognitionData(String identifier, String label, Float confidenceScore, RectF boundingBox) {
            this.identifier = identifier;
            this.label = label;
            this.confidenceScore = confidenceScore;
            this.boundingBox = boundingBox;
            this.featureVector = null;
            this.faceThumbnail = null;
        }

        public RecognitionData(String label, Object featureVector) {
            this.identifier = null;
            this.label = label;
            this.confidenceScore = null;
            this.boundingBox = null;
            this.featureVector = featureVector;
            this.faceThumbnail = null;
        }

        public void setFeatureVector(Object vector) {
            this.featureVector = vector;
        }

        public Object getFeatureVector() {
            return this.featureVector;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getLabel() {
            return label;
        }

        public Float getConfidenceScore() {
            return confidenceScore;
        }

        public RectF getBoundingBox() {
            return boundingBox == null ? null : new RectF(boundingBox);
        }

        public void setBoundingBox(RectF boundingBox) {
            this.boundingBox = boundingBox;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            if (identifier != null) {
                result.append("[").append(identifier).append("] ");
            }
            if (label != null) {
                result.append(label).append(" ");
            }
            if (confidenceScore != null) {
                result.append(String.format("(%.1f%%) ", confidenceScore * 100.0f));
            }
            if (boundingBox != null) {
                result.append(boundingBox).append(" ");
            }
            return result.toString().trim();
        }

        public void setFaceThumbnail(Bitmap thumbnail) {
            this.faceThumbnail = thumbnail;
        }

        public Bitmap getFaceThumbnail() {
            return this.faceThumbnail;
        }
    }
}