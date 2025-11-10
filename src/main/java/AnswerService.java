import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser; // CORRECTED IMPORT
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; 
import java.util.stream.Stream;

// Removed: import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

public class AnswerService {

    private static final Logger LOGGER = LogManager.getLogger(AnswerService.class);
    private static final Path KNOWLEDGE_BASE_PATH = Paths.get("src/main/resources/knowledge-base");

    private Assistant assistant;

    public void init(SearchAction action) {
        action.appendAnswer("Initiating...");
        
        // 1. Initialize RAG components
        EmbeddingModel embeddingModel = initEmbeddingModel();
        
        // Replaced initEmbeddingStore logic with custom loading for error logging
        EmbeddingStore<TextSegment> embeddingStore = initEmbeddingStore(action, embeddingModel); 
        
        ContentRetriever contentRetriever = initContentRetriever(embeddingStore, embeddingModel);

        // 2. Initialize Chat Model and Assistant
        initChat(action, contentRetriever);
        
        action.appendAnswer("Done");
        action.setFinished();
    }

    private EmbeddingModel initEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("embeddinggemma")
                .timeout(Duration.ofMinutes(10)) 
                .build();
    }

    private EmbeddingStore<TextSegment> initEmbeddingStore(SearchAction action, EmbeddingModel embeddingModel) {
        action.appendAnswer("\nLoading and chunking documents...");
        
        // 1. Load documents using the custom function to log skipped files
        List<Document> documents = loadAndFilterDocuments(KNOWLEDGE_BASE_PATH);

        // 2. Split documents into manageable segments (chunks)
        DocumentSplitter splitter = DocumentSplitters.recursive(
                1000, // Max segment size in characters
                100   // Max segment overlap in characters
        );

        // 3. Create a Chroma vector store client
        EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000") 
                .collectionName("my_gsv_chatbot_collection") 
                .apiVersion(ChromaApiVersion.V2)
                .build();

        // 4. Ingest documents into the store.
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        action.appendAnswer("\nIngesting documents into Chroma (will skip if already exists)...");
        ingestor.ingest(documents);
        
        action.appendAnswer(" (Ingested " + documents.size() + " documents)");
        return embeddingStore;
    }
    
    // NEW METHOD: Iterates over files and logs errors for skipped documents
    private List<Document> loadAndFilterDocuments(Path path) {
        List<Document> validDocuments = new ArrayList<>();
        // The DocumentParser is created once to be reused
        DocumentParser parser = new ApachePdfBoxDocumentParser(); 
        
        try (Stream<Path> paths = Files.walk(path)) {
            // Filter to only regular files (ignoring directories)
            List<Path> filesToProcess = paths.filter(Files::isRegularFile).collect(Collectors.toList());

            for (Path filePath : filesToProcess) {
                // Only process files with a .pdf extension
                if (filePath.getFileName().toString().toLowerCase().endsWith(".pdf")) {
                    try {
                        // Load the document individually
                        Document document = FileSystemDocumentLoader.loadDocument(filePath, parser);
                        
                        if (document.text().trim().isEmpty()) {
                            // Case 1: Successfully parsed, but resulted in zero text (e.g., image-only PDF)
                            LOGGER.warn("Skipped PDF (Empty Content): File: {}", filePath.getFileName());
                        } else {
                            validDocuments.add(document);
                        }
                    } catch (Exception e) {
                        // Case 2: Parsing failed due to corruption or encryption
                        LOGGER.error("Skipped PDF (CRITICAL PARSING ERROR): File: {} | Error: {}", filePath.getFileName(), e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read knowledge base directory: {}", e.getMessage());
        }
        return validDocuments;
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