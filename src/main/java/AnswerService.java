import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AnswerService {

    private static final Logger LOGGER = LogManager.getLogger(AnswerService.class);

    private Assistant assistant;

    public void init(SearchAction action) {
        action.appendAnswer("Initiating...");
        initChat(action);
    }

    private void initChat(SearchAction action) {
        StreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(ApiKeys.GEMINI_API_KEY)
                .modelName("gemini-2.5-flash")
                .build();

        assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
        action.appendAnswer("Done");
        action.setFinished();
    }

    void ask(SearchAction action) {
        LOGGER.info("Asking question '" + action.getQuestion() + "'");

        var responseHandler = new CustomStreamingResponseHandler(action);

        assistant.chat(action.getQuestion())
                .onPartialResponse(responseHandler::onNext)
                .onCompleteResponse(responseHandler::onComplete)
                .onError(responseHandler::onError)
                .start();
    }
}