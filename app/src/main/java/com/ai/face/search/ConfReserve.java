package com.ai.face.search;

import static com.ai.face.network.RetrofitClient.retrofit;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ai.face.R;
import com.ai.face.bean.MeetingData;
import com.ai.face.network.ApiService;
import com.ai.face.utils.VoicePlayer;
import com.ai.face.network.RetrofitClient;
import com.example.activity.TextToSpeechUtil;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfReserve extends AppCompatActivity {

    private TextView matchedFaceTextView;
    private TextView etUsername;
    private Spinner spinnerDate, spinnerTimeSlot, spinnerMeetingRoom;
    private Button btnBook;
    private TableLayout tableLayout;
    private Handler handler = new Handler();

    // 保存预定状态的Map
    private Map<String, Boolean> bookingStatusMap;

    @SuppressLint({"SetTextI18n", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 初始化 SpeechUtility
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=b8585b05");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conf_reserve);

        matchedFaceTextView = findViewById(R.id.matched_face_text_view);
        etUsername = findViewById(R.id.matched_face_text_view); // 确保 ID 正确
        etUsername.setFocusable(false);
        etUsername.setFocusableInTouchMode(false);
        etUsername.setClickable(false);
        spinnerDate = findViewById(R.id.spinner_date);
        spinnerTimeSlot = findViewById(R.id.spinner_time_slot);
        spinnerMeetingRoom = findViewById(R.id.spinner_meeting_room);
        btnBook = findViewById(R.id.btn_book);
        tableLayout = findViewById(R.id.table_layout);

        // 打开界面时立即更新表格
        fetchAndUpdateTable();

        // 设置定时调用查询功能更新表格数据
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchAndUpdateTable(); // 调用新的方法来获取预定信息
                handler.postDelayed(this, 60000); // 每分钟更新一次
            }
        }, 60000);

        // 获取 Intent 中的 ID
        Intent intent = getIntent();
        if (intent != null) {
            String matchedFace = intent.getStringExtra("matchedFace"); // 获取匹配到的人脸信息
            if (matchedFace != null) {
                matchedFaceTextView.setText(matchedFace);
                etUsername.setText(matchedFace);
            } else {
                String userInfo = intent.getStringExtra("userInfo");
                if (userInfo != null) {
                    etUsername.setText(userInfo);
                }
            }
        }

        // 初始化下拉框数据
        initializeSpinners();
        // 初始化预定状态Map
        initializeBookingStatus();
        // 初始化表格
        initializeTable();

        spinnerDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMeetingRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTimeSlot.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String date = spinnerDate.getSelectedItem().toString();
                String timeSlot = spinnerTimeSlot.getSelectedItem().toString();
                String meetingRoom = spinnerMeetingRoom.getSelectedItem().toString();
                String username = etUsername.getText().toString();

                if (username.isEmpty()) {
                    showCustomDialog("预订信息有误", "请填写用户姓名");
                    return;
                } else {
                    // 弹出确认对话框
                    showConfirmationDialog(date, timeSlot, meetingRoom, username);
                }
            }

            private void showConfirmationDialog(String date, String timeSlot, String meetingRoom, String username) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConfReserve.this);
                builder.setTitle("确认预订");
                String message="你确定要预订 " + date + " 的 " + timeSlot + " 时间段，会议室 " + meetingRoom + " 吗？";
                builder.setMessage(message);

                // 点击确定按钮时执行预定操作
                builder.setPositiveButton("确定", (dialog, which) -> {
                    // 停止语音播放
                    TextToSpeechUtil.getInstance(ConfReserve.this).stopSpeaking();
                    MeetingData data = saveBookingData(date, timeSlot, meetingRoom, username);
                    ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
                    Call<ResponseBody> call = apiService.sendData(data);
                    call.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                try {
                                    String jsonResponse = response.body().string();
                                    JSONObject jsonObject = new JSONObject(jsonResponse);
                                    String responseStr = jsonObject.getString("responseStr");

                                    if (isExpired(date, timeSlot)) {
                                        showCustomDialog("预订信息有误", "该时段已过期，不可预订");
                                    } else {
                                        if ("会议预定成功!".equals(responseStr)) {
                                            showCustomDialog("预定成功", "预订成功！");
                                            bookMeetingRoom(date, timeSlot, meetingRoom);
                                            fetchAndUpdateTable(); // 提交成功后立即更新表格
                                        } else if ("和该会议室已有会议时间冲突，预定失败！".equals(responseStr)) {
                                            showCustomDialog("会议室已预定", "该会议室在此时段已被预定");
                                        } else if ("该会议室不存在".equals(responseStr)) {
                                            showCustomDialog("预订信息有误", "该会议室不存在");
                                        } else {
                                            showCustomDialog("会议预定失败", "网络不畅");
                                        }
                                    }
                                } catch (IOException | JSONException e) {
                                    Log.e("Network Response", "Error: " + e.getMessage());
                                    showCustomDialog("网络错误", "无法读取或解析服务器响应");
                                }
                            } else {
                                showCustomDialog("网络请求失败", "请检查网络连接");
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e("Network Error", "Request failed: " + t.getMessage());
                            showCustomDialog("网络请求失败", "请检查网络连接");
                        }
                    });
                });

                // 点击取消按钮时关闭对话框
                builder.setNegativeButton("取消", (dialog, which) -> {
                    // 停止语音播放
                    TextToSpeechUtil.getInstance(ConfReserve.this).stopSpeaking();
                    dialog.dismiss();
                });

                // 显示对话框
                AlertDialog dialog = builder.create();
                dialog.show();

                speakText(message);
            }

            private void showCustomDialog(String title, String message) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ConfReserve.this, R.style.CustomDialogStyle);
                builder.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                        .setNegativeButton("取消", null);

                int messageResId = getMessageResIdFromText();
                VoicePlayer voicePlayer = VoicePlayer.getInstance();
                voicePlayer.init(ConfReserve.this);

                AlertDialog dialog = builder.create();
                dialog.show();

                TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
                if (messageView != null) {
                    messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                }
            }

            private int getMessageResIdFromText() {
                return R.raw.smile;
            }
        });

        // 定时更新表格以检查状态
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchAndUpdateTable();
                handler.postDelayed(this, 60000); // 每分钟更新一次
            }
        }, 60000);
    }

    private void initializeSpinners() {
        String[] dates = new String[3];
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        dates[0] = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        dates[1] = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        dates[2] = sdf.format(calendar.getTime());

        ArrayAdapter<String> dateAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, dates) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                return view;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                return view;
            }
        };
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate.setAdapter(dateAdapter);

        String[] timeSlots = generateTimeSlots();
        ArrayAdapter<String> timeSlotAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, timeSlots) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                return view;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                return view;
            }
        };
        timeSlotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeSlot.setAdapter(timeSlotAdapter);

        String[] meetingRooms = {"A102", "A103", "A208", "B203", "B205"};
        ArrayAdapter<String> roomAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, meetingRooms) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                return view;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                return view;
            }
        };
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeetingRoom.setAdapter(roomAdapter);
    }

    private String[] generateTimeSlots() {
        String[] timeSlots = new String[12];
        int hour = 0;
        for (int i = 0; i < 12; i++) {
            String start = String.format(Locale.getDefault(), "%02d:00", hour);
            hour += 2;
            String end = String.format(Locale.getDefault(), "%02d:00", hour);
            timeSlots[i] = start + "-" + end;
        }
        return timeSlots;
    }

    private void initializeBookingStatus() {
        bookingStatusMap = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        String[] dates = new String[3];
        dates[0] = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        dates[1] = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        dates[2] = sdf.format(calendar.getTime());

        String[] timeSlots = generateTimeSlots();
        String[] meetingRooms = {"A102", "A103", "A208", "B203", "B205"};

        for (String date : dates) {
            for (String timeSlot : timeSlots) {
                if (isExpired(date, timeSlot)) {
                    continue;
                }
                for (String meetingRoom : meetingRooms) {
                    bookingStatusMap.put(date + "-" + timeSlot + "-" + meetingRoom, false);
                }
            }
        }
    }

    private void initializeTable() {
        tableLayout.removeAllViews();
        addTableHeader(); // 添加表头
        TableRow headerRow = new TableRow(this);
        tableLayout.addView(headerRow);
        updateTable();
    }
    private void addTableHeader() {
        // 创建表头行
        TableRow headerRow = new TableRow(this);

        // 添加空单元格（时间段列的占位符）
        headerRow.addView(createTextView("日期", true, 150));
        headerRow.addView(createTextView("时间段", true, 200));

        // 添加会议室名称作为表头
        String[] meetingRooms = {"A102", "A103", "A208", "B203", "B205"};
        for (String meetingRoom : meetingRooms) {
            headerRow.addView(createTextView(meetingRoom, true, 200));
        }

        // 将表头添加到表格中
        tableLayout.addView(headerRow);
    }

    private void updateTable() {
        tableLayout.removeViews(1, tableLayout.getChildCount() - 1); // 保留表头不删除

        String[] meetingRooms = {"A102", "A103", "A208", "B203", "B205"};
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        String[] dates = new String[3];
        dates[0] = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        dates[1] = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        dates[2] = sdf.format(calendar.getTime());

        String[] timeSlots = generateTimeSlots();
        String selectedDate = spinnerDate.getSelectedItem().toString();

        Arrays.sort(timeSlots, (t1, t2) -> {
            String startTime1 = t1.split("-")[0];
            String startTime2 = t2.split("-")[0];
            return startTime1.compareTo(startTime2);
        });

        for (String timeSlot : timeSlots) {
            if (isExpired(selectedDate, timeSlot)) {
                continue;
            }
            TableRow row = new TableRow(this);
            row.addView(createTextView(selectedDate, false, 150));
            row.addView(createTextView(timeSlot, false, 200));

            for (String meetingRoom : meetingRooms) {
                String key = selectedDate + "-" + timeSlot + "-" + meetingRoom;
                boolean isBooked = bookingStatusMap.getOrDefault(key, false);
                row.addView(createStatusTextView(selectedDate, timeSlot, meetingRoom, isBooked));
            }

            tableLayout.addView(row);
        }

        tableLayout.invalidate();
    }

    private TextView createStatusTextView(String date, String timeSlot, String meetingRoom, boolean isBooked) {
        TextView textView = createTextView(isBooked ? "已预定" : "未预定", false, 200);
        long currentTime = System.currentTimeMillis();

        String[] timeRange = timeSlot.split("-");
        String endTimeString = date + " " + timeRange[1];
        long slotEndTime = convertTimeToMillis(endTimeString);
        String selectedDate = spinnerDate.getSelectedItem().toString();
        String selectedMeetingRoom = spinnerMeetingRoom.getSelectedItem().toString();
        String selectedTime = spinnerTimeSlot.getSelectedItem().toString();

        if (slotEndTime < currentTime) {
            textView.setText("已过期");
            textView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        } else if (isBooked) {
            textView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            textView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        }

        if (date.equals(selectedDate) && timeSlot.equals(selectedTime) && meetingRoom.equals(selectedMeetingRoom)) {
            textView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }

        return textView;
    }

    private void bookMeetingRoom(String date, String timeSlot, String meetingRoom) {
        String key = date + "-" + timeSlot + "-" + meetingRoom;
        bookingStatusMap.put(key, true);
        updateTable(); // 确保表格在状态改变后立即更新
    }

    private boolean isExpired(String date, String timeSlot) {
        String[] timeRange = timeSlot.split("-");
        String endTimeString = date + " " + timeRange[1];
        long slotEndTime = convertTimeToMillis(endTimeString);
        return slotEndTime < System.currentTimeMillis();
    }

    private long convertTimeToMillis(String dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            return sdf.parse(dateTime).getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private TextView createTextView(String text, boolean isHeader, int width) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setLayoutParams(new TableRow.LayoutParams(width, TableRow.LayoutParams.MATCH_PARENT));
        textView.setSingleLine(false);
        textView.setEllipsize(null);
        textView.setMaxLines(1);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        if (isHeader) {
            textView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }
        return textView;
    }

    private MeetingData saveBookingData(String date, String timeSlot, String meetingRoom, String username) {
        MeetingData data = new MeetingData();
        data.name = meetingRoom;
        data.date = date;
        data.beginTime = timeSlot.split("-")[0] + ":00";
        data.endTime = timeSlot.split("-")[1] + ":00";
        data.mobile = "13000";
        data.mark = username;
        return data;
    }

    private void fetchAndUpdateTable() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.getReservations();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        updateBookingStatus(jsonResponse);
                        runOnUiThread(() -> updateTable());
                    } catch (IOException e) {
                        Log.e("Network Response", "Error reading network response: " + e.getMessage());
                    }
                } else {
                    Log.e("Network Error", "Request failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Network Error", "Request failed: " + t.getMessage());
            }
        });
    }

    private void updateBookingStatus(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            bookingStatusMap.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String user = jsonObject.getString("user");

                if ("robot".equals(user)) {
                    String date = jsonObject.getString("date");
                    String beginTime = jsonObject.getString("beginTime");
                    String endTime = jsonObject.getString("endTime");
                    String roomName = jsonObject.getString("name");

                    Log.d("BookingStatus", "Booking room " + roomName + " on " + date + " from " + beginTime + " to " + endTime);
                    markCellsAsBooked(date, beginTime, endTime, roomName);
                }
            }
        } catch (JSONException e) {
            Log.e("JSON Parsing", "Error parsing JSON: " + e.getMessage());
        }
    }

    private void markCellsAsBooked(String date, String beginTime, String endTime, String roomName) {
        String[] timeSlots = generateTimeSlots();
        for (String timeSlot : timeSlots) {
            String[] times = timeSlot.split("-");
            String start = times[0] + ":00";
            String end = times[1] + ":00";

            if (isTimeOverlap(start, end, beginTime, endTime)) {
                String key = date + "-" + timeSlot + "-" + roomName;
                bookingStatusMap.put(key, true);
            }
        }
    }

    private boolean isTimeOverlap(String start1, String end1, String start2, String end2) {
        return (start1.compareTo(end2) < 0 && start2.compareTo(end1) < 0);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 取消所有的Handler任务
        handler.removeCallbacksAndMessages(null);

        // 释放 TextToSpeechUtil 资源
        TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(this);
        ttsUtil.release();

    }
    public void speakText(String content) {
        TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(this);
        ttsUtil.speakText(content);
    }

}
