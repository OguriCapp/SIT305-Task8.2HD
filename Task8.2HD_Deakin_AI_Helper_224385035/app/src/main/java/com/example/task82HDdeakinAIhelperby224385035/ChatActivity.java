package com.example.task82HDdeakinAIhelperby224385035;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // UI elements
    private LinearLayout messagesContainer;
    private EditText chatInputBox;
    private View sendButton;
    private ProgressBar progressBar;
    private ImageButton backButton;
    private ImageButton saveButton;
    private Chip questionChip1, questionChip2, questionChip3, questionChip4, questionChip5;
    
    // SharedPreferences
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPrefs";
    
    // User information
    private String username;
    private String studentId;
    
    // API Service
    private ApiService apiService;
    
    // Chat history
    private List<ChatMessage> chatHistory = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Initialize SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Initialize API Service
        apiService = new ApiService(this);
        
        // Get user information
        loadUserData();
        
        // Initialize UI elements
        messagesContainer = findViewById(R.id.messagesContainer);
        chatInputBox = findViewById(R.id.chatInputBox);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        
        // Initialize question chips
        questionChip1 = findViewById(R.id.questionChip1);
        questionChip2 = findViewById(R.id.questionChip2);
        questionChip3 = findViewById(R.id.questionChip3);
        questionChip4 = findViewById(R.id.questionChip4);
        questionChip5 = findViewById(R.id.questionChip5);
        
        // Set click listeners
        sendButton.setOnClickListener(v -> sendMessage());
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveChatToHistory());
        
        // Set click listeners for question chips
        questionChip1.setOnClickListener(v -> sendSuggestedQuestion(questionChip1.getText().toString()));
        questionChip2.setOnClickListener(v -> sendSuggestedQuestion(questionChip2.getText().toString()));
        questionChip3.setOnClickListener(v -> sendSuggestedQuestion(questionChip3.getText().toString()));
        questionChip4.setOnClickListener(v -> sendSuggestedQuestion(questionChip4.getText().toString()));
        questionChip5.setOnClickListener(v -> sendSuggestedQuestion(questionChip5.getText().toString()));
        
        // Show welcome message
        addBotMessage("Hello " + username + "! I'm your Deakin University AI assistant powered by Llama-2. You can ask me about Cloud Deakin, campus facilities, or other university-related questions, or select one of the suggested questions below.");
    }
    
    private void loadUserData() {
        studentId = preferences.getString("currentUserId", "");
        username = preferences.getString("name_" + studentId, "Student");
    }
    
    private void sendSuggestedQuestion(String question) {
        chatInputBox.setText(question);
        sendMessage();
    }
    
    private void sendMessage() {
        // Get message text
        final String userMessage = chatInputBox.getText().toString().trim();
        
        // Check if the input is empty
        if (userMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to chat and clear input
        addUserMessage(userMessage);
        chatInputBox.setText("");
        
        // Add to chat history
        chatHistory.add(new ChatMessage(userMessage, true));

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // Send message using API service
        apiService.sendChatMessage(userMessage, studentId, new ApiService.ApiResponseListener<String>() {
            @Override
            public void onSuccess(String botMessage) {
                progressBar.setVisibility(View.GONE);
                
                // Show response
                addBotMessage(botMessage);
                
                // Add to chat history
                chatHistory.add(new ChatMessage(botMessage, false));
            }
            
            @Override
            public void onError(String errorMessage) {
                // Handle error
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this, "Error connecting to Llama-2 server: " + errorMessage, Toast.LENGTH_LONG).show();
                String fallbackError = "Sorry, I'm unable to respond right now. Please check your server connection and ensure Ollama is running.";
                addBotMessage(fallbackError);
                
                // Add to chat history
                chatHistory.add(new ChatMessage(fallbackError, false));
            }
        });
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
    
    private void saveChatToHistory() {
        if (chatHistory.size() == 0) {
            Toast.makeText(this, "No conversation to save", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            SharedPreferences.Editor editor = preferences.edit();
            
            // Get history count
            int historyCount = preferences.getInt("history_count_" + studentId, 0);
            
            // Create new history entry
            JSONObject historyEntry = new JSONObject();
            historyEntry.put("timestamp", new Date().getTime());
            historyEntry.put("title", chatHistory.get(0).message.length() > 30 ? 
                    chatHistory.get(0).message.substring(0, 27) + "..." : 
                    chatHistory.get(0).message);
            
            // Add messages
            JSONArray messages = new JSONArray();
            for (ChatMessage message : chatHistory) {
                JSONObject messageObj = new JSONObject();
                messageObj.put("message", message.message);
                messageObj.put("isUser", message.isUser);
                messages.put(messageObj);
            }
            historyEntry.put("messages", messages);
            
            // Save to preferences
            editor.putString("history_" + studentId + "_" + historyCount, historyEntry.toString());
            editor.putInt("history_count_" + studentId, historyCount + 1);
            editor.apply();
            
            Toast.makeText(this, "Conversation saved to history", Toast.LENGTH_SHORT).show();
            
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to save conversation", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    // Chat message class for local storage
    private static class ChatMessage {
        String message;
        boolean isUser;
        
        ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
        }
    }
} 