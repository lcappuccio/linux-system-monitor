package org.lcappuccio.systemmonitor.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Right-side panel displaying fixed chart groups for temperature, load, memory and frequencies.
 *
 * <p>Each chart group is always visible and always accumulating history, regardless of
 * which metric is selected in the table. The left table has no interaction with this panel.
 *
 * <p>History is maintained via an internal {@link Timeline} tick at 2-second intervals.
 * On each tick, the last known value for every tracked metric is appended to its deque, ensuring uniform history growth
 * regardless of collector polling interval.
 *
 * <p>Value changes are captured via JavaFX {@code ChangeListener} on each row's
 * {@code valueProperty()}. Conversion is delegated to {@link MetricValueParser}. This class has no knowledge of
 * {@code PollerService} or any collector.
 */
public class ChartPanel {

  private static final Logger LOG = LoggerFactory.getLogger(ChartPanel.class);

  /**
   * Rolling window: 5 minutes at 2-second tick interval.
   */
  private static final int HISTORY_SIZE = 150;
  private static final double TICK_SECONDS = 2.0;
  private static final int CHART_MIN_HEIGHT = 150;

  private final VBox root;
  private final Map<String, ArrayDeque<Double>> history;
  private final Map<String, Double> lastKnownValue;
  private final Map<String, XYChart.Series<Number, Number>> seriesMap;
  private final Timeline timeline;

  private final String colorCpu;
  private final String colorGpu;
  private final String colorVram;
  private final String colorNvme;
  private final String colorSata;
  private final String colorMemoryUsed;
  private final String colorSwapUsed;
  private final List<String> colorCpuClocks;

  /**
   * Constructs a {@code ChartPanel}, builds all chart groups, and subscribes to all rows.
   *
   * @param rows      the observable list of metric rows to monitor
   * @param appConfig
   */
  public ChartPanel(ObservableList<MetricRow> rows, AppConfig appConfig) {
    this.history = new HashMap<>();
    this.lastKnownValue = new HashMap<>();
    this.seriesMap = new HashMap<>();
    this.root = new VBox(4);
    this.root.setPadding(new Insets(4));
    this.timeline = buildTimeline();

    this.colorCpu = appConfig.getColorCpu();
    this.colorGpu = appConfig.getColorGpu();
    this.colorVram = appConfig.getColorVram();
    this.colorNvme = appConfig.getColorNvme();
    this.colorSata = appConfig.getColorSata();
    this.colorMemoryUsed = appConfig.getColorMemoryUsed();
    this.colorSwapUsed = appConfig.getColorSwapUsed();
    this.colorCpuClocks = appConfig.getColorCpuClocks();

    List<ChartGroup> groups = buildGroups(rows);
    for (ChartGroup group : groups) {
      LineChart<Number, Number> chart = buildChart(group);
      VBox.setVgrow(chart, Priority.ALWAYS);
      root.getChildren().add(chart);
    }

    subscribeToRows(rows);
    timeline.play();
    LOG.debug("ChartPanel initialized with {} groups", groups.size());
  }

  /**
   * Returns the root {@link VBox} to embed in the main window's {@link javafx.scene.control.SplitPane}.
   *
   * @return the root pane
   */
  public VBox getRoot() {
    return root;
  }

  /**
   * Stops the internal timeline. Must be called from {@code MainWindow.shutdown()}.
   */
  public void shutdown() {
    timeline.stop();
  }

  private List<ChartGroup> buildGroups(ObservableList<MetricRow> rows) {
    List<ChartGroup> groups = new ArrayList<>();

    groups.add(new ChartGroup(
        "Temperature (°C)",
        List.of("CPU.Temperature", "GPU.Temperature", "GPU.VRAM Temperature",
            "Disks.NVMe Temperature", "Disks.SSD Temperature"),
        List.of(colorCpu, colorGpu, colorVram, colorNvme, colorSata),
        List.of("CPU", "GPU", "VRAM", "NVMe", "SSD")
    ));
    groups.add(new ChartGroup(
        "Load (%)",
        List.of("CPU.Load", "GPU.Load", "GPU.VRAM Load"),
        List.of(colorCpu, colorGpu, colorVram),
        List.of("CPU", "GPU", "VRAM")
    ));
    groups.add(new ChartGroup(
        "Memory (GB)",
        List.of("Memory.Used", "Memory.Swap Used"),
        List.of(colorMemoryUsed, colorSwapUsed),
        List.of("RAM", "Swap")
    ));

    // Frequencies — dynamic: only add cores that exist in rows
    List<String> coreKeys = new ArrayList<>();
    List<String> coreColors = new ArrayList<>();
    List<String> coreLabels = new ArrayList<>();

    for (int i = 0; i < 8; i++) {
      String key = "CPU.Core " + i;
      boolean exists = rows.stream().anyMatch(r -> (r.getSection() + "."
          + r.getMetric()).equals(key));
      if (exists) {
        coreKeys.add(key);
        coreColors.add(colorCpuClocks.get(i % colorCpuClocks.size()));
        coreLabels.add("Core " + i);
      }
    }
    if (!coreKeys.isEmpty()) {
      groups.add(new ChartGroup("Frequencies (GHz)", coreKeys, coreColors, coreLabels));
    }

    return groups;
  }

  private LineChart<Number, Number> buildChart(ChartGroup group) {
    NumberAxis axisX = new NumberAxis();
    axisX.setAutoRanging(true);
    axisX.setTickLabelsVisible(false);
    axisX.setTickMarkVisible(false);

    NumberAxis axisY = new NumberAxis();
    axisY.setAutoRanging(true);

    // Fix Load chart Y axis to 0-100
    if (group.title().startsWith("Load")) {
      axisY.setAutoRanging(false);
      axisY.setLowerBound(0);
      axisY.setUpperBound(100);
      axisY.setTickUnit(25);
    }

    LineChart<Number, Number> chart = new LineChart<>(axisX, axisY);
    chart.setTitle(group.title());
    chart.setAnimated(false);
    chart.setCreateSymbols(false);
    chart.setLegendVisible(true);
    chart.setMinHeight(CHART_MIN_HEIGHT);

    for (int i = 0; i < group.metricKeys().size(); i++) {
      String key = group.metricKeys().get(i);
      String label = group.seriesLabels().get(i);

      XYChart.Series<Number, Number> series = new XYChart.Series<>();
      series.setName(label);
      chart.getData().add(series);
      seriesMap.put(key, series);

      // Apply color after node is attached to scene
      String color = group.seriesColors().get(i);
      int finalI = i;
      series.nodeProperty().addListener((obs, oldNode, newNode) -> {
        if (newNode != null) {
          newNode.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 0.5px;");
        }
        // Color legend symbol
        if (chart.getData().size() > finalI) {
          var legendItems = chart.lookupAll(".chart-legend-item-symbol");
          legendItems.forEach(node -> {
            if (node.getUserData() != null && node.getUserData().equals(label)) {
              node.setStyle("-fx-background-color: " + color + ";");
            }
          });
        }
      });
    }

    return chart;
  }

  private void subscribeToRows(ObservableList<MetricRow> rows) {
    for (MetricRow row : rows) {
      String key = row.getSection() + "." + row.getMetric();
      history.put(key, new ArrayDeque<>(HISTORY_SIZE));
      row.valueProperty().addListener((obs, oldVal, newVal) ->
          MetricValueParser.parse(newVal).ifPresent(v -> lastKnownValue.put(key, v))
      );
    }
    LOG.debug("Subscribed to {} metric rows", rows.size());
  }

  private Timeline buildTimeline() {
    Timeline timeline = new Timeline(new KeyFrame(
        Duration.seconds(TICK_SECONDS),
        e -> onTick()
    ));
    timeline.setCycleCount(Animation.INDEFINITE);
    return timeline;
  }

  private void onTick() {
    for (Map.Entry<String, ArrayDeque<Double>> entry : history.entrySet()) {
      String key = entry.getKey();
      ArrayDeque<Double> deque = entry.getValue();
      Double value = lastKnownValue.get(key);

      if (value == null) {
        continue;
      }

      if (deque.size() >= HISTORY_SIZE) {
        deque.pollFirst();
      }
      deque.addLast(value);

      XYChart.Series<Number, Number> series = seriesMap.get(key);
      if (series != null) {
        boolean atCapacity = series.getData().size() >= HISTORY_SIZE;
        series.getData().add(new XYChart.Data<>(series.getData().size(), value));
        if (atCapacity) {
          series.getData().remove(0);
          for (int i = 0; i < series.getData().size(); i++) {
            series.getData().get(i).setXValue(i);
          }
        }
      }
    }
  }
}
