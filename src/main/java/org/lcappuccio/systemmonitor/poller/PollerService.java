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

public class PollerService {

  private static final Logger LOG = LoggerFactory.getLogger(PollerService.class);

  private final ScheduledExecutorService executor;
  private final ObservableList<MetricRow> rows;
  private final Map<String, MetricRow> rowMap;
  private final List<Collector<?>> defaultCollectors;
  private final List<Collector<?>> filesystemCollectors;
  private final List<Collector<?>> diskTempCollectors;

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