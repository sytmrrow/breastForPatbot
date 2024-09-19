package com.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    // 绑定服务并更新悬浮窗的状态
    public void updateFloatingWindowStatus(String status) {
        if (floatingWindowService != null) {
            floatingWindowService.updateActivationStatus(status);
        }
    }

    // 新增方法，用于更新 TextView 的内容
    public void updateReceivedData(String data) {
        runOnUiThread(() -> {
            receivedDataTextView.setText(data);
        });
    }
}
