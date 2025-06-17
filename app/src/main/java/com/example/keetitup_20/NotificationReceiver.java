package com.example.keetitup_20;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notification_id", -1);
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        Log.d(TAG, "Received alarm for notification ID: " + notificationId);

        new NotificationService(context).showNotification(title, message);

    }
}