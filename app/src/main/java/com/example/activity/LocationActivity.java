package com.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.R;

public class LocationActivity extends AppCompatActivity {

    private EditText locationInput;
    private Button confirmButton;
    private String location;  // 定义location变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        locationInput = findViewById(R.id.location_input);
        confirmButton = findViewById(R.id.confirm_button);

        // 设置确认按钮的点击事件
        confirmButton.setOnClickListener(v -> {
            // 获取用户输入的 location 值
            location = locationInput.getText().toString().trim();

            if (!location.isEmpty()) {
                // 启动 LocationDetailActivity 并传递 location
                Intent intent = new Intent(LocationActivity.this, LocationDetailActivity.class);
                intent.putExtra("location", location);
                startActivity(intent);
            } else {
                locationInput.setError("请输入位置信息");
            }
        });
    }
}
