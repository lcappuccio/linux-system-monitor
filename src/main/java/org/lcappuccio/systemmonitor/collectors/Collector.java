package org.lcappuccio.systemmonitor.collectors;

import java.util.Optional;

import org.lcappuccio.systemmonitor.collectors.CollectorStatus;

/**
 * Contract for all system metric collectors.
 *
 * <p>Implementations read from sysfs, procfs, or external processes and return
 * an immutable result record. All method signatures use only {@code java.*} types.
 *
 * <p>Rules:
 * <ul>
 *   <li>Never return null — use {@link Optional#empty()} to express absence.</li>
 *   <li>Never block the JavaFX Application Thread.</li>
 *   <li>Never throw unchecked exceptions — handle internally and reflect in status.</li>
 * </ul>
 *
 * @param <T> the immutable result record type produced by this collector
 */
public interface Collector<T> {

    /**
     * Initializes the collector, discovers hardware paths, and validates configuration.
     *
     * <p>Must be called once before {@link #collect()}. Sets the collector status to
     * {@link CollectorStatus#UNAVAILABLE} if required paths or devices are missing.
     */
    void initialize();

    /**
     * Collects the current metrics snapshot.
     *
     * <p>Returns {@link Optional#empty()} if the collector status is not {@link CollectorStatus#OK}
     * or {@link CollectorStatus#DEGRADED}, or if a transient read error occurs.
     *
     * @return an {@link Optional} containing the metrics record, or empty on failure
     */
    Optional<T> collect();

    /**
     * Returns the current operational status of this collector.
     *
     * @return the {@link CollectorStatus}
     */
    CollectorStatus getStatus();

    /**
     * Returns a human-readable name for this collector, used in logging and UI.
     *
     * @return collector name
     */
    String getName();
}