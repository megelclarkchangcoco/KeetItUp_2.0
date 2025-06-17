package com.example.keetitup_20;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TaskShareActivity extends AppCompatActivity {
    TextView tvUserName, tvTaskName;
    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_share);

        tvUserName = findViewById(R.id.tv_user_name);
        tvTaskName = findViewById(R.id.tv_task_completed);
        btnBack = findViewById(R.id.btn_back);

        // Get extras
        String fullName = getIntent().getStringExtra("FULL_NAME");
        String firstName = fullName != null ? fullName.split(" ")[0] : "Friend";
        String taskName = getIntent().getStringExtra("TASK_NAME");

        if (fullName != null) {
            tvUserName.setText("Keep It Up! " + firstName + "!");
        }

        if (taskName != null) {
            tvTaskName.setText("You Completed of your " + taskName + " Task");
        }

        btnBack.setOnClickListener(v -> {
            finish(); // go back to TaskDetailsActivity
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
