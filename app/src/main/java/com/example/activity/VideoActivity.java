package com.example.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.R;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class VideoActivity extends AppCompatActivity {
    private SimpleExoPlayer player;
    private Button playPauseButton;
    private boolean isPlaying = true;
    private static final String TAG = "VideoActivity";
    private SocketServer socketServer;
    private Handler uiHandler;
    private TextView receivedDataTextView;
    private boolean isWebpageOpen = false;  // 用于判断网页是否已经打开
    private String pendingSpeechContent = null;  // 用于保存待播放的 speech 内容
    private FloatingWindowService floatingWindowService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // 初始化 SpeechUtility
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=b8585b05");

        setupPlayer();
        // 确保 FloatingWindowService 已经启动
        Intent intent = new Intent(this, FloatingWindowService.class);
        startService(intent);

        // 初始化 Handler，用于在 SocketServer 中与 UI 线程通信
        uiHandler = new Handler(Looper.getMainLooper());

        // 创建并启动 SocketServer
        socketServer = new SocketServer(this, 5000, uiHandler);
        socketServer.startServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止 SocketServer
        if (socketServer != null) {
            socketServer.stopServer();
        }

        // 释放播放器资源
        if (player != null) {
            player.release();
            player = null;
        }

        // 释放 TextToSpeechUtil 资源
        TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(this);
        ttsUtil.release();
    }

    private void setupPlayer() {
        PlayerView playerView = findViewById(R.id.playerView);
        playPauseButton = findViewById(R.id.playPauseButton);

        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // 设置视频 URL
        String videoUrl = "http://222.200.184.74:8082/video/Sample.mp4";
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);

        // 设置视频并准备播放
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play(); // 自动开始播放

        // 设置为循环播放单个视频
        player.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_ONE);

        playPauseButton.setOnClickListener(v -> togglePlayback());
    }

    private void togglePlayback() {
        if (isPlaying) {
            player.pause();
            playPauseButton.setText("播放");
        } else {
            player.play();
            playPauseButton.setText("暂停");
        }
        isPlaying = !isPlaying;
    }

    // 提供给 SocketServer 调用的方法，用于暂停视频
    public void pauseVideo() {
        if (player != null && player.isPlaying()) {
            player.pause();
            runOnUiThread(() -> playPauseButton.setText("播放"));
            isPlaying = false;
            Toast.makeText(this, "视频已暂停", Toast.LENGTH_SHORT).show();
        }
    }

    // 提供给 SocketServer 调用的方法，用于启动其他活动
    public void startMeetingActivity() {
        Intent intent = new Intent(this, MeetingActivity.class);
        startActivity(intent);
    }

    // 提供给 SocketServer 调用的方法，用于打开网页
    public void openWebpage(String url) {
        Intent intent = new Intent(this, FullScreenViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    // 提供给 SocketServer 调用的方法，用于播放文本语音
    public void speakText(String content) {
        TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(this);
        ttsUtil.speakText(content);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 获取服务实例
            floatingWindowService = ((FloatingWindowService.LocalBinder) service).getService();
            Log.d("VideoActivity", "成功绑定 FloatingWindowService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatingWindowService = null;
            Log.e("VideoActivity", "FloatingWindowService 断开连接");
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, FloatingWindowService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("VideoActivity", "尝试绑定 FloatingWindowService");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        Log.d("VideoActivity", "解除绑定 FloatingWindowService");
    }

    // 调用更新方法
    public void updateFloatingWindowStatus(String status) {
        if (floatingWindowService != null) {
            floatingWindowService.updateActivationStatus(status);
            Log.d("VideoActivity", "调用了 FloatingWindowService 的 updateActivationStatus 方法，状态：" + status);
        } else {
            Log.e("VideoActivity", "FloatingWindowService 为 null，无法调用 updateActivationStatus");
        }
    }
    // 新增方法，用于更新第三个悬浮窗的内容
    public void updateServerResultWindow(String content) {
        if (floatingWindowService != null) {
            floatingWindowService.updateServerResult(content);
            Log.d("VideoActivity", "调用了 FloatingWindowService 的 updateServerResult 方法，内容：" + content);
        } else {
            Log.e("VideoActivity", "FloatingWindowService 为 null，无法调用 updateServerResult");
        }
    }


    // VideoActivity.java
    public void updateFloatingWindowQuestion(String question) {
        if (floatingWindowService != null) {
            floatingWindowService.updateUserQuestion(question);
            Log.d("VideoActivity", "调用了 FloatingWindowService 的 updateUserQuestion 方法，内容：" + question);
        } else {
            Log.e("VideoActivity", "FloatingWindowService 为 null，无法调用 updateUserQuestion");
        }
    }
    // 新增方法，用于更新第三个悬浮窗的内容
    // 在 VideoActivity.java 中新增此方法
    public void updateResultWindow(String content) {
        if (floatingWindowService != null) {
            floatingWindowService.updateServerResult(content);
            Log.d("VideoActivity", "调用了 FloatingWindowService 的 updateServerResult 方法，内容：" + content);
        } else {
            Log.e("VideoActivity", "FloatingWindowService 为 null，无法调用 updateServerResult");
        }
    }

    // 新增方法，用于更新 TextView 的内容
    public void updateReceivedData(String data) {
        runOnUiThread(() -> {
            receivedDataTextView.setText(data);
        });
    }
}
