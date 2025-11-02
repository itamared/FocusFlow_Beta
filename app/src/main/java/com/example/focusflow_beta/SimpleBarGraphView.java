package com.example.focusflow_beta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SimpleBarGraphView extends View {

    private Paint paintStudy = new Paint();
    private Paint paintBreak = new Paint();
    private float studyHours = 7f;  // ברירת מחדל
    private float breakHours = 1f;  // ברירת מחדל

    public SimpleBarGraphView(Context context) {
        super(context);
        init();
    }

    public SimpleBarGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintStudy.setColor(Color.BLUE);
        paintBreak.setColor(Color.RED);
    }

    public void setData(float studyHours, float breakHours) {
        this.studyHours = studyHours;
        this.breakHours = breakHours;
        invalidate(); // מצייר מחדש
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float total = studyHours + breakHours;

        if (total == 0) total = 1; // מניעת חלוקה באפס

        float studyHeight = (studyHours / total) * height;
        float breakHeight = (breakHours / total) * height;

        // ציור בר לימודים
        canvas.drawRect(width * 0.1f, height - studyHeight, width * 0.4f, height, paintStudy);

        // ציור בר הפסקה
        canvas.drawRect(width * 0.6f, height - breakHeight, width * 0.9f, height, paintBreak);
    }
}

