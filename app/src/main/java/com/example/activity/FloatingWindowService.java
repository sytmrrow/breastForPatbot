// FloatingWindowService.java
package com.example.activity;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.ai.face.R;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingViewActivation;
    private View floatingViewQuestion;
    private TextView activationStatusTextView;
    private TextView userQuestionTextView;

    // 添加 Binder 对象，用于绑定服务
    private final IBinder binder = new LocalBinder();

    // LocalBinder 用于返回当前服务的实例
    public class LocalBinder extends Binder {
        public FloatingWindowService getService() {
            return FloatingWindowService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("FloatingWindowService", "服务已绑定");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FloatingWindowService", "FloatingWindowService 已启动");

        // 加载第一个悬浮窗的布局 (用于显示是否激活)
        floatingViewActivation = LayoutInflater.from(this).inflate(R.layout.layout_floating_window_activation, null);

        // 修正 ID 名称，确保与布局文件中的 ID 一致
        activationStatusTextView = floatingViewActivation.findViewById(R.id.activation_status);

        // 检查是否成功获取到 TextView
        if (activationStatusTextView != null) {
            Log.d("FloatingWindowService", "成功获取 activationStatusTextView");
        } else {
            Log.e("FloatingWindowService", "获取 activationStatusTextView 失败，视图可能未正确加载");
        }

        // 加载第二个悬浮窗的布局 (用于显示用户提问)
        floatingViewQuestion = LayoutInflater.from(this).inflate(R.layout.layout_floating_window_question, null);
        userQuestionTextView = floatingViewQuestion.findViewById(R.id.userQuestionTextView);

        // 设置第一个悬浮窗的参数 (显示激活状态)
        final WindowManager.LayoutParams paramsActivation = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        paramsActivation.gravity = Gravity.TOP | Gravity.START; // 设置悬浮窗的初始位置
        paramsActivation.x = 0;
        paramsActivation.y = 100;

        // 获取 WindowManager 服务并添加第一个悬浮窗
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingViewActivation, paramsActivation);

        // 设置第二个悬浮窗的参数并添加
        final WindowManager.LayoutParams paramsQuestion = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        paramsQuestion.gravity = Gravity.TOP | Gravity.START;
        paramsQuestion.x = 300;
        paramsQuestion.y = 100;

        windowManager.addView(floatingViewQuestion, paramsQuestion);

        // 悬浮窗拖动逻辑
        setTouchListener(floatingViewActivation, paramsActivation);
        setTouchListener(floatingViewQuestion, paramsQuestion);
    }


    private void setTouchListener(View floatingView, WindowManager.LayoutParams params) {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // 单击事件处理
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // 实现实时更新悬浮窗内容的方法
    public void updateActivationStatus(String status) {
        Log.d("FloatingWindowService", "调用 updateActivationStatus 方法，状态: " + status);
        if (activationStatusTextView != null) {
            activationStatusTextView.setText(status);
            Log.d("FloatingWindowService", "更新悬浮窗状态为：" + status);
        } else {
            Log.e("FloatingWindowService", "activationStatusTextView 为 null，无法更新状态");
        }
    }

    public void updateUserQuestion(String question) {
        if (userQuestionTextView != null) {
            userQuestionTextView.setText(question);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingViewActivation != null) windowManager.removeView(floatingViewActivation);
        if (floatingViewQuestion != null) windowManager.removeView(floatingViewQuestion);
    }
}
