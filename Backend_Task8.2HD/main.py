from flask import Flask, request, Response, jsonify
import requests
import json
import argparse
import time
import os
import uuid
from datetime import datetime
from pymongo import MongoClient
from werkzeug.security import generate_password_hash, check_password_hash

# Try to import dotenv to load .env file
try:
    from dotenv import load_dotenv
    # Load environment variables from .env file
    load_dotenv()
    print("Loaded environment variables from .env file")
except ImportError:
    print("python-dotenv not installed, using hardcoded connection string")

app = Flask(__name__)

# MongoDB Atlas Connection
# Try to get connection string from environment variables, otherwise use hardcoded string
MONGODB_URI = os.getenv("MONGODB_URI", "mongodb+srv://s224385035:asdjawldk8675543@cluster82hd.gvlea3c.mongodb.net/?retryWrites=true&w=majority&appName=Cluster82HD")
MONGODB_DB = "deakin_assistant_db"
MONGODB_USERS_COLLECTION = "users"
MONGODB_HISTORY_COLLECTION = "chat_history"
MONGODB_INTEREST_COLLECTION = "user_interests"

# Initialize MongoDB client
try:
    mongo_client = MongoClient(MONGODB_URI)
    db = mongo_client[MONGODB_DB]
    users_collection = db[MONGODB_USERS_COLLECTION]
    history_collection = db[MONGODB_HISTORY_COLLECTION]
    interests_collection = db[MONGODB_INTEREST_COLLECTION]
    print("Connected to MongoDB Atlas successfully!")
except Exception as e:
    print(f"Failed to connect to MongoDB: {str(e)}")
    # Use an in-memory database as fallback
    print("Using in-memory database as fallback")
    in_memory_db = {
        "users": {},
        "history": {},
        "interests": {}
    }

# Ollama API settings
OLLAMA_API = "http://localhost:11434/api/generate"
MODEL = "llama2:7b"

# Load Deakin University information from markdown file
def load_deakin_info():
    try:
        # First try to load from current directory
        file_path = "deakin_info.md"
        if not os.path.exists(file_path):
            # Then try to load from BackendTask8.1C directory
            file_path = os.path.join("BackendTask8.1C", "deakin_info.md")
            if not os.path.exists(file_path):
                print(f"Warning: Could not find deakin_info.md file. Using default information.")
                return """
# Deakin University Information

## Campuses
- Burwood (Melbourne): The largest campus, located in Melbourne's eastern suburbs
- Geelong Waurn Ponds: A large regional campus with modern facilities and research centers
- Geelong Waterfront: A city campus located in Geelong's CBD with beautiful waterfront views
- Warrnambool: A coastal campus in southwest Victoria

## Online Systems
- Cloud Deakin: The main online learning management system where students access course materials, assignments, and announcements
- Deakin Sync: University email and Microsoft 365 collaboration platform
- OnTrack: Assessment submission and feedback system
- DeakinTALENT: Career guidance and job search platform
- StudentConnect: System for enrollment, fees, and academic records

## Academic Faculties
- Faculty of Arts and Education
- Faculty of Business and Law
- Faculty of Health
- Faculty of Science, Engineering and Built Environment

## Student Services
- DUSA (Deakin University Student Association): Provides advocacy, services and events for students
- Library Services: Physical and digital resources, research support
- IT Support: Technical assistance for students and staff
- International Student Support: Services specifically for international students
- Counseling and Psychological Support: Mental health services
- Disability Resource Centre: Support for students with disabilities
"""
                
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
            print(f"Successfully loaded Deakin information from {file_path}")
            return content
    except Exception as e:
        print(f"Error loading Deakin information: {str(e)}")
        return "No Deakin University information available."

# Load Deakin information at startup
DEAKIN_INFO = load_deakin_info()

@app.route('/')
def index():
    return "Welcome to the Deakin University AI Student Helper API powered by Llama-2!"

@app.route('/health')
def health_check():
    try:
        # Check if Ollama is running
        response = requests.get("http://localhost:11434/api/tags")
        if response.status_code == 200:
            return Response("Server is healthy and Ollama is running", status=200, mimetype='text/plain')
        else:
            return Response("Server is running but Ollama API is not responding correctly", status=500, mimetype='text/plain')
    except requests.exceptions.RequestException:
        return Response("Server is running but cannot connect to Ollama", status=500, mimetype='text/plain')

@app.route('/chat', methods=['POST'])
def chat():
    start_time = time.time()
    
    # Get userMessage and user ID from form data
    user_message = request.form.get('userMessage')
    user_id = request.form.get('userId')
    
    # Validate userMessage
    if not user_message:
        return Response("Error: userMessage cannot be empty", status=400, mimetype='text/plain')

    # Print received request
    print("\nReceived Request:")
    print(f"userMessage: {user_message}")
    print(f"userId: {user_id}")

    # Format the prompt for Llama-2 chat model with Deakin University context
    prompt = f"""You are a helpful AI assistant for Deakin University students. You provide accurate information about campus facilities, academic programs, university services, and student life at Deakin. Use the following information as your knowledge base:

{DEAKIN_INFO}

Question: {user_message}

Answer in a clear, concise and helpful manner:"""

    # Prepare request to Ollama API
    ollama_payload = {
        "model": MODEL,
        "prompt": prompt,
        "stream": False,
        "options": {
            "temperature": 0.7,
            "top_p": 0.9,
            "top_k": 40,
            "num_predict": 512,
            "repeat_penalty": 1.2
        }
    }

    try:
        # Send request to Ollama API
        response = requests.post(OLLAMA_API, json=ollama_payload, timeout=45)
        response.raise_for_status()  # Raise exception for HTTP errors
        
        # Extract the generated text from the response
        result = response.json()
        answer = result.get("response", "")
        
        # Print raw output for debugging
        print(f"Raw Model Output: {answer}")
        
        # Fallback for empty or irrelevant responses
        if not answer or answer.isspace() or len(answer.split()) < 3:
            answer = f"I apologize, but I couldn't provide a relevant answer to: '{user_message}'. Please try rephrasing your question."
        
        # Save chat history to MongoDB if user_id is provided
        if user_id:
            save_chat_message(user_id, user_message, answer)
        
        # Processing time info
        processing_time = time.time() - start_time
        print(f"Processing time: {processing_time:.2f} seconds")
        
        # Print final response
        print(f"Final Response: {answer}\n")
        
        # Return plain text response
        return Response(answer, mimetype='text/plain')
        
    except requests.exceptions.RequestException as e:
        print(f"Error calling Ollama API: {str(e)}")
        error_message = "I'm sorry, but I encountered an error processing your request. Please ensure Ollama is running and try again later."
        return Response(error_message, mimetype='text/plain')

# User registration
@app.route('/api/register', methods=['POST'])
def register():
    try:
        data = request.json
        required_fields = ['name', 'studentId', 'email', 'campus', 'password']
        
        # Check for required fields
        for field in required_fields:
            if field not in data or not data[field]:
                return jsonify({
                    'success': False,
                    'message': f'{field} is required'
                }), 400
        
        # Check if student ID already exists
        if mongo_client is not None:
            existing_user = users_collection.find_one({'studentId': data['studentId']})
            if existing_user:
                return jsonify({
                    'success': False,
                    'message': 'Student ID already registered'
                }), 409
        else:
            if data['studentId'] in in_memory_db['users']:
                return jsonify({
                    'success': False,
                    'message': 'Student ID already registered'
                }), 409
        
        # Create user object
        user = {
            'name': data['name'],
            'studentId': data['studentId'],
            'email': data['email'],
            'campus': data['campus'],
            'password': generate_password_hash(data['password']),
            'createdAt': datetime.now(),
            'lastLogin': datetime.now(),
            'isActive': True
        }
        
        # Save to database
        if mongo_client is not None:
            users_collection.insert_one(user)
            print(f"User registered in MongoDB: {data['studentId']}")
        else:
            in_memory_db['users'][data['studentId']] = user
            print(f"User registered in memory: {data['studentId']}")
        
        return jsonify({
            'success': True,
            'message': 'Registration successful',
            'userId': data['studentId']
        }), 201
    except Exception as e:
        print(f"Error in registration: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Registration failed, please try again later'
        }), 500

# User login
@app.route('/api/login', methods=['POST'])
def login():
    try:
        data = request.json
        
        # Check for required fields
        if 'studentId' not in data or 'password' not in data:
            return jsonify({
                'success': False,
                'message': 'Student ID and password are required'
            }), 400
        
        # Get user from database
        user = None
        if mongo_client is not None:
            user = users_collection.find_one({'studentId': data['studentId']})
            
            # Update lastLogin in users collection
            if user:
                users_collection.update_one(
                    {'studentId': data['studentId']},
                    {'$set': {'lastLogin': datetime.now()}}
                )
            
            print(f"User found in MongoDB: {data['studentId']}")
        else:
            user = in_memory_db['users'].get(data['studentId'])
            
            # Update lastLogin in memory
            if user:
                in_memory_db['users'][data['studentId']]['lastLogin'] = datetime.now()
                
            print(f"User found in memory: {data['studentId']}")
        
        # Check if user exists and password matches
        if not user or not check_password_hash(user['password'], data['password']):
            return jsonify({
                'success': False,
                'message': 'Invalid student ID or password'
            }), 401
        
        return jsonify({
            'success': True,
            'message': 'Login successful',
            'userId': data['studentId'],
            'name': user['name']
        }), 200
    except Exception as e:
        print(f"Error in login: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Login failed, please try again later'
        }), 500

# Save user interests
@app.route('/api/interests', methods=['POST'])
def save_interests():
    try:
        data = request.json
        
        # Check for required fields
        if 'userId' not in data or 'interests' not in data:
            return jsonify({
                'success': False,
                'message': 'User ID and interests are required'
            }), 400
        
        # Check if interests are at least 3
        if len(data['interests']) < 3:
            return jsonify({
                'success': False,
                'message': 'At least 3 interests are required'
            }), 400
        
        # Create interest object
        interest_data = {
            'userId': data['userId'],
            'interests': data['interests'],
            'updatedAt': datetime.now()
        }
        
        # Save to database
        if 'mongo_client' in globals():
            # Update if exists, insert if not
            interests_collection.update_one(
                {'userId': data['userId']},
                {'$set': interest_data},
                upsert=True
            )
        else:
            in_memory_db['interests'][data['userId']] = interest_data
        
        return jsonify({
            'success': True,
            'message': 'Interests saved successfully'
        }), 200
    except Exception as e:
        print(f"Error saving interests: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Failed to save interests, please try again later'
        }), 500

# Get user interests
@app.route('/api/interests/<user_id>', methods=['GET'])
def get_interests(user_id):
    try:
        # Get interests from database
        interests = None
        if 'mongo_client' in globals():
            interest_data = interests_collection.find_one({'userId': user_id})
            interests = interest_data['interests'] if interest_data else []
        else:
            interest_data = in_memory_db['interests'].get(user_id, {})
            interests = interest_data.get('interests', [])
        
        return jsonify({
            'success': True,
            'interests': interests
        }), 200
    except Exception as e:
        print(f"Error getting interests: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Failed to get interests, please try again later'
        }), 500

# Save chat message to history
def save_chat_message(user_id, user_message, bot_message):
    try:
        # Create message object
        chat_entry = {
            'userId': user_id,
            'messageId': str(uuid.uuid4()),
            'userMessage': user_message,
            'botMessage': bot_message,
            'timestamp': datetime.now()
        }
        
        # Save to database
        if 'mongo_client' in globals():
            history_collection.insert_one(chat_entry)
        else:
            if user_id not in in_memory_db['history']:
                in_memory_db['history'][user_id] = []
            in_memory_db['history'][user_id].append(chat_entry)
        
        print(f"Chat message saved for user {user_id}")
    except Exception as e:
        print(f"Error saving chat message: {str(e)}")

# Get chat history
@app.route('/api/history/<user_id>', methods=['GET'])
def get_chat_history(user_id):
    try:
        # Get history from database
        history = []
        if 'mongo_client' in globals():
            cursor = history_collection.find({'userId': user_id}).sort('timestamp', -1)
            history = list(cursor)
            # Convert ObjectId to string for JSON serialization
            for item in history:
                item['_id'] = str(item['_id'])
                item['timestamp'] = item['timestamp'].isoformat()
        else:
            history = in_memory_db['history'].get(user_id, [])
            for item in history:
                if isinstance(item['timestamp'], datetime):
                    item['timestamp'] = item['timestamp'].isoformat()
        
        return jsonify({
            'success': True,
            'history': history
        }), 200
    except Exception as e:
        print(f"Error getting chat history: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Failed to get chat history, please try again later'
        }), 500

# Get a specific chat history entry
@app.route('/api/history/<user_id>/<history_id>', methods=['GET'])
def get_chat_history_entry(user_id, history_id):
    try:
        # Get history entry from database
        history_entry = None
        if 'mongo_client' in globals():
            entry = history_collection.find_one({'userId': user_id, 'messageId': history_id})
            if entry:
                entry['_id'] = str(entry['_id'])
                entry['timestamp'] = entry['timestamp'].isoformat()
                history_entry = entry
        else:
            for entry in in_memory_db['history'].get(user_id, []):
                if entry.get('messageId') == history_id:
                    history_entry = entry
                    if isinstance(entry['timestamp'], datetime):
                        history_entry['timestamp'] = entry['timestamp'].isoformat()
                    break
        
        if not history_entry:
            return jsonify({
                'success': False,
                'message': 'Chat history entry not found'
            }), 404
        
        return jsonify({
            'success': True,
            'history': history_entry
        }), 200
    except Exception as e:
        print(f"Error getting chat history entry: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Failed to get chat history entry, please try again later'
        }), 500

# Delete a specific chat history entry
@app.route('/api/history/<user_id>/<history_id>', methods=['DELETE'])
def delete_chat_history_entry(user_id, history_id):
    try:
        # Delete history entry from database
        if 'mongo_client' in globals() and mongo_client is not None:
            result = history_collection.delete_one({'userId': user_id, 'messageId': history_id})
            if result.deleted_count == 0:
                return jsonify({
                    'success': False,
                    'message': 'Chat history entry not found'
                }), 404
        else:
            # In-memory database deletion
            found = False
            history_entries = in_memory_db['history'].get(user_id, [])
            new_entries = []
            for entry in history_entries:
                if entry.get('messageId') != history_id:
                    new_entries.append(entry)
                else:
                    found = True
            
            if not found:
                return jsonify({
                    'success': False,
                    'message': 'Chat history entry not found'
                }), 404
            
            in_memory_db['history'][user_id] = new_entries
        
        return jsonify({
            'success': True,
            'message': 'Chat history entry deleted'
        }), 200
    except Exception as e:
        print(f"Error deleting chat history entry: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Failed to delete chat history entry, please try again later'
        }), 500

# Get all users (admin endpoint)
@app.route('/api/users', methods=['GET'])
def get_all_users():
    try:
        users = []
        if mongo_client is not None:
            # Get all users from users collection
            cursor = users_collection.find({})
            users = list(cursor)
            # Convert ObjectId to string for JSON serialization
            for user in users:
                user['_id'] = str(user['_id'])
                if isinstance(user.get('createdAt'), datetime):
                    user['createdAt'] = user['createdAt'].isoformat()
                if isinstance(user.get('lastLogin'), datetime):
                    user['lastLogin'] = user['lastLogin'].isoformat()
                # Remove password hash for security
                if 'password' in user:
                    user['password'] = '[HIDDEN]'
        else:
            # Get all users from in-memory database
            for student_id, user in in_memory_db['users'].items():
                user_copy = user.copy()
                if isinstance(user_copy.get('createdAt'), datetime):
                    user_copy['createdAt'] = user_copy['createdAt'].isoformat()
                if isinstance(user_copy.get('lastLogin'), datetime):
                    user_copy['lastLogin'] = user_copy['lastLogin'].isoformat()
                # Remove password hash for security
                if 'password' in user_copy:
                    user_copy['password'] = '[HIDDEN]'
                users.append(user_copy)
        
        return jsonify({
            'success': True,
            'users': users
        }), 200
    except Exception as e:
        print(f"Error getting users: {str(e)}")
        return jsonify({
            'success': False,
            'message': 'Failed to get users, please try again later'
        }), 500

def add_deakin_info(filename):
    """Add Deakin information from an external file if it exists"""
    global DEAKIN_INFO
    try:
        if os.path.exists(filename):
            with open(filename, 'r', encoding='utf-8') as file:
                DEAKIN_INFO = file.read()
                print(f"Loaded Deakin information from {filename}")
    except Exception as e:
        print(f"Error loading Deakin information file: {str(e)}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Deakin University AI Student Helper API")
    parser.add_argument('--port', type=int, default=5000, help='Specify the port number')
    parser.add_argument('--model', type=str, default="llama2:7b", help='Specify the Ollama model to use')
    parser.add_argument('--info', type=str, default="deakin_info.md", help='Path to Deakin information file')
    parser.add_argument('--mongodb', type=str, help='MongoDB Atlas connection string')
    args = parser.parse_args()

    # Update settings from arguments
    MODEL = args.model
    if args.mongodb:
        MONGODB_URI = args.mongodb
    
    # Try to load additional Deakin info if file exists
    add_deakin_info(args.info)
    
    # Start the server
    port_num = args.port
    print(f"App running on port {port_num}")
    print(f"Using Ollama API with model {MODEL}")
    app.run(host='0.0.0.0', port=port_num)