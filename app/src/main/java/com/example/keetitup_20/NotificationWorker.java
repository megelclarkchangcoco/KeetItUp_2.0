package com.example.keetitup_20;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotifWorker";
    private DatabaseConnection db;
    private NotificationService notificationService;
    private SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static long lastNotificationTimestamp = 0L;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = new DatabaseConnection(context);
        notificationService = new NotificationService(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "NotificationWorker started at " + fullDateTimeFormat.format(new Date()));

        // Fetch all users to check notifications for each
        List<Map<String, String>> users = db.getAllUsers();
        if (users == null || users.isEmpty()) {
            Log.d(TAG, "No users found in database.");
            return Result.success();
        }

        for (Map<String, String> user : users) {
            int userId = Integer.parseInt(user.get("user_id"));
            List<Map<String, String>> notificationList = db.getNotificationsForUser(userId);
            checkAndTriggerNotifications(notificationList);
        }

        return Result.success();
    }

    private void checkAndTriggerNotifications(List<Map<String, String>> notificationList) {
        if (notificationList == null || notificationList.isEmpty()) {
            Log.d(TAG, "No notifications in list.");
            return;
        }

        Date now = new Date();
        Log.d(TAG, "Current device time: " + fullDateTimeFormat.format(now));

        for (Map<String, String> notification : notificationList) {
            String notifyDate = notification.get("notify_date");
            String notifyTime = notification.get("notify_time");
            if (notifyDate == null || notifyTime == null) {
                Log.d(TAG, "Skipping notification with null date/time: " + notification.toString());
                continue;
            }

            String fullDateTimeStr = notifyDate + " " + notifyTime;
            Log.d(TAG, "Checking notification: " + fullDateTimeStr);

            try {
                Date notifyDateTime = fullDateTimeFormat.parse(fullDateTimeStr);
                if (notifyDateTime == null) continue;

                long diffMillis = now.getTime() - notifyDateTime.getTime();
                Log.d(TAG, "Time difference (ms): " + diffMillis);

                if (diffMillis >= 0 && diffMillis <= 60000) {
                    if (notifyDateTime.getTime() != lastNotificationTimestamp) {
                        lastNotificationTimestamp = notifyDateTime.getTime();

                        String title = notification.getOrDefault("task_name", "Notification");
                        String message = "Reminder: " + title;

                        notificationService.showNotification(title, message);
                        Log.d(TAG, "Notification triggered for: " + fullDateTimeStr);
                        break;
                    }
                } else {
                    Log.d(TAG, "Notification not in window: " + fullDateTimeStr + ", diff: " + diffMillis);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Failed to parse date/time: " + e.getMessage());
            }
        }
    }
}