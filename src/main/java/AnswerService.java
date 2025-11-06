import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

public class AnswerService {

    private static final Logger LOGGER = LogManager.getLogger(AnswerService.class);
    private static final Path KNOWLEDGE_BASE_PATH = Paths.get("src/main/resources/knowledge-base");

    private Assistant assistant;

    public void init(SearchAction action) {
        action.appendAnswer("Initiating...");
        
        // 1. Initialize RAG components
        EmbeddingModel embeddingModel = initEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = initEmbeddingStore(action, embeddingModel);
        ContentRetriever contentRetriever = initContentRetriever(embeddingStore, embeddingModel);

        // 2. Initialize Chat Model and Assistant
        initChat(action, contentRetriever);
        
        action.appendAnswer("Done");
        action.setFinished();
    }

    private EmbeddingModel initEmbeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(ApiKeys.GEMINI_API_KEY)
                // Use a compatible embedding model
                .modelName("gemini-2.5-flash") 
                .build();
    }

    private EmbeddingStore<TextSegment> initEmbeddingStore(SearchAction action, EmbeddingModel embeddingModel) {
        action.appendAnswer("\nLoading and chunking documents...");
        
        // 1. Load documents from the knowledge-base directory
        List<Document> documents = loadDocuments(KNOWLEDGE_BASE_PATH, new ApachePdfBoxDocumentParser());

        // 2. Split documents into manageable segments (chunks)
        DocumentSplitter splitter = DocumentSplitters.recursive(
                1000, // Max segment size in characters
                100   // Max segment overlap in characters
        );

        // 3. Create an in-memory vector store
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 4. Ingest documents into the store
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        
        action.appendAnswer(" (Ingested " + documents.size() + " documents)");
        return embeddingStore;
    }
    
    private ContentRetriever initContentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        // Create a retriever to fetch the top 3 most relevant segments
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // How many relevant chunks to retrieve
                .build();
    }

    private void initChat(SearchAction action, ContentRetriever contentRetriever) {
        StreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(ApiKeys.GEMINI_API_KEY)
                .modelName("gemini-2.5-flash")
                .build();

        // Use the contentRetriever to augment the chat responses
        assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(model)
                .contentRetriever(contentRetriever) // KEY CHANGE: Adds RAG capability
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
        action.appendAnswer(" (Assistant ready)");
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