package org.lcappuccio.systemmonitor.collectors;

import java.util.Optional;
import org.lcappuccio.systemmonitor.model.NetworkMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects network metrics from /proc/net/dev and sysfs.
 */
public class NetworkCollector implements Collector<NetworkMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkCollector.class);

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  @Override
  public void initialize() {
    status = CollectorStatus.OK;
    LOG.info("NetworkCollector initialized");
  }

  @Override
  public Optional<NetworkMetrics> collect() {
    return Optional.empty();
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Network";
  }
}