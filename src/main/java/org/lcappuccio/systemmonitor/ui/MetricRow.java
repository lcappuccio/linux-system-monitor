package org.lcappuccio.systemmonitor.ui;

import java.util.Optional;
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

  private final String color;
  private final String section;
  private final String metric;
  private final StringProperty value;
  private final boolean isHardwareNode;
  private final String parentSection;

  public MetricRow(String section, String metric, String initialValue) {
    this(section, metric, initialValue, false, null, null);
  }

  /**
   * Constructs a {@code MetricRow} with hierarchical support.
   *
   * @param section the display group
   * @param metric the metric name
   * @param initialValue the initial display value
   * @param isHardwareNode true if parent node
   * @param parentSection parent section name
   */
  public MetricRow(String section, String metric, String initialValue,
      boolean isHardwareNode, String parentSection) {
    this(section, metric, initialValue, isHardwareNode, parentSection, null);
  }

  /**
   * Constructs a {@code MetricRow} with color.
   *
   * @param section the display group
   * @param metric the metric name
   * @param initialValue the initial display value
   * @param isHardwareNode true if parent node
   * @param parentSection parent section name
   * @param color hex color for node
   */
  public MetricRow(String section, String metric, String initialValue,
      boolean isHardwareNode, String parentSection, String color) {
    this.section = section;
    this.metric = metric;
    this.value = new SimpleStringProperty(initialValue);
    this.isHardwareNode = isHardwareNode;
    this.parentSection = parentSection;
    this.color = color;
  }

  public String getSection() {
    return section;
  }

  public String getMetric() {
    return metric;
  }

  public String getValue() {
    return value.get();
  }

  public void setValue(String newValue) {
    value.set(newValue);
  }

  public StringProperty valueProperty() {
    return value;
  }

  public boolean isHardwareNode() {
    return isHardwareNode;
  }

  public String getParentSection() {
    return parentSection;
  }

  public Optional<String> getColor() {
    return Optional.ofNullable(color);
  }
}