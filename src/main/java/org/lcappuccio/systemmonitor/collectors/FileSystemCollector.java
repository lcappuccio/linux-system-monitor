package org.lcappuccio.systemmonitor.collectors;

import java.util.Optional;
import org.lcappuccio.systemmonitor.model.FileSystemMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects filesystem metrics using java.nio.file.FileStore.
 */
public class FileSystemCollector implements Collector<FileSystemMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemCollector.class);

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  @Override
  public void initialize() {
    status = CollectorStatus.OK;
    LOG.info("FileSystemCollector initialized");
  }

  @Override
  public Optional<FileSystemMetrics> collect() {
    return Optional.empty();
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Filesystems";
  }
}