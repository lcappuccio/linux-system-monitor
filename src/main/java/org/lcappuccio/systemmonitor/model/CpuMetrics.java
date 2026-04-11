package org.lcappuccio.systemmonitor.model;

import java.util.List;

/**
 * Immutable snapshot of CPU metrics.
 *
 * @param temperatureCelsius CPU temperature in °C (Tccd1)
 * @param loadPercent        overall CPU load percentage (0–100)
 * @param coreFrequenciesGhz per-core frequency in GHz, indexed by physical core number
 */
public record CpuMetrics(
    double temperatureCelsius,
    double loadPercent,
    List<Double> coreFrequenciesGhz
) {

}