package com.example.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ai.face.R;

public class FloorPlanActivity extends AppCompatActivity {

    private TargetPoint targetPoint;
    private StarImageView floorPlanImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取位置参数
        String location = getIntent().getStringExtra("location");
        targetPoint = getTargetPointFromJson(this, location);

        // 创建 ImageView 来显示楼层图片
        floorPlanImageView = new StarImageView(this);
        floorPlanImageView.setScaleType(ImageView.ScaleType.FIT_CENTER); // 确保图片按比例缩放

        if (targetPoint != null) {
            updateUI();
        } else {
            showAlertDialog("该目标点暂未开放");
        }

        // 创建一个布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER); // 布局居中

        // 将 ImageView 添加到布局
        layout.addView(floorPlanImageView);

        // 创建一个 TextView 来显示提示信息
        TextView infoTextView = new TextView(this);
        String message = "您当前位于一楼大厅，" + targetPoint.getNLP_name() + "位于" + targetPoint.getFloor() + "层" + targetPoint.getNum_name() + "。";
        if (targetPoint.getFloor() > 1) {
            message += "您可以选择楼梯或乘坐电梯前往目的地。";
        }
        infoTextView.setText(message);
        infoTextView.setGravity(Gravity.CENTER); // 文字居中
        infoTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // 调整字体大小


        // 设置 TextView 的布局参数，使其居中
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, 30, 0, 30); // 设置上方和下方的间距
        infoTextView.setLayoutParams(textParams);
        // 将 TextView 添加到布局
        layout.addView(infoTextView);

        // 创建一个按钮
        Button goToVideoButton = new Button(this);
        goToVideoButton.setText("返回");

        // 设置按钮大小并居中
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 20, 0, 20); // 设置上方和下方的间距
        goToVideoButton.setLayoutParams(buttonParams);
        goToVideoButton.setGravity(Gravity.CENTER); // 按钮居中

        // 设置按钮的点击事件
        goToVideoButton.setOnClickListener(v -> {
            finish();  // 结束当前的Activity，返回到上一个Activity
        });


        // 将按钮添加到布局
        layout.addView(goToVideoButton);

        // 将布局设置为内容视图
        setContentView(layout);
    }

    // 获取楼层图片资源 ID
    private int getFloorImageResource(int floor) {
        switch (floor) {
            case 1:
                return R.drawable.floor1;
            case 2:
                return R.drawable.floor2;
            case 3:
                return R.drawable.floor3;
            case 4:
                return R.drawable.floor4;
            default:
                return R.drawable.floor1; // 默认楼层图片
        }
    }

    // 从 JSON 中获取目标点信息
    private TargetPoint getTargetPointFromJson(Context context, String location) {
        try {
            InputStream is = context.getAssets().open("location_final.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            String json = jsonBuilder.toString();

            Log.d("FloorPlanActivity", "JSON Content: " + json);

            List<TargetPoint> targetPointList = new Gson().fromJson(json, new TypeToken<List<TargetPoint>>() {}.getType());

            for (TargetPoint point : targetPointList) {
                Log.d("FloorPlanActivity", "Checking point: " + point.getId() + ", " + point.getNLP_name());
                if (point.getId().equals(location) || point.getNLP_name().equals(location) || point.getNum_name().equals(location)) {
                    return point;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    // 获取图片在 ImageView 中的实际显示位置和大小
    RectF getImageRect(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return null;
        }

        // 获取 ImageView 的矩阵
        Matrix matrix = imageView.getImageMatrix();
        float[] values = new float[9];
        matrix.getValues(values);

        // 获取图片的原始尺寸
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        // 计算图片的显示区域
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        float left = transX;
        float top = transY;
        float right = left + intrinsicWidth * scaleX;
        float bottom = top + intrinsicHeight * scaleY;

        return new RectF(left, top, right, bottom);
    }

    private void updateUI() {
        int floorResource = getFloorImageResource(targetPoint.getFloor());
        floorPlanImageView.setImageResource(floorResource); // 设置图片资源

        // 构建提示信息
        String message = "您当前位于一楼大厅，" + targetPoint.getNLP_name() + "位于" + targetPoint.getFloor() + "层" + targetPoint.getNum_name() + "。";
        if (targetPoint.getFloor() > 1) {
            message += "您可以选择楼梯或乘坐电梯前往目的地。";
        }


        // 显示提示信息
        Toast.makeText(this, message, Toast.LENGTH_LONG).show(); // 约3.5s

        // 监听图片的布局变化，获取 ImageView 的实际宽高
        floorPlanImageView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            RectF imageRect = getImageRect(floorPlanImageView);

            if (imageRect != null) {
                float centerX = imageRect.left + targetPoint.getX() * imageRect.width();
                float centerY = imageRect.top + targetPoint.getY() * imageRect.height();

                // 设置星星的位置
                floorPlanImageView.setCenter(centerX, centerY);

                // 获取大厅、电梯点、候梯点并绘制
                List<TargetPoint> additionalPoints = getAdditionalPoints(targetPoint.getFloor());
                for (TargetPoint point : additionalPoints) {
                    float pointX = imageRect.left + point.getX() * imageRect.width();
                    float pointY = imageRect.top + point.getY() * imageRect.height();

                    // 根据地点名称设置不同的标记
                    if (point.getNLP_name().contains("大厅")) {
                        floorPlanImageView.addMarker(pointX, pointY, "hall");
                    } else if (point.getNLP_name().contains("电梯点")) {
                        floorPlanImageView.addMarker(pointX, pointY, "elevator");
                    } else if (point.getNLP_name().contains("候梯点")) {
                        floorPlanImageView.addMarker(pointX, pointY, "waiting");
                    }
                }

            }
        });
    }

    // 获取大厅、电梯点、候梯点
    private List<TargetPoint> getAdditionalPoints(int floor) {
        List<TargetPoint> additionalPoints = new ArrayList<>();
        try {
            InputStream is = getAssets().open("location_final.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            String json = jsonBuilder.toString();

            List<TargetPoint> targetPointList = new Gson().fromJson(json, new TypeToken<List<TargetPoint>>() {}.getType());

            for (TargetPoint point : targetPointList) {
                if (point.getFloor() == floor &&
                        (point.getNLP_name().contains("大厅") ||
                                point.getNLP_name().contains("电梯点") ||
                                point.getNLP_name().contains("候梯点"))) {
                    additionalPoints.add(point);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return additionalPoints;
    }

    public class StarImageView extends androidx.appcompat.widget.AppCompatImageView {
        private float centerX;
        private float centerY;
        private List<Marker> markers = new ArrayList<>();

        public StarImageView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);  // 文字颜色
            textPaint.setTextSize(30);  // 设置较小字体

            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.WHITE);  // 背景颜色设置为白色
            backgroundPaint.setStyle(Paint.Style.FILL);

            // 绘制红色星星
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

                canvas.drawPath(starPath, paint); // 在指定位置绘制星星
            }

            // 绘制其他标记
            for (Marker marker : markers) {
                if (marker.type.equals("hall")) {
                    drawCircleMarker(canvas, marker.x, marker.y);
                    // 添加大厅文字描述

                    // 添加白色背景矩形
                    float textWidth = textPaint.measureText("您在1楼大厅");
                    float textHeight = textPaint.getTextSize();
                    canvas.drawRect(marker.x + 25, marker.y - textHeight, marker.x + 25 + textWidth, marker.y + 10, backgroundPaint);
                    canvas.drawText("您在1楼大厅", marker.x + 25, marker.y, textPaint);  // 显示文字在标记旁边
                } else if (marker.type.equals("elevator")) {
                    drawArrowMarker(canvas, marker.x, marker.y);


                    // 添加白色背景矩形
                    float textWidth = textPaint.measureText("电梯");
                    float textHeight = textPaint.getTextSize();
                    canvas.drawRect(marker.x + 25, marker.y - textHeight, marker.x + 25 + textWidth, marker.y + 10, backgroundPaint);

                    // 添加电梯点文字描述
                    canvas.drawText("电梯", marker.x + 25, marker.y, textPaint);  // 显示文字在标记旁边
                } else if (marker.type.equals("waiting")) {
                    drawArrowMarker(canvas, marker.x, marker.y);
                }
            }
        }

        // 绘制绿色圆点（大厅）
        private void drawCircleMarker(Canvas canvas, float x, float y) {
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);

            canvas.drawCircle(x, y, 10f, paint); // 在指定位置绘制圆点
        }

        // 绘制绿色箭头（电梯点和候梯点）
        private void drawArrowMarker(Canvas canvas, float x, float y) {
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);

            Path arrowPath = new Path();
            arrowPath.moveTo(x, y - 20f);
            arrowPath.lineTo(x + 10f, y + 10f);
            arrowPath.lineTo(x, y);
            arrowPath.lineTo(x - 10f, y + 10f);
            arrowPath.close();

            canvas.drawPath(arrowPath, paint); // 在指定位置绘制箭头
        }

        // 添加标记
        public void addMarker(float x, float y, String type) {
            markers.add(new Marker(x, y, type));
            invalidate(); // 重新绘制视图
        }

        // 设置星星的中心坐标
        public void setCenter(float x, float y) {
            this.centerX = x;
            this.centerY = y;
            invalidate(); // 重新绘制视图
        }

        // 用于存储标记信息的内部类
        private class Marker {
            float x, y;
            String type;

            Marker(float x, float y, String type) {
                this.x = x;
                this.y = y;
                this.type = type;
            }
        }
    }




    // 显示弹窗
    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(true);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // 3 秒后关闭弹窗并跳转
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            alertDialog.dismiss();
            startActivity(new Intent(this, VideoActivity.class));
            finish();
        }, 3000);
    }
}
