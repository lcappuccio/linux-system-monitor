package org.lcappuccio.systemmonitor.collectors;

import java.util.Optional;
import org.lcappuccio.systemmonitor.model.CpuMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects CPU metrics from sysfs and hwmon.
 */
public class CpuCollector implements Collector<CpuMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(CpuCollector.class);

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  @Override
  public void initialize() {
    status = CollectorStatus.OK;
    LOG.info("CpuCollector initialized");
  }

  @Override
  public Optional<CpuMetrics> collect() {
    return Optional.empty();
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "CPU";
  }
}