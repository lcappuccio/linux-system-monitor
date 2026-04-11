package org.lcappuccio.systemmonitor.model;

/**
 * Immutable snapshot of network interface metrics.
 *
 * @param ipAddress       current IPv4 address of the interface
 * @param linkSpeedMbps   link speed in Mbit/s as reported by the kernel
 * @param uploadBytesPerSec   current upload rate in bytes/s (delta calculation)
 * @param downloadBytesPerSec current download rate in bytes/s (delta calculation)
 */
public record NetworkMetrics(
        String ipAddress,
        int linkSpeedMbps,
        long uploadBytesPerSec,
        long downloadBytesPerSec
) {}