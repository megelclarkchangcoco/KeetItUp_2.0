package com.example.keetitup_20;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;
    private DatabaseConnection db;
    private List<Map<String, String>> notificationList;

    private Handler timeHandler = new Handler();
    private Runnable timeCheckerRunnable;
    private SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private long lastPopupTimestamp = 0L;

    private String fullName;
    private String username;
    private int userId = -1;

    private static final int NOTIFICATION_PERMISSION_CODE = 1;
    private List<PendingNotification> pendingNotifications = new ArrayList<>();
    private NotificationService notificationService;

    private static class PendingNotification {
        String title;
        String message;

        PendingNotification(String title, String message) {
            this.title = title;
            this.message = message;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        Log.d("NotifDebug", "NotificationActivity onCreate called");

        // Initialize NotificationService
        notificationService = new NotificationService(this);
        Log.d("NotifDebug", "NotificationService initialized");

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("NotifCheck", "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                Log.d("NotifCheck", "POST_NOTIFICATIONS permission already granted");
            }
        } else {
            Log.d("NotifCheck", "POST_NOTIFICATIONS permission not required (pre-Android 13)");
        }

        // Navigation
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navAdd = findViewById(R.id.nav_add);
        LinearLayout navTasks = findViewById(R.id.nav_tasks);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        db = new DatabaseConnection(this);
        db.logAllNotifications();
        Log.d("NotifDebug", "DatabaseConnection initialized");

        Intent intent = getIntent();
        fullName = intent.getStringExtra("FULL_NAME");
        username = intent.getStringExtra("USERNAME");
        userId = intent.getIntExtra("USER_ID", -1);

        if (userId == -1) {
            Log.e("NotificationActivity", "Invalid user ID, redirecting to HomeActivity");
            Intent homeIntent = new Intent(NotificationActivity.this, HomeActivity.class);
            startActivity(homeIntent);
            finish();
            return;
        }
        Log.d("NotifDebug", "User ID: " + userId + ", Full Name: " + fullName + ", Username: " + username);

        recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        notificationList = db.getNotificationsForUser(userId);
        Log.d("NotifDebug", "Loaded " + notificationList.size() + " notifications from DB");

        // Add a test notification for the CURRENT TIME (no delay)
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar now = Calendar.getInstance();
        String testDate = dateFormat.format(now.getTime());
        String testTime = timeFormat.format(now.getTime());

        SQLiteDatabase dbWritable = db.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("Task_ID", 0);
        cv.put("Notify_Date", testDate);
        cv.put("Notify_Time", testTime);
        long notifId = dbWritable.insert("Task_Notifications", null, cv);
        dbWritable.close();

        if (notifId != -1) {
            Map<String, String> testNotif = new HashMap<>();
            testNotif.put("notification_id", String.valueOf(notifId));
            testNotif.put("task_id", "0");
            testNotif.put("notify_date", testDate);
            testNotif.put("notify_time", testTime);
            testNotif.put("task_name", "Test Notification");
            testNotif.put("category", "System");
            testNotif.put("frequency", "One-time");
            testNotif.put("last_completed_date", "");
            testNotif.put("description", "This is a test notification");
            testNotif.put("status", "Ongoing");
            notificationList.add(testNotif);
            Log.d("NotifCheck", "Added test notification for: " + testDate + " " + testTime);
        }

        notificationAdapter = new NotificationAdapter(this, notificationList, fullName, username, userId);
        recyclerView.setAdapter(notificationAdapter);
        Log.d("NotifDebug", "RecyclerView adapter set");

        // nanvigate to Home, Notification, Add Task, Task List, Profile activity
        navHome.setOnClickListener(v -> navigateToActivity(HomeActivity.class));

        navNotifications.setOnClickListener(v -> navigateToActivity(NotificationActivity.class));

        navAdd.setOnClickListener(v -> navigateToActivity(AddTaskActivity.class) );

        navTasks.setOnClickListener(v -> navigateToActivity(TaskListActivity.class));

        navProfile.setOnClickListener(v -> navigateToActivity(ProfileActivity.class));

        updateNavigationState();
        startNotificationTimeChecker();
        Log.d("NotifDebug", "Notification time checker started");
    }

    private void updateNavigationState() {
        ImageView iconNotifications = findViewById(R.id.icon_notifications);
        TextView textNotifications = findViewById(R.id.text_notifications);
        if (iconNotifications != null && textNotifications != null) {
            iconNotifications.setColorFilter(getResources().getColor(R.color.black, null));
            textNotifications.setTextColor(getResources().getColor(R.color.black, null));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NotifDebug", "NotificationActivity onResume");
        if (db != null && recyclerView != null && userId != -1) {
            notificationList = db.getNotificationsForUser(userId);
            notificationAdapter.updateNotifications(notificationList);
            Log.d("NotifDebug", "Updated notification list in onResume: " + notificationList.size());
        }
        if (!pendingNotifications.isEmpty() && hasNotificationPermission()) {
            Log.d("NotifCheck", "Posting " + pendingNotifications.size() + " pending notifications after resume");
            for (PendingNotification pn : new ArrayList<>(pendingNotifications)) {
                notificationService.showNotification(pn.title, pn.message);
                pendingNotifications.remove(pn);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacks(timeCheckerRunnable);
        Log.d("NotifDebug", "NotificationActivity onDestroy, stopping time checker");
    }

    private void startNotificationTimeChecker() {
        timeCheckerRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("NotifCheck", "Checking notifications at " + fullDateTimeFormat.format(new Date()));
                checkAndShowNotificationPopup();
                timeHandler.postDelayed(this, 1000); // Check every 1 second instead of 60 seconds
            }
        };
        timeHandler.post(timeCheckerRunnable);
    }

    private void checkAndShowNotificationPopup() {
        if (notificationList == null || notificationList.isEmpty()) {
            Log.d("NotifCheck", "No notifications in list.");
            return;
        }

        Date now = new Date();
        Log.d("NotifCheck", "Current device time: " + fullDateTimeFormat.format(now));

        for (Map<String, String> notification : notificationList) {
            String notifyDate = notification.get("notify_date");
            String notifyTime = notification.get("notify_time");
            if (notifyDate == null || notifyTime == null) {
                Log.d("NotifCheck", "Skipping notification with null date/time: " + notification.toString());
                continue;
            }

            String fullDateTimeStr = notifyDate + " " + notifyTime;
            Log.d("NotifCheck", "Checking notification: " + fullDateTimeStr);

            try {
                Date notifyDateTime = fullDateTimeFormat.parse(fullDateTimeStr);
                if (notifyDateTime == null) continue;

                long diffMillis = now.getTime() - notifyDateTime.getTime();
                Log.d("NotifCheck", "Time difference (ms): " + diffMillis);

                if (diffMillis >= 0 && diffMillis <= 60000) { // Keep the 60-second window for flexibility
                    if (notifyDateTime.getTime() != lastPopupTimestamp) {
                        lastPopupTimestamp = notifyDateTime.getTime();

                        String title = notification.getOrDefault("task_name", "Notification");
                        String message = "Reminder: " + title;

                        showNotificationPopup(title);
                        showSystemNotification(title, message);

                        Log.d("NotifCheck", "Notification triggered for: " + fullDateTimeStr);
                        break;
                    }
                } else {
                    Log.d("NotifCheck", "Notification not in window: " + fullDateTimeStr + ", diff: " + diffMillis);
                }
            } catch (ParseException e) {
                Log.e("NotifCheck", "Failed to parse date/time: " + e.getMessage());
            }
        }
    }

    public void showNotificationPopup(String title) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_notification);

        TextView titleText = dialog.findViewById(R.id.tvPopupTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvPopupMessage);
        Button btnClose = dialog.findViewById(R.id.btnPopupClose);

        titleText.setText(title);
        tvMessage.setText("Reminder: " + title);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        Log.d("NotifDebug", "Showing popup for: " + title);
    }

    private void showSystemNotification(String title, String message) {
        if (hasNotificationPermission()) {
            notificationService.showNotification(title, message);
            Log.d("NotifDebug", "Posting system notification: " + title);
        } else {
            pendingNotifications.add(new PendingNotification(title, message));
            Log.d("NotifCheck", "Notification permission not granted, queuing notification: " + title);
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            Log.d("PermissionCheck", "Notification permission status: " + hasPermission);
            return hasPermission;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NotifCheck", "POST_NOTIFICATIONS permission granted");
                if (!pendingNotifications.isEmpty()) {
                    Log.d("NotifCheck", "Posting " + pendingNotifications.size() + " pending notifications");
                    for (PendingNotification pn : new ArrayList<>(pendingNotifications)) {
                        notificationService.showNotification(pn.title, pn.message);
                        pendingNotifications.remove(pn);
                    }
                }
            } else {
                Log.d("NotifCheck", "POST_NOTIFICATIONS permission denied");
            }
        }
    }

    private void setClickHighlight(View view) {
        view.setBackgroundResource(R.color.custom_yellow);
        view.postDelayed(() -> view.setBackgroundResource(android.R.color.transparent), 300);
    }

    private void navigateToActivity(Class<?> activityClass){
        Intent intent = new Intent(NotificationActivity.this, activityClass);
        if(fullName != null) intent.putExtra("FULL_NAME", fullName);
        if(username != null) intent.putExtra("USERNAME", username);
        if(userId != -1) intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }
}