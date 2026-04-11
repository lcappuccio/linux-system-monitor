package org.lcappuccio.systemmonitor.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.lcappuccio.systemmonitor.config.AppConfig;

class MainWindowTest {

  @Test
  void constructor_doesNotThrow() {
    AppConfig config = AppConfig.load();
    assertDoesNotThrow(() -> new MainWindow(config));
  }

  @Test
  void getRoot_doesNotThrow() {
    AppConfig config = AppConfig.load();
    MainWindow window = new MainWindow(config);
    assertDoesNotThrow(() -> window.getRoot());
  }

  @Test
  void getRoot_returnsNonNull() {
    AppConfig config = AppConfig.load();
    MainWindow window = new MainWindow(config);
    Parent root = window.getRoot();
    assertNotNull(root);
  }

  @Test
  void shutdown_doesNotThrow() {
    AppConfig config = AppConfig.load();
    MainWindow window = new MainWindow(config);
    assertDoesNotThrow(() -> window.shutdown());
  }
}