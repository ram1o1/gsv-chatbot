package com.gsv.gsvchatbot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Load the FXML file for the UI
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/gsv/gsvchatbot/ChatbotView.fxml"));
        Parent root = fxmlLoader.load();
        
        // Basic window configuration
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("GSV University Chatbot");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}