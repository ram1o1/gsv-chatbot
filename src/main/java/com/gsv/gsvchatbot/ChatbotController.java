package com.gsv.gsvchatbot;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatbotController {

    @FXML
    private TextArea chatHistory;

    @FXML
    private TextField userInput;

    private final ChatbotService chatbotService = new ChatbotService();
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // This method is called when the "Send" button is clicked or Enter is pressed in the TextField
    @FXML
    private void sendMessage() {
        String userMessage = userInput.getText().trim();
        
        if (userMessage.isEmpty()) {
            return;
        }

        // 1. Display user message
        appendMessage("You", userMessage);
        
        // 2. Clear input field immediately
        userInput.clear();

        // 3. Get response from Gemini (This should ideally run on a background thread for a real app)
        try {
            String geminiResponse = chatbotService.getResponse(userMessage);
            
            // 4. Display model response
            appendMessage("GSV Bot", geminiResponse);
            
        } catch (Exception e) {
            appendMessage("ERROR", "Could not connect to Gemini: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void appendMessage(String sender, String message) {
        String timestamp = LocalTime.now().format(timeFormatter);
        chatHistory.appendText(String.format("[%s] %s: %s\n", timestamp, sender, message));
    }
}