package com.example.keetitup_20;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.Calendar;

public class AddTaskActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTextView;
    private AutoCompleteTextView categoryDropdown;
    private TextView selectDate_text;
    private EditText taskNameInput;
    private EditText taskDescriptionInput;
    private CardView addNewCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addtask);

        // Initialize views
        selectDate_text = findViewById(R.id.selectDate_text);
        taskNameInput = findViewById(R.id.task_name_input);
        taskDescriptionInput = findViewById(R.id.task_description_input);
        addNewCard = findViewById(R.id.Addnew_card);

        // Initialize navigation items from button_customnav.xml
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navAdd = findViewById(R.id.nav_add);
        LinearLayout navTasks = findViewById(R.id.nav_tasks);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        // Retrieve FULL_NAME from intent
        String fullName = getIntent().getStringExtra("FULL_NAME");

        // Add New Category Card Click Listener
        addNewCard.setOnClickListener(v -> showAddCategoryDialog());

        // Date picker setup
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        selectDate_text.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(AddTaskActivity.this, (view, y, m, d) -> {
                m += 1;
                String selectedDate = d + "/" + m + "/" + y;
                selectDate_text.setText(selectedDate);
            }, year, month, day);
            dialog.show();
        });

        // Frequency dropdown
        autoCompleteTextView = findViewById(R.id.auto_complete_text);
        String[] frequencies = getResources().getStringArray(R.array.taskfrequency);
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, frequencies);
        autoCompleteTextView.setAdapter(freqAdapter);

        // Notification dropdown
        categoryDropdown = findViewById(R.id.selectnotify_Text);
        String[] notifyOptions = getResources().getStringArray(R.array.notificationCategory);
        ArrayAdapter<String> notifyAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, notifyOptions);
        categoryDropdown.setAdapter(notifyAdapter);

        // Navigation setup
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(AddTaskActivity.this, HomeActivity.class);
            if (fullName != null) {
                intent.putExtra("FULL_NAME", fullName);
            }
            startActivity(intent);
            finish();
        });

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(AddTaskActivity.this, NotificationActivity.class);
            if (fullName != null) {
                intent.putExtra("FULL_NAME", fullName);
            }
            startActivity(intent);
            finish();
        });

        navAdd.setOnClickListener(v -> {
            // Already in AddTaskActivity, validate and save task
            String taskName = taskNameInput.getText().toString();
            String taskDescription = taskDescriptionInput.getText().toString();
            String taskDate = selectDate_text.getText().toString();
            String frequency = autoCompleteTextView.getText().toString();
            String category = categoryDropdown.getText().toString();

            // Simple validation
            if (taskName.isEmpty() || taskDescription.isEmpty() || taskDate.isEmpty()) {
                Toast.makeText(AddTaskActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                Intent homeIntent = new Intent(AddTaskActivity.this, HomeActivity.class);
                homeIntent.putExtra("FULL_NAME", fullName);
                startActivity(homeIntent);
                finish();
            }
        });

        navTasks.setOnClickListener(v -> {
            Intent intent = new Intent(AddTaskActivity.this, TaskListActivity.class);
            if (fullName != null) {
                intent.putExtra("FULL_NAME", fullName);
            }
            startActivity(intent);
            finish();
        });

//        navProfile.setOnClickListener(v -> {
//            Intent intent = new Intent(AddTaskActivity.this, ProfileActivity.class);
//            if (fullName != null) {
//                intent.putExtra("FULL_NAME", fullName);
//            }
//            startActivity(intent);
//            finish();
//        });

        // Update UI to indicate Add is selected

    }



    private void showAddCategoryDialog() {
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Find views in the dialog
        EditText categoryNameInput = dialogView.findViewById(R.id.category_name_input);
        AutoCompleteTextView iconDropdown = dialogView.findViewById(R.id.icon_dropdown);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button submitButton = dialogView.findViewById(R.id.submitButton);

        // Setup icon dropdown
        String[] iconOptions = {
                "🎂 Birthday",
                "🚗 Vehicle",
                "🐕 Pet Care",
                "🏠 Home",
                "💪 Fitness",
                "💼 Work",
                "🍎 Health",
                "🎓 Education",
                "💰 Finance",
                "🎮 Entertainment"
        };
        ArrayAdapter<String> iconAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, iconOptions);
        iconDropdown.setAdapter(iconAdapter);

        // Cancel button action
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Submit button action
        submitButton.setOnClickListener(v -> {
            String categoryName = categoryNameInput.getText().toString().trim();
            String selectedIcon = iconDropdown.getText().toString().trim();

            if (categoryName.isEmpty()) {
                Toast.makeText(AddTaskActivity.this, "Please enter a category name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedIcon.isEmpty()) {
                Toast.makeText(AddTaskActivity.this, "Please select an icon", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Save the new category to your database
            Toast.makeText(AddTaskActivity.this,
                    "Category '" + categoryName + "' added successfully!",
                    Toast.LENGTH_SHORT).show();

            dialog.dismiss();
        });

        dialog.show();
    }
}