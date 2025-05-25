package com.example.task82HDdeakinAIhelperby224385035;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for handling API communication with the backend server.
 * This class provides methods for user registration, login, chat, and other API operations.
 */
public class ApiService {
    private static final String TAG = "ApiService";
    
    // Base URL of the backend API server (Android emulator localhost)
    // For physical devices, replace with your computer's IP address
    // Example: "http://192.168.1.100:5000" (use your computer's actual IP address)
    private static final String BASE_URL = "http://10.0.2.2:5000";
    
    // API endpoints
    private static final String CHAT_ENDPOINT = BASE_URL + "/chat";
    private static final String REGISTER_ENDPOINT = BASE_URL + "/api/register";
    private static final String LOGIN_ENDPOINT = BASE_URL + "/api/login";
    private static final String INTERESTS_ENDPOINT = BASE_URL + "/api/interests";
    private static final String HISTORY_ENDPOINT = BASE_URL + "/api/history";
    
    // Volley RequestQueue
    private RequestQueue requestQueue;
    
    // Constructor
    public ApiService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }
    
    /**
     * Interface for handling API responses
     */
    public interface ApiResponseListener<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
    
    /**
     * Register a new user
     * 
     * @param name User's name
     * @param studentId Student ID
     * @param email Email address
     * @param campus Campus
     * @param password Password
     * @param listener Response listener
     */
    public void registerUser(String name, String studentId, String email, String campus, String password, 
                            ApiResponseListener<String> listener) {
        try {
            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", name);
            requestBody.put("studentId", studentId);
            requestBody.put("email", email);
            requestBody.put("campus", campus);
            requestBody.put("password", password);
            
            // Create JSON request
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    REGISTER_ENDPOINT,
                    requestBody,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");
                            String message = response.getString("message");
                            
                            if (success) {
                                listener.onSuccess(response.getString("userId"));
                            } else {
                                listener.onError(message);
                            }
                        } catch (JSONException e) {
                            listener.onError("Failed to parse response: " + e.getMessage());
                        }
                    },
                    error -> {
                        handleApiError(error, listener);
                    }
            );
            
            // Set timeout (10 seconds)
            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            // Add to request queue
            requestQueue.add(request);
            
        } catch (JSONException e) {
            listener.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Login a user
     * 
     * @param studentId Student ID
     * @param password Password
     * @param listener Response listener
     */
    public void loginUser(String studentId, String password, ApiResponseListener<JSONObject> listener) {
        try {
            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("studentId", studentId);
            requestBody.put("password", password);
            
            // Create JSON request
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    LOGIN_ENDPOINT,
                    requestBody,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");
                            String message = response.getString("message");
                            
                            if (success) {
                                JSONObject userData = new JSONObject();
                                userData.put("userId", response.getString("userId"));
                                userData.put("name", response.getString("name"));
                                listener.onSuccess(userData);
                            } else {
                                listener.onError(message);
                            }
                        } catch (JSONException e) {
                            listener.onError("Failed to parse response: " + e.getMessage());
                        }
                    },
                    error -> {
                        handleApiError(error, listener);
                    }
            );
            
            // Set timeout (10 seconds)
            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            // Add to request queue
            requestQueue.add(request);
            
        } catch (JSONException e) {
            listener.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Save user interests
     * 
     * @param userId User ID
     * @param interests List of interests
     * @param listener Response listener
     */
    public void saveInterests(String userId, List<String> interests, ApiResponseListener<Boolean> listener) {
        try {
            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("userId", userId);
            requestBody.put("interests", new JSONArray(interests));
            
            // Create JSON request
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    INTERESTS_ENDPOINT,
                    requestBody,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");
                            String message = response.getString("message");
                            
                            if (success) {
                                listener.onSuccess(true);
                            } else {
                                listener.onError(message);
                            }
                        } catch (JSONException e) {
                            listener.onError("Failed to parse response: " + e.getMessage());
                        }
                    },
                    error -> {
                        handleApiError(error, listener);
                    }
            );
            
            // Set timeout (10 seconds)
            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            // Add to request queue
            requestQueue.add(request);
            
        } catch (JSONException e) {
            listener.onError("Failed to create request: " + e.getMessage());
        }
    }
    
    /**
     * Get user interests
     * 
     * @param userId User ID
     * @param listener Response listener
     */
    public void getInterests(String userId, ApiResponseListener<List<String>> listener) {
        // Create JSON request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                INTERESTS_ENDPOINT + "/" + userId,
                null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        
                        if (success) {
                            // Parse interests array
                            JSONArray interestsArray = response.getJSONArray("interests");
                            List<String> interests = new ArrayList<>();
                            
                            for (int i = 0; i < interestsArray.length(); i++) {
                                interests.add(interestsArray.getString(i));
                            }
                            
                            listener.onSuccess(interests);
                        } else {
                            listener.onError(response.getString("message"));
                        }
                    } catch (JSONException e) {
                        listener.onError("Failed to parse response: " + e.getMessage());
                    }
                },
                error -> {
                    handleApiError(error, listener);
                }
        );
        
        // Set timeout (10 seconds)
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        // Add to request queue
        requestQueue.add(request);
    }
    
    /**
     * Send a chat message to the AI assistant
     * 
     * @param userMessage User's message
     * @param userId User ID (optional, for saving chat history)
     * @param listener Response listener
     */
    public void sendChatMessage(String userMessage, String userId, ApiResponseListener<String> listener) {
        // Create a POST request with form data
        StringRequest request = new StringRequest(
                Request.Method.POST,
                CHAT_ENDPOINT,
                response -> {
                    listener.onSuccess(response.trim());
                },
                error -> {
                    handleApiError(error, listener);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("userMessage", userMessage);
                if (userId != null && !userId.isEmpty()) {
                    params.put("userId", userId);
                }
                return params;
            }
        };
        
        // Set timeout (45 seconds) - Llama-2 may need more time for processing
        request.setRetryPolicy(new DefaultRetryPolicy(
                45000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        // Add to request queue
        requestQueue.add(request);
    }
    
    /**
     * Get chat history for a user
     * 
     * @param userId User ID
     * @param listener Response listener
     */
    public void getChatHistory(String userId, ApiResponseListener<List<JSONObject>> listener) {
        // Create JSON request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                HISTORY_ENDPOINT + "/" + userId,
                null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        
                        if (success) {
                            // Parse history array
                            JSONArray historyArray = response.getJSONArray("history");
                            List<JSONObject> history = new ArrayList<>();
                            
                            for (int i = 0; i < historyArray.length(); i++) {
                                history.add(historyArray.getJSONObject(i));
                            }
                            
                            listener.onSuccess(history);
                        } else {
                            listener.onError(response.getString("message"));
                        }
                    } catch (JSONException e) {
                        listener.onError("Failed to parse response: " + e.getMessage());
                    }
                },
                error -> {
                    handleApiError(error, listener);
                }
        );
        
        // Set timeout (10 seconds)
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        // Add to request queue
        requestQueue.add(request);
    }
    
    /**
     * Get a specific chat history entry
     * 
     * @param userId User ID
     * @param historyId History entry ID
     * @param listener Response listener
     */
    public void getChatHistoryEntry(String userId, String historyId, ApiResponseListener<JSONObject> listener) {
        // Create JSON request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                HISTORY_ENDPOINT + "/" + userId + "/" + historyId,
                null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        
                        if (success) {
                            // Get history entry
                            JSONObject historyEntry = response.getJSONObject("history");
                            listener.onSuccess(historyEntry);
                        } else {
                            listener.onError(response.getString("message"));
                        }
                    } catch (JSONException e) {
                        listener.onError("Failed to parse response: " + e.getMessage());
                    }
                },
                error -> {
                    handleApiError(error, listener);
                }
        );
        
        // Set timeout (10 seconds)
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        // Add to request queue
        requestQueue.add(request);
    }
    
    /**
     * Delete chat history entry
     * 
     * @param userId User ID
     * @param historyId History entry ID
     * @param listener Response listener
     */
    public void deleteChatHistoryEntry(String userId, String historyId, final ApiResponseListener<Boolean> listener) {
        // Build URL for API
        String url = HISTORY_ENDPOINT + "/" + userId + "/" + historyId;
        
        // Create DELETE request
        StringRequest request = new StringRequest(
            Request.Method.DELETE,
            url,
            response -> {
                try {
                    // Parse response
                    JSONObject responseJson = new JSONObject(response);
                    boolean success = responseJson.getBoolean("success");
                    
                    if (success) {
                        listener.onSuccess(true);
                    } else {
                        String message = responseJson.getString("message");
                        listener.onError(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onError("Error parsing response: " + e.getMessage());
                }
            },
            error -> {
                // Handle connection errors and report back to the caller
                handleApiError(error, listener);
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        
        // Set timeout (30 seconds)
        request.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        // Add request to queue
        requestQueue.add(request);
    }
    
    /**
     * Handle API errors
     * 
     * @param error Volley error
     * @param listener Response listener
     */
    private <T> void handleApiError(VolleyError error, ApiResponseListener<T> listener) {
        String errorMessage = "Network error occurred";
        
        if (error.networkResponse != null) {
            // Try to parse error response
            try {
                String errorData = new String(error.networkResponse.data);
                JSONObject errorJson = new JSONObject(errorData);
                if (errorJson.has("message")) {
                    errorMessage = errorJson.getString("message");
                }
            } catch (Exception e) {
                // If we can't parse the error, use HTTP status code
                errorMessage = "Server error: " + error.networkResponse.statusCode;
            }
        } else if (error.getMessage() != null) {
            errorMessage = error.getMessage();
        }
        
        Log.e(TAG, "API Error: " + errorMessage);
        listener.onError(errorMessage);
    }
} 