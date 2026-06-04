package com.cellsimulation.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point of the disease-propagation simulation.
 *
 * <p>The class extends {@link Application} and bootstraps the graphical front
 * end: it loads the FXML view from {@code /fxml/main.fxml}, applies the
 * stylesheet at {@code /css/style.css}, and shows a single primary stage
 * titled {@code "Cell Simulation 2D"}.
 *
 * <p>The actual user-interface logic lives in {@link MainController}, which is
 * instantiated by JavaFX when the FXML is loaded. This class therefore only
 * carries the JavaFX lifecycle plumbing and never references the simulation
 * model directly.
 *
 * <p>The minimum window size is locked to {@code 1280 x 800} pixels so that
 * the configuration panel, the central canvas, the statistics panel and the
 * status bar all remain usable; the window can be enlarged but not shrunk
 * below that footprint.
 */
public class Main extends Application {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 800;

    /**
     * Builds and shows the primary stage.
     *
     * <p>Called once by the JavaFX runtime after {@link #main(String[])} has
     * invoked {@link Application#launch(String...)}. The method loads the FXML
     * view, wires the stylesheet to the scene and displays the window.
     *
     * @param primaryStage the primary stage provided by the JavaFX runtime
     * @throws Exception if loading the FXML file or the stylesheet fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setTitle("Cell Simulation 2D");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(WINDOW_WIDTH);
        primaryStage.setMinHeight(WINDOW_HEIGHT);
        primaryStage.show();
    }

    /**
     * Standard Java entry point.
     *
     * <p>Delegates immediately to {@link Application#launch(String...)} which
     * sets up the JavaFX runtime and eventually calls {@link #start(Stage)} on
     * the JavaFX application thread.
     *
     * @param args command-line arguments forwarded to the JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
