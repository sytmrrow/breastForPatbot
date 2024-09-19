package com.ai.face;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.activity.ClientActivity;
import com.example.activity.LocationActivity;
import com.example.activity.WebViewActivity;
import com.example.activity.FloatingWindowService; // 确保导入悬浮窗服务类

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.canDrawOverlays(this)) {
                // 请求悬浮窗权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            } else {
                // 如果权限已授予，则启动悬浮窗服务
                startFloatingWindowService();
            }
        } else {
            // 如果是低于 Android O 的版本，直接启动悬浮窗服务
            startFloatingWindowService();
        }

        // 找到按钮并设置点击事件
        Button videoButton = findViewById(R.id.videoButton);
        Button webButton = findViewById(R.id.webButton);
        Button meetingButton = findViewById(R.id.meetingButton);
        Button clientButton = findViewById(R.id.clientButton);
        Button buttonOpenTtsDemo = findViewById(R.id.button_open_tts_demo);
        Button locationButton = findViewById(R.id.location_Button);

        buttonOpenTtsDemo.setOnClickListener(v -> {
            //创建 Intent 跳转到 TTSChinese 模块的 DemoActivity
            //Intent intent = new Intent(MainActivity.this, com.air4.ttschineseDemo.DemoActivity.class);
            //startActivity(intent);
        });

        videoButton.setOnClickListener(v -> {
            // 启动 VideoActivity
            Intent intent = new Intent(MainActivity.this, com.example.activity.VideoActivity.class);
            startActivity(intent);
        });

        webButton.setOnClickListener(v -> {
            // 启动 WebActivity
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            startActivity(intent);
        });

        meetingButton.setOnClickListener(v -> {
            // 启动 MeetingActivity
            Intent intent = new Intent(MainActivity.this, com.example.activity.MeetingActivity.class);
            startActivity(intent);
        });

        clientButton.setOnClickListener(v -> {
            // 启动 ClientActivity
            Intent intent = new Intent(MainActivity.this, ClientActivity.class);
            startActivity(intent);
        });

        locationButton.setOnClickListener(v -> {
            // 启动 LocationActivity
            Intent intent = new Intent(MainActivity.this, LocationActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!Settings.canDrawOverlays(this)) {
                    // 提示用户需要允许悬浮窗权限
                    Toast.makeText(this, "请允许悬浮窗权限", Toast.LENGTH_SHORT).show();
                } else {
                    // 如果用户授予了权限，则启动悬浮窗服务
                    startFloatingWindowService();
                }
            }
        }
    }

    private void startFloatingWindowService() {
        // 启动悬浮窗服务
        Intent intent = new Intent(MainActivity.this, FloatingWindowService.class);
        startService(intent);
    }
}
