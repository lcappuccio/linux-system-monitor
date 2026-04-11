package org.lcappuccio.systemmonitor.model;

/**
 * Immutable snapshot of storage device temperatures.
 *
 * @param nvmeTempCelsius NVMe composite temperature in °C
 * @param sataTempCelsius SATA SSD temperature in °C
 */
public record DiskMetrics(
        double nvmeTempCelsius,
        double sataTempCelsius
) {}