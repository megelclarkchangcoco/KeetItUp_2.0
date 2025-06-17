package com.example.keetitup_20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// Activity to display the list of tasks for a user
public class TaskListActivity extends AppCompatActivity {

    RecyclerView recyclerView; // UI component to display the task list
    TaskAdapter taskAdapter; // Adapter to manage and display task items
    DatabaseConnection db; // Database connection to retrieve tasks
    TextView emptyTaskListText; // UI component for empty task list message
    String fullName; // User's full name
    String username; // User's username
    int userId = -1; // User ID for database lookup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        // Initialize navigation items for bottom navigation menu
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navAdd = findViewById(R.id.nav_add);
        LinearLayout navTasks = findViewById(R.id.nav_tasks);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        // Initialize database connection
        db = new DatabaseConnection(this);

        // Retrieve user data from the previous activity via Intent
        Intent intent = getIntent();
        fullName = intent.getStringExtra("FULL_NAME");
        username = intent.getStringExtra("USERNAME");
        userId = intent.getIntExtra("USER_ID", -1);

        // Update task summary counts
        int totalTasks = db.countTasksByStatus(userId, "Ongoing");
        int completedTasks = db.countTasksByStatus(userId, "Complete");

        TextView totalTasksText = findViewById(R.id.total_task_count);
        totalTasksText.setText(totalTasks + " total tasks Item");

        TextView completedText = findViewById(R.id.completed_task_count);
        completedText.setText(completedTasks + " tasks completed");

        // Set up RecyclerView for displaying tasks
        recyclerView = findViewById(R.id.recyclerView);
        emptyTaskListText = findViewById(R.id.empty_task_list_text); // Initialize empty state TextView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // If no valid user ID is provided, exit the method
        if (userId == -1) {
            return; // Avoid further execution
        }

        // Retrieve tasks for the logged-in user from the database
        List<Map<String, String>> taskList = db.getTasksForUser(userId);
        // Initialize adapter with task list and user details
        taskAdapter = new TaskAdapter(taskList, fullName, username, userId);
        recyclerView.setAdapter(taskAdapter); // Attach adapter to RecyclerView

        // Sort tasks: ongoing first, complete last
        taskList.sort((a, b) -> {
            String statusA = a.get("status");
            String statusB = b.get("status");
            if (statusA.equalsIgnoreCase(statusB)) return 0;
            if (statusA.equalsIgnoreCase("Complete")) return 1; // a after b
            if (statusB.equalsIgnoreCase("Complete")) return -1; // a before b
            return 0;
        });

        // Update empty state visibility
        updateEmptyState(taskList);

        // Set up click listeners for navigation items
        navHome.setOnClickListener(v -> {
            // Navigate to the HomeActivity and pass user details
            Intent intentNav = new Intent(TaskListActivity.this, HomeActivity.class);
            if (fullName != null) intentNav.putExtra("FULL_NAME", fullName);
            if (username != null) intentNav.putExtra("USERNAME", username);
            if (userId != -1) intentNav.putExtra("USER_ID", userId);
            startActivity(intentNav);
            finish(); // Close current activity
        });

        navNotifications.setOnClickListener(v -> {
            // Navigate to the NotificationActivity
            Intent intentNav = new Intent(TaskListActivity.this, NotificationActivity.class);
            if (fullName != null) intentNav.putExtra("FULL_NAME", fullName);
            if (username != null) intentNav.putExtra("USERNAME", username);
            if (userId != -1) intentNav.putExtra("USER_ID", userId);
            startActivity(intentNav);
            finish();
        });

        navAdd.setOnClickListener(v -> {
            // Navigate to the AddTaskActivity
            Intent intentNav = new Intent(TaskListActivity.this, AddTaskActivity.class);
            if (fullName != null) intentNav.putExtra("FULL_NAME", fullName);
            if (username != null) intentNav.putExtra("USERNAME", username);
            if (userId != -1) intentNav.putExtra("USER_ID", userId);
            startActivity(intentNav);
            finish();
        });

        navTasks.setOnClickListener(v -> {
            // Already in TaskListActivity, no action needed
        });

        navProfile.setOnClickListener(v -> {
            // Navigate to the ProfileActivity
            Intent intentNav = new Intent(TaskListActivity.this, ProfileActivity.class);
            if (fullName != null) intentNav.putExtra("FULL_NAME", fullName);
            if (username != null) intentNav.putExtra("USERNAME", username);
            if (userId != -1) intentNav.putExtra("USER_ID", userId);
            startActivity(intentNav);
            finish();
        });

        // Update UI to highlight the selected "Tasks" section
        updateNavigationState();
    }

    // Method to update the visibility of UI components based on task list state
    private void updateEmptyState(List<Map<String, String>> taskList) {
        LinearLayout taskSummary = findViewById(R.id.taskSummary);
        if (taskList == null || taskList.isEmpty()) {
            emptyTaskListText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            taskSummary.setVisibility(View.GONE); // Hide summary when no tasks
        } else {
            emptyTaskListText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            taskSummary.setVisibility(View.VISIBLE); // Show summary when tasks exist
        }
    }

    // Method to update navigation UI to indicate that "Tasks" is currently active
    private void updateNavigationState() {
        ImageView iconTasks = findViewById(R.id.icon_tasks);
        TextView textTasks = findViewById(R.id.text_tasks);
        iconTasks.setColorFilter(getResources().getColor(R.color.black, null)); // Highlight icon Ascertain that the icon_tasks resource exists
        textTasks.setTextColor(getResources().getColor(R.color.black, null)); // Highlight text
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh task list when activity resumes
        if (db != null && recyclerView != null && userId != -1) {
            List<Map<String, String>> taskList = db.getTasksForUser(userId);
            // Sort tasks: ongoing first, complete last
            taskList.sort((a, b) -> {
                String statusA = a.get("status");
                String statusB = b.get("status");
                if (statusA.equalsIgnoreCase(statusB)) return 0;
                if (statusA.equalsIgnoreCase("Complete")) return 1; // a after b
                if (statusB.equalsIgnoreCase("Complete")) return -1; // a before b
                return 0;
            });
            taskAdapter.updateTasks(taskList); // Update the adapter with the sorted task list
            updateEmptyState(taskList);

            // Update task summary counts
            int totalTasks = db.countTasksByStatus(userId, "Ongoing");
            int completedTasks = db.countTasksByStatus(userId, "Complete");
            TextView totalTasksText = findViewById(R.id.total_task_count);
            totalTasksText.setText(totalTasks + " total tasks Item");
            TextView completedText = findViewById(R.id.completed_task_count);
            completedText.setText(completedTasks + " tasks completed");
        }
    }
}