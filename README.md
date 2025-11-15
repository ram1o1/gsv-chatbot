# JavaFX RAG Chatbot (Langchain4J + Gemini)

This project is a JavaFX chat application demonstrating **Retrieval-Augmented Generation (RAG)**. It uses **Langchain4J** to connect to the **Google Gemini** model and answer questions based on a private knowledge base of PDF documents.

## Key Technologies
* **Frontend**: JavaFX (MaterialFX style).
* **LLM**: Google Gemini (`gemini models`).
* **RAG Components**:
    * **Embedding Model**: Ollama (`embeddinggemma`) on `http://localhost:11434`.
    * **Vector Store**: ChromaDB on `http://localhost:8000`.

## Prerequisites

1.  **JDK 21** with JavaFX.
2.  **Maven**.
3.  **Google Gemini API Key**: Must be set in a **`.env`** file as `GEMINI_API_KEY`.
4.  **ChromaDB** and **Ollama** running locally on their respective default ports.

## Setup and Run

1.  **Prepare Knowledge Base**:
    Place your PDF files in the directory: `src/main/resources/knowledge-base/`

2.  **Execute**:
    ```bash
    # Build the project
    $ mvn clean install

    # Run the application
    $ mvn javafx:run
    ```

The RAG pipeline will automatically load and ingest your PDFs into the vector store upon startup.
```eof