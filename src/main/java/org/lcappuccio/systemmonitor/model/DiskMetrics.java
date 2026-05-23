package org.lcappuccio.systemmonitor.model;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable snapshot of storage device temperatures.
 *
 * @param temperatures map of disk label (model name) to temperature in °C
 */
public record DiskMetrics(
    Map<String, Double> temperatures
) {

  public DiskMetrics {
    temperatures = Collections.unmodifiableMap(temperatures);
  }

}
