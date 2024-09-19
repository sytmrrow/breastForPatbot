package com.ai.face.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button button_F = findViewById(R.id.faceLogin);
        Button button_P = findViewById(R.id.PwLogin);
        button_F.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(LoginActivity.this, FaceSearch1NActivity.class);
                startActivity(intent);
            }
        });
        button_P.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(LoginActivity.this, MimaLoginActivity.class);
                startActivity(intent);
            }
        });
    }
}