package com.example.keetitup_20;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.Map;

public class NotificationService {
    private final Context context;
    private final NotificationManager notificationManager;
    private int notificationIdCounter = 0;

    public static final String CHANNEL_ID = "keepitup_notifications";

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showNotification(String title, String message) {
        // Fetch user info from the database
        DatabaseConnection db = new DatabaseConnection(context);
        List<Map<String, String>> users = db.getAllUsers();
        int userId = -1;
        String fullName = null;
        String username = null;

        if (users != null && !users.isEmpty()) {
            Map<String, String> user = users.get(0); // Take the first user
            userId = Integer.parseInt(user.get("user_id"));
            // Fetch user details using the correct user ID
            Map<String, String> userDetails = db.getUserDetails(userId);
            if (userDetails != null) {
                fullName = userDetails.get("full_name");
                username = userDetails.get("username");
            }
        }

        Intent activityIntent = new Intent(context, NotificationActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activityIntent.putExtra("USER_ID", userId);
        activityIntent.putExtra("FULL_NAME", fullName);
        activityIntent.putExtra("USERNAME", username);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(activityPendingIntent);

        notificationManager.notify(notificationIdCounter++, builder.build());
    }
}