package org.lcappuccio.systemmonitor.collectors;

import java.util.Optional;
import org.lcappuccio.systemmonitor.model.GpuMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects GPU metrics from sysfs and hwmon.
 */
public class GpuCollector implements Collector<GpuMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(GpuCollector.class);

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  @Override
  public void initialize() {
    status = CollectorStatus.OK;
    LOG.info("GpuCollector initialized");
  }

  @Override
  public Optional<GpuMetrics> collect() {
    return Optional.empty();
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "GPU";
  }
}