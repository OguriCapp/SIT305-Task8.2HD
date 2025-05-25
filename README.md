# Deakin AI Helper

This is a simple AI assistant project for Deakin University students. It helps students get information about Deakin University by asking questions.

This project was developed as part of SIT305 Task 8.2HD.

The main idea of this project is to create a smart helper for Deakin students. I wanted to build an AI that could answer questions about Deakin University using real information.

## Key Features

Uses a Llama-2 AI model running locally with Ollama.

Stores conversation history and user data in a MongoDB Atlas cloud database.

Has a Python scrapper to automatically collect information from the Deakin University website (a web scraper).

Uses Flask to create a simple web server (API) that connects the mobile app to the AI and database.

## Development Process

The biggest challenge was getting a lot of information about Deakin University. At first, I thought about typing the information myself, but that would take too much time and the information might become old quickly.

So, I decided to write a Python program (a web scraper) to automatically visit Deakin's website and collect the information. This data is then saved in a file (`deakin_info.md`).

After collecting the data, I used the Llama-2 model and the collected data to make the AI smart enough to answer questions.

Then, I built the backend API using Flask to handle communication between the mobile app and the AI/database.

Finally, I developed the Android app in Java, adding features like user login, chat, viewing history, and sharing.

Sometimes, the web scraper had problems because some website pages changed or were removed (like getting 404 errors). I fixed these issues by updating the list of website addresses and making the scraper better at handling errors.

## How to Run

To run this project, you need to set up a few things:

1.  Install Ollama and download the Llama-2 model. Make sure the Ollama service is running.
  
2.  Create a Python virtual environment and activate it. Install the required Python packages and then type "python main.py"

3.  Then you can start Android Project and use it!


## Future Improvements

*   Make the data collection more real-time.
*   Improve the AI's understanding and responses.
*   Add more features to the mobile application.
