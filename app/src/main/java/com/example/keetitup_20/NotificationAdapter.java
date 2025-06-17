package com.example.keetitup_20;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Map;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Map<String, String>> notificationList;
    private Context context;
    private String fullName;
    private String username;
    private int userId;

    public NotificationAdapter(Context context,
                               List<Map<String, String>> notificationList,
                               String fullName,
                               String username,
                               int userId) {
        this.context = context;
        this.notificationList = notificationList;
        this.fullName = fullName;
        this.username = username;
        this.userId = userId;
    }

    public void updateNotifications(List<Map<String, String>> newNotificationList) {
        this.notificationList = newNotificationList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_list_row, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Map<String, String> notification = notificationList.get(position);

        // 1) Bind data into each TextView/ImageView
        holder.taskName.setText(notification.get("task_name"));
        holder.notificationDate.setText(notification.get("notify_date"));
        holder.notificationTime.setText(notification.get("notify_time"));
        holder.taskCategory.setText(notification.get("category"));
        holder.taskFrequency.setText(notification.get("frequency"));
        String lastCompleted = notification.get("last_completed_date");
        holder.lastCompleted.setText(
                (lastCompleted != null)
                        ? "Last completed " + lastCompleted
                        : "Not completed yet"
        );

        // 2) Hook up a click listener on the CardView itself
        holder.notificationCard.setOnClickListener(v -> {
            setClickHighlight(holder.notificationCard);

            holder.notificationCard.postDelayed(() -> {
                Intent intent = new Intent(context, TaskDetailsActivity.class);
                intent.putExtra("TASK_ID", Integer.parseInt(notification.get("task_id")));
                intent.putExtra("USER_ID", userId);
                intent.putExtra("FULL_NAME", fullName);
                intent.putExtra("USERNAME", username);

                // Pass along all your task details:
                intent.putExtra("task_name", notification.get("task_name"));
                intent.putExtra("description", notification.get("description"));
                intent.putExtra("category", notification.get("category"));
                intent.putExtra("frequency", notification.get("frequency"));
                intent.putExtra("last_completed_date", notification.get("last_completed_date"));
                intent.putExtra("status", notification.get("status"));

                context.startActivity(intent);
            }, 300);
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView notificationCard;
        TextView taskName, notificationDate, notificationTime,
                taskCategory, taskFrequency, lastCompleted;
        ShapeableImageView categoryIcon, frequencyIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find your CardView by ID
            notificationCard = itemView.findViewById(R.id.notification_card);

            // Find all other views:
            taskName         = itemView.findViewById(R.id.task_name);
            notificationDate = itemView.findViewById(R.id.notification_date);
            notificationTime = itemView.findViewById(R.id.notification_time);
            taskCategory     = itemView.findViewById(R.id.task_category);
            taskFrequency    = itemView.findViewById(R.id.task_frequency);
            lastCompleted    = itemView.findViewById(R.id.last_completed);
            categoryIcon     = itemView.findViewById(R.id.category_icon);
            frequencyIcon    = itemView.findViewById(R.id.frequency_icon);
        }
    }

    private void setClickHighlight(View view) {
        view.setBackgroundResource(R.color.custom_yellow);
        view.postDelayed(() -> view.setBackgroundResource(android.R.color.transparent), 300);
    }
}
