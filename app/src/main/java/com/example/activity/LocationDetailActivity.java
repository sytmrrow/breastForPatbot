package com.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.ai.face.R;
public class LocationDetailActivity extends AppCompatActivity {

    private TextView locationDetailTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        // 获取传递过来的location数据
        String location = getIntent().getStringExtra("location");

        // 显示location数据
        locationDetailTextView = findViewById(R.id.location_detail_textview);
        locationDetailTextView.setText("加载中...");

        // 2秒后自动跳转到FloorPlanActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(LocationDetailActivity.this, FloorPlanActivity.class);
            intent.putExtra("location", location);
            startActivity(intent);
            finish();  // 结束当前Activity
        }, 1000);

//        // 按钮点击后跳转到NavigationActivity
//        findViewById(R.id.navigate_button).setOnClickListener(v -> {
//            Intent intent = new Intent(LocationDetailActivity.this, NavigationActivity.class);
//            intent.putExtra("location", location);
//            startActivity(intent);
//        });
    }
}
