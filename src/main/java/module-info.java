module org.lcappuccio.systemmonitor {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.slf4j;
  requires ch.qos.logback.classic;
  requires java.management;
  requires java.net.http;

  opens org.lcappuccio.systemmonitor to javafx.graphics;
  opens org.lcappuccio.systemmonitor.ui to javafx.fxml;
}