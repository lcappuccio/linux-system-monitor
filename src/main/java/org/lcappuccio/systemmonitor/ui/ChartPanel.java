package org.lcappuccio.systemmonitor.ui;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Right-side panel displaying a live {@link LineChart} for a selected metric.
 *
 * <p>Maintains an in-memory rolling history for every {@link MetricRow} via an internal
 * {@link Timeline} tick. On each tick, the last known value for every tracked metric is
 * appended to its deque — ensuring all histories grow at a uniform rate regardless of
 * how frequently the underlying collector fires.
 *
 * <p>Value changes are captured via JavaFX {@code ChangeListener} on each row's
 * {@code valueProperty()}. Conversion from display strings to doubles is delegated
 * to {@link MetricValueParser}.
 *
 * <p>This class has no knowledge of {@code PollerService} or any collector.
 */
public class ChartPanel {

  private static final Logger LOG = LoggerFactory.getLogger(ChartPanel.class);

  /** Rolling window: 5 minutes at 2-second tick interval. */
  private static final int HISTORY_SIZE = 150;
  private static final double TICK_SECONDS = 2.0;

  private final StackPane root;
  private final Map<String, ArrayDeque<Double>> history;
  private final Map<String, Double> lastKnownValue;
  private final LineChart<Number, Number> chart;
  private final NumberAxis axisX;
  private final NumberAxis axisY;
  private final Timeline timeline;

  private String selectedKey = null;
  private XYChart.Series<Number, Number> activeSeries = null;
  private int tickCount = 0;

  /**
   * Constructs a {@code ChartPanel} and subscribes to value changes for all provided rows.
   *
   * @param rows the observable list of metric rows to monitor
   */
  public ChartPanel(ObservableList<MetricRow> rows) {
    this.history = new HashMap<>();
    this.lastKnownValue = new HashMap<>();
    this.axisX = new NumberAxis();
    this.axisY = new NumberAxis();
    this.chart = buildChart();
    this.root = new StackPane(chart);
    this.timeline = buildTimeline();

    axisX.setLabel("Samples (2s each)");
    axisX.setAutoRanging(true);
    axisY.setAutoRanging(true);

    subscribeToRows(rows);
    timeline.play();
  }

  /**
   * Returns the root pane to embed in the main window's {@link javafx.scene.control.SplitPane}.
   *
   * @return the root {@link StackPane}
   */
  public StackPane getRoot() {
    return root;
  }

  /**
   * Selects or deselects a metric for plotting.
   *
   * <p>If the given row is already selected, it is deselected and the chart is cleared.
   * Otherwise, the chart is populated with existing history for the new selection.
   *
   * @param row the metric row to toggle
   */
  public void toggle(MetricRow row) {
    String key = toKey(row);

    if (key.equals(selectedKey)) {
      deselect();
      return;
    }

    selectedKey = key;
    chart.getData().clear();

    activeSeries = new XYChart.Series<>();
    activeSeries.setName(row.getSection() + " — " + row.getMetric());

    ArrayDeque<Double> deque = history.getOrDefault(key, new ArrayDeque<>());
    int startIndex = 0;
    for (double value : deque) {
      activeSeries.getData().add(new XYChart.Data<>(startIndex++, value));
    }

    chart.getData().add(activeSeries);
  }

  /**
   * Stops the internal timeline. Should be called when the application shuts down.
   */
  public void shutdown() {
    timeline.stop();
  }

  private void deselect() {
    selectedKey = null;
    activeSeries = null;
    chart.getData().clear();
    chart.setTitle("Select a metric from the table");
  }

  private void subscribeToRows(ObservableList<MetricRow> rows) {
    for (MetricRow row : rows) {
      String key = toKey(row);
      history.put(key, new ArrayDeque<>(HISTORY_SIZE));

      row.valueProperty().addListener((obs, oldVal, newVal) ->
          MetricValueParser.parse(newVal).ifPresent(v -> lastKnownValue.put(key, v))
      );
    }
    LOG.info("Subscribed to {} metric rows", rows.size());
  }

  private Timeline buildTimeline() {
    Timeline tl = new Timeline(new KeyFrame(
        Duration.seconds(TICK_SECONDS),
        e -> onTick()
    ));
    tl.setCycleCount(Animation.INDEFINITE);
    return tl;
  }

  private void onTick() {
    tickCount++;

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

      if (key.equals(selectedKey) && activeSeries != null) {
        boolean atCapacity = activeSeries.getData().size() >= HISTORY_SIZE;
        activeSeries.getData().add(new XYChart.Data<>(activeSeries.getData().size(), value));
        if (atCapacity) {
          activeSeries.getData().remove(0);
          // Renumber X values so axis always runs 0..HISTORY_SIZE-1
          for (int i = 0; i < activeSeries.getData().size(); i++) {
            activeSeries.getData().get(i).setXValue(i);
          }
        }
      }
    }
  }

  private LineChart<Number, Number> buildChart() {
    LineChart<Number, Number> lineChart = new LineChart<>(axisX, axisY);
    lineChart.setAnimated(false);
    lineChart.setCreateSymbols(false);
    lineChart.setTitle("Select a metric from the table");
    return lineChart;
  }

  private static String toKey(MetricRow row) {
    return row.getSection() + "." + row.getMetric();
  }
}