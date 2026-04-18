package org.lcappuccio.systemmonitor.ui;

import java.util.List;

/**
 * Immutable descriptor for a chart group displayed in {@link ChartPanel}.
 *
 * <p>Each group maps to one {@link javafx.scene.chart.LineChart} containing
 * one series per metric key. Colors and labels are positionally aligned
 * with {@code metricKeys}.
 *
 * @param title        chart title displayed above the chart
 * @param metricKeys   metric keys in {@code "Section.Metric"} format
 * @param seriesColors hex color strings for each series, e.g. {@code "#2196F3"}
 * @param seriesLabels display names for each series shown in the legend
 */
public record ChartGroup(
    String title,
    List<String> metricKeys,
    List<String> seriesColors,
    List<String> seriesLabels
) {}