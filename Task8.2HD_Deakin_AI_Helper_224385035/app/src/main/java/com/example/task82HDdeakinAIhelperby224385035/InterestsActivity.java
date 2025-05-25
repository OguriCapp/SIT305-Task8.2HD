package com.example.task82HDdeakinAIhelperby224385035;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class InterestsActivity extends AppCompatActivity {

    private CheckBox checkboxDegree, checkboxAccommodation, checkboxTransport, 
                     checkboxClubs, checkboxCulture, checkboxCloudDeakin,
                     checkboxFacilities, checkboxEvents, checkboxInternships;
    private Button continueButton;
    private TextView welcomeText;
    
    // SharedPreferences
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPrefs";
    
    // API Service
    private ApiService apiService;
    
    // User information
    private String username;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Initialize API Service
        apiService = new ApiService(this);
        
        // Get user information
        loadUserData();
        
        // Initialize UI elements
        welcomeText = findViewById(R.id.interestsWelcomeText);
        welcomeText.setText("Welcome, " + username + "! Please select your interests");
        
        // Initialize checkboxes
        checkboxDegree = findViewById(R.id.checkboxDegree);
        checkboxAccommodation = findViewById(R.id.checkboxAccommodation);
        checkboxTransport = findViewById(R.id.checkboxTransport);
        checkboxClubs = findViewById(R.id.checkboxClubs);
        checkboxCulture = findViewById(R.id.checkboxCulture);
        checkboxCloudDeakin = findViewById(R.id.checkboxCloudDeakin);
        checkboxFacilities = findViewById(R.id.checkboxFacilities);
        checkboxEvents = findViewById(R.id.checkboxEvents);
        checkboxInternships = findViewById(R.id.checkboxInternships);
        
        // Continue button
        continueButton = findViewById(R.id.continueButton);
        continueButton.setOnClickListener(v -> validateAndContinue());
    }
    
    private void loadUserData() {
        studentId = preferences.getString("currentUserId", "");
        username = preferences.getString("name_" + studentId, "Student");
    }
    
    private void validateAndContinue() {
        List<String> selectedInterests = getSelectedInterests();
        
        // Check if at least 3 interests are selected
        if (selectedInterests.size() < 3) {
            Toast.makeText(this, "Please select at least 3 interests", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save selected interests to API
        apiService.saveInterests(studentId, selectedInterests, new ApiService.ApiResponseListener<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                // Also save to SharedPreferences as fallback
                saveSelectedInterestsToPreferences(selectedInterests);
                
                // Start the main activity
                Intent intent = new Intent(InterestsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onError(String errorMessage) {
                // Save to SharedPreferences anyway
                saveSelectedInterestsToPreferences(selectedInterests);
                Toast.makeText(InterestsActivity.this, "Saved interests locally. " + errorMessage, Toast.LENGTH_SHORT).show();
                
                // Start the main activity
                Intent intent = new Intent(InterestsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    
    private List<String> getSelectedInterests() {
        List<String> interests = new ArrayList<>();
        
        if (checkboxDegree.isChecked()) interests.add("Degree");
        if (checkboxAccommodation.isChecked()) interests.add("Accommodation");
        if (checkboxTransport.isChecked()) interests.add("Transport");
        if (checkboxClubs.isChecked()) interests.add("Clubs");
        if (checkboxCulture.isChecked()) interests.add("Culture");
        if (checkboxCloudDeakin.isChecked()) interests.add("CloudDeakin");
        if (checkboxFacilities.isChecked()) interests.add("Facilities");
        if (checkboxEvents.isChecked()) interests.add("Events");
        if (checkboxInternships.isChecked()) interests.add("Internships");
        
        return interests;
    }
    
    private void saveSelectedInterestsToPreferences(List<String> interests) {
        SharedPreferences.Editor editor = preferences.edit();
        
        // First, clear previous interests
        editor.putInt("interests_count_" + studentId, interests.size());
        
        // Save each interest
        for (int i = 0; i < interests.size(); i++) {
            editor.putString("interest_" + studentId + "_" + i, interests.get(i));
        }
        
        // Mark interests as selected
        editor.putBoolean("interests_selected_" + studentId, true);
        
        // Apply changes
        editor.apply();
    }
} 