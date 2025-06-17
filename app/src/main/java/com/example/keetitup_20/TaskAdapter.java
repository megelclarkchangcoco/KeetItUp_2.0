package com.example.keetitup_20;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private static final String TAG = "TaskAdapter";
    private final List<Map<String, String>> taskList;
    private final String fullName;
    private final String username;
    private final int userId;

    public TaskAdapter(List<Map<String, String>> taskList, String fullName, String username, int userId) {
        this.taskList = taskList != null ? taskList : new ArrayList<>(); // Initialize with empty list if null
        this.fullName = fullName;
        this.username = username;
        this.userId = userId;
    }

    // Method to update the task list and notify the RecyclerView of changes
    public void updateTasks(List<Map<String, String>> newTaskList) {
        taskList.clear();
        if (newTaskList != null) {
            taskList.addAll(newTaskList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_task_list_row, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Map<String, String> task = taskList.get(position);

        // 1) Set basic fields
        holder.taskName.setText(task.get("task_name"));
        holder.taskCategory.setText(task.get("category"));
        holder.taskFrequency.setText(task.get("frequency"));

        int taskId = Integer.parseInt(task.get("task_id"));
        DatabaseConnection db = new DatabaseConnection(holder.itemView.getContext());

        // 2) Get timeline total and completed count for progress bar
        int total = db.getTimelineTotal(taskId);
        int completed = db.getCompletedProgressCount(taskId);
        holder.progressBar.setMax(total); // Avoid division by zero handled by default max value
        holder.progressBar.setProgress(completed);
        holder.progressText.setText(completed + "/" + total);

        // 3) Set check icon state based on status
        String status = task.get("status");
        if ("Complete".equalsIgnoreCase(status)) {
            holder.checkIcon.setBackgroundResource(R.drawable.circle_background_green);
            holder.checkIcon.setEnabled(false);
        } else {
            holder.checkIcon.setBackgroundResource(R.drawable.circle_background);
            holder.checkIcon.setEnabled(true);
        }

        // 4) Check icon click: add progress if not complete
        holder.checkIcon.setOnClickListener(v -> {
            int updatedCompleted = db.getCompletedProgressCount(taskId);
            int updatedTotal = db.getTimelineTotal(taskId);

            if (updatedCompleted < updatedTotal) {
                // Use date and time format
                String today = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                Log.d(TAG, "Check button clicked at: " + today); // Debug log

                if (db.addProgress(userId, taskId, today)) {
                    db.updateLastCompletedDate(taskId, today);
                    task.put("last_completed_date", today); // Store the updated date in the task object
                    Log.d(TAG, "Progress added, last_completed_date set to: " + today);

                    updatedCompleted = db.getCompletedProgressCount(taskId);

                    // Show popup_congratulate dialog
                    Dialog dialog = new Dialog(holder.itemView.getContext());
                    dialog.setContentView(R.layout.popup_congratulate);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                    // Set dialog message based on completion status
                    TextView dialogMessage = dialog.findViewById(R.id.dialog_message);
                    if (updatedCompleted >= updatedTotal) {
                        dialogMessage.setText("You completed all tasks! Great job!");
                        db.markTaskComplete(taskId);
                        task.put("status", "Complete");
                    } else {
                        dialogMessage.setText("1 task complete for today");
                    }

                    // Set up Okay button
                    Button okButton = dialog.findViewById(R.id.ok_button);
                    int[][] states = new int[][] {
                            new int[] { android.R.attr.state_pressed }, // pressed
                            new int[] {}                               // default
                    };
                    int[] colors = new int[] {
                            Color.rgb(0, 240, 102),  // lime green when pressed
                            Color.WHITE              // white by default
                    };
                    okButton.setBackgroundTintList(new ColorStateList(states, colors));

                    // Keep click-to-dismiss behavior with UI update
                    final int finalUpdatedCompleted = updatedCompleted;
                    final int finalUpdatedTotal = updatedTotal;
                    okButton.setOnClickListener(view -> {
                        dialog.dismiss();
                        // Update UI to reflect new progress
                        holder.progressBar.setProgress(finalUpdatedCompleted);
                        holder.progressText.setText(finalUpdatedCompleted + "/" + finalUpdatedTotal);
                        notifyItemChanged(position); // Refresh the item
                    });

                    // Set up Undo button
                    Button undoButton = dialog.findViewById(R.id.undo_button);
                    int[][] undoStates = new int[][] {
                            new int[] { android.R.attr.state_pressed }, // pressed
                            new int[] {}                               // default
                    };
                    int[] undoColors = new int[] {
                            Color.rgb(255, 68, 68),  // Red when pressed
                            Color.WHITE              // White by default
                    };
                    undoButton.setBackgroundTintList(new ColorStateList(undoStates, undoColors));

                    // Undo button logic
                    undoButton.setOnClickListener(view -> {
                        List<String> progressDates = db.getProgressDates(userId, taskId);
                        if (!progressDates.isEmpty()) {
                            String latestDate = progressDates.get(progressDates.size() - 1);
                            db.removeProgress(userId, taskId, latestDate);

                            // Recalculate completed progress after removal
                            int newCompleted = db.getCompletedProgressCount(taskId);

                            // Revert to "Ongoing" if progress is less than total
                            if (newCompleted < updatedTotal) {
                                db.markTaskOngoing(taskId);
                                task.put("status", "Ongoing");
                            }

                            // Update last completed date if no progress remains
                            if (newCompleted == 0) {
                                db.updateLastCompletedDate(taskId, null);
                                task.put("last_completed_date", "");
                            } else {
                                progressDates = db.getProgressDates(userId, taskId);
                                if (!progressDates.isEmpty()) {
                                    db.updateLastCompletedDate(taskId, progressDates.get(progressDates.size() - 1));
                                    task.put("last_completed_date", progressDates.get(progressDates.size() - 1));
                                }
                            }

                            // Update UI and dismiss dialog
                            holder.progressBar.setProgress(newCompleted);
                            holder.progressText.setText(newCompleted + "/" + updatedTotal);
                            notifyItemChanged(position);
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                    WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT; // Changed to MATCH_PARENT for full width
                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    dialog.getWindow().setAttributes(layoutParams);
                }
            }
        });

        // 5) Entire row click: launch TaskDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            setClickHighlight(holder.tasklistCard);

            Intent intent = new Intent(v.getContext(), TaskDetailsActivity.class);
            intent.putExtra("task_name", task.get("task_name"));
            intent.putExtra("description", task.get("description"));
            intent.putExtra("category", task.get("category"));
            intent.putExtra("frequency", task.get("frequency"));
            intent.putExtra("last_completed_date", task.get("last_completed_date"));
            intent.putExtra("status", task.get("status"));
            intent.putExtra("TASK_ID", taskId);
            intent.putExtra("FULL_NAME", fullName);
            intent.putExtra("USERNAME", username);
            intent.putExtra("USER_ID", userId);

            // Fetch normalized notification date & time
            Map<String, String> notifyMap = db.getTaskNotification(taskId);
            String nd = notifyMap.get("notify_date");
            String nt = notifyMap.get("notify_time");
            String notifyCombined = "";
            if (nd != null && nt != null) {
                notifyCombined = nd + " " + nt;
            }
            intent.putExtra("notify_before", notifyCombined);

            // Also pass progress dates if TaskDetails needs them
            List<String> progressDates = db.getProgressDates(userId, taskId);
            intent.putStringArrayListExtra("progress_dates", new ArrayList<>(progressDates));

            v.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskCategory, taskFrequency, progressText;
        ProgressBar progressBar;
        ImageView checkIcon;
        CardView tasklistCard;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tasklistCard = itemView.findViewById(R.id.list_card);
            taskName = itemView.findViewById(R.id.task_name);
            taskCategory = itemView.findViewById(R.id.task_category);
            taskFrequency = itemView.findViewById(R.id.task_frequency);
            progressText = itemView.findViewById(R.id.progress_text);
            progressBar = itemView.findViewById(R.id.progress_bar);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }
    }

    private void setClickHighlight(View view) {
        view.setBackgroundResource(R.color.custom_yellow);
        view.postDelayed(() -> view.setBackgroundResource(android.R.color.transparent), 300);
    }
}