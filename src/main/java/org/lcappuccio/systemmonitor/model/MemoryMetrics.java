package org.lcappuccio.systemmonitor.model;

/**
 * Immutable snapshot of memory and swap usage.
 *
 * @param memUsedBytes   used RAM in bytes
 * @param memTotalBytes  total RAM in bytes
 * @param swapUsedBytes  used swap in bytes
 * @param swapTotalBytes total swap in bytes
 */
public record MemoryMetrics(
    long memUsedBytes,
    long memTotalBytes,
    long swapUsedBytes,
    long swapTotalBytes) {

}