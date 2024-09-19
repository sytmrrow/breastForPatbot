package com.example.activity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;


public class StarImageView extends AppCompatImageView {
    private float centerX;
    private float centerY;

    public StarImageView(Context context) {
        super(context);
    }

    public StarImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StarImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 画星形标记
        if (centerX != 0 && centerY != 0) {
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);

            Path starPath = new Path();
            starPath.moveTo(centerX, centerY - 20f);
            starPath.lineTo(centerX + 12f, centerY + 15f);
            starPath.lineTo(centerX - 20f, centerY - 5f);
            starPath.lineTo(centerX + 20f, centerY - 5f);
            starPath.lineTo(centerX - 12f, centerY + 15f);
            starPath.close();

            canvas.drawPath(starPath, paint);
        }
    }

    public void setCenter(float x, float y) {
        this.centerX = x;
        this.centerY = y;
        invalidate(); // 重新绘制视图
    }
}
