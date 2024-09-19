// FloatingWindowService.java
package com.example.activity;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 加载第一个悬浮窗的布局 (用于显示是否激活)
        floatingViewActivation = LayoutInflater.from(this).inflate(R.layout.layout_floating_window_activation, null);
        // 加载第二个悬浮窗的布局 (用于显示用户提问)
        floatingViewQuestion = LayoutInflater.from(this).inflate(R.layout.layout_floating_window_question, null);

        // 获取 TextView 引用
        activationStatusTextView = floatingViewActivation.findViewById(R.id.activationStatusTextView);
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

        // 设置第二个悬浮窗的参数 (显示用户提问)
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

        // 获取 WindowManager 服务
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingViewActivation, paramsActivation);  // 添加第一个悬浮窗
        windowManager.addView(floatingViewQuestion, paramsQuestion);  // 添加第二个悬浮窗

        // 悬浮窗拖动逻辑（适用于两个悬浮窗）
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
        if (activationStatusTextView != null) {
            activationStatusTextView.setText(status);
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
