package org.lcappuccio.systemmonitor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point.
 *
 * <p>Loads configuration, builds the main window, and starts the JavaFX runtime.
 */
public class Main extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  private static final int WINDOW_WIDTH = 1200;
  private static final int WINDOW_HEIGHT = 800;

  /**
   * JavaFX application entry point.
   *
   * @param stage the primary stage provided by the JavaFX runtime
   */
  @Override
  public void start(Stage stage) {
    LOG.info("Starting Linux System Monitor");
    AppConfig config = AppConfig.load();

    MainWindow mainWindow = new MainWindow(config);
    Scene scene = new Scene(mainWindow.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

    stage.setTitle("Linux System Monitor");
    stage.setScene(scene);
    stage.setOnCloseRequest(e -> {
      LOG.info("Shutting down");
      mainWindow.shutdown();
    });
    stage.show();
  }

  /**
   * Application main method.
   *
   * @param args command-line arguments (unused)
   */
  public static void main(String[] args) {
    launch(args);
  }
}