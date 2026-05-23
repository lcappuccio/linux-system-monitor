package org.lcappuccio.systemmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
  private static final int WINDOW_WIDTH = 1680;
  private static final int WINDOW_HEIGHT = 900;

  /**
   * JavaFX application entry point.
   *
   * @param stage the primary stage provided by the JavaFX runtime
   */
  @Override
  public void start(Stage stage) {
    LOG.info("Starting Linux System Monitor");
    AppConfig config = AppConfig.load();
    String version = loadVersion();

    MainWindow mainWindow = new MainWindow(config, version);
    Scene scene = new Scene(mainWindow.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

    stage.setTitle("Linux System Monitor v" + version);
    stage.setScene(scene);
    stage.setOnCloseRequest(e -> {
      LOG.info("Shutting down");
      mainWindow.shutdown();
    });
    stage.show();
  }

  private static String loadVersion() {
    Properties props = new Properties();
    try (InputStream in = Main.class.getResourceAsStream("/config.properties")) {
      if (in != null) {
        props.load(in);
        return props.getProperty("app.version", "0.0.0");
      }
    } catch (IOException e) {
      LOG.warn("Could not load config.properties", e);
    }
    return "0.0.0";
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