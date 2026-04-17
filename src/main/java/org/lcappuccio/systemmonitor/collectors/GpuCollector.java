package org.lcappuccio.systemmonitor.collectors;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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

  private static final Map<String, String> AMD_DEVICE_ID_MAP;
  static {
    AMD_DEVICE_ID_MAP = new HashMap<>();
    AMD_DEVICE_ID_MAP.put("7450", "AMD Radeon HD 8870M");
    AMD_DEVICE_ID_MAP.put("7640", "AMD Kaveri");
    AMD_DEVICE_ID_MAP.put("7642", "AMD Kaveri");
    AMD_DEVICE_ID_MAP.put("9830", "AMD Carrizo");
    AMD_DEVICE_ID_MAP.put("9870", "AMD Carrizo");
    AMD_DEVICE_ID_MAP.put("15DD", "AMD Picasso");
    AMD_DEVICE_ID_MAP.put("15D8", "AMD Picasso");
    AMD_DEVICE_ID_MAP.put("1636", "AMD Renoir");
    AMD_DEVICE_ID_MAP.put("1638", "AMD Renoir");
    AMD_DEVICE_ID_MAP.put("15E7", "AMD Raven Ridge");
    AMD_DEVICE_ID_MAP.put("15EA", "AMD Raven Ridge");
    AMD_DEVICE_ID_MAP.put("15EB", "AMD Raven Ridge");
    AMD_DEVICE_ID_MAP.put("15EC", "AMD Raven Ridge");
    AMD_DEVICE_ID_MAP.put("15FF", "AMD Renoir");
    AMD_DEVICE_ID_MAP.put("7310", "AMD Arcturus");
    AMD_DEVICE_ID_MAP.put("7318", "AMD Arcturus");
    AMD_DEVICE_ID_MAP.put("7319", "AMD Arcturus");
    AMD_DEVICE_ID_MAP.put("731F", "AMD Arcturus");
    AMD_DEVICE_ID_MAP.put("7360", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("740F", "AMD Instinct MI210");
    AMD_DEVICE_ID_MAP.put("7408", "AMD Instinct MI250X");
    AMD_DEVICE_ID_MAP.put("67FF", "AMD Navi 10");
    AMD_DEVICE_ID_MAP.put("687F", "AMD Navi 10");
    AMD_DEVICE_ID_MAP.put("15BF", "AMD Navi 12");
    AMD_DEVICE_ID_MAP.put("7362", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("67C0", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("67C1", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("67C2", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("67C4", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("67C7", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("67DF", "AMD Navi 14");
    AMD_DEVICE_ID_MAP.put("73A0", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73A1", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73A2", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73A3", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73A4", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73A5", "AMD Navi 17");
    AMD_DEVICE_ID_MAP.put("73A6", "AMD Navi 17");
    AMD_DEVICE_ID_MAP.put("73A7", "AMD Navi 17");
    AMD_DEVICE_ID_MAP.put("73A8", "AMD Navi 17");
    AMD_DEVICE_ID_MAP.put("73A9", "AMD Navi 17");
    AMD_DEVICE_ID_MAP.put("73AA", "AMD Navi 17");
    AMD_DEVICE_ID_MAP.put("73AB", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73AC", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73AD", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73AE", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73AF", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("73BF", "AMD Navi 16");
    AMD_DEVICE_ID_MAP.put("1002", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("1618", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("1619", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("161A", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("161B", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("161C", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("161D", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("161E", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("161F", "AMD Rembrandt");
    AMD_DEVICE_ID_MAP.put("1620", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1622", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1623", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1624", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1625", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1626", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1627", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("162A", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("162B", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("162C", "AMD Cezanne");
    AMD_DEVICE_ID_MAP.put("1639", "AMD Lucienne");
    AMD_DEVICE_ID_MAP.put("163F", "AMD Barcelona");
    AMD_DEVICE_ID_MAP.put("164C", "AMD Van Gogh");
    AMD_DEVICE_ID_MAP.put("164D", "AMD Van Gogh");
    AMD_DEVICE_ID_MAP.put("164F", "AMD Van Gogh");
    AMD_DEVICE_ID_MAP.put("1650", "AMD Van Gogh");
    AMD_DEVICE_ID_MAP.put("1651", "AMD Van Gogh");
    AMD_DEVICE_ID_MAP.put("165F", "AMD Van Gogh");
    AMD_DEVICE_ID_MAP.put("740C", "AMD Instinct MI250");
    AMD_DEVICE_ID_MAP.put("744C", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("744E", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("744F", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("7460", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("7462", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("7480", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("748B", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("748C", "AMD Aldebaran");
    AMD_DEVICE_ID_MAP.put("7550", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("7551", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("7552", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("7553", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("7557", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("7558", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("7559", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("755A", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("755C", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("755D", "AMD Radeon RX 9070");
    AMD_DEVICE_ID_MAP.put("755F", "AMD Radeon RX 9070");
  }

  private final String drmPath;
  private String hwmonPath = null;
  private String modelName = "GPU";
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

    discoverModelName();
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

  private void discoverModelName() {
    if (drmPath != null) {
      try {
        Path namePath = Paths.get(drmPath, "device", "name");
        if (Files.exists(namePath)) {
          String name = Files.readString(namePath).trim();
          if (!name.isEmpty()) {
            modelName = name;
            LOG.info("Discovered GPU model from DRM: {}", modelName);
            return;
          }
        }
      } catch (IOException e) {
        LOG.debug("Failed to read GPU model from DRM: {}", e.getMessage());
      }

      try {
        Path ueventPath = Paths.get(drmPath, "device", "uevent");
        if (Files.exists(ueventPath)) {
          String content = Files.readString(ueventPath);
          String pciId = extractPciId(content);
          if (pciId != null) {
            String deviceId = pciId.contains(":") ? pciId.split(":")[1] : pciId;
            String mappedName = AMD_DEVICE_ID_MAP.get(deviceId.toUpperCase());
            if (mappedName != null) {
              modelName = mappedName;
              LOG.info("Discovered GPU model from PCI ID {}: {}", deviceId, modelName);
              return;
            }
            LOG.info("GPU device ID {} not in mapping, using fallback", deviceId);
          }
        }
      } catch (IOException e) {
        LOG.debug("Failed to read GPU uevent: {}", e.getMessage());
      }
    }

    LOG.warn("GPU model name not found, using fallback: {}", modelName);
  }

  private String extractPciId(String ueventContent) {
    for (String line : ueventContent.split("\n")) {
      if (line.startsWith("PCI_ID=")) {
        return line.substring("PCI_ID=".length()).trim();
      }
    }
    return null;
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

  public String getModelName() {
    return modelName;
  }
}