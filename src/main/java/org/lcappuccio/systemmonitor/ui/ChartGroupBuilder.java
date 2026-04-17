package org.lcappuccio.systemmonitor.ui;

import java.util.List;

/**
 * Builder for creating {@link ChartGroup} instances.
 */
class ChartGroupBuilder {
  private String title;
  private List<String> metricKeys;
  private List<String> seriesColors;
  private List<String> seriesLabels;

  ChartGroupBuilder title(String title) {
    this.title = title;
    return this;
  }

  ChartGroupBuilder metricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  ChartGroupBuilder seriesColors(List<String> seriesColors) {
    this.seriesColors = seriesColors;
    return this;
  }

  ChartGroupBuilder seriesLabels(List<String> seriesLabels) {
    this.seriesLabels = seriesLabels;
    return this;
  }

  ChartGroup build() {
    return new ChartGroup(title, metricKeys, seriesColors, seriesLabels);
  }
}