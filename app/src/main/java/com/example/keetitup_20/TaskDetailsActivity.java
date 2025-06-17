package com.example.keetitup_20;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskDetailsActivity extends AppCompatActivity {
    private static final String TAG = "TaskDetailsActivity";

    TextView taskName,
            taskDescription,
            category,
            taskRepeats,
            notificationType,
            dateLastCompleted;

    LinearLayout navHome, navNotifications, navAdd, navTasks, navProfile;
    ImageView back, deleteButton, editButton;
    RecyclerView timelineRecyclerView;
    TimelineAdapter timelineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // ─── FindViewById on all views ───
        navHome = findViewById(R.id.nav_home);
        navNotifications = findViewById(R.id.nav_notifications);
        navAdd = findViewById(R.id.nav_add);
        navTasks = findViewById(R.id.nav_tasks);
        navProfile = findViewById(R.id.nav_profile);

        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        category = findViewById(R.id.category);
        taskRepeats = findViewById(R.id.taskRepeats);
        notificationType = findViewById(R.id.notificationDateandTime);
        dateLastCompleted = findViewById(R.id.dateLastCompleted);

        back = findViewById(R.id.back_icon);
        deleteButton = findViewById(R.id.deleteButton);
        editButton = findViewById(R.id.editButton);

        timelineRecyclerView = findViewById(R.id.timelineRecyclerView);

        // ─── Retrieve extras from Intent ───
        Intent intent = getIntent();
        String name = intent.getStringExtra("task_name");
        String desc = intent.getStringExtra("description");
        String cat = intent.getStringExtra("category");
        String freq = intent.getStringExtra("frequency");
        String date = intent.getStringExtra("last_completed_date");

        int taskId = intent.getIntExtra("TASK_ID", -1);
        int userId = intent.getIntExtra("USER_ID", -1);
        String fullName = intent.getStringExtra("FULL_NAME");
        String username = intent.getStringExtra("USERNAME");

        Log.d(TAG, "Received last_completed_date: " + date);
        Log.d(TAG, "Task ID: " + taskId + ", User ID: " + userId);

        //  Fetch progressDates from DB ───
        List<String> progressDates = new ArrayList<>();
        if (userId != -1 && taskId != -1) {
            DatabaseConnection db = new DatabaseConnection(this);
            progressDates = db.getProgressDates(userId, taskId);
            Log.d(TAG, "Progress dates from DB: " + progressDates);
        } else {
            Log.e(TAG, "Invalid userId or taskId: userId=" + userId + ", taskId=" + taskId);
        }

        //  Determine which “last completed date” to display ───
        String displayDate = null;
        if (!progressDates.isEmpty()) {
            // Use the most recent progress entry
            displayDate = progressDates.get(progressDates.size() - 1);
            Log.d(TAG, "Using most recent progress date: " + displayDate);
        } else if (date != null && !date.isEmpty()) {
            displayDate = date;
            Log.d(TAG, "Using Intent last_completed_date: " + displayDate);
        }

        String formattedDate = formatDate(displayDate);
        Log.d(TAG, "Formatted date: " + formattedDate);

        //  Fetch normalized notification date & time from DB ───
        String formattedNotify = "N/A";
        if (taskId != -1) {
            DatabaseConnection db = new DatabaseConnection(this);
            Map<String, String> notifyMap = db.getTaskNotification(taskId);
            // notifyMap may contain keys "notify_date" and "notify_time"
            String nd = notifyMap.get("notify_date");
            String nt = notifyMap.get("notify_time");
            if (nd != null && nt != null) {
                // Display as "Date : Time"
                formattedNotify = nd + " : " + nt;
            } else {
                // If DB has no entry, fallback to "N/A"
                formattedNotify = "N/A";
            }
            Log.d(TAG, "Fetched from Task_Notifications: date=" + nd + ", time=" + nt);
        }

        //  Populate UI fields ───
        taskName.setText(name != null ? name : "N/A");
        taskDescription.setText(desc != null ? desc : "No Description Provided");
        category.setText(cat != null ? cat : "N/A");
        taskRepeats.setText(freq != null ? freq : "N/A");

        dateLastCompleted.setText(formattedDate != null ? formattedDate : "Not yet completed");
        notificationType.setText(formattedNotify);

        //  Populate RecyclerView with timeline (formatted) ───
        List<String> formattedProgressDates = new ArrayList<>();
        for (String progressDate : progressDates) {
            String f = formatDate(progressDate);
            formattedProgressDates.add(f != null ? f : progressDate);
        }
        timelineRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timelineAdapter = new TimelineAdapter(formattedProgressDates);
        timelineRecyclerView.setAdapter(timelineAdapter);

        //  Set Back button behavior ───
        back.setOnClickListener(v -> {
            Intent backIntent = new Intent(this, TaskListActivity.class);
            backIntent.putExtra("FULL_NAME", fullName);
            backIntent.putExtra("USERNAME", username);
            backIntent.putExtra("USER_ID", userId);
            startActivity(backIntent);
            finish();
        });

        //  Edit Button: pass everything to UpdateTaskActivity ───
        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(TaskDetailsActivity.this, UpdateTaskActivity.class);
            editIntent.putExtra("TASK_ID", taskId);
            editIntent.putExtra("USER_ID", userId);
            editIntent.putExtra("task_name", name);
            editIntent.putExtra("description", desc);
            editIntent.putExtra("category", cat);
            editIntent.putExtra("frequency", freq);
            editIntent.putExtra("last_completed_date", date);
            // We do NOT pass the raw "notify_before"; UpdateTaskActivity will fetch from DB.
            editIntent.putExtra("status", intent.getStringExtra("status"));
            editIntent.putExtra("FULL_NAME", fullName);
            editIntent.putExtra("USERNAME", username);
            startActivity(editIntent);
        });

        ImageView shareButton = findViewById(R.id.shareButton);
        String taskStatus = intent.getStringExtra("status"); // or get from DB if needed

        if ("Complete".equalsIgnoreCase(taskStatus)) {
            shareButton.setVisibility(View.VISIBLE);
        } else {
            shareButton.setVisibility(View.GONE);
        }
        // Share Button:
        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(TaskDetailsActivity.this, TaskShareActivity.class);
            shareIntent.putExtra("FULL_NAME", fullName);
            shareIntent.putExtra("TASK_NAME", name);
            startActivity(shareIntent);
        });

        // Delete Button: show custom confirmation dialog ───
        deleteButton.setOnClickListener(v -> {
            if (taskId != -1 && userId != -1) {
                Dialog dialog = new Dialog(this);
                // Request feature before setting content
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.popup_delete_dialog);
                dialog.setCancelable(true);

                // Remove title bar and set full-width layout
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                Button cancelBtn = dialog.findViewById(R.id.cancel_button);
                Button deleteBtn = dialog.findViewById(R.id.delete_button);

                // instead, define pressed/default colors:
                int[][] states = new int[][] {
                        new int[] { android.R.attr.state_pressed }, // pressed
                        new int[] {}                               // default
                };
                int[] colors = new int[] {
                        Color.rgb(255,179,0),  // red when pressed
                        Color.WHITE    // white by default
                };

                // apply as a tint list:
                cancelBtn.setBackgroundTintList(new ColorStateList(states, colors));


                cancelBtn.setOnClickListener(view -> dialog.dismiss());
                deleteBtn.setOnClickListener(view -> {
                    DatabaseConnection db = new DatabaseConnection(this);
                    boolean deleted = db.deleteTask(userId, taskId);
                    if (deleted) {
                        Log.d(TAG, "Task deleted successfully: taskId=" + taskId);
                        Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                        Intent backIntent = new Intent(this, TaskListActivity.class);
                        backIntent.putExtra("FULL_NAME", fullName);
                        backIntent.putExtra("USERNAME", username);
                        backIntent.putExtra("USER_ID", userId);
                        startActivity(backIntent);
                        finish();
                    } else {
                        Log.e(TAG, "Failed to delete task: taskId=" + taskId);
                        Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                });

                dialog.show();
            } else {
                Log.e(TAG, "Invalid taskId or userId for deletion: taskId=" + taskId + ", userId=" + userId);
            }
        });

        // ─── 9) Bottom Navigation ───
        setupNavigationListeners(fullName, username, userId);
        updateNavigationState();
    }

    /**
     * Format a date string "dd/MM/yyyy" → "MMMM dd, yyyy"
     * Returns null if input is null/empty.
     */
    private String formatDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date parsedDate = inputFormat.parse(date);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            return outputFormat.format(parsedDate);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + date, e);
            return date;
        }
    }

    private void setupNavigationListeners(String fullName, String username, int userId) {
        navHome.setOnClickListener(v -> navigateTo(HomeActivity.class, fullName, username, userId));
        navNotifications.setOnClickListener(v -> navigateTo(NotificationActivity.class, fullName, username, userId));
        navAdd.setOnClickListener(v -> navigateTo(AddTaskActivity.class, fullName, username, userId));
        navTasks.setOnClickListener(v -> navigateTo(TaskListActivity.class, fullName, username, userId));
        navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class, fullName, username, userId));
    }

    private void navigateTo(Class<?> activityClass, String fullName, String username, int userId) {
        Intent intent = new Intent(TaskDetailsActivity.this, activityClass);
        if (fullName != null) intent.putExtra("FULL_NAME", fullName);
        if (username != null) intent.putExtra("USERNAME", username);
        if (userId != -1) intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }

    private void updateNavigationState() {
        ImageView iconTasks = findViewById(R.id.icon_tasks);
        TextView textTasks = findViewById(R.id.text_tasks);
        iconTasks.setColorFilter(getResources().getColor(android.R.color.black));
        textTasks.setTextColor(getResources().getColor(android.R.color.black));
    }
}