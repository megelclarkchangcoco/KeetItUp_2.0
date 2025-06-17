package com.example.keetitup_20;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {

    private DatabaseConnection db;
    private int userId;
    private String fullName;
    private String username;
    private RecyclerView rvCalendarStrip;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1) Use setContentView (not inflater.inflate) for an Activity
        setContentView(R.layout.activity_home);

        // 2) Find the RecyclerView for our two-week strip
        rvCalendarStrip = findViewById(R.id.rv_calendar_strip);
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvCalendarStrip.setLayoutManager(layoutManager);

        // 3) Build a 14-day list (Mon→Sun this week, Mon→Sun next week)
        List<CalendarDay> dayList = generateTwoWeekDates();
        CalendarAdapter adapter = new CalendarAdapter(this, dayList);
        rvCalendarStrip.setAdapter(adapter);

        // 4) Initialize the rest of your UI elements
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navAdd = findViewById(R.id.nav_add);
        LinearLayout navTasks = findViewById(R.id.nav_tasks);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        TextView taskProgressText = findViewById(R.id.progress_text);
        ImageView bigIconImage = findViewById(R.id.ic_big_icon);
        TextView taskNameText = findViewById(R.id.task_name);
        TextView taskCategoryText = findViewById(R.id.task_category);
        TextView taskFrequencyText = findViewById(R.id.task_frequency);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        ConstraintLayout taskCardLayout = findViewById(R.id.task_card_layout);

        // Get intent extras
        Intent intent = getIntent();
        fullName = intent.getStringExtra("FULL_NAME");
        username = intent.getStringExtra("USERNAME");
        userId = intent.getIntExtra("USER_ID", -1);

        // Initialize database
        db = new DatabaseConnection(this);

        // Set welcome text (only first name)
        String firstName = "";
        if (fullName != null && !fullName.isEmpty()) {
            firstName = fullName.split(" ")[0];
        }
        TextView headerNameText = findViewById(R.id.welcome_text);
        headerNameText.setText("Keep It Up, " + firstName + "!");

        // Load tasks and update UI
        if (userId != -1) {
            List<Map<String, String>> taskList = db.getTasksForUser(userId);
            int totalTasks = taskList.size();
            int completedTasks = db.countTasksByStatus(userId, "Complete");
            int ongoingTasks = db.countTasksByStatus(userId, "Ongoing");

            if (totalTasks == 0) {
                updateEmptyTaskUI(
                        bigIconImage,
                        taskProgressText,
                        taskNameText,
                        taskCategoryText,
                        taskFrequencyText,
                        progressBar
                );
            } else {
                // Find the task with the nearest due date among ongoing tasks
                Map<String, String> currentTask = findNearestDueTask(taskList);
                if (currentTask == null) {
                    // Fallback to first ongoing task if no due dates are imminent
                    for (Map<String, String> task : taskList) {
                        if ("Ongoing".equalsIgnoreCase(task.get("status"))) {
                            currentTask = task;
                            break;
                        }
                    }
                    if (currentTask == null && !taskList.isEmpty()) {
                        currentTask = taskList.get(0);
                    }
                }

                if (currentTask != null) {
                    final Map<String, String> currentTaskFinal = currentTask;
                    final int currentTaskId = Integer.parseInt(currentTaskFinal.get("task_id"));

                    // Update task UI
                    taskNameText.setText(currentTaskFinal.get("task_name"));
                    taskCategoryText.setText(currentTaskFinal.get("category"));
                    taskFrequencyText.setText(currentTaskFinal.get("frequency"));

                    int timelineTotal = db.getTimelineTotal(currentTaskId);
                    int progressCount = db.getCompletedProgressCount(currentTaskId);

                    progressBar.setMax(timelineTotal > 0 ? timelineTotal : 1);
                    progressBar.setProgress(progressCount);
                    taskProgressText.setText(progressCount + " / " + timelineTotal + " completed");

                    // Navigation to TaskDetailsActivity on card click
                    taskCardLayout.setOnClickListener(v -> {
                        setClickHighlight(v);
                        Intent detailsIntent = new Intent(HomeActivity.this, TaskDetailsActivity.class);
                        detailsIntent.putExtra("task_name", currentTaskFinal.get("task_name"));
                        detailsIntent.putExtra("description", currentTaskFinal.get("description"));
                        detailsIntent.putExtra("category", currentTaskFinal.get("category"));
                        detailsIntent.putExtra("frequency", currentTaskFinal.get("frequency"));
                        detailsIntent.putExtra("last_completed_date", currentTaskFinal.get("last_completed_date"));
                        detailsIntent.putExtra("notify_before", currentTaskFinal.get("notify_before"));
                        detailsIntent.putExtra("status", currentTaskFinal.get("status"));
                        detailsIntent.putExtra("TASK_ID", currentTaskId);
                        detailsIntent.putExtra("FULL_NAME", fullName);
                        detailsIntent.putExtra("USERNAME", username);
                        detailsIntent.putExtra("USER_ID", userId);
                        startActivity(detailsIntent);
                    });

                    // Update mood icon based on task progress
                    updateMoodIcon(bigIconImage, completedTasks, ongoingTasks, totalTasks);
                } else {
                    updateEmptyTaskUI(
                            bigIconImage,
                            taskProgressText,
                            taskNameText,
                            taskCategoryText,
                            taskFrequencyText,
                            progressBar
                    );
                }
            }
        } else {
            updateEmptyTaskUI(
                    bigIconImage,
                    taskProgressText,
                    taskNameText,
                    taskCategoryText,
                    taskFrequencyText,
                    progressBar
            );
            taskProgressText.setText("User not found");
        }

        // Navigation listeners
        navHome.setOnClickListener(v -> {
            // Already on home
        });
        navNotifications.setOnClickListener(v -> navigateToActivity(NotificationActivity.class));
        navAdd.setOnClickListener(v -> navigateToActivity(AddTaskActivity.class));
        navTasks.setOnClickListener(v -> navigateToActivity(TaskListActivity.class));
        navProfile.setOnClickListener(v -> navigateToActivity(ProfileActivity.class));

        updateNavigationState();
    }

    /**
     * Generate a list of exactly 14 CalendarDay objects:
     * - 0..6   = Monday → Sunday of the current week
     * - 7..13 = Monday → Sunday of the next week
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<CalendarDay> generateTwoWeekDates() {
        List<CalendarDay> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1) Find Monday of this week
        DayOfWeek dow = today.getDayOfWeek();
        int shiftToMonday = dow.getValue() - DayOfWeek.MONDAY.getValue();
        LocalDate thisWeekMonday = today.minusDays(shiftToMonday);

        // 2) Fill Monday → Sunday for this week
        for (int i = 0; i < 7; i++) {
            LocalDate date = thisWeekMonday.plusDays(i);
            result.add(new CalendarDay(date));
        }

        // 3) Next week Monday = thisWeekMonday + 7 days
        LocalDate nextWeekMonday = thisWeekMonday.plusDays(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = nextWeekMonday.plusDays(i);
            result.add(new CalendarDay(date));
        }

        return result;
    }
    // method for display task ui this only if no task assign
    private void updateEmptyTaskUI(
            ImageView bigIconImage,
            TextView taskProgressText,
            TextView taskNameText,
            TextView taskCategoryText,
            TextView taskFrequencyText,
            ProgressBar progressBar
    ) {
        bigIconImage.setImageResource(R.drawable.ic_big_neutral);
        taskProgressText.setText("No tasks assigned");
        taskNameText.setText("");
        taskCategoryText.setText("");
        taskFrequencyText.setText("");
        progressBar.setProgress(0);

        ConstraintLayout taskCardLayout = findViewById(R.id.task_card_layout);
        taskCardLayout.setVisibility(View.GONE);
        taskCardLayout.setClickable(false);
    }
    // method for display icon with functionality based on task
    private void updateMoodIcon(
            ImageView bigIconImage,
            int completedTasks,
            int ongoingTasks,
            int totalTasks
    ) {
        if (ongoingTasks > 0) {
            if (completedTasks == 0) {
                bigIconImage.setImageResource(R.drawable.ic_big_angry);
            } else if (completedTasks >= 1 && completedTasks <= 2) {
                bigIconImage.setImageResource(R.drawable.ic_big_sick);
            } else if (completedTasks >= 3 && completedTasks <= 5) {
                bigIconImage.setImageResource(R.drawable.ic_big_sad);
            } else if (completedTasks >= 3) {
                bigIconImage.setImageResource(R.drawable.ic_big_happy);
            } else {
                bigIconImage.setImageResource(R.drawable.ic_big_neutral);
            }
        } else if (completedTasks == totalTasks && totalTasks > 0) {
            bigIconImage.setImageResource(R.drawable.ic_big_elated);
        } else {
            bigIconImage.setImageResource(R.drawable.ic_big_neutral);
        }
    }

    // method for navigation to change activity with id
    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(HomeActivity.this, activityClass);
        if (fullName != null) intent.putExtra("FULL_NAME", fullName);
        if (username != null) intent.putExtra("USERNAME", username);
        if (userId != -1) intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }
    private void updateNavigationState() {
        ImageView iconHome = findViewById(R.id.icon_home);
        TextView textHome = findViewById(R.id.text_home);
        iconHome.setColorFilter(getResources().getColor(R.color.black, null));
        textHome.setTextColor(getResources().getColor(R.color.black, null));
    }

    private void setClickHighlight(View view) {
        view.setBackgroundResource(R.color.custom_yellow);
        view.postDelayed(() -> view.setBackgroundResource(android.R.color.transparent), 300);
    }

    /**
     * Find the task with the nearest due date among ongoing tasks within the next 7 days.
     * Includes tasks due today. Uses Asia/Singapore time zone.
     * @param taskList List of tasks to process
     * @return The task with the nearest due date, or null if no valid tasks are found
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Map<String, String> findNearestDueTask(List<Map<String, String>> taskList) {
        // Current date and time in Asia/Singapore
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Singapore"));
        LocalDate sevenDaysLater = now.plusDays(7);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));

        Map<String, String> nearestTask = null;
        LocalDate nearestDueDate = null;

        Log.d("HomeActivity", "Current date: " + now + ", 7 days later: " + sevenDaysLater);

        for (Map<String, String> task : taskList) {
            if ("Ongoing".equalsIgnoreCase(task.get("status"))) {
                String notifyBefore = task.get("notify_before");
                if (notifyBefore != null && !notifyBefore.isEmpty()) {
                    try {
                        // Extract date part (dd/MM/yyyy)
                        String[] parts = notifyBefore.split(" ");
                        if (parts.length >= 1) {
                            String dueDateStr = parts[0];
                            LocalDate dueDate = LocalDate.parse(dueDateStr,
                                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                            Log.d("HomeActivity", "Task ID: " + task.get("task_id") +
                                    ", Name: " + task.get("task_name") +
                                    ", Due: " + dueDate +
                                    ", Status: " + task.get("status"));

                            // Include tasks due today or within 7 days
                            if (!dueDate.isBefore(now) && !dueDate.isAfter(sevenDaysLater)) {
                                if (nearestDueDate == null || dueDate.isBefore(nearestDueDate)) {
                                    nearestDueDate = dueDate;
                                    nearestTask = task;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("HomeActivity", "Error parsing due date for task ID " +
                                task.get("task_id") + ": " + notifyBefore, e);
                    }
                } else {
                    Log.d("HomeActivity", "Task ID: " + task.get("task_id") +
                            ", No notify_before date");
                }
            } else {
                Log.d("HomeActivity", "Task ID: " + task.get("task_id") +
                        ", Skipped (not Ongoing)");
            }
        }

        if (nearestTask != null) {
            Log.d("HomeActivity", "Selected nearest task: ID " + nearestTask.get("task_id") +
                    ", Name: " + nearestTask.get("task_name") +
                    ", Due: " + nearestTask.get("notify_before"));
        } else {
            Log.d("HomeActivity", "No nearest task found");
        }

        return nearestTask;
    }


}