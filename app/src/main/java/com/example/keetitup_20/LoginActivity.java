package com.example.keetitup_20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    EditText username_input, password_input;
    TextView toSignupButton;
    Button login_button;
    DatabaseConnection databaseConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        // Intialize the database connection
        databaseConnection = new DatabaseConnection(this);

        // Initialize UI components
        username_input = findViewById(R.id.username_input);
        password_input = findViewById(R.id.password_input);
        login_button = findViewById(R.id.login_button);
        toSignupButton = findViewById(R.id.toSignup_button);

        // Event Listener to Handle login button click
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get input values and trim spaces
                String username = username_input.getText().toString().trim();
                String password = password_input.getText().toString().trim();

                // Validate input
                if(username.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check credentials in database
                String fullname = databaseConnection.checkLogin(username, password);
                if(fullname != null){
                    // If login success, show welcome message
                    Toast.makeText(LoginActivity.this, "Welcome " + fullname, Toast.LENGTH_SHORT).show();
                } else {
                    // If login fails
                    Toast.makeText(LoginActivity.this, "Incorrect credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Navigate to sign up activity
        toSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

    }
}