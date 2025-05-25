package com.example.task82HDdeakinAIhelperby224385035;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatHistoryActivity extends AppCompatActivity {

    // UI elements
    private LinearLayout messagesContainer;
    private ImageButton backButton;
    private ImageButton deleteButton;
    private TextView noHistoryText;
    
    // SharedPreferences
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPrefs";
    
    // User information
    private String username;
    private String studentId;
    
    // API Service
    private ApiService apiService;
    
    // History ID
    private String historyId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Initialize API Service
        apiService = new ApiService(this);
        
        // Get user information
        loadUserData();
        
        // Get history ID from intent
        historyId = getIntent().getStringExtra("history_id");
        
        if (historyId == null) {
            Toast.makeText(this, "Invalid history ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI elements
        messagesContainer = findViewById(R.id.messagesContainer);
        backButton = findViewById(R.id.backButton);
        deleteButton = findViewById(R.id.deleteButton);
        noHistoryText = findViewById(R.id.noHistoryText);
        
        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> deleteHistory());
        
        // Load chat history
        loadChatHistory();
    }
    
    private void loadUserData() {
        studentId = preferences.getString("currentUserId", "");
        username = preferences.getString("name_" + studentId, "Student");
    }
    
    private void loadChatHistory() {
        // Show loading message
        noHistoryText.setVisibility(View.VISIBLE);
        noHistoryText.setText("Loading chat history...");
        
        // Get specific chat history from API
        apiService.getChatHistoryEntry(studentId, historyId, new ApiService.ApiResponseListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject historyEntry) {
                try {
                    // Get user and bot messages
                    String userMessage = historyEntry.getString("userMessage");
                    String botMessage = historyEntry.getString("botMessage");
                    
                    // Clear messages container
                    messagesContainer.removeAllViews();
                    noHistoryText.setVisibility(View.GONE);
                    
                    // Display messages
                    addUserMessage(userMessage);
                    addBotMessage(botMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                    showNoHistoryMessage();
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
        // Get history JSON from SharedPreferences
        try {
            // Parse integer history ID from string
            int historyIdInt = Integer.parseInt(historyId);
            String historyJson = preferences.getString("history_" + studentId + "_" + historyIdInt, "");
            
            if (historyJson.isEmpty()) {
                showNoHistoryMessage();
                return;
            }
            
            // Parse history
            JSONObject historyEntry = new JSONObject(historyJson);
            JSONArray messages = historyEntry.getJSONArray("messages");
            
            // Clear previous views
            messagesContainer.removeAllViews();
            noHistoryText.setVisibility(View.GONE);
            
            // Display messages
            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);
                String messageText = message.getString("message");
                boolean isUser = message.getBoolean("isUser");
                
                if (isUser) {
                    addUserMessage(messageText);
                } else {
                    addBotMessage(messageText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showNoHistoryMessage();
        }
    }
    
    private void showNoHistoryMessage() {
        messagesContainer.removeAllViews();
        noHistoryText.setVisibility(View.VISIBLE);
    }
    
    private void addUserMessage(String message) {
        // To create message card
        CardView cardView = new CardView(this);
        
        // Set card style
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.END;
        cardParams.setMargins(50, 10, 10, 10);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.light_green));
        cardView.setContentPadding(15, 10, 15, 10);

        // Add message text
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        cardView.addView(textView);
        
        // Create user badge
        TextView userIndicator = new TextView(this);
        userIndicator.setText(username.substring(0, 1).toUpperCase());
        userIndicator.setTextSize(12);
        userIndicator.setTextColor(ContextCompat.getColor(this, R.color.light_green));
        userIndicator.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        userIndicator.setPadding(10, 5, 10, 5);
        userIndicator.setGravity(Gravity.CENTER);
        
        // Set badge position
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        indicatorParams.gravity = Gravity.END;
        indicatorParams.setMargins(0, 5, 10, 0);
        userIndicator.setLayoutParams(indicatorParams);
        
        // Add to chat
        messagesContainer.addView(userIndicator);
        messagesContainer.addView(cardView);
    }

    private void addBotMessage(String message) {
        // Create horizontal layout for bot message with icon
        LinearLayout messageRow = new LinearLayout(this);
        messageRow.setOrientation(LinearLayout.HORIZONTAL);
        messageRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        messageRow.setGravity(Gravity.CENTER_VERTICAL);
        
        // Create image view with Deakin logo
        ImageView deakinLogo = new ImageView(this);
        deakinLogo.setImageResource(R.drawable.deakin_logo);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(80, 80);
        logoParams.setMargins(0, 0, 12, 0);
        logoParams.gravity = Gravity.CENTER_VERTICAL;
        deakinLogo.setLayoutParams(logoParams);
        deakinLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // Create message card
        CardView cardView = new CardView(this);
        
        // Set card style
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(8, 0, 50, 0);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(16);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        cardView.setContentPadding(15, 10, 15, 10);

        // Add message text
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(ContextCompat.getColor(this, R.color.black));
        cardView.addView(textView);
        
        // Add components to horizontal layout
        messageRow.addView(deakinLogo);
        messageRow.addView(cardView);
        
        // Add to message container with bottom margin
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 15);
        messageRow.setLayoutParams(rowParams);
        
        messagesContainer.addView(messageRow);
    }

    private void deleteHistory() {
        apiService.deleteChatHistoryEntry(studentId, historyId, new ApiService.ApiResponseListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Toast.makeText(ChatHistoryActivity.this, "History record deleted successfully", Toast.LENGTH_SHORT).show();
                
                // Also remove from SharedPreferences if present
                try {
                    int historyIdInt = Integer.parseInt(historyId);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("history_" + studentId + "_" + historyIdInt);
                    editor.apply();
                } catch (NumberFormatException e) {
                    // Ignore errors - this is just for local backup cleaning
                }
                
                // Go back to main screen
                finish();
            }
            
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ChatHistoryActivity.this, "Error deleting history: " + errorMessage, Toast.LENGTH_SHORT).show();
                
                // Try fallback to local deletion if history ID can be parsed as integer
                try {
                    int historyIdInt = Integer.parseInt(historyId);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("history_" + studentId + "_" + historyIdInt);
                    editor.apply();
                    Toast.makeText(ChatHistoryActivity.this, "History removed from local storage", Toast.LENGTH_SHORT).show();
                    finish();
                } catch (NumberFormatException e) {
                    // If this fails too, just show an error
                    Toast.makeText(ChatHistoryActivity.this, "Failed to delete history record", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
} 