package org.lcappuccio.systemmonitor.collectors;

/**
 * Represents the operational status of a {@link Collector} instance.
 *
 * <ul>
 *   <li>{@code OK} - collector is fully operational</li>
 *   <li>{@code DEGRADED} - collector is partially operational (some metrics unavailable)</li>
 *   <li>{@code UNAVAILABLE} - collector failed to initialize or all paths are missing</li>
 * </ul>
 */
public enum CollectorStatus {
    OK,
    DEGRADED,
    UNAVAILABLE
}