package org.lcappuccio.systemmonitor.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Observable model for a single row in the metrics {@code TableView}.
 *
 * <p>Each row represents one metric value, grouped by section.
 * JavaFX binds directly to the {@link StringProperty} fields for live updates.
 * All updates to {@code value} must be performed on the JavaFX Application Thread.
 */
public final class MetricRow {

  private final String section;
  private final String metric;
  private final StringProperty value;

  /**
   * Constructs a new {@code MetricRow} with an initial value.
   *
   * @param section the display group this metric belongs to (e.g. {@code "CPU"}, {@code "GPU"})
   * @param metric  the metric name (e.g. {@code "Temperature"}, {@code "Load"})
   * @param initialValue the initial display value (e.g. {@code "—"} when not yet available)
   */
  public MetricRow(String section, String metric, String initialValue) {
    this.section = section;
    this.metric = metric;
    this.value = new SimpleStringProperty(initialValue);
  }

  /**
   * Returns the section name this row belongs to.
   *
   * @return section name
   */
  public String getSection() {
    return section;
  }

  /**
   * Returns the metric name for this row.
   *
   * @return metric name
   */
  public String getMetric() {
    return metric;
  }

  /**
   * Returns the current display value.
   *
   * @return current value string
   */
  public String getValue() {
    return value.get();
  }

  /**
   * Updates the display value.
   *
   * <p>Must be called from the JavaFX Application Thread.
   *
   * @param newValue the new display value
   */
  public void setValue(String newValue) {
    value.set(newValue);
  }

  /**
   * Returns the {@link StringProperty} for JavaFX {@code TableView} binding.
   *
   * @return the value property
   */
  public StringProperty valueProperty() {
    return value;
  }
}