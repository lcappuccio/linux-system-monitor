package org.lcappuccio.systemmonitor.collectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
 * Collects filesystem metrics using the df command.
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
      ProcessBuilder pb = new ProcessBuilder("df", mountPoint);
      pb.redirectErrorStream(true);
      Process process = pb.start();
      int exitCode = process.waitFor();
      return exitCode == 0;
    } catch (IOException | InterruptedException e) {
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
          var usage = getUsageForMount(mountPoint);
          if (usage != null) {
            usageMap.put(mountPoint, usage);
          }
        } catch (Exception e) {
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

  private FileSystemMetrics.FileSystemUsage getUsageForMount(String mountPoint) throws IOException {
    ProcessBuilder pb = new ProcessBuilder("df", "-B1", mountPoint);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
      String line;
      // Skip header line
      reader.readLine();
      line = reader.readLine();

      if (line == null) {
        return null;
      }

      // df --block-size=1 output: Filesystem 1B-blocks Used Available Use% Mounted on
      String[] parts = line.trim().split("\\s+");
      if (parts.length < 4) {
        return null;
      }

      // parts: [0]=Filesystem, [1]=1B-blocks, [2]=Used, [3]=Available, [4]=Use%, [5]=Mounted
      long total = Long.parseLong(parts[1]);
      long used = Long.parseLong(parts[2]);
      long free = Long.parseLong(parts[3]);

      return new FileSystemMetrics.FileSystemUsage(used, free, total);
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