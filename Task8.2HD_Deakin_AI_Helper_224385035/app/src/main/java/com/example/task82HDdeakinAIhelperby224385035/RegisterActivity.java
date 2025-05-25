package com.example.task82HDdeakinAIhelperby224385035;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText studentIdInput;
    private EditText emailInput;
    private AutoCompleteTextView campusInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private TextView loginLink;
    
    // SharedPreferences for storing user data
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Initialize UI elements
        nameInput = findViewById(R.id.nameInput);
        studentIdInput = findViewById(R.id.registerStudentIdInput);
        emailInput = findViewById(R.id.emailInput);
        campusInput = findViewById(R.id.campusInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        
        // Set up campus dropdown
        setupCampusDropdown();
        
        // Set click listeners
        registerButton.setOnClickListener(v -> attemptRegistration());
        loginLink.setOnClickListener(v -> finish()); // Go back to login
    }
    
    private void setupCampusDropdown() {
        String[] campusOptions = getResources().getStringArray(R.array.campus_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                campusOptions
        );
        campusInput.setAdapter(adapter);
    }
    
    private void attemptRegistration() {
        // Get input values
        String name = nameInput.getText().toString().trim();
        String studentId = studentIdInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String campus = campusInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        
        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            return;
        }
        
        if (TextUtils.isEmpty(studentId)) {
            studentIdInput.setError("Student ID is required");
            return;
        }
        
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }
        
        if (!isValidEmail(email)) {
            emailInput.setError("Invalid email format");
            return;
        }
        
        if (TextUtils.isEmpty(campus)) {
            campusInput.setError("Campus is required");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }
        
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }
        
        // Check if student ID already exists
        if (isStudentIdRegistered(studentId)) {
            studentIdInput.setError("Student ID already registered");
            return;
        }
        
        // Save user data
        saveUserData(name, studentId, email, campus, password);
        
        // Show success message
        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
        
        // Redirect to login
        finish();
    }
    
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    private boolean isStudentIdRegistered(String studentId) {
        return preferences.contains("password_" + studentId);
    }
    
    private void saveUserData(String name, String studentId, String email, String campus, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("name_" + studentId, name);
        editor.putString("email_" + studentId, email);
        editor.putString("campus_" + studentId, campus);
        editor.putString("password_" + studentId, password);
        editor.apply();
    }
} 