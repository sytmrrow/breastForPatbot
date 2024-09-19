package com.example.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.R;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {
    private EditText ipAddressInput;      // 输入IP地址
    private EditText messageInput;        // 输入消息
    private Button connectButton;         // 发送按钮
    private TextView clientStatus;        // 显示状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        ipAddressInput = findViewById(R.id.ip_address_input);
        messageInput = findViewById(R.id.message_input);    // 消息输入框
        connectButton = findViewById(R.id.connect_button);
        clientStatus = findViewById(R.id.client_status);

        connectButton.setOnClickListener(v -> {
            final String ipAddress = ipAddressInput.getText().toString().trim();
            final String message = messageInput.getText().toString().trim();

            if (ipAddress.isEmpty()) {
                Toast.makeText(ClientActivity.this, "请输入IP地址", Toast.LENGTH_SHORT).show();
                return;
            }
            if (message.isEmpty()) {
                Toast.makeText(ClientActivity.this, "请输入要发送的消息", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    // 创建与服务器的Socket连接
                    Socket socket = new Socket(ipAddress, 5000);
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                    // 发送消息
                    out.println(message);

                    // 在主线程更新UI
                    runOnUiThread(() -> clientStatus.setText("消息已发送到服务器"));

                    out.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    // 处理连接错误，在主线程显示错误信息
                    runOnUiThread(() -> clientStatus.setText("连接失败: " + e.getMessage()));
                }
            }).start();
        });
    }
}
