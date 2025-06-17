package com.example.keetitup_20;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private final List<CalendarDay> days;
    private final LocalDate today;
    private final Context context;

    public CalendarAdapter(Context context, List<CalendarDay> days) {
        this.context = context;
        this.days = days;
        this.today = LocalDate.now();
    }

    @NonNull
    @Override
    public CalendarAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarAdapter.ViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.tvDateNumber.setText(String.valueOf(day.getDayOfMonth()));
        holder.tvDayInitial.setText(day.getDayInitial());

        // If this date is “today,” highlight with custom_yellow fill + black text
        if (day.getDate().equals(today)) {
            holder.tvDateNumber.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.selected_cell_background)
            );
            holder.tvDateNumber.setTextColor(
                    ContextCompat.getColor(context, android.R.color.black)
            );
        } else {
            // All other days: white fill + gray border (cell_background), black text
            holder.tvDateNumber.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.cell_background)
            );
            holder.tvDateNumber.setTextColor(
                    ContextCompat.getColor(context, android.R.color.black)
            );
        }

        // Day‐initial is always black
        holder.tvDayInitial.setTextColor(
                ContextCompat.getColor(context, android.R.color.black)
        );
    }


    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateNumber, tvDayInitial;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateNumber = itemView.findViewById(R.id.tv_date_number);
            tvDayInitial = itemView.findViewById(R.id.tv_day_initial);
        }
    }
}
