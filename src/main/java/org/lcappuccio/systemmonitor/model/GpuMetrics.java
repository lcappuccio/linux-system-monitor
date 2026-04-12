package org.lcappuccio.systemmonitor.model;

/**
 * Immutable snapshot of AMD GPU metrics.
 *
 * @param temperatureCelsius     GPU junction temperature in °C
 * @param loadPercent            GPU load percentage (0–100)
 * @param vramUsedBytes          VRAM used in bytes
 * @param vramTotalBytes         VRAM total in bytes
 * @param vramTemperatureCelsius VRAM temperature in °C
 * @param vramLoadPercent        VRAM controller load percentage (0–100)
 * @param powerWatts             GPU power draw in watts (PPT)
 * @param fanRpm               GPU fan speed in RPM
 */
public record GpuMetrics(
    double temperatureCelsius,
    double loadPercent,
    long vramUsedBytes,
    long vramTotalBytes,
    double vramTemperatureCelsius,
    double vramLoadPercent,
    double powerWatts,
    double fanRpm
) {

}