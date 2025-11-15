import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority; 
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;

public class ChatApp extends Application {

    private static final Logger LOGGER = LogManager.getLogger(ChatApp.class);

    private static final ObservableList<SearchAction> data = FXCollections.observableArrayList();
    private static final AnswerService docsAnswerService = new AnswerService();
    private final TextArea lastAnswer = new TextArea();

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting...");

        var holder = new VBox();
        holder.getStyleClass().add("chat-container");

        MFXTextField input = new MFXTextField();
        input.setOnAction(e -> doSearch(input.getText()));
        input.setFloatingText("Ask Gemini a question...");
        input.setPrefWidth(600);
        input.setPrefHeight(40);

        // Button reverted to text-only to avoid MFXFontIcon error
        MFXButton search = new MFXButton("Send"); 
        search.setOnAction(e -> doSearch(input.getText()));
        // Style class for icon removed

        var inputHolder = new HBox(10, input, search);
        inputHolder.getStyleClass().add("input-holder");
        
        // --- Single Scrollable Answer Area ---
        lastAnswer.setWrapText(true);
        lastAnswer.setEditable(false);
        lastAnswer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        lastAnswer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        ScrollPane scrollPane = new ScrollPane(lastAnswer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("chat-history-area");
        
        holder.getChildren().addAll(scrollPane, inputHolder);
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // --- Apply MaterialFX Stylesheet Globally ---
        UserAgentBuilder.builder()
                .themes(MaterialFXStylesheets.DEFAULT)
                .build(); 

        Scene scene = new Scene(holder, 850, 600);
        
        // --- Load Custom Gemini CSS ---
        scene.getStylesheets().add(getClass().getResource("/gemini-style.css").toExternalForm());

        stage.setTitle("GSV Chatbot");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        data.add(new SearchAction("Application started", true));

        var initAction = new SearchAction("Initializing search engine, please stand by...");
        data.add(initAction);
        lastAnswer.textProperty().bind(initAction.getAnswerProperty());
        new Thread(() -> docsAnswerService.init(initAction)).start();
    }

    private void doSearch(String question) {
        if (question.isEmpty()) {
            return;
        }

        var searchAction = new SearchAction(question);
        data.add(searchAction);
        lastAnswer.textProperty().bind(searchAction.getAnswerProperty());
        new Thread(() -> docsAnswerService.ask(searchAction)).start();
    }
}