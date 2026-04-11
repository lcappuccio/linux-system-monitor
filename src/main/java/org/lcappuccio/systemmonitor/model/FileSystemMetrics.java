package org.lcappuccio.systemmonitor.model;

import java.util.Map;

/**
 * Immutable snapshot of filesystem usage for all configured mount points.
 *
 * <p>The map key is the mount point path (e.g. {@code /}, {@code /home}).
 *
 * @param usage map of mount point to {@link FileSystemUsage}
 */
public record FileSystemMetrics (
        Map<String, FileSystemUsage> usage
) {

    /**
     * Usage data for a single mount point.
     *
     * @param usedBytes  used space in bytes
     * @param freeBytes  free space in bytes
     * @param totalBytes total space in bytes
     */
    public record FileSystemUsage (
            long usedBytes,
            long freeBytes,
            long totalBytes
    ) {}
}