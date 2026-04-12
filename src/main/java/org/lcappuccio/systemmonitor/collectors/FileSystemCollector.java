package org.lcappuccio.systemmonitor.collectors;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.FileSystemMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects filesystem metrics using java.nio.file.FileStore.
 */
public class FileSystemCollector implements Collector<FileSystemMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemCollector.class);

  private final AppConfig config;
  private final List<String> mountPoints;
  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  public FileSystemCollector(AppConfig config) {
    this.config = config;
    this.mountPoints = config.getFsMountpoints();
  }

  @Override
  public void initialize() {
    List<String> validMounts = mountPoints.stream()
        .filter(this::isMountValid)
        .collect(Collectors.toList());

    if (validMounts.isEmpty()) {
      status = CollectorStatus.UNAVAILABLE;
      LOG.error("No valid mount points found");
      return;
    }

    if (validMounts.size() < mountPoints.size()) {
      status = CollectorStatus.DEGRADED;
      LOG.warn("Some mount points unavailable: {}/{} valid",
          validMounts.size(), mountPoints.size());
    } else {
      status = CollectorStatus.OK;
    }

    LOG.info("FileSystemCollector initialized: {} mount points valid", validMounts.size());
  }

  private boolean isMountValid(String mountPoint) {
    try {
      Path path = Path.of(mountPoint);
      if (!Files.exists(path)) {
        LOG.warn("Mount point does not exist: {}", mountPoint);
        return false;
      }
      FileStore store = Files.getFileStore(path);
      return store != null;
    } catch (IOException | SecurityException e) {
      LOG.error("Cannot access mount point {}: {}", mountPoint, e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<FileSystemMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    try {
      Map<String, FileSystemMetrics.FileSystemUsage> usageMap = new HashMap<>();

      for (String mountPoint : mountPoints) {
        try {
          Path path = Path.of(mountPoint);
          if (!Files.exists(path)) {
            continue;
          }

          FileStore store = Files.getFileStore(path);
          long total = store.getTotalSpace();
          long free = store.getUsableSpace();
          long used = total - free;

          usageMap.put(mountPoint, new FileSystemMetrics.FileSystemUsage(used, free, total));
        } catch (IOException e) {
          LOG.error("Failed to read mount point {}: {}", mountPoint, e.getMessage());
        }
      }

      if (usageMap.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(new FileSystemMetrics(usageMap));
    } catch (Exception e) {
      LOG.error("Failed to collect filesystem metrics: {}", e.getMessage());
      return Optional.empty();
    }
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