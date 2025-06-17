package com.example.keetitup_20;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {
    private AutoCompleteTextView autoCompleteTextView; // frequency dropdown
    private TextView selectDateText;     // Task date picker
    private TextView notifyDateText;     // Notify date picker
    private TextView notifyTimeText;     // Notify time picker
    private EditText taskNameInput;      // Task name
    private EditText taskDescriptionInput; // Task description
    private CardView addNewCard;         // “+ Add new” category card
    private GridLayout cardGrid;         // container for category cards

    private DatabaseConnection db;       // database helper
    private int userId;                  // passed from previous activity
    private String selectedCategory;     // currently‐selected category

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addtask);

        // ─── Link UI components ───
        selectDateText = findViewById(R.id.selectDate_text);
        notifyDateText = findViewById(R.id.notify_date_text);
        notifyTimeText = findViewById(R.id.notify_time_text);
        taskNameInput = findViewById(R.id.task_name_input);
        taskDescriptionInput = findViewById(R.id.task_description_input);
        addNewCard = findViewById(R.id.Addnew_card);
        cardGrid = findViewById(R.id.cardGrid);
        autoCompleteTextView = findViewById(R.id.auto_complete_text);

        // ─── Get user data passed from previous activity ───
        String username = getIntent().getStringExtra("USERNAME");
        String fullName = getIntent().getStringExtra("FULL_NAME");
        userId = getIntent().getIntExtra("USER_ID", -1);

        db = new DatabaseConnection(this);

        // ─── If userId is invalid, bail out ───
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ─── “+ Add New Category” Card Click: show dialog ───
        addNewCard.setOnClickListener(v -> showAddCategoryDialog());

        // ─── Pre‐fill “Task Date” picker with today as default ───
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // ─── Task Date TextView Click: show DatePickerDialog ───
        selectDateText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    AddTaskActivity.this,
                    (view, y, m, d) -> {
                        m += 1; // month is 0‐based internally
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y);
                        selectDateText.setText(selectedDate);
                    },
                    year, month, day
            );
            dialog.show();
        });

        // ─── Notify Date TextView Click: show DatePickerDialog ───
        notifyDateText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    AddTaskActivity.this,
                    (view, y, m, d) -> {
                        m += 1;
                        String selectedNotifyDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y);
                        notifyDateText.setText(selectedNotifyDate);
                    },
                    year, month, day
            );
            dialog.show();
        });

        // ─── Notify Time TextView Click: show TimePickerDialog ───
        notifyTimeText.setOnClickListener(v -> {
            // Default to current hour/min
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timeDialog = new TimePickerDialog(
                    AddTaskActivity.this,
                    (TimePicker view, int h, int m1) -> {
                        // Format as HH:mm
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m1);
                        notifyTimeText.setText(selectedTime);
                    },
                    hour, minute,
                    true // use 24‐hour view
            );
            timeDialog.show();
        });

        // ─── Frequency Dropdown Setup ───
        String[] frequencies = getResources().getStringArray(R.array.taskfrequency);
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, frequencies);
        autoCompleteTextView.setAdapter(freqAdapter);

        // ─── Submit Button: collect inputs & add task ───
        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> {
            String taskName = taskNameInput.getText().toString().trim();
            String taskDescription = taskDescriptionInput.getText().toString().trim();
            String taskDate = selectDateText.getText().toString().trim();
            String frequency = autoCompleteTextView.getText().toString().trim();
            String notifyDate = notifyDateText.getText().toString().trim();
            String notifyTime = notifyTimeText.getText().toString().trim();
            String createAt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

            // 1) Basic field validation
            if (taskName.isEmpty()) {
                taskNameInput.setError("Task name is required");
                return;
            }
            if (taskDescription.isEmpty()) {
                taskDescriptionInput.setError("Task description is required");
                return;
            }
            if (taskDate.isEmpty() || taskDate.equals("Select Date")) {
                Toast.makeText(AddTaskActivity.this,
                        "Please select a task date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (frequency.isEmpty()) {
                Toast.makeText(AddTaskActivity.this,
                        "Please select a frequency", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedCategory == null || selectedCategory.isEmpty()) {
                Toast.makeText(AddTaskActivity.this,
                        "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }
            if (notifyDate.isEmpty() || notifyDate.equals("Select Notify Date")) {
                Toast.makeText(AddTaskActivity.this,
                        "Please select a notify date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (notifyTime.isEmpty() || notifyTime.equals("Select Notify Time")) {
                Toast.makeText(AddTaskActivity.this,
                        "Please select a notify time", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) Frequency vs. Date validation
            if (!validateFrequencyAndDate(frequency, taskDate)) {
                return;
            }

            // 3) Combine notify date + time
            String notifyCombined = notifyDate + " " + notifyTime;

            // 4) Validate that notify datetime is in the future
            try {
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date notifyDateTime = dateTimeFormat.parse(notifyCombined);
                Date now = new Date();

                if (notifyDateTime != null && notifyDateTime.before(now)) {
                    Toast.makeText(AddTaskActivity.this,
                            "Notify time must be in the future.", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(AddTaskActivity.this,
                        "Failed to parse notification time.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 5) Attempt to save to database
            try {
                boolean success = db.addTask(
                        taskName,
                        selectedCategory,
                        taskDescription,
                        taskDate,
                        frequency,
                        notifyCombined,
                        createAt,
                        userId,
                        0,
                        0
                );

                if (success) {
                    Toast.makeText(AddTaskActivity.this,
                            "Task added successfully!", Toast.LENGTH_SHORT).show();
                    navigateTo(TaskListActivity.class, fullName, username);
                } else {
                    Toast.makeText(AddTaskActivity.this,
                            "Failed to add task.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                Toast.makeText(AddTaskActivity.this,
                        "Error saving task: " + ex.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

        // ─── Bottom Navigation Listeners ───
        findViewById(R.id.nav_home).setOnClickListener(v -> navigateTo(HomeActivity.class, fullName, username));
        findViewById(R.id.nav_notifications).setOnClickListener(v -> navigateTo(NotificationActivity.class, fullName, username));
        findViewById(R.id.nav_tasks).setOnClickListener(v -> navigateTo(TaskListActivity.class, fullName, username));
        findViewById(R.id.nav_profile).setOnClickListener(v -> navigateTo(ProfileActivity.class, fullName, username));

        // ─── Setup category grid with user-specific categories ───
        setupCategoryGrid(userId);
    }

    /**
     * Show the “Add Category” dialog. On submit, creates a new CardView
     * inside cardGrid and sets it clickable to pick that category.
     */
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Request the window to remove the title bar
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        EditText categoryNameInput = dialogView.findViewById(R.id.category_name_input);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button submitButton = dialogView.findViewById(R.id.submitButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        submitButton.setOnClickListener(v -> {
            String categoryName = categoryNameInput.getText().toString().trim();
            if (categoryName.isEmpty()) {
                Toast.makeText(AddTaskActivity.this, "Please enter a category name", Toast.LENGTH_SHORT).show();
                return;
            }

            // ─── Check if category already exists in database and cardGrid ───
            List<String> existingCategories = new ArrayList<>();
            List<String> dbCategories = db.getDistinctCategories(userId);
            if (dbCategories != null) {
                existingCategories.addAll(dbCategories);
            }
            for (int i = 0; i < cardGrid.getChildCount(); i++) {
                View child = cardGrid.getChildAt(i);
                if (child instanceof CardView) {
                    TextView categoryText = child.findViewById(R.id.categoryText);
                    if (categoryText != null) {
                        String cardCategory = categoryText.getText().toString().trim();
                        if (!cardCategory.isEmpty() && !cardCategory.equals("+ Add New")) {
                            existingCategories.add(cardCategory);
                        }
                    }
                }
            }
            if (existingCategories.contains(categoryName)) {
                Toast.makeText(AddTaskActivity.this, "Category '" + categoryName + "' already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reset all cards before adding new one
            for (int i = 0; i < cardGrid.getChildCount(); i++) {
                View child = cardGrid.getChildAt(i);
                if (child instanceof CardView) {
                    ((CardView) child).setCardBackgroundColor(Color.WHITE);
                }
            }
            selectedCategory = null; // Clear selection until clicked
            addCategoryCard(categoryName, false); // Add new category without highlight
            Toast.makeText(AddTaskActivity.this, "Category '" + categoryName + "' added successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Adjust dialog width to match screen
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Optional: match background style
        }

        dialog.show();
    }

    /**
     * Dynamically create a new CardView in the grid for the given category name.
     */
    private void addCategoryCard(String categoryName, boolean isSelected) {
        // Inflate a new instance of the card layout
        View newCardView = LayoutInflater.from(this).inflate(R.layout.category_card_layout, null);

        // Ensure it's treated as a CardView
        CardView categoryCard = newCardView.findViewById(R.id.myCardView);

        // Apply CardView properties to match "+ Add New" design
        categoryCard.setRadius(8f); // Rounded corners (adjust to match design, e.g., 8dp)
        categoryCard.setCardElevation(4f); // Shadow/elevation effect (adjust to match design, e.g., 4dp)
        categoryCard.setPadding(16, 16, 16, 16); // Consistent padding

        TextView categoryText = newCardView.findViewById(R.id.categoryText);
        categoryText.setText(categoryName != null ? categoryName : "");

        // Set background based on selection
        if (isSelected) {
            categoryCard.setCardBackgroundColor(Color.parseColor("#FFB300")); // Highlight color
        } else {
            categoryCard.setCardBackgroundColor(Color.WHITE); // Default color with shadow
        }

        // Set layout parameters for the GridLayout
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0; // Use weight to stretch
        params.height = (int) (60 * getResources().getDisplayMetrics().density); // Consistent height
        params.setMargins(8, 8, 8, 8); // Consistent margins
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Equal weight for columns
        newCardView.setLayoutParams(params);

        // Add click listener to select the category
        newCardView.setOnClickListener(v -> {
            selectedCategory = categoryName;
            // Reset background of all cards
            for (int i = 0; i < cardGrid.getChildCount(); i++) {
                View child = cardGrid.getChildAt(i);
                if (child instanceof CardView) {
                    ((CardView) child).setCardBackgroundColor(Color.WHITE);
                }
            }
            // Highlight the selected card
            categoryCard.setCardBackgroundColor(Color.parseColor("#FFB300"));
        });

        // Long press to remove category
        newCardView.setOnLongClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.popup_delete_category);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            Button cancelButton = dialog.findViewById(R.id.cancel_button);
            Button deleteButton = dialog.findViewById(R.id.delete_button);

            cancelButton.setOnClickListener(view -> dialog.dismiss());

            deleteButton.setOnClickListener(view -> {
                if (categoryName.equals(selectedCategory)) {
                    Toast.makeText(this, "Cannot remove selected category", Toast.LENGTH_SHORT).show();
                } else {
                    cardGrid.removeView(newCardView);
                    Toast.makeText(this, "Category removed", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });

            WindowManager.LayoutParams dialogParams = dialog.getWindow().getAttributes();
            dialogParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            dialogParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(dialogParams);

            dialog.show();
            return true;
        });

        // Detach from any existing parent before adding to cardGrid
        if (newCardView.getParent() != null) {
            ((ViewGroup) newCardView.getParent()).removeView(newCardView);
        }
        // Remove the "+ Add New" card temporarily
        cardGrid.removeView(addNewCard);
        // Add the new category card
        cardGrid.addView(newCardView);
        // Re-add the "+ Add New" card at the end
        GridLayout.LayoutParams addNewParams = new GridLayout.LayoutParams();
        addNewParams.width = 0;
        addNewParams.height = (int) (60 * getResources().getDisplayMetrics().density);
        addNewParams.setMargins(8, 8, 8, 8);
        addNewParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        addNewCard.setLayoutParams(addNewParams);
        cardGrid.addView(addNewCard);
    }

    //Navigate to another Activity, passing along the same username / full name / user_id.
    private void navigateTo(Class<?> targetActivity, String fullName, String username) {
        Intent intent = new Intent(this, targetActivity);
        intent.putExtra("FULL_NAME", fullName);
        intent.putExtra("USERNAME", username);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }

    /**
     * Setup the category grid with user-specific categories.
     */
    private void setupCategoryGrid(int userId) {
        cardGrid.removeAllViews();

        List<String> categories = db.getDistinctCategories(userId);
        if (categories != null && !categories.isEmpty() && selectedCategory == null) {
            selectedCategory = categories.get(0); // Default to first category
        }

        for (String category : categories) {
            boolean isSelected = category != null && category.equals(selectedCategory);
            addCategoryCard(category, isSelected);
        }

        // Ensure "+ Add New" card is added with consistent parameters
        if (addNewCard.getParent() == null) {
            GridLayout.LayoutParams addNewParams = new GridLayout.LayoutParams();
            addNewParams.width = 0;
            addNewParams.height = (int) (60 * getResources().getDisplayMetrics().density);
            addNewParams.setMargins(8, 8, 8, 8);
            addNewParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            addNewCard.setLayoutParams(addNewParams);
            cardGrid.addView(addNewCard);
        }
    }

    /**
     * Validate that the chosen frequency and taskDate make sense.
     * Returns false and shows a Toast if invalid; otherwise true.
     */
    private boolean validateFrequencyAndDate(String frequency, String taskDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date selectedDate = sdf.parse(taskDate);

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTime(selectedDate);
            selectedCal.set(Calendar.HOUR_OF_DAY, 0);
            selectedCal.set(Calendar.MINUTE, 0);
            selectedCal.set(Calendar.SECOND, 0);
            selectedCal.set(Calendar.MILLISECOND, 0);

            if (frequency.equalsIgnoreCase("daily")) {
                return true;
            }

            if (!selectedCal.after(todayCal)) {
                Toast.makeText(this, "Task date must be in the future for " + frequency + " frequency.", Toast.LENGTH_LONG).show();
                return false;
            }

            long diffMillis = selectedDate.getTime() - todayCal.getTimeInMillis();
            long diffDays = diffMillis / (24L * 60L * 60L * 1000L);
            long diffMonths = diffDays / 30L;

            switch (frequency.toLowerCase()) {
                case "every weekly (mon-fri)":
                case "weekly":
                    if (diffDays < 7) {
                        Toast.makeText(this, "Need at least 7 days for Weekly frequency.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    break;
                case "monthly":
                    if (diffMonths < 1) {
                        Toast.makeText(this, "Need at least 1 month for Monthly frequency.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    break;
                case "quarterly":
                    if (diffMonths < 3) {
                        Toast.makeText(this, "Need at least 3 months for Quarterly frequency.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    break;
                case "yearly":
                    if (diffMonths < 12) {
                        Toast.makeText(this, "Need at least 12 months for Yearly frequency.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    break;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid date format.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}