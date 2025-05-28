package com.example.keetitup_20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize navigation items
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navAdd = findViewById(R.id.nav_add);
        LinearLayout navTasks = findViewById(R.id.nav_tasks);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        // Retrieve the full name from the intent safely
        Intent intent2 = getIntent();
        String fullName = intent2.getStringExtra("FULL_NAME");
        String firstName = "";
        if (fullName != null && !fullName.isEmpty()) {
            firstName = fullName.split(" ")[0];
        }
        TextView headerNameText = findViewById(R.id.welcome_text);
        headerNameText.setText("Keep It Up, " +firstName + "!");
        // Set up click listeners for navigation items
        navHome.setOnClickListener(v -> {
            // Already in HomeActivity, no action needed
        });

        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
            if (fullName != null) {
                intent.putExtra("FULL_NAME", fullName);
            }
            startActivity(intent);
            finish(); // Close HomeActivity
        });

        navAdd.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddTaskActivity.class);
            if (fullName != null) {
                intent.putExtra("FULL_NAME", fullName);
            }
            startActivity(intent);
            finish(); // Close HomeActivity
        });

//        navTasks.setOnClickListener(v -> {
//            Intent intent = new Intent(HomeActivity.this, TasksListActivity.class);
//            if (fullName != null) {
//                intent.putExtra("FULL_NAME", fullName);
//            }
//            startActivity(intent);
//            finish(); // Close HomeActivity
//        });

//        navProfile.setOnClickListener(v -> {
//            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
//            if (fullName != null) {
//                intent.putExtra("FULL_NAME", fullName);
//            }
//            startActivity(intent);
//            finish(); // Close HomeActivity
//        });

        // Optionally update UI to indicate Home is selected
        updateNavigationState();
    }

    private void updateNavigationState() {
        // Highlight Home icon/text as selected
        ImageView iconHome = findViewById(R.id.icon_home);
        TextView textHome = findViewById(R.id.text_home);
        iconHome.setColorFilter(getResources().getColor(R.color.black, null)); // Assuming a color resource for active state
        textHome.setTextColor(getResources().getColor(R.color.black, null));
    }
}