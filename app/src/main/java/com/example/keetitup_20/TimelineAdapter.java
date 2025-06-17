package com.example.keetitup_20;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {
    private final List<String> dates; // List of timeline dates as strings

    // Constructor: accepts a list of date strings to display
    public TimelineAdapter(List<String> dates) {
        this.dates = dates;
    }

    // Inflate layout for each timeline item and create ViewHolder
    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_item, parent, false);
        return new TimelineViewHolder(view);
    }

    // Bind date data to each ViewHolder's views
    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        String date = dates.get(position); // Get date string at current position
        String formattedDate = formatDate(date); // Format date to readable string
        // Set the formatted date text or original if formatting fails
        holder.dateText.setText(formattedDate != null ? formattedDate : date);
    }

    // Return total number of dates in the timeline
    @Override
    public int getItemCount() {
        return dates.size();
    }

    // Helper method to convert date string from "dd/MM/yyyy" to "MMMM dd, yyyy" format
    private String formatDate(String date) {
        if (date == null || date.isEmpty()) {
            return null; // Return null if input invalid
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date parsedDate = inputFormat.parse(date); // Parse input date string
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
            return outputFormat.format(parsedDate); // Format date to output format
        } catch (Exception e) {
            // Return original date string if parsing fails
            return date;
        }
    }

    // ViewHolder class holds references to views for each timeline item
    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView dateText; // TextView displaying formatted date
        View verticalLineProgress; // View representing vertical progress line in timeline

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bind views from the timeline item layout
            dateText = itemView.findViewById(R.id.dateText);
            verticalLineProgress = itemView.findViewById(R.id.verticalLineProgress);
        }
    }
}
