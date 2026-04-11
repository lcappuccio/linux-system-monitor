package org.lcappuccio.systemmonitor.collectors;

import java.util.Optional;
import org.lcappuccio.systemmonitor.model.DiskMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects disk temperature metrics.
 */
public class DiskCollector implements Collector<DiskMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(DiskCollector.class);

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  @Override
  public void initialize() {
    status = CollectorStatus.OK;
    LOG.info("DiskCollector initialized");
  }

  @Override
  public Optional<DiskMetrics> collect() {
    return Optional.empty();
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Disks";
  }
}