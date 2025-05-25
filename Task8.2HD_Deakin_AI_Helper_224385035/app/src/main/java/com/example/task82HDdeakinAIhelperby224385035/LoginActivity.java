package com.example.task82HDdeakinAIhelperby224385035;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText studentIdInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerLink;
    
    // SharedPreferences for storing user data
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateBasedOnUserStatus();
            return;
        }
        
        // Initialize UI elements
        studentIdInput = findViewById(R.id.studentIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        
        // Set click listeners
        loginButton.setOnClickListener(v -> attemptLogin());
        registerLink.setOnClickListener(v -> openRegisterActivity());
    }
    
    private boolean isUserLoggedIn() {
        // Check if a user ID is stored in preferences
        return preferences.getBoolean("isLoggedIn", false);
    }
    
    private void attemptLogin() {
        // Get input values
        String studentId = studentIdInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        // Validate inputs
        if (TextUtils.isEmpty(studentId)) {
            studentIdInput.setError("Student ID is required");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }
        
        // Check credentials
        if (validateCredentials(studentId, password)) {
            // Save login state
            saveLoginState(studentId);
            
            // Navigate to appropriate screen
            navigateBasedOnUserStatus();
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean validateCredentials(String studentId, String password) {
        // Get stored credentials for the given student ID
        String storedPassword = preferences.getString("password_" + studentId, null);
        
        // Check if the student ID exists and password matches
        return storedPassword != null && storedPassword.equals(password);
    }
    
    private void saveLoginState(String studentId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("currentUserId", studentId);
        editor.apply();
    }
    
    private void navigateBasedOnUserStatus() {
        String studentId = preferences.getString("currentUserId", "");
        
        // Check if user has already selected interests
        boolean hasSelectedInterests = preferences.getBoolean("interests_selected_" + studentId, false);
        
        if (hasSelectedInterests) {
            // If interests are already selected, go to main activity
            startMainActivity();
        } else {
            // If interests are not selected, go to interests activity
            startInterestsActivity();
        }
    }
    
    private void openRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Close login activity
    }
    
    private void startInterestsActivity() {
        Intent intent = new Intent(this, InterestsActivity.class);
        startActivity(intent);
        finish(); // Close login activity
    }
} 