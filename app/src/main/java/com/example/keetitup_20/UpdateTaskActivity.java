package com.example.keetitup_20;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpdateTaskActivity extends AppCompatActivity {

    // UI references
    private EditText etTaskName, etTaskDescription;
    private AutoCompleteTextView dropdownFrequency;
    private TextView selectDateText, notifyDateText, notifyTimeText;
    private GridLayout cardGrid;
    private CardView addNewCard;
    private Button submitButton;

    // Data fields
    private int taskId, userId;
    private String oldLastCompletedDate, oldFrequency, oldNotifyBefore;
    private String oldTaskName, oldCategory, oldDescription;
    private String fullName, username;

    private DatabaseConnection db;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_task);

        db = new DatabaseConnection(this);

        // Retrieve from Intent extras
        taskId = getIntent().getIntExtra("TASK_ID", -1);
        userId = getIntent().getIntExtra("USER_ID", -1);
        fullName = getIntent().getStringExtra("FULL_NAME");
        username = getIntent().getStringExtra("USERNAME");

        if (taskId == -1 || userId == -1) {
            Toast.makeText(this, "Invalid Task or User ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind views
        etTaskName = findViewById(R.id.etTaskName);
        etTaskDescription = findViewById(R.id.task_description_input);
        dropdownFrequency = findViewById(R.id.auto_complete_text);
        selectDateText = findViewById(R.id.selectDate_text);
        notifyDateText = findViewById(R.id.notify_date_text);
        notifyTimeText = findViewById(R.id.notify_time_text);
        cardGrid = findViewById(R.id.cardGrid);
        addNewCard = findViewById(R.id.Addnew_card);
        submitButton = findViewById(R.id.submitButton);

        // Load existing task data from DB into the UI fields
        loadTaskData();

        // Set up the date picker for Last Completed Date
        setupDatePicker();

        // Set up the notify date and time pickers
        setupNotifyPickers();

        // Build the category grid: show all categories + highlight oldCategory
        setupCategoryGrid();

        // Frequency dropdown
        setupFrequencyDropdown();

        // Wire "Update Task" button
        submitButton.setOnClickListener(v -> onSubmit());

        // Wire "+ Add New" card
        addNewCard.setOnClickListener(v -> showAddNewCategoryDialog());

        // Bottom Navigation Listeners (ensure these activities exist)
        findViewById(R.id.nav_home).setOnClickListener(v -> navigateTo(HomeActivity.class));
        findViewById(R.id.nav_notifications).setOnClickListener(v -> navigateTo(NotificationActivity.class));
        findViewById(R.id.nav_add).setOnClickListener(v -> navigateTo(AddTaskActivity.class));
        findViewById(R.id.nav_tasks).setOnClickListener(v -> navigateTo(TaskListActivity.class));
        findViewById(R.id.nav_profile).setOnClickListener(v -> navigateTo(ProfileActivity.class));
    }

    private void loadTaskData() {
        Map<String, String> task = db.getTaskById(taskId);
        if (task == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        oldTaskName = task.get("task_name");
        oldCategory = task.get("category");
        oldDescription = task.get("description");
        oldLastCompletedDate = task.get("last_completed_date");
        oldFrequency = task.get("frequency");
        oldNotifyBefore = task.get("notify_before");

        etTaskName.setText(oldTaskName);
        etTaskDescription.setText(oldDescription);

        if (!TextUtils.isEmpty(oldLastCompletedDate)) {
            selectDateText.setText(oldLastCompletedDate);
        } else {
            selectDateText.setText("Select Date");
        }

        dropdownFrequency.setText(oldFrequency != null ? oldFrequency : "", false);

        // Parse notifyBefore into date and time
        if (oldNotifyBefore != null && !oldNotifyBefore.isEmpty()) {
            String[] parts = oldNotifyBefore.split(" ");
            if (parts.length == 2) {
                notifyDateText.setText(parts[0]);
                notifyTimeText.setText(parts[1]);
            } else {
                notifyDateText.setText("Select Notify Date");
                notifyTimeText.setText("Select Notify Time");
            }
        } else {
            notifyDateText.setText("Select Notify Date");
            notifyTimeText.setText("Select Notify Time");
        }
    }

    private void setupDatePicker() {
        selectDateText.setOnClickListener(v -> {
            final Calendar cal = Calendar.getInstance();
            String currentText = selectDateText.getText().toString();
            if (!TextUtils.isEmpty(currentText) && !currentText.equals("Select Date")) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    cal.setTime(sdf.parse(currentText));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    UpdateTaskActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        String dateSelected = String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%04d",
                                dayOfMonth,
                                month1 + 1,
                                year1
                        );
                        selectDateText.setText(dateSelected);
                    },
                    year, month, day
            );
            dialog.show();
        });
    }

    private void setupNotifyPickers() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        notifyDateText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    UpdateTaskActivity.this,
                    (view, y, m, d) -> {
                        m += 1;
                        String selectedNotifyDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y);
                        notifyDateText.setText(selectedNotifyDate);
                    },
                    year, month, day
            );
            dialog.show();
        });

        notifyTimeText.setOnClickListener(v -> {
            TimePickerDialog timeDialog = new TimePickerDialog(
                    UpdateTaskActivity.this,
                    (view, h, m) -> {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                        notifyTimeText.setText(selectedTime);
                    },
                    hour, minute,
                    true
            );
            timeDialog.show();
        });
    }

    private void setupCategoryGrid() {
        cardGrid.removeAllViews();

        // Load all unique categories from the database
        List<String> categories = db.getDistinctCategories(userId); // Pass userId

        // Add all categories, highlighting the current one
        for (String category : categories) {
            boolean isSelected = category != null && category.equals(oldCategory);
            addCategoryCard(category, isSelected);
        }

        // Add the "+ Add New" card at the end with consistent parameters
        GridLayout.LayoutParams addNewParams = new GridLayout.LayoutParams();
        addNewParams.width = 0; // Use weight to stretch
        addNewParams.height = (int) (60 * getResources().getDisplayMetrics().density);
        addNewParams.setMargins(8, 8, 8, 8);
        addNewParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Equal weight for columns
        addNewCard.setLayoutParams(addNewParams);
        cardGrid.addView(addNewCard);
    }

    private void addCategoryCard(String categoryName, boolean isSelected) {
        try {
            // Inflate the card layout from category_card_layout.xml
            View newCardView = LayoutInflater.from(this).inflate(R.layout.category_card_layout, cardGrid, false);
            TextView categoryText = newCardView.findViewById(R.id.categoryText);
            CardView categoryCard = newCardView.findViewById(R.id.myCardView);

            // Set the text
            categoryText.setText(categoryName != null ? categoryName : "");

            // Apply CardView properties to match "+ Add New" design
            categoryCard.setRadius(8f); // Rounded corners
            categoryCard.setCardElevation(4f); // Shadow/elevation effect
            categoryCard.setPadding(16, 16, 16, 16); // Consistent padding

            // Set layout parameters for the GridLayout
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Use weight to stretch
            params.height = (int) (60 * getResources().getDisplayMetrics().density);
            params.setMargins(8, 8, 8, 8);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Equal weight for columns
            newCardView.setLayoutParams(params);

            // Highlight if this is the selected category
            if (isSelected) {
                categoryCard.setCardBackgroundColor(Color.parseColor("#FFB300")); // Yellow highlight
            } else {
                categoryCard.setCardBackgroundColor(Color.WHITE); // Default color with shadow
            }

            // Add click listener to select the category
            newCardView.setOnClickListener(v -> {
                // Reset background of all cards
                for (int i = 0; i < cardGrid.getChildCount(); i++) {
                    View child = cardGrid.getChildAt(i);
                    if (child instanceof CardView) {
                        ((CardView) child).setCardBackgroundColor(Color.WHITE);
                    }
                }
                // Highlight the selected card
                categoryCard.setCardBackgroundColor(Color.parseColor("#FFB300"));
                oldCategory = categoryName; // Ensure oldCategory is updated globally
            });

            // Long press to remove
            newCardView.setOnLongClickListener(v -> {
                // Create a Dialog with custom layout
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.popup_delete_category);

                // Make dialog background transparent (optional, for better appearance)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // Find views in the custom layout
                Button cancelButton = dialog.findViewById(R.id.cancel_button);
                Button deleteButton = dialog.findViewById(R.id.delete_button);

                // Set up Cancel button
                cancelButton.setOnClickListener(view -> dialog.dismiss());

                // Set up Delete button
                deleteButton.setOnClickListener(view -> {
                    cardGrid.removeView(newCardView);
                    Toast.makeText(this, "Category removed", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

                // Adjust dialog width to match screen before showing
                WindowManager.LayoutParams dialogParams = dialog.getWindow().getAttributes();
                dialogParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                dialogParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                dialog.getWindow().setAttributes(dialogParams);

                // Show the dialog
                dialog.show();

                return true;
            });

            cardGrid.addView(newCardView);
        } catch (Exception e) {
            Toast.makeText(this, "Error adding category card: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showAddNewCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Request the window to remove the title bar
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        EditText categoryNameInput = dialogView.findViewById(R.id.category_name_input);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button submitBtn = dialogView.findViewById(R.id.submitButton);

        // Hide keyboard + dismiss
        View.OnClickListener hideKeyboardAndDismiss = v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(categoryNameInput.getWindowToken(), 0);
            dialog.dismiss();
        };
        cancelButton.setOnClickListener(hideKeyboardAndDismiss);

        submitBtn.setOnClickListener(v -> {
            String newCategory = categoryNameInput.getText().toString().trim();
            if (!newCategory.isEmpty()) {
                // Check for duplicates in database and cardGrid
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
                if (existingCategories.contains(newCategory)) {
                    Toast.makeText(this, "Category '" + newCategory + "' already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Since there's no separate Categories table, this is a no-op for now
                db.insertCategory(newCategory);

                // Reset all cards before adding new one
                for (int i = 0; i < cardGrid.getChildCount(); i++) {
                    View child = cardGrid.getChildAt(i);
                    if (child instanceof CardView) {
                        ((CardView) child).setCardBackgroundColor(Color.WHITE);
                    }
                }
                oldCategory = null; // Clear selection until clicked

                // Remove the "+ Add New" card temporarily
                cardGrid.removeView(addNewCard);
                // Add the new category card without highlight
                addCategoryCard(newCategory, false);
                // Re-add the "+ Add New" card at the end
                GridLayout.LayoutParams addNewParams = new GridLayout.LayoutParams();
                addNewParams.width = 0; // Use weight to stretch
                addNewParams.height = (int) (60 * getResources().getDisplayMetrics().density);
                addNewParams.setMargins(8, 8, 8, 8);
                addNewParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Equal weight for columns
                addNewCard.setLayoutParams(addNewParams);
                cardGrid.addView(addNewCard);

                Toast.makeText(this, "Category '" + newCategory + "' added!", Toast.LENGTH_SHORT).show();
                hideKeyboardAndDismiss.onClick(v);
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show();
            }
        });

        // Adjust dialog width to match screen
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Optional: match background style
        }

        // Show the dialog
        dialog.show();
    }

    private void setupFrequencyDropdown() {
        String[] frequencies = getResources().getStringArray(R.array.taskfrequency);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                frequencies
        );
        dropdownFrequency.setAdapter(adapter);
    }

    private void onSubmit() {
        String newTaskName = etTaskName.getText().toString().trim();
        String newDescription = etTaskDescription.getText().toString().trim();
        String newCategory = oldCategory != null ? oldCategory : "";
        String newLastCompletedDate = selectDateText.getText().toString().trim();
        String newFrequency = dropdownFrequency.getText().toString().trim();
        String newNotifyDate = notifyDateText.getText().toString().trim();
        String newNotifyTime = notifyTimeText.getText().toString().trim();

        // Basic validations
        if (newTaskName.isEmpty()) {
            etTaskName.setError("Task name is required");
            etTaskName.requestFocus();
            return;
        }
        if (newCategory.isEmpty()) {
            Toast.makeText(this, "Please select or add a category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newFrequency.isEmpty()) {
            Toast.makeText(this, "Please select a frequency", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newNotifyDate.isEmpty() || newNotifyDate.equals("Select Notify Date")) {
            Toast.makeText(this, "Please select a notify date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newNotifyTime.isEmpty() || newNotifyTime.equals("Select Notify Time")) {
            Toast.makeText(this, "Please select a notify time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(newLastCompletedDate) || newLastCompletedDate.equals("Select Date")) {
            Toast.makeText(this, "Please select last completed date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine notify date and time
        String newNotifyBefore = newNotifyDate + " " + newNotifyTime;

        // Fetch current task data to compare
        Map<String, String> currentTask = db.getTaskById(taskId);
        if (currentTask == null) {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentTaskName = currentTask.get("task_name");
        String currentCategory = currentTask.get("category");
        String currentDescription = currentTask.get("description");
        String currentLastCompletedDate = currentTask.get("last_completed_date");
        String currentFrequency = currentTask.get("frequency");
        String currentNotifyBefore = currentTask.get("notify_before");

        // Check for actual changes
        boolean hasChanges = !newTaskName.equals(currentTaskName)
                || !newCategory.equals(currentCategory)
                || !newDescription.equals(currentDescription)
                || !newLastCompletedDate.equals(currentLastCompletedDate)
                || !newFrequency.equalsIgnoreCase(currentFrequency)
                || !newNotifyBefore.equalsIgnoreCase(currentNotifyBefore);

        boolean updated = false;

        // Perform updates only if there are changes
        if (hasChanges) {
            if (!newTaskName.equals(currentTaskName) || !newCategory.equals(currentCategory) || !newDescription.equals(currentDescription)) {
                updated = db.updateTaskDetails(taskId, newTaskName, newCategory, newDescription) || updated;
            }
            if (!newLastCompletedDate.equals(currentLastCompletedDate) || !newFrequency.equalsIgnoreCase(currentFrequency) || !newNotifyBefore.equalsIgnoreCase(currentNotifyBefore)) {
                updated = db.updateTaskDateFrequencyNotify(taskId, newLastCompletedDate, newFrequency, newNotifyBefore) || updated;
            }
        }

        if (updated) {
            Toast.makeText(this, "Task, including all updated fields, updated successfully", Toast.LENGTH_SHORT).show();
            navigateTo(TaskListActivity.class);
        } else {
            Toast.makeText(this, "No changes detected or update failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(UpdateTaskActivity.this, targetActivity);
        intent.putExtra("FULL_NAME", fullName);
        intent.putExtra("USERNAME", username);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}