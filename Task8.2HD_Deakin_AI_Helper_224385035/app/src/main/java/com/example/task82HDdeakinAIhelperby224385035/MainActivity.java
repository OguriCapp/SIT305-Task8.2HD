package com.example.task82HDdeakinAIhelperby224385035;
//Deakin University AI Student Helper using Llama-2 by 224385035
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Main Activity class for the Deakin University AI Student Helper app
public class MainActivity extends AppCompatActivity {

    // UI elements
    private TextView welcomeText;
    private Button logoutButton;
    private Button startChatButton;
    private Button shareButton;
    private TextView noHistoryText;
    private LinearLayout historyContainer;
    private TextView recommendationTitle1, recommendationTitle2, recommendationTitle3;
    private TextView recommendationText1, recommendationText2, recommendationText3;
    private CardView recommendationCard1, recommendationCard2, recommendationCard3;
    
    // SharedPreferences for storing user data
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPrefs";
    
    // User information
    private String username;              
    private String studentId;
    private List<String> userInterests = new ArrayList<>();
    
    // API Service
    private ApiService apiService;
    
    // Volley request queue
    private RequestQueue requestQueue;

    // To setup UI parts and button clicks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Initialize API Service
        apiService = new ApiService(this);
        
        // Check if user is logged in
        if (!isUserLoggedIn()) {
            startLoginActivity();
            return;
        }
        
        // Get user information
        loadUserData();
        
        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Initialize UI elements
        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);
        startChatButton = findViewById(R.id.startChatButton);
        shareButton = findViewById(R.id.shareButton);
        noHistoryText = findViewById(R.id.noHistoryText);
        historyContainer = findViewById(R.id.historyContainer);
        
        // Initialize recommendation text views
        recommendationTitle1 = findViewById(R.id.recommendationTitle1);
        recommendationTitle2 = findViewById(R.id.recommendationTitle2);
        recommendationTitle3 = findViewById(R.id.recommendationTitle3);
        recommendationText1 = findViewById(R.id.recommendationText1);
        recommendationText2 = findViewById(R.id.recommendationText2);
        recommendationText3 = findViewById(R.id.recommendationText3);
        recommendationCard1 = findViewById(R.id.recommendationCard1);
        recommendationCard2 = findViewById(R.id.recommendationCard2);
        recommendationCard3 = findViewById(R.id.recommendationCard3);

        // Set welcome message with user's name
        welcomeText.setText("Welcome, " + username);
        
        // Set click listeners
        logoutButton.setOnClickListener(v -> logout());
        startChatButton.setOnClickListener(v -> startChatActivity());
        shareButton.setOnClickListener(v -> shareApp());
        
        // Set click listeners for recommendation cards
        recommendationCard1.setOnClickListener(v -> openRecommendation(0));
        recommendationCard2.setOnClickListener(v -> openRecommendation(1));
        recommendationCard3.setOnClickListener(v -> openRecommendation(2));
        
        // Load recommendations based on user interests
        loadRecommendations();
        
        // Load chat history
        loadChatHistory();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload chat history in case it was updated
        loadChatHistory();
    }
    
    private boolean isUserLoggedIn() {
        return preferences.getBoolean("isLoggedIn", false);
    }
    
    private void loadUserData() {
        studentId = preferences.getString("currentUserId", "");
        username = preferences.getString("name_" + studentId, "Student");
        
        // Load user interests from the API
        apiService.getInterests(studentId, new ApiService.ApiResponseListener<List<String>>() {
            @Override
            public void onSuccess(List<String> interests) {
                userInterests.clear();
                userInterests.addAll(interests);
                loadRecommendations();
            }
            
            @Override
            public void onError(String errorMessage) {
                // If API fails, try to load from SharedPreferences as fallback
                loadInterestsFromPreferences();
            }
        });
    }
    
    private void loadInterestsFromPreferences() {
        // Load interests from SharedPreferences as before
        userInterests.clear();
        int interestsCount = preferences.getInt("interests_count_" + studentId, 0);
        for (int i = 0; i < interestsCount; i++) {
            userInterests.add(preferences.getString("interest_" + studentId + "_" + i, ""));
        }
        loadRecommendations();
    }
    
    private void logout() {
        // Clear login state
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("currentUserId");
        editor.apply();
        
        // Start login activity
        startLoginActivity();
        finish();
    }
    
    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void startChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }
    
    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Deakin University AI Student Helper");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "I'm using the Deakin University AI Student Helper app. It uses Llama-2 AI to answer questions about Deakin University! You should try it too!");
        startActivity(Intent.createChooser(shareIntent, "Share App"));
    }
    
    private void loadRecommendations() {
        // If user has no interests, use default recommendations
        if (userInterests.isEmpty()) {
            setDefaultRecommendations();
            return;
        }
        
        // Generate three questions based on user interests
        for (int i = 0; i < Math.min(3, userInterests.size()); i++) {
            generateRecommendation(userInterests.get(i), i);
        }
        
        // If user has less than 3 interests, fill remaining with defaults
        for (int i = userInterests.size(); i < 3; i++) {
            setDefaultRecommendation(i);
        }
    }
    
    private void generateRecommendation(String interest, int index) {
        final String prompt;
        
        switch (interest) {
            case "Degree":
                prompt = "What are the features of degrees and courses at Deakin University?";
                break;
            case "Accommodation":
                prompt = "What accommodation options are available at Deakin University?";
                break;
            case "Transport":
                prompt = "How to use public transport to reach Deakin University campuses?";
                break;
            case "Clubs":
                prompt = "What student clubs can I join at Deakin University?";
                break;
            case "Culture":
                prompt = "What is the campus culture like at Deakin University?";
                break;
            case "CloudDeakin":
                prompt = "How to effectively use the Cloud Deakin learning platform?";
                break;
            case "Facilities":
                prompt = "What are the main campus facilities at Deakin University?";
                break;
            case "Events":
                prompt = "What annual campus events are held at Deakin University?";
                break;
            case "Internships":
                prompt = "What internship and employment opportunities does Deakin University offer?";
                break;
            default:
                setDefaultRecommendation(index);
                return;
        }
        
        // Set the title
        setRecommendationTitle(index, prompt);
        
        // Get recommendation from Llama-2
        String url = "http://10.0.2.2:5000/chat";
        
        StringRequest request = new StringRequest(
                Request.Method.POST, 
                url,
                response -> {
                    String botMessage = response.trim();
                    // Limit to first two sentences
                    String summary = limitToFirstTwoSentences(botMessage);
                    setRecommendationText(index, summary);
                },
                error -> {
                    setRecommendationText(index, "Unable to get recommendations. Please check your network connection.");
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userMessage", prompt);
                return params;
            }
        };

        // Set timeout
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Add to queue
        requestQueue.add(request);
    }
    
    private String limitToFirstTwoSentences(String text) {
        String[] sentences = text.split("[.!?]+");
        if (sentences.length <= 2) {
            return text;
        }
        return sentences[0].trim() + ". " + sentences[1].trim() + "...";
    }
    
    private void setRecommendationTitle(int index, String title) {
        switch (index) {
            case 0:
                recommendationTitle1.setText(title);
                break;
            case 1:
                recommendationTitle2.setText(title);
                break;
            case 2:
                recommendationTitle3.setText(title);
                break;
        }
    }
    
    private void setRecommendationText(int index, String text) {
        switch (index) {
            case 0:
                recommendationText1.setText(text);
                break;
            case 1:
                recommendationText2.setText(text);
                break;
            case 2:
                recommendationText3.setText(text);
                break;
        }
    }
    
    private void setDefaultRecommendations() {
        setDefaultRecommendation(0);
        setDefaultRecommendation(1);
        setDefaultRecommendation(2);
    }
    
    private void setDefaultRecommendation(int index) {
        switch (index) {
            case 0:
                recommendationTitle1.setText("How many campuses does Deakin University have?");
                recommendationText1.setText("Deakin University has four main campuses: Melbourne Burwood campus, Geelong Waurn Ponds campus, Geelong Waterfront campus, and Warrnambool campus. Each campus has different programs and facilities...");
                break;
            case 1:
                recommendationTitle2.setText("How to log in to Cloud Deakin?");
                recommendationText2.setText("To log in to Cloud Deakin, you need to visit deakin.edu.au/students, then click on the 'CloudDeakin' link. Use your Deakin username and password to log into the system...");
                break;
            case 2:
                recommendationTitle3.setText("What services does Deakin University Student Association (DUSA) provide?");
                recommendationText3.setText("Deakin University Student Association (DUSA) provides various services including academic support, legal advice, social activities, clubs and society organizations, student representation, and more...");
                break;
        }
    }
    
    private void openRecommendation(int index) {
        Intent intent = new Intent(this, ChatActivity.class);
        switch (index) {
            case 0:
                intent.putExtra("initial_question", recommendationTitle1.getText().toString());
                break;
            case 1:
                intent.putExtra("initial_question", recommendationTitle2.getText().toString());
                break;
            case 2:
                intent.putExtra("initial_question", recommendationTitle3.getText().toString());
                break;
        }
        startActivity(intent);
    }
    
    private void loadChatHistory() {
        // Clear current history container
        historyContainer.removeAllViews();
        historyContainer.addView(noHistoryText);
        
        // Load history from API
        apiService.getChatHistory(studentId, new ApiService.ApiResponseListener<List<JSONObject>>() {
            @Override
            public void onSuccess(List<JSONObject> historyList) {
                if (historyList.isEmpty()) {
                    return; // No history
                }
                
                // Hide no history text
                noHistoryText.setVisibility(View.GONE);
                
                // Add history items to UI
                for (int i = 0; i < historyList.size(); i++) {
                    try {
                        JSONObject historyEntry = historyList.get(i);
                        String messageId = historyEntry.getString("messageId");
                        String userMessage = historyEntry.getString("userMessage");
                        String timestamp = historyEntry.getString("timestamp");
                        
                        // Create title (use first user message)
                        String title = userMessage.length() > 30 ? 
                                userMessage.substring(0, 27) + "..." : 
                                userMessage;
                                
                        // Add history item to view
                        addHistoryItemToView(title, timestamp, messageId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                // If API fails, try to load from SharedPreferences as fallback
                loadHistoryFromPreferences();
            }
        });
    }
    
    private void loadHistoryFromPreferences() {
        // Get history count
        int historyCount = preferences.getInt("history_count_" + studentId, 0);
        
        if (historyCount == 0) {
            return; // No history
        }
        
        // Hide no history text
        noHistoryText.setVisibility(View.GONE);
        
        // Load history from newest to oldest
        for (int i = historyCount - 1; i >= 0; i--) {
            String historyJson = preferences.getString("history_" + studentId + "_" + i, "");
            if (!historyJson.isEmpty()) {
                try {
                    addHistoryItem(historyJson, i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void addHistoryItem(String historyJson, int index) throws JSONException {
        // Parse JSON string to JSON object
        JSONObject historyEntry = new JSONObject(historyJson);
        
        // Extract necessary information
        String messageId = historyEntry.optString("messageId", "local_" + index);
        String userMessage = historyEntry.getString("userMessage");
        String timestamp = historyEntry.optString("timestamp", new Date().toString());
        
        // Create title (use first user message)
        String title = userMessage.length() > 30 ? 
                userMessage.substring(0, 27) + "..." : 
                userMessage;
                
        // Add history item to view
        addHistoryItemToView(title, timestamp, messageId);
    }
    
    private void addHistoryItemToView(String title, String timestamp, String historyId) {
        // Create history item view
        View historyItemView = LayoutInflater.from(this).inflate(R.layout.item_history, historyContainer, false);
        
        // Set title and date
        TextView titleView = historyItemView.findViewById(R.id.historyTitle);
        TextView dateView = historyItemView.findViewById(R.id.historyDate);
        
        titleView.setText(title);
        dateView.setText(formatTimestamp(timestamp));
        
        // Set click listener
        historyItemView.setOnClickListener(v -> openHistoryChat(historyId));
        
        // Set delete button click listener
        ImageView deleteButton = historyItemView.findViewById(R.id.deleteHistoryButton);
        deleteButton.setOnClickListener(v -> deleteHistoryEntry(historyId));
        
        // Add to container
        historyContainer.addView(historyItemView);
    }
    
    private void deleteHistoryEntry(String historyId) {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete History")
            .setMessage("Are you sure you want to delete this conversation?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Call API to delete history
                apiService.deleteChatHistoryEntry(studentId, historyId, new ApiService.ApiResponseListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(MainActivity.this, "History deleted successfully", Toast.LENGTH_SHORT).show();
                        // Reload history
                        loadChatHistory();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // Try to delete from local preferences if it's a local ID
                        if (historyId.startsWith("local_")) {
                            try {
                                int historyIndex = Integer.parseInt(historyId.replace("local_", ""));
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.remove("history_" + studentId + "_" + historyIndex);
                                editor.apply();
                                Toast.makeText(MainActivity.this, "Local history deleted", Toast.LENGTH_SHORT).show();
                                loadChatHistory();
                            } catch (NumberFormatException e) {
                                Toast.makeText(MainActivity.this, "Error deleting history: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Error deleting history: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private String formatTimestamp(String timestamp) {
        // Format ISO8601 timestamp to readable format
        try {
            // Parse ISO8601 timestamp
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(timestamp);
            
            // Format date
            if (date != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                return sdf.format(date);
            }
        } catch (Exception e) {
            // If parsing fails, just return the original timestamp
            e.printStackTrace();
        }
        return timestamp;
    }
    
    private void openHistoryChat(String historyId) {
        Intent intent = new Intent(this, ChatHistoryActivity.class);
        intent.putExtra("history_id", historyId);
        intent.putExtra("user_id", studentId);
        startActivity(intent);
    }
}