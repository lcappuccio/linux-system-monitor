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
import org.lcappuccio.systemmonitor.model.MetricKey;
import org.lcappuccio.systemmonitor.ui.MetricRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that polls system metric collectors at configured intervals and updates the UI.
 *
 * <p>Uses a {@link ScheduledExecutorService} with separate threads for different polling intervals:
 * default (2s), filesystem (60s), and disk temperature (15s).
 */
public class PollerService {

  private static final Logger LOG = LoggerFactory.getLogger(PollerService.class);

  private final AppConfig config;
  private final ScheduledExecutorService executor;
  private final ObservableList<MetricRow> rows;
  private final Map<String, MetricRow> rowMap;
  private final List<Collector<?>> defaultCollectors;
  private final List<Collector<?>> filesystemCollectors;
  private final List<Collector<?>> diskTempCollectors;

  /**
   * Creates a new PollerService with the given configuration and UI components.
   *
   * @param config               the application configuration
   * @param rows                 the observable list of metric rows to update
   * @param defaultCollectors    collectors polled at default interval (2s)
   * @param filesystemCollectors collectors polled at filesystem interval (60s)
   * @param diskTempCollectors   collectors polled at disk temperature interval (15s)
   */
  public PollerService(AppConfig config, ObservableList<MetricRow> rows,
      List<Collector<?>> defaultCollectors, List<Collector<?>> filesystemCollectors,
      List<Collector<?>> diskTempCollectors) {
    this.config = config;
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
    executor.scheduleAtFixedRate(this::runDefaultCollectors, 0,
        config.getPollIntervalDefault(), TimeUnit.SECONDS);
    executor.scheduleAtFixedRate(this::runFilesystemCollectors, 0,
        config.getPollIntervalFilesystem(), TimeUnit.SECONDS);
    executor.scheduleAtFixedRate(this::runDiskTempCollectors, 0,
        config.getPollIntervalDiskTemp(), TimeUnit.SECONDS);
    LOG.info("PollerService started");
  }

  private void runDefaultCollectors() {
    for (Collector<?> collector : defaultCollectors) {
      try {
        collector.collect().ifPresent(metrics -> updateRows(collector.getName(), metrics));
      } catch (RuntimeException e) {
        LOG.error("Error collecting {}: {}", collector.getName(), e.getMessage());
      }
    }
  }

  private void runFilesystemCollectors() {
    for (Collector<?> collector : filesystemCollectors) {
      try {
        collector.collect().ifPresent(metrics -> updateRows(collector.getName(), metrics));
      } catch (RuntimeException e) {
        LOG.error("Error collecting {}: {}", collector.getName(), e.getMessage());
      }
    }
  }

  private void runDiskTempCollectors() {
    for (Collector<?> collector : diskTempCollectors) {
      try {
        collector.collect().ifPresent(metrics -> updateRows(collector.getName(), metrics));
      } catch (RuntimeException e) {
        LOG.error("Error collecting {}: {}", collector.getName(), e.getMessage());
      }
    }
  }

  private void updateRows(String section, Object metrics) {
    if (metrics instanceof org.lcappuccio.systemmonitor.model.MemoryMetrics mem) {
      Platform.runLater(() -> {
        setIfPresent(MetricKey.Mem.USED.key(), formatBytesMemory(mem.memUsedBytes()) + " / "
            + formatBytesMemory(mem.memTotalBytes()));
        setIfPresent(MetricKey.Mem.SWAP_USED.key(), formatBytesMemory(mem.swapUsedBytes()));
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.NetworkMetrics net) {
      Platform.runLater(() -> {
        setIfPresent(MetricKey.Net.IP_ADDRESS.key(), net.ipAddress());
        setIfPresent(MetricKey.Net.LINK_SPEED.key(), net.linkSpeedMbps() + " Mbps");
        setIfPresent(MetricKey.Net.UPLOAD.key(),
            formatNetworkBitsPerSec(net.uploadBytesPerSec()));
        setIfPresent(MetricKey.Net.DOWNLOAD.key(),
            formatNetworkBitsPerSec(net.downloadBytesPerSec()));
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.CpuMetrics cpu) {
      Platform.runLater(() -> {
        String tempStr;
        if (Double.isNaN(cpu.temperatureCelsius())) {
          tempStr = "N/A";
        } else {
          tempStr = String.format("%.0f°C", cpu.temperatureCelsius());
        }
        setIfPresent(MetricKey.Cpu.TEMPERATURE.key(), tempStr);
        setIfPresent(MetricKey.Cpu.LOAD.key(), String.format("%.1f%%", cpu.loadPercent()));
        for (int i = 0; i < cpu.coreFrequenciesGhz().size(); i++) {
          double ghz = cpu.coreFrequenciesGhz().get(i);
          setIfPresent(MetricKey.CPU.key("Core " + i), String.format("%.2f GHz", ghz));
        }
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.GpuMetrics gpu) {
      Platform.runLater(() -> {
        String tempStr;
        if (Double.isNaN(gpu.temperatureCelsius())) {
          tempStr = "N/A";
        } else {
          tempStr = String.format("%.0f°C", gpu.temperatureCelsius());
        }
        setIfPresent(MetricKey.Gpu.TEMPERATURE.key(), tempStr);
        setIfPresent(MetricKey.Gpu.LOAD.key(), String.format("%.0f%%", gpu.loadPercent()));
        String vramUsedStr = formatBytesMemory(gpu.vramUsedBytes());
        String vramTotalStr = formatBytesMemory(gpu.vramTotalBytes());
        setIfPresent(MetricKey.Gpu.VRAM_USED.key(), vramUsedStr + " / " + vramTotalStr);
        String vramTempStr;
        if (Double.isNaN(gpu.vramTemperatureCelsius())) {
          vramTempStr = "N/A";
        } else {
          vramTempStr = String.format("%.0f°C", gpu.vramTemperatureCelsius());
        }
        setIfPresent(MetricKey.Gpu.VRAM_TEMPERATURE.key(), vramTempStr);
        setIfPresent(MetricKey.Gpu.VRAM_LOAD.key(), String.format("%.0f%%", gpu.vramLoadPercent()));
        setIfPresent(MetricKey.Gpu.POWER.key(), String.format("%.0f W", gpu.powerWatts()));
        int fanRpm = (int) gpu.fanRpm();
        setIfPresent(MetricKey.Gpu.FAN.key(), fanRpm + " RPM");
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.FileSystemMetrics fs) {
      Platform.runLater(() -> {
        for (var entry : fs.usage().entrySet()) {
          String mount = entry.getKey();
          var usage = entry.getValue();
          String usedStr = formatBytesFileSystem(usage.usedBytes());
          String freeStr = formatBytesFileSystem(usage.freeBytes());
          String totalStr = formatBytesFileSystem(usage.totalBytes());
          setIfPresent(MetricKey.Filesystem.key(mount),
              usedStr + " / " + freeStr + " / " + totalStr);
        }
      });
    } else if (metrics instanceof org.lcappuccio.systemmonitor.model.DiskMetrics disk) {
      Platform.runLater(() -> {
        String nvmeStr = Double.isNaN(disk.nvmeTempCelsius())
            ? "N/A" : String.format("%.0f°C", disk.nvmeTempCelsius());
        String sataStr = Double.isNaN(disk.sataTempCelsius())
            ? "N/A" : String.format("%.0f°C", disk.sataTempCelsius());
        setIfPresent(MetricKey.Disk.NVME_TEMPERATURE.key(), nvmeStr);
        setIfPresent(MetricKey.Disk.SSD_TEMPERATURE.key(), sataStr);
      });
    }
  }

  private void setIfPresent(String key, String value) {
    MetricRow row = rowMap.get(key);
    if (row != null) {
      row.setValue(value);
    }
  }

  private String formatBytesMemory(long bytes) {
    return String.format("%d GB", Math.round(bytes / (1024.0 * 1024 * 1024)));
  }

  private String formatBytesFileSystem(long bytes) {
    if (bytes < (1024L * 1024 * 1024 * 1024)) {
      return Math.round(bytes / (1024.0 * 1024 * 1024)) + " GB";
    } else {
      return Math.round(bytes / (1024.0 * 1024 * 1024 * 1024)) + " TB";
    }
  }

  private String formatNetworkBitsPerSec(long bytes) {
    // Options: KBps, MBps, GB/s, bps, Kbps, Mbps, Gbps
    return switch (config.getNetworkSpeedUnit()) {
      case ("KBps") -> String.format("%.0f KB/s", bytes / 1024.0);
      case ("MBps") -> String.format("%.0f MB/s", bytes / (1024.0 * 1024));
      case ("GBps") -> String.format("%.0f GB/s", bytes / (1024.0 * 1024 * 1024));
      case ("Mbps") -> String.format("%.0f Mbps", bytes * 8 / (1024.0 * 1024));
      case ("Gbps") -> String.format("%.0f Gbps", bytes * 8 / (1024.0 * 1024 * 1024));
      default -> String.format("%.0f Kbps", bytes * 8 / 1024.0);
    };
  }

  private static java.util.function.Function<Long, String> buildNetworkFormatter(
      String unit) {
    return switch (unit) {
      case "KBps" -> bytes -> String.format("%.0f KB/s", bytes / 1024.0);
      case "MBps" -> bytes -> String.format("%.0f MB/s", bytes / (1024.0 * 1024));
      case "GBps" -> bytes -> String.format("%.0f GB/s", bytes / (1024.0 * 1024 * 1024));
      case "Mbps" -> bytes -> String.format("%.0f Mbps", bytes * 8 / (1024.0 * 1024));
      case "Gbps" -> bytes -> String.format("%.0f Gbps", bytes * 8 / (1024.0 * 1024 * 1024));
      default -> bytes -> String.format("%.0f Kbps", bytes * 8 / 1024.0);
    };
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