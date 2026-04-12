package org.lcappuccio.systemmonitor.collectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import org.lcappuccio.systemmonitor.model.MemoryMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects memory and swap usage metrics from /proc/meminfo.
 */
public class MemoryCollector implements Collector<MemoryMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryCollector.class);
  private static final String PROC_MEMINFO = "/proc/meminfo";

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  @Override
  public void initialize() {
    try (BufferedReader reader = new BufferedReader(new FileReader(PROC_MEMINFO))) {
      if (reader.readLine() != null) {
        status = CollectorStatus.OK;
        LOG.info("MemoryCollector initialized successfully");
      }
    } catch (IOException e) {
      LOG.error("Failed to initialize MemoryCollector: {}", e.getMessage());
      status = CollectorStatus.UNAVAILABLE;
    }
  }

  @Override
  public Optional<MemoryMetrics> collect() {
    if (status != CollectorStatus.OK && status != CollectorStatus.DEGRADED) {
      return Optional.empty();
    }
    try {
      return Optional.of(readMemoryMetrics());
    } catch (IOException e) {
      LOG.error("Failed to collect memory metrics: {}", e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Memory";
  }

  private MemoryMetrics readMemoryMetrics() throws IOException {
    long memTotal = 0;
    long memAvailable = 0;
    long swapTotal = 0;
    long swapFree = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(PROC_MEMINFO))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("MemTotal:")) {
          memTotal = parseKilobytes(line);
        } else if (line.startsWith("MemAvailable:")) {
          memAvailable = parseKilobytes(line);
        } else if (line.startsWith("SwapTotal:")) {
          swapTotal = parseKilobytes(line);
        } else if (line.startsWith("SwapFree:")) {
          swapFree = parseKilobytes(line);
        }
      }
    }

    long memUsed = memTotal - memAvailable;
    long swapUsed = swapTotal - swapFree;

    return new MemoryMetrics(
        memUsed * 1024,
        memTotal * 1024,
        swapUsed * 1024,
        swapTotal * 1024);
  }

  private long parseKilobytes(String line) {
    String[] parts = line.trim().split("\\s+");
    if (parts.length >= 2) {
      try {
        return Long.parseLong(parts[1]);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }
}