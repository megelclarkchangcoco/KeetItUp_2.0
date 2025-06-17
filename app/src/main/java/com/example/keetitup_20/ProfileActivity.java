package com.example.keetitup_20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileActivity
 *
 * - Displays user’s current Fullname and Username.
 * - Allows editing any or all of Fullname, Username, and Password.
 * - Shows a “mood” icon (imgProfile) based on task‐completion status.
 * - Toasts on successful edit (or no changes).
 * - Logs out on btnLogout.
 */
public class ProfileActivity extends AppCompatActivity {

    // UI references
    private ImageView    imgProfile;
    private TextView     tvDisplayName, tvFullName;
    private EditText     nameInput, usernameInput, passwordInput;
    private Button       btnEditProfile, btnLogout;

    // Passed‐in user data
    private String       fullName, username;
    private int          userId;

    // Database helper
    private DatabaseConnection db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1) Find all UI elements by ID
        imgProfile      = findViewById(R.id.imgProfile);
        tvDisplayName   = findViewById(R.id.tvDisplayName);
        tvFullName      = findViewById(R.id.tvFullName);
        nameInput       = findViewById(R.id.name_input);
        usernameInput   = findViewById(R.id.username_input);
        passwordInput   = findViewById(R.id.password_input);
        btnEditProfile  = findViewById(R.id.btnEditProfile);
        btnLogout       = findViewById(R.id.btnLogout);

        // 2) Get user data from Intent
        Intent intentIn = getIntent();
        fullName = intentIn.getStringExtra("FULL_NAME");
        username = intentIn.getStringExtra("USERNAME");
        userId   = intentIn.getIntExtra("USER_ID", -1);

        // 3) Initialize DatabaseConnection
        db = new DatabaseConnection(this);

        // 4) Populate TextViews and EditText hints
        if (fullName != null) {
            tvFullName.setText(fullName);
            nameInput.setHint(fullName);
        }
        if (username != null) {
            tvDisplayName.setText(username);
            usernameInput.setHint(username);
        }
        // For password, show placeholder
        passwordInput.setHint("*********");

        // 5) Disable editing initially
        nameInput.setEnabled(false);
        usernameInput.setEnabled(false);
        passwordInput.setEnabled(false);

        // 6) Compute counts to drive the mood icon
        int ongoingTasks   = db.countTasksByStatus(userId, "Ongoing");
        int completedTasks = db.countTasksByStatus(userId, "Complete");
        int totalTasks     = ongoingTasks + completedTasks;

        // 7) Set the mood icon based on those counts
        updateMoodIcon(imgProfile, completedTasks, ongoingTasks, totalTasks);

        // 8) “Edit Profile” button: enable fields and flip to “Save”
        btnEditProfile.setOnClickListener(this::beginEditing);

        // 9) “Log Out” button: clear backstack and return to LoginActivity
        btnLogout.setOnClickListener(v -> {
            Intent logoutIntent = new Intent(ProfileActivity.this, LoginActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(logoutIntent);
            finish();
        });

        // 10) Bottom‐nav highlighting (Profile is active)
        setupBottomNavigation();
    }

    private void beginEditing(View v) {
        nameInput.setEnabled(true);
        usernameInput.setEnabled(true);
        passwordInput.setEnabled(true);

        btnEditProfile.setText("Save");
        btnEditProfile.setOnClickListener(this::attemptSave);
    }

    private void attemptSave(View v) {
        String newFullName = nameInput.getText().toString().trim();
        String newUsername = usernameInput.getText().toString().trim();
        String newPassword = passwordInput.getText().toString().trim();

        boolean updated = db.updateUser(userId, newFullName, newUsername, newPassword);

        if (updated) {
            // If Fullname changed, update TextView and hint
            if (!newFullName.isEmpty()) {
                tvFullName.setText(newFullName);
                nameInput.setHint(newFullName);
                fullName = newFullName;
            }
            // If Username changed, update TextView and hint
            if (!newUsername.isEmpty()) {
                tvDisplayName.setText(newUsername);
                usernameInput.setHint(newUsername);
                username = newUsername;
            }
            // Clear password field and reset hint
            passwordInput.setText("");
            passwordInput.setHint("*********");

            Toast.makeText(ProfileActivity.this,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT).show();
        } else {
            // No fields changed (or update failed)
            Toast.makeText(ProfileActivity.this,
                    "No changes to save",
                    Toast.LENGTH_SHORT).show();
        }

        // Disable editing again
        nameInput.setEnabled(false);
        usernameInput.setEnabled(false);
        passwordInput.setEnabled(false);

        // Restore Edit Profile button text and listener
        btnEditProfile.setText("Edit Profile");
        btnEditProfile.setOnClickListener(this::beginEditing);
    }

    private void updateMoodIcon(ImageView bigIconImage,
                                int completedTasks,
                                int ongoingTasks,
                                int totalTasks) {
        if (ongoingTasks > 0) {
            if (completedTasks == 0) {
                bigIconImage.setImageResource(R.drawable.profile_angry);
            } else if (completedTasks >= 1 && completedTasks <= 2) {
                bigIconImage.setImageResource(R.drawable.profile_sick);
            } else if (completedTasks >= 3 && completedTasks <= 5) {
                bigIconImage.setImageResource(R.drawable.profile_sad);
            } else if (completedTasks >= 3) {
                bigIconImage.setImageResource(R.drawable.profile_happy);
            } else {
                bigIconImage.setImageResource(R.drawable.profile_neutral);
            }
        } else if (completedTasks == totalTasks && totalTasks > 0) {
            bigIconImage.setImageResource(R.drawable.profile_elated);
        } else {
            bigIconImage.setImageResource(R.drawable.profile_neutral);
        }
    }

    private void setupBottomNavigation() {
        LinearLayout navHome          = findViewById(R.id.nav_home);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navAdd           = findViewById(R.id.nav_add);
        LinearLayout navTasks         = findViewById(R.id.nav_tasks);
        LinearLayout navProfile       = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            if (fullName != null) intent.putExtra("FULL_NAME", fullName);
            if (username != null) intent.putExtra("USERNAME", username);
            if (userId != -1) intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });
        navNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, NotificationActivity.class);
            if (fullName != null) intent.putExtra("FULL_NAME", fullName);
            if (username != null) intent.putExtra("USERNAME", username);
            if (userId != -1) intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });
        navAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AddTaskActivity.class);
            if (fullName != null) intent.putExtra("FULL_NAME", fullName);
            if (username != null) intent.putExtra("USERNAME", username);
            if (userId != -1) intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });
        navTasks.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, TaskListActivity.class);
            if (fullName != null) intent.putExtra("FULL_NAME", fullName);
            if (username != null) intent.putExtra("USERNAME", username);
            if (userId != -1) intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        });

        // Highlight “Profile” (active)
        ImageView iconProfile = findViewById(R.id.icon_profile);
        TextView  textProfile = findViewById(R.id.text_profile);
        iconProfile.setColorFilter(getResources().getColor(R.color.black, null));
        textProfile.setTextColor(getResources().getColor(R.color.black, null));
    }
}
