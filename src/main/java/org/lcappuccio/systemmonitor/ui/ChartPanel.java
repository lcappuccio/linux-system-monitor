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
import org.lcappuccio.systemmonitor.model.MetricKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Right-side panel displaying fixed chart groups for temperature, load, memory and frequencies.
 *
 * <p>Each chart group is always visible and always accumulating history, regardless of
 * which metric is selected in the table. The left table has no interaction with this panel.
 *
 * <p>History is maintained via an internal {@link Timeline} tick at 2-second intervals.
 * On each tick, the last known value for every tracked metric is appended to its deque,
 * ensuring uniform history growth regardless of collector polling interval.
 *
 * <p>Value changes are captured via JavaFX {@code ChangeListener} on each row's
 * {@code valueProperty()}. Conversion is delegated to {@link MetricValueParser}.
 * This class has no knowledge of {@code PollerService} or any collector.
 */
public class ChartPanel {

  private static final Logger LOG = LoggerFactory.getLogger(ChartPanel.class);

  private static final int CHART_MIN_HEIGHT = 150;

  private final int historySize;
  private final double tickSeconds;

  private final VBox root;
  private final Map<String, ArrayDeque<Double>> history;
  private final Map<String, Double> lastKnownValue;
  private final Map<String, XYChart.Series<Number, Number>> seriesMap;
  private final Timeline timeline;

  private final AppConfig appConfig;

  /**
   * Constructs a {@code ChartPanel}, builds all chart groups, and subscribes to all rows.
   *
   * @param rows      the observable list of metric rows to monitor
   * @param appConfig the configuration object
   */
  public ChartPanel(ObservableList<MetricRow> rows, AppConfig appConfig) {
    this.history = new HashMap<>();
    this.lastKnownValue = new HashMap<>();
    this.seriesMap = new HashMap<>();
    this.root = new VBox(4);
    this.root.setPadding(new Insets(4));

    this.historySize = appConfig.getHistorySize();
    this.tickSeconds = appConfig.getTickSeconds();
    this.timeline = buildTimeline();
    this.appConfig = appConfig;

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
   * Returns the root {@link VBox} to embed in the main window's
   * {@link javafx.scene.control.SplitPane}.
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

    if (appConfig.isChartCpuEnabled()) {
      groups.add(new ChartGroup(
          "Temperature (°C)",
          List.of(MetricKey.Cpu.TEMPERATURE.key(), MetricKey.Gpu.TEMPERATURE.key(),
              MetricKey.Gpu.VRAM_TEMPERATURE.key(), MetricKey.Disk.NVME_TEMPERATURE.key(),
              MetricKey.Disk.SSD_TEMPERATURE.key()),
          List.of(appConfig.getColorCpu(), appConfig.getColorGpu(), appConfig.getColorVram(),
              appConfig.getColorNvme(), appConfig.getColorSata()),
          List.of("CPU", "GPU", "VRAM", "NVMe", "SSD")
      ));
    }
    if (appConfig.isChartLoadEnabled()) {
      groups.add(new ChartGroup(
          "Load (%)",
          List.of(MetricKey.Cpu.LOAD.key(), MetricKey.Gpu.LOAD.key(),
              MetricKey.Gpu.VRAM_LOAD.key()),
          List.of(appConfig.getColorCpu(), appConfig.getColorGpu(), appConfig.getColorVram()),
          List.of("CPU", "GPU", "VRAM")
      ));
    }
    if (appConfig.isChartMemoryEnabled()) {
      groups.add(new ChartGroup(
          "Memory (GB)",
          List.of(MetricKey.Mem.USED.key(), MetricKey.Mem.SWAP_USED.key(),
              MetricKey.Gpu.VRAM_USED.key()),
          List.of(appConfig.getColorMemoryUsed(), appConfig.getColorSwapUsed(),
              appConfig.getColorVram()),
          List.of("RAM", "Swap", "VRAM")
      ));
    }
    if (appConfig.isChartFrequencyEnabled()) {
      List<String> coreKeys = new ArrayList<>();
      List<String> coreColors = new ArrayList<>();
      List<String> coreLabels = new ArrayList<>();
      List<String> colorCpuClocks = appConfig.getColorCpuClocks();

      for (int i = 0; i < 8; i++) {
        String key = MetricKey.CPU.key("Core " + i);
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
    }

    return groups;
  }

  private LineChart<Number, Number> buildChart(ChartGroup group) {
    NumberAxis axisX = new NumberAxis();
    axisX.setAutoRanging(false);
    axisX.setLowerBound(0);
    axisX.setUpperBound(historySize - 1d);
    axisX.setTickUnit(historySize / 5.0);
    axisX.setTickLabelsVisible(true);
    axisX.setTickMarkVisible(true);

    NumberAxis axisY = new NumberAxis();
    axisY.setAutoRanging(true);

    LineChart<Number, Number> chart = new LineChart<>(axisX, axisY);
    chart.setTitle(group.title());
    chart.setAnimated(false);
    chart.setCreateSymbols(false);
    chart.setLegendVisible(true);
    chart.setMinHeight(CHART_MIN_HEIGHT);

    // Inject CSS before adding series — applied on first layout pass, no timing issues
    String css = buildChartCss(group);
    chart.getStylesheets().add(
        "data:text/css," + java.net.URLEncoder.encode(
            css, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));

    for (int i = 0; i < group.metricKeys().size(); i++) {
      String key = group.metricKeys().get(i);
      String label = group.seriesLabels().get(i);

      XYChart.Series<Number, Number> series = new XYChart.Series<>();
      series.setName(label);
      chart.getData().add(series);
      seriesMap.put(key, series);
    }

    return chart;
  }

  private String buildChartCss(ChartGroup group) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < group.seriesColors().size(); i++) {
      String color = group.seriesColors().get(i);
      sb.append(".default-color").append(i)
          .append(".chart-series-line { -fx-stroke: ").append(color)
          .append("; -fx-stroke-width: 1.5px; }\n");
      sb.append(".default-color").append(i)
          .append(".chart-legend-item-symbol { -fx-background-color: ").append(color)
          .append(", white; }\n");
    }
    return sb.toString();
  }

  private void subscribeToRows(ObservableList<MetricRow> rows) {
    for (MetricRow row : rows) {
      String key = row.getSection() + "." + row.getMetric();
      history.put(key, new ArrayDeque<>(historySize));
      row.valueProperty().addListener((obs, oldVal, newVal) ->
          MetricValueParser.parse(newVal).ifPresent(v -> lastKnownValue.put(key, v))
      );
    }
    LOG.debug("Subscribed to {} metric rows", rows.size());
  }

  private Timeline buildTimeline() {
    Timeline tl = new Timeline(new KeyFrame(
        Duration.seconds(tickSeconds),
        e -> onTick()
    ));
    tl.setCycleCount(Animation.INDEFINITE);
    return tl;
  }

  private void onTick() {
    for (Map.Entry<String, ArrayDeque<Double>> entry : history.entrySet()) {
      String key = entry.getKey();
      ArrayDeque<Double> deque = entry.getValue();
      Double value = lastKnownValue.get(key);

      if (value == null) {
        continue;
      }

      if (deque.size() >= historySize) {
        deque.pollFirst();
      }
      deque.addLast(value);

      XYChart.Series<Number, Number> series = seriesMap.get(key);
      if (series != null) {
        boolean atCapacity = series.getData().size() >= historySize;
        series.getData().add(new XYChart.Data<>(series.getData().size(), value));
        if (atCapacity) {
          series.getData().removeFirst();
          for (int i = 0; i < series.getData().size(); i++) {
            series.getData().get(i).setXValue(i);
          }
        }
      }
    }
  }
}
