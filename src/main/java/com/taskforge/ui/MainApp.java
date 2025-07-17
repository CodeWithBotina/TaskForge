package com.taskforge.ui;

import com.taskforge.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader; // Import FXMLLoader
import javafx.scene.Parent;    // Import Parent
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException; // Import IOException

/**
 * Main application class for TaskForge.
 * This class extends JavaFX's Application, serving as the entry point for the GUI.
 * It initializes the database and loads the initial login/registration view.
 *
 * <p>
 * The {@code start} method is the primary entry point for all JavaFX applications.
 * It is called after the {@code init} method has returned, and after the system
 * is ready for the application to begin running.
 * </p>
 *
 * <p>
 * The {@code main} method is the standard entry point for a Java application.
 * It calls the {@code launch()} method, which is inherited from {@code Application},
 * to start the JavaFX runtime and subsequently invoke the {@code start()} method.
 * </p>
 */
public class MainApp extends Application {

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     *
     * @param primaryStage The primary stage for this application, onto which
     * the application scene can be set. The primary stage
     * will be embedded in the browser if the application was
     * launched as an applet.
     */
    @Override
    public void start(Stage primaryStage) {
        // Initialize the database schema when the application starts.
        // This will create all necessary tables if they don't already exist.
        DatabaseManager.initializeDatabase();

        try {
            // Load the FXML file for the login view.
            // The path is relative to the classpath.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/taskforge/ui/views/LoginView.fxml"));
            Parent root = loader.load(); // Load the FXML and get the root node

            // Create a Scene with the loaded root layout and define its initial dimensions
            Scene mainScene = new Scene(root, 800, 600); // Set initial size for the window

            // Set the title of the primary window
            primaryStage.setTitle("TaskForge - Login / Register");
            // Set the scene to the primary stage
            primaryStage.setScene(mainScene);
            // Display the stage
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load LoginView.fxml: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed error information
            // In a real application, you might want to show an error dialog to the user
            // and gracefully exit or fallback to a simpler UI.
        }
    }

    /**
     * The main method is the entry point for the Java application.
     * It calls the launch() method, which is inherited from Application,
     * to start the JavaFX runtime and subsequently call the start() method.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        launch(args); // Launches the JavaFX application
    }
}
