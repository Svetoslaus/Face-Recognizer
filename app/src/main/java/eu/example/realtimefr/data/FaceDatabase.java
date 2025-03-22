package eu.example.realtimefr.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import eu.example.realtimefr.viewmodel.FaceRecognizer;

public class FaceDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Faces.db";
    private static final String TABLE_NAME = "faces";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_EMBEDDING = "embedding";

    public FaceDatabase(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_NAME + " TEXT UNIQUE, " +
                        COLUMN_EMBEDDING + " TEXT NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    private String embeddingToString(float[][] embedding) {
        if (embedding == null || embedding.length == 0) return "";
        StringBuilder embeddingString = new StringBuilder();
        for (Float f : embedding[0]) {
            embeddingString.append(f).append(",");
        }
        return embeddingString.toString();
    }


    private float[][] stringToEmbedding(String embeddingString) {
        if (embeddingString == null || embeddingString.isEmpty()) return new float[1][0];

        String[] stringList = embeddingString.split(",");
        float[][] embeddingArray = new float[1][stringList.length];

        for (int i = 0; i < stringList.length; i++) {
            embeddingArray[0][i] = Float.parseFloat(stringList[i]);
        }
        return embeddingArray;
    }


    public boolean addFace(String name, Object embedding) {
        float[][] floatList = (float[][]) embedding;
        String embeddingString = embeddingToString(floatList);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, name);
        contentValues.put(COLUMN_EMBEDDING, embeddingString);

        long result = db.insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        Log.d("FaceDatabase", "Face added: " + name);
        return result != -1;
    }


    public Cursor getFace(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + "=" + id, null);
    }


    public int getFaceCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
    }


    public boolean updateFace(String name, Object embedding) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_EMBEDDING, embeddingToString((float[][]) embedding));

        int affectedRows = db.update(TABLE_NAME, contentValues, COLUMN_NAME + " = ?", new String[]{name});
        db.close();

        Log.d("FaceDatabase", "Face updated: " + name + " | Rows affected: " + affectedRows);
        return affectedRows > 0;
    }


    public boolean removeFace(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_NAME, COLUMN_NAME + " = ?", new String[]{name});
        db.close();

        Log.d("FaceDatabase", "Face removed: " + name);
        return deletedRows > 0;
    }


    @SuppressLint("Range")
    public Map<String, FaceRecognizer.RecognitionData> getAllFaces() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        HashMap<String, FaceRecognizer.RecognitionData> registeredFaces = new HashMap<>();

        while (res.moveToNext()) {
            String name = res.getString(res.getColumnIndex(COLUMN_NAME));
            String embeddingString = res.getString(res.getColumnIndex(COLUMN_EMBEDDING));

            float[][] embeddingArray = stringToEmbedding(embeddingString);
            FaceRecognizer.RecognitionData recognition = new FaceRecognizer.RecognitionData(name, embeddingArray);

            registeredFaces.put(name, recognition);
        }
        res.close();
        db.close();

        Log.d("FaceDatabase", "Total stored faces: " + registeredFaces.size());
        return registeredFaces;
    }
}
