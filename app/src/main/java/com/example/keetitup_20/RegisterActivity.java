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

public class RegisterActivity extends AppCompatActivity {
    EditText registerName_input, registerUsername_input, registerPassword_input;
    Button register_button;
    TextView toSigninButton;

    DatabaseConnection dbConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize the database connection
        dbConnection = new DatabaseConnection(this);

        // Initialize UI components
        registerName_input = findViewById(R.id.registerName_input);
        registerUsername_input = findViewById(R.id.registerUsername_input);
        registerPassword_input = findViewById(R.id.registerPassword_input);
        register_button = findViewById(R.id.register_button);
        toSigninButton = findViewById(R.id.toSignin_button);

        // Event listener for register button
        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve the input from the EditText fields
                String Name = registerName_input.getText().toString().trim();
                String User = registerUsername_input.getText().toString().trim();
                String Pass = registerPassword_input.getText().toString().trim();

                // Check if any fields are empty
                if (Name.isEmpty() || User.isEmpty() || Pass.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields ", Toast.LENGTH_SHORT).show();
                } else {
                    // Check if the user already exists in the database
                    if (dbConnection.isUserExist(Name, User)) {
                        Toast.makeText(RegisterActivity.this, "Username or Fullname already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Create an instance of database helper and add the user
                        dbConnection.addUsers(Name, User, Pass);

                        // Clear the input fields after successful entry
                        registerName_input.setText("");
                        registerUsername_input.setText("");
                        registerPassword_input.setText("");

                        // Close the activity and go back after adding the user
                        finish();
                    }
                }
            }
        });

        // Event listener for navigate to sign in activity
        toSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
