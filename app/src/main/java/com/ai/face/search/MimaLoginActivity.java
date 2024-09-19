package com.ai.face.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.R;
import com.ai.face.utils.VoicePlayer;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MimaLoginActivity extends AppCompatActivity {

    private EditText edstuname;
    private EditText edstunum;
    private Button mimalogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mima_login); // 确保布局文件正确

        edstuname = findViewById(R.id.edstuname);
        edstunum = findViewById(R.id.edstunum);
        mimalogin = findViewById(R.id.mimalogin);

        /*mimalogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edstuname.getText().toString();
                String studentNumber = edstunum.getText().toString();

                if (username.isEmpty() || studentNumber.isEmpty()) {
                    Toast.makeText(MimaLoginActivity.this, "请输入姓名和学号", Toast.LENGTH_SHORT).show();
                    return;
                }

                User user = new User(username, studentNumber);

                // 发送网络请求到后端
                ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
                Call<ResponseBody> call = apiService.login();
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try {
                                String jsonResponse = response.body().string();
                                JSONObject jsonObject = new JSONObject(jsonResponse);
                                boolean success = jsonObject.getBoolean("success");

                                if (success) {
                                    // 登录成功，跳转到新页面
                                    Intent intent = new Intent(MimaLoginActivity.this, ConfReserve.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // 登录失败，显示错误信息
                                    String message = jsonObject.getString("message");
                                    Toast.makeText(MimaLoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(MimaLoginActivity.this, "服务器响应解析失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MimaLoginActivity.this, "服务器响应失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(MimaLoginActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });*/


        mimalogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edstuname.getText().toString();
                String studentNumber = edstunum.getText().toString();

                if (username.isEmpty() || studentNumber.isEmpty()) {
                    Toast.makeText(MimaLoginActivity.this, "请输入姓名和学号", Toast.LENGTH_SHORT).show();
                    return;
                }

                OkHttpClient client = new OkHttpClient();

                HttpUrl url = HttpUrl.parse("http://222.200.184.74:8082/checkStudent")
                        .newBuilder()
                        .addQueryParameter("studentId", studentNumber)
                        .addQueryParameter("studentName", username)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(MimaLoginActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            boolean studentExists = Boolean.parseBoolean(response.body().string());
                            runOnUiThread(() -> {
                                if (studentExists) {
                                    // 登录成功，跳转到新页面
                                    Intent intent = new Intent(MimaLoginActivity.this, ConfReserve.class);
                                    String userInfo = username+studentNumber;
                                    intent.putExtra("userInfo",userInfo);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // 用户名或学号错误，显示错误信息
                                    showCustomDialog("用户名或学号错误", "请检查您的姓名和学号！");
                                }
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(MimaLoginActivity.this, "服务器响应失败", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }

            private void showCustomDialog(String title, String message) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MimaLoginActivity.this, R.style.CustomDialogStyle);
                builder.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                        .setNegativeButton("取消", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                // 获取消息视图并设置文本大小
                TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
                if (messageView != null) {
                    messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                }
            }
        });



        class User {
            String username;
            String studentNumber;

            public User(String username, String studentNumber) {
                this.username = username;
                this.studentNumber = studentNumber;
            }
        }
    }
}
