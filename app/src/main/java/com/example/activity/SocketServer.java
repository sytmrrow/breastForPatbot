package com.example.activity;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;

public class SocketServer {
    private Context context;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private int port;
    private Handler uiHandler; // 用于与主线程通信
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public SocketServer(Context context, int port, Handler handler) {
        this.context = context;
        this.port = port;
        this.uiHandler = handler;
    }

    // 启动服务器
    public void startServer() {
        isRunning = true;
        new Thread(new ServerThread()).start();
    }

    // 停止服务器
    public void stopServer() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close(); // 关闭服务器套接字，释放端口
            } catch (IOException e) {
                Log.e("SocketServer", "关闭服务器套接字时出错: " + e.getMessage());
            }
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }

    // 服务器线程，监听客户端连接
    private class ServerThread implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port); // 创建服务器套接字，绑定指定端口
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept(); // 等待客户端连接（阻塞调用）
                    threadPool.execute(new ClientHandler(clientSocket)); // 使用线程池处理客户端连接
                }
            } catch (IOException e) {
                Log.e("SocketServer", "服务器线程运行出错: " + e.getMessage());
            }
        }
    }

    // 客户端处理线程，处理客户端发送的数据
    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                String receivedData = reader.readLine();

                if (receivedData != null) {
                    // 不再更新 TextView，仅处理接收到的数据

                    // 解析 JSON 数据
                    try {
                        JSONObject jsonResponse = new JSONObject(receivedData);
                        String type = jsonResponse.getString("type");
                        Log.d("SocketServer", "接收到的数据类型: " + type); // 打印接收到的 type 值
                        switch (type) {
                            case "A":
                                // 处理 A 类型事件，需要在主线程中执行
                                uiHandler.post(() -> {
                                    try {
                                        // 从 JSON 字符串中创建 JSONObject
                                        JSONArray events = jsonResponse.getJSONObject("response").getJSONArray("events");
                                        String pointId = null;

                                        for (int i = 0; i < events.length(); i++) {
                                            JSONObject event = events.getJSONObject(i);
                                            if (event.getString("eventType").equals("navigation")) {
                                                pointId = event.getJSONObject("data").getString("pointId");
                                                break;
                                            }
                                        }

                                        if (pointId != null) {
                                            // 暂停视频
                                            if (context instanceof VideoActivity) {
                                                ((VideoActivity) context).pauseVideo();
                                            }

                                            // 启动 LocationDetailActivity 并传递 pointId
                                            Intent intent = new Intent(context, LocationDetailActivity.class);
                                            intent.putExtra("location", pointId);
                                            context.startActivity(intent);
                                        }

                                    } catch (JSONException e) {
                                        Log.e("SocketServer", "解析 JSON 数据时出错：" + e.getMessage());
                                    }
                                });
                                break;
                            case "B":
                                // 打开会议室预定界面，需要在主线程中执行
                                uiHandler.post(() -> {
                                    if (context instanceof VideoActivity) {
                                        ((VideoActivity) context).startMeetingActivity();
                                    }
                                });
                                break;
                            case "C":
                                // 处理 C 类事件（智能问答）
                                handleCTypeResponse(jsonResponse);
                                break;
                            default:
                                Log.e("SocketServer", "未知类型：" + type);
                                break;
                            // 添加 type 为 "E" 的处理逻辑
                            case "E":
                                uiHandler.post(() -> {
                                    Log.d("SocketServer", "处理 type 为 E 的数据，准备更新悬浮窗状态");
                                    // 创建一个 Intent，指向 FloatingWindowService
                                    Intent intent = new Intent(context, FloatingWindowService.class);
                                    // 启动服务，调用 updateActivationStatus 方法更新文字
                                    context.startService(intent);
                                    // 通过绑定服务的方式调用更新方法
                                    if (context instanceof VideoActivity) {
                                        ((VideoActivity) context).updateFloatingWindowStatus("已激活");
                                        Log.d("SocketServer", "已调用 VideoActivity 的 updateFloatingWindowStatus 方法");
                                    }else {
                                        Log.e("SocketServer", "当前 context 不是 VideoActivity，无法更新悬浮窗状态");
                                    }
                                });
                                break;

                        }

                    } catch (JSONException e) {
                        Log.e("SocketServer", "解析 JSON 数据出错: " + e.getMessage());
                        // 如果解析 JSON 出错，可以选择处理为未知数据
                        handleInvalidData(receivedData);
                    }
                }

                clientSocket.close();
            } catch (IOException e) {
                Log.e("SocketServer", "客户端处理线程出错: " + e.getMessage());
            }
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
                        uiHandler.post(() -> {
                            if (context instanceof VideoActivity) {
                                ((VideoActivity) context).openWebpage(url);
                            }
                        });
                    } else if ("speech".equals(eventType)) {
                        String content = event.getJSONObject("data").getString("content");
                        if (content != null) {
                            // 尝试在主线程中暂停视频，避免崩溃
                            uiHandler.post(() -> {
                                if (context instanceof VideoActivity) {
                                    try {
                                        ((VideoActivity) context).pauseVideo(); // 确保在主线程中调用
                                    } catch (Exception e) {
                                        Log.e("SocketServer", "暂停视频时出错: " + e.getMessage());
                                    }
                                }
                            });
                        }

                        // 使用 uiHandler 切换到主线程来进行文本语音播放
                        uiHandler.post(() -> {
                            if (context instanceof VideoActivity) {
                                ((VideoActivity) context).speakText(content);
                            }
                        });
                    }
                }
            } catch (JSONException e) {
                Log.e("SocketServer", "处理 C 类事件错误: " + e.getMessage());
            }
        }

        // 处理无法解析的非 JSON 数据
        private void handleInvalidData(String data) {
            // 可以在此处理无法解析的数据，例如记录日志或提示
            Log.e("SocketServer", "无法解析的数据: " + data);
        }
    }
}
