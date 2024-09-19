package com.example.activity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import android.os.Handler;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ResponseHandler {
    private Context context;
    private Handler handler = new Handler();

    public ResponseHandler(Context context) {
        this.context = context;
    }
    public void handleResponse(String responseData) {
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            String type = jsonResponse.getString("type");
            switch (type) {
                case "A":
                    // 处理A类型事件
                    handleATypeResponse(jsonResponse);
                    break;
                case "B":
                    // 打开会议室预定界面
                    Intent intent = new Intent(context, MeetingActivity.class);
                    context.startActivity(intent);
                    break;
                case "C":
                    // 处理C类事件（智能问答）
                    handleCTypeResponse(jsonResponse);
                    break;
                default:
                    Log.e("ResponseHandler", "未知类型：" + type);
                    break;
            }
        } catch (JSONException e) {
            Log.e("ResponseHandler", "JSON解析错误: " + e.getMessage());
        }
    }

    public void handleATypeResponse(JSONObject jsonResponse) {
        try {
            JSONObject responseObj = jsonResponse.getJSONObject("response"); // 获取response对象
            JSONArray events = responseObj.getJSONArray("events"); // 获取events数组
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                String eventType = event.getString("eventType");
                if ("navigation".equals(eventType)) {
                    JSONObject data = event.getJSONObject("data"); // 获取data对象
                    String pointId = data.getString("pointId"); // 获取pointId的值
                    sendMessageToServerB(pointId); // 将pointId的值传递给服务器B
                }
            }
        } catch (JSONException e) {
            Log.e("ResponseHandler", "处理A类事件错误: " + e.getMessage());
        }
    }

    private void sendMessageToServerB(String pointId) {
        final String ipAddress = "192.168.11.223"; // 服务器B的IP地址
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(ipAddress, 8080); // 创建Socket连接
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                    out.println(pointId); // 发送pointId

                    Log.d("ResponseHandler", "pointId已发送到服务器B: " + pointId);

                    out.close();
                    socket.close();
                } catch (Exception e) {
                    Log.e("ResponseHandler", "发送pointId失败: " + e.getMessage());
                }
            }
        }).start();
    }
    private void handleCTypeResponse(JSONObject jsonResponse) {
        try {
            JSONObject response = jsonResponse.getJSONObject("response");
            JSONArray events = response.getJSONArray("events");

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                String eventType = event.getString("eventType");
                if ("webpage".equals(eventType)) {
                    String url = event.getJSONObject("data").getString("path");
                    openWebpage(url);
                } else if ("speech".equals(eventType)) {
                    String content = event.getJSONObject("data").getString("content");
                    // 延迟播放语音，确保网页已打开
                    TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(context);
                    //调用文字转语音的方法
                    ttsUtil.speakText(content);
                }
            }
        } catch (JSONException e) {
            Log.e("ResponseHandler", "处理C类事件错误: " + e.getMessage());
        }
    }


    private void openWebpage(String url) {
        Intent intent = new Intent(context, FullScreenViewActivity.class);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }
}
