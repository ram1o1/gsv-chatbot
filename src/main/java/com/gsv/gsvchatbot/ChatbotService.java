package com.gsv.gsvchatbot;

import dev.langchain4j.model.chat.ChatLanguageModel; // CORRECTED: Reverted to original import package
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

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
            // .systemMessage(SYSTEM_PROMPT) // REMOVED: This method is not available on the builder
            .build();
    }

    public String getResponse(String userMessage) {
        // Use the chat model to generate a response
        // Note: The model's personality (SYSTEM_PROMPT) is not being applied here,
        // as the simple builder method is unavailable. For multi-turn chat with
        // a system prompt, you would need to implement ChatMemory/AiService.
        return model.generate(userMessage);
    }
}