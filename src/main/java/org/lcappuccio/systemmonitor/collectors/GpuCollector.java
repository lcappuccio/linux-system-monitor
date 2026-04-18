package org.lcappuccio.systemmonitor.collectors;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.GpuMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects AMD GPU metrics from sysfs and hwmon.
 */
public class GpuCollector implements Collector<GpuMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(GpuCollector.class);
  private static final String HWMON_PATH = "/sys/class/hwmon";
  private static final double NO_TEMP = Double.NaN;

  private final String drmPath;
  private String hwmonPath = null;
  private String junctionTempPath = null;
  private String vramTempPath = null;
  private String powerPath = null;
  private String fanPath = null;

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  public GpuCollector(AppConfig config) {
    this.drmPath = config.getGpuDrmPath();
  }

  @Override
  public void initialize() {
    boolean drmValid = drmPath != null && Files.exists(Paths.get(drmPath));
    if (!drmValid) {
      LOG.error("GPU DRM path {} not found", drmPath);
    }

    discoverHwmon();

    boolean hwmonValid = hwmonPath != null
        && (junctionTempPath != null || vramTempPath != null || powerPath != null);

    if (!drmValid && !hwmonValid) {
      status = CollectorStatus.UNAVAILABLE;
      LOG.error("GPU unavailable - no DRM and no hwmon");
    } else if (!drmValid) {
      LOG.warn("GPU DRM path invalid, using hwmon only");
      status = CollectorStatus.DEGRADED;
    } else if (!hwmonValid) {
      LOG.warn("GPU hwmon invalid, using DRM only");
      status = CollectorStatus.DEGRADED;
    } else {
      status = CollectorStatus.OK;
    }

    LOG.info("GpuCollector initialized: status={}", status);
  }

  private void discoverHwmon() {
    try {
      Path hwmonDir = Paths.get(HWMON_PATH);
      if (!Files.exists(hwmonDir)) {
        return;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(hwmonDir, "hwmon*")) {
        for (Path hwmon : stream) {
          Path nameFile = hwmon.resolve("name");
          if (Files.exists(nameFile)) {
            String name = Files.readString(nameFile).trim();
            if ("amdgpu".equals(name)) {
              hwmonPath = hwmon.toString();
              findSensors(hwmon);
              return;
            }
          }
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to discover hwmon: {}", e.getMessage());
    }
  }

  private void findSensors(Path hwmon) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(hwmon, "temp*_label")) {
      for (Path labelFile : stream) {
        String label = Files.readString(labelFile).trim();
        String num = labelFile.getFileName().toString().split("_")[0];

        if ("junction".equals(label)) {
          junctionTempPath = hwmon.resolve(num + "_input").toString();
        } else if ("mem".equals(label)) {
          vramTempPath = hwmon.resolve(num + "_input").toString();
        }
      }

      Path powerLabelFile = hwmon.resolve("power1_label");
      if (Files.exists(powerLabelFile)) {
        String label = Files.readString(powerLabelFile).trim();
        if ("PPT".equals(label)) {
          powerPath = hwmon.resolve("power1_average").toString();
        }
      }

      Path fanFile = hwmon.resolve("fan1_input");
      if (Files.exists(fanFile)) {
        fanPath = fanFile.toString();
      }
    } catch (IOException e) {
      LOG.error("Failed to find GPU sensors: {}", e.getMessage());
    }
  }

  @Override
  public Optional<GpuMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    try {
      double junctionTemp = readTemperature(junctionTempPath);
      double vramTemp = readTemperature(vramTempPath);
      double power = readPower();
      double load = readDrmMetric("gpu_busy_percent");
      double vramLoad = readDrmMetric("mem_busy_percent");
      long[] vram = readVramUsage();
      double fan = readFan();

      return Optional.of(new GpuMetrics(
          junctionTemp,
          load,
          vram[0],
          vram[1],
          vramTemp,
          vramLoad,
          power,
          fan));
    } catch (RuntimeException e) {
      LOG.error("Failed to collect GPU metrics: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private double readTemperature(String path) {
    if (path == null) {
      return NO_TEMP;
    }
    try {
      String content = Files.readString(Paths.get(path)).trim();
      double millidegrees = Double.parseDouble(content);
      return millidegrees / 1000.0;
    } catch (IOException e) {
      LOG.debug("Failed to read temperature from {}: {}", path, e.getMessage());
      return NO_TEMP;
    }
  }

  private double readPower() {
    if (powerPath == null) {
      return 0.0;
    }
    try {
      String content = Files.readString(Paths.get(powerPath)).trim();
      return Double.parseDouble(content) / 1_000_000.0;
    } catch (IOException e) {
      LOG.debug("Failed to read power: {}", e.getMessage());
      return 0.0;
    }
  }

  private double readFan() {
    if (fanPath == null) {
      return 0.0;
    }
    try {
      String content = Files.readString(Paths.get(fanPath)).trim();
      return Double.parseDouble(content);
    } catch (IOException e) {
      LOG.debug("Failed to read fan: {}", e.getMessage());
      return 0.0;
    }
  }

  private double readDrmMetric(String name) {
    if (drmPath == null) {
      return 0.0;
    }
    try {
      Path path = Paths.get(drmPath, "device", name);
      if (Files.exists(path)) {
        String content = Files.readString(path).trim();
        return Double.parseDouble(content);
      }
    } catch (IOException e) {
      LOG.debug("Failed to read {}: {}", name, e.getMessage());
    }
    return 0.0;
  }

  private long[] readVramUsage() {
    long used = 0;
    long total = 0;
    if (drmPath != null) {
      try {
        Path usedPath = Paths.get(drmPath, "device", "mem_info_vram_used");
        Path totalPath = Paths.get(drmPath, "device", "mem_info_vram_total");
        if (Files.exists(usedPath)) {
          used = Long.parseLong(Files.readString(usedPath).trim());
        }
        if (Files.exists(totalPath)) {
          total = Long.parseLong(Files.readString(totalPath).trim());
        }
      } catch (IOException e) {
        LOG.debug("Failed to read VRAM usage: {}", e.getMessage());
      }
    }
    return new long[]{used, total};
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