package com.gsv.gsvchatbot;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.google.GoogleAiGeminiChatModel;

public class ChatbotService {

    private final ChatLanguageModel model;
    
    // The system message guides the model's behavior and personality
    private static final String SYSTEM_PROMPT = 
        "You are GSV Bot, a helpful, friendly, and professional chatbot for a university. " +
        "Keep your answers concise and only relevant to university topics.";

    public ChatbotService() {
        // LangChain4j automatically reads the GEMINI_API_KEY environment variable.
        this.model = GoogleAiGeminiChatModel.builder()
            .apiKey(System.getenv("GEMINI_API_KEY"))
            .modelName("gemini-2.5-flash") // A recommended fast and capable model
            .systemInstruction(SYSTEM_PROMPT)
            .build();
    }

    public String getResponse(String userMessage) {
        // Use the chat model to generate a response
        return model.generate(userMessage);
    }
}