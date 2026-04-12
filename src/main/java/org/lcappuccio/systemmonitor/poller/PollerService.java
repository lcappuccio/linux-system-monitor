package org.lcappuccio.systemmonitor.poller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.lcappuccio.systemmonitor.collectors.Collector;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.ui.MetricRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that polls system metric collectors at configured intervals and updates the UI.
 *
 * <p>Uses a {@link ScheduledExecutorService} with separate threads for different polling intervals:
 * default (2s), filesystem (60s), and disk temperature (15s). All UI updates go through
 * {@link Platform#runLater()} to ensure thread safety.
 */
public class PollerService {

  private static final Logger LOG = LoggerFactory.getLogger(PollerService.class);

  private final ScheduledExecutorService executor;
  private final ObservableList<MetricRow> rows;
  private final Map<String, MetricRow> rowMap;
  private final List<Collector<?>> defaultCollectors;
  private final List<Collector<?>> filesystemCollectors;
  private final List<Collector<?>> diskTempCollectors;

  /**
   * Creates a new PollerService with the given configuration and UI components.
   *
   * @param config the application configuration
   * @param rows the observable list of metric rows to update
   * @param defaultCollectors collectors polled at default interval (2s)
   * @param filesystemCollectors collectors polled at filesystem interval (60s)
   * @param diskTempCollectors collectors polled at disk temperature interval (15s)
   */
  public PollerService(AppConfig config, ObservableList<MetricRow> rows,
      List<Collector<?>> defaultCollectors, List<Collector<?>> filesystemCollectors,
      List<Collector<?>> diskTempCollectors) {
    this.executor = Executors.newScheduledThreadPool(3);
    this.rows = rows;
    this.rowMap = buildRowMap(rows);
    this.defaultCollectors = defaultCollectors;
    this.filesystemCollectors = filesystemCollectors;
    this.diskTempCollectors = diskTempCollectors;

    initializeCollectors();
  }

  private void initializeCollectors() {
    for (Collector<?> collector : defaultCollectors) {
      collector.initialize();
    }
    for (Collector<?> collector : filesystemCollectors) {
      collector.initialize();
    }
    for (Collector<?> collector : diskTempCollectors) {
      collector.initialize();
    }
  }

  private Map<String, MetricRow> buildRowMap(ObservableList<MetricRow> rows) {
    return rows.stream()
        .collect(java.util.stream.Collectors.toMap(
            row -> row.getSection() + "." + row.getMetric(),
            row -> row,
            (existing, replacement) -> existing));
  }

  /**
   * Starts the polling service with configured intervals.
   *
   * <p>Schedules three separate executor threads:
   * <ul>
   *   <li>Default collectors (CPU, GPU, memory, network) at 2-second intervals</li>
   *   <li>Filesystem collectors at 60-second intervals</li>
   *   <li>Disk temperature collectors at 15-second intervals</li>
   * </ul>
   */
  public void start() {
    long defaultInterval = getPollInterval(config());
    long filesystemInterval = getFilesystemInterval(config());
    long diskTempInterval = getDiskTempInterval(config());

    executor.scheduleAtFixedRate(this::runDefaultCollectors, 0, defaultInterval,
        TimeUnit.SECONDS);
    executor.scheduleAtFixedRate(this::runFilesystemCollectors, 0, filesystemInterval,
        TimeUnit.SECONDS);
    executor.scheduleAtFixedRate(this::runDiskTempCollectors, 0, diskTempInterval,
        TimeUnit.SECONDS);

    LOG.info("PollerService started");
  }

  private AppConfig config() {
    return AppConfig.load();
  }

  private long getPollInterval(AppConfig config) {
    return config.getPollIntervalDefault();
  }

  private long getFilesystemInterval(AppConfig config) {
    return config.getPollIntervalFilesystem();
  }

  private long getDiskTempInterval(AppConfig config) {
    return config.getPollIntervalDiskTemp();
  }

  private void runDefaultCollectors() {
    for (Collector<?> collector : defaultCollectors) {
      try {
        collector.collect().ifPresent(metrics -> updateRows(collector.getName(), metrics));
      } catch (Exception e) {
        LOG.error("Error collecting {}: {}", collector.getName(), e.getMessage());
      }
    }
  }

  private void runFilesystemCollectors() {
    for (Collector<?> collector : filesystemCollectors) {
      try {
        collector.collect().ifPresent(metrics -> updateRows(collector.getName(), metrics));
      } catch (Exception e) {
        LOG.error("Error collecting {}: {}", collector.getName(), e.getMessage());
      }
    }
  }

  private void runDiskTempCollectors() {
    for (Collector<?> collector : diskTempCollectors) {
      try {
        collector.collect().ifPresent(metrics -> updateRows(collector.getName(), metrics));
      } catch (Exception e) {
        LOG.error("Error collecting {}: {}", collector.getName(), e.getMessage());
      }
    }
  }

  private void updateRows(String section, Object metrics) {
    if (metrics instanceof org.lcappuccio.systemmonitor.model.MemoryMetrics mem) {
      Platform.runLater(() -> {
        setIfPresent("Memory.Used", formatBytes(mem.memUsedBytes()));
        setIfPresent("Memory.Total", formatBytes(mem.memTotalBytes()));
        setIfPresent("Memory.Swap Used", formatBytes(mem.swapUsedBytes()));
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.NetworkMetrics net) {
      Platform.runLater(() -> {
        setIfPresent("Network.IP Address", net.ipAddress());
        setIfPresent("Network.Link Speed", net.linkSpeedMbps() + " Mbps");
        setIfPresent("Network.Upload", formatBytesPerSec(net.uploadBytesPerSec()));
        setIfPresent("Network.Download", formatBytesPerSec(net.downloadBytesPerSec()));
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.CpuMetrics cpu) {
      Platform.runLater(() -> {
        String tempStr;
        if (Double.isNaN(cpu.temperatureCelsius())) {
          tempStr = "N/A";
        } else {
          tempStr = String.format("%.1f°C", cpu.temperatureCelsius());
        }
        setIfPresent("CPU.Temperature", tempStr);
        setIfPresent("CPU.Load", String.format("%.1f%%", cpu.loadPercent()));
        for (int i = 0; i < cpu.coreFrequenciesGhz().size(); i++) {
          double ghz = cpu.coreFrequenciesGhz().get(i);
          setIfPresent("CPU.Core " + i, String.format("%.2f GHz", ghz));
        }
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.FileSystemMetrics fs) {
      Platform.runLater(() -> {
        for (var entry : fs.usage().entrySet()) {
          String mount = entry.getKey();
          var usage = entry.getValue();
          String usedStr = formatBytesWhole(usage.usedBytes());
          String freeStr = formatBytesWhole(usage.freeBytes());
          String totalStr = formatBytesWhole(usage.totalBytes());
          setIfPresent("Filesystems." + mount, usedStr + " / " + freeStr + " / " + totalStr);
        }
      });
    }
  }

  private void setIfPresent(String key, String value) {
    MetricRow row = rowMap.get(key);
    if (row != null) {
      row.setValue(value);
    }
  }

  private String formatBytes(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024));
    } else {
      return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
  }

  private String formatBytesWhole(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return (bytes / 1024) + " KB";
    } else if (bytes < 1024 * 1024 * 1024) {
      return (bytes / (1024 * 1024)) + " MB";
    } else {
      return Math.round(bytes / (1024.0 * 1024 * 1024)) + " GB";
    }
  }

  private String formatBytesPerSec(long bytes) {
    if (bytes < 1024) {
      return bytes + " B/s";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB/s", bytes / 1024.0);
    } else if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MB/s", bytes / (1024.0 * 1024));
    } else {
      return String.format("%.2f GB/s", bytes / (1024.0 * 1024 * 1024));
    }
  }

  /**
   * Shuts down the polling service and terminates all executor threads.
   *
   * <p>Attempts graceful shutdown with a 5-second timeout. If threads don't terminate
   * in time, forces shutdown and logs a warning.
   */
  public void shutdown() {
    LOG.info("Shutting down PollerService");
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        LOG.warn("Executor did not terminate in time");
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}