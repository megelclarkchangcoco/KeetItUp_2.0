package com.example.keetitup_20;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private static final SimpleDateFormat fullDateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public static void scheduleNotification(Context context, int notificationId, String notifyDate, String notifyTime, String title, String message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // Use notificationId as the request code to avoid overwriting
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        try {
            String fullDateTimeStr = notifyDate + " " + notifyTime;
            long triggerTime = fullDateTimeFormat.parse(fullDateTimeStr).getTime();
            if (triggerTime < System.currentTimeMillis()) {
                Log.d(TAG, "Scheduled time is in the past: " + fullDateTimeStr);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Scheduled notification for: " + fullDateTimeStr + ", ID: " + notificationId);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date/time: " + e.getMessage());
        }
    }
}