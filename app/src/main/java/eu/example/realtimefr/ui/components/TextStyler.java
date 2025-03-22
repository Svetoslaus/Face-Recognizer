package eu.example.realtimefr.ui.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import java.util.List;


public class TextStyler {
    private final Paint fillPaint;
    private final Paint outlinePaint;
    private final float textSize;


    public TextStyler(final float textSize) {
        this(Color.WHITE, Color.BLACK, textSize);
    }


    public TextStyler(final int fillColor, final int outlineColor, final float textSize) {
        fillPaint = new Paint();
        fillPaint.setTextSize(textSize);
        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        outlinePaint = new Paint();
        outlinePaint.setTextSize(textSize);
        outlinePaint.setColor(outlineColor);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(textSize / 8);
        outlinePaint.setAntiAlias(true);

        this.textSize = textSize;
    }

    public void applyTypeface(Typeface typeface) {
        fillPaint.setTypeface(typeface);
        outlinePaint.setTypeface(typeface);
    }

    public void renderText(final Canvas canvas, final float x, final float y, final String content) {
        canvas.drawText(content, x, y, outlinePaint);
        canvas.drawText(content, x, y, fillPaint);
    }

    public void renderTextWithBackground(final Canvas canvas, final float x, final float y, final String content, Paint bgPaint) {
        float width = outlinePaint.measureText(content);
        float height = outlinePaint.getTextSize();
        Paint background = new Paint(bgPaint);
        background.setStyle(Paint.Style.FILL);
        background.setAlpha(160);
        canvas.drawRect(x, y, x + width, y + height, background);
        renderText(canvas, x, y + height, content);
    }

    public void renderMultipleLines(Canvas canvas, final float x, final float y, List<String> textLines) {
        for (int i = 0; i < textLines.size(); i++) {
            renderText(canvas, x, y - textSize * (textLines.size() - i - 1), textLines.get(i));
        }
    }

    public void updateTextColor(final int color) {
        fillPaint.setColor(color);
    }

    public void updateOutlineColor(final int color) {
        outlinePaint.setColor(color);
    }

    public float retrieveTextSize() {
        return textSize;
    }

    public void adjustAlpha(final int alphaValue) {
        fillPaint.setAlpha(alphaValue);
        outlinePaint.setAlpha(alphaValue);
    }

    public void computeTextBounds(final String text, final int start, final int end, final Rect bounds) {
        fillPaint.getTextBounds(text, start, end, bounds);
    }

    public void defineTextAlignment(Paint.Align align) {
        fillPaint.setTextAlign(align);
        outlinePaint.setTextAlign(align);
    }
}

