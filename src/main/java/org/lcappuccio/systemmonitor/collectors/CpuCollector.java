package org.lcappuccio.systemmonitor.collectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.lcappuccio.systemmonitor.model.CpuMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects CPU metrics from sysfs, hwmon, and /proc/stat.
 */
public class CpuCollector implements Collector<CpuMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(CpuCollector.class);
  private static final String PROC_STAT = "/proc/stat";
  private static final String SYS_CPU_PATH = "/sys/devices/system/cpu";
  private static final String HWMON_PATH = "/sys/class/hwmon";
  private static final double NO_TEMP = Double.NaN;

  private final List<Integer> coreIds = new ArrayList<>();
  private String hwmonPath = null;
  private String tempInputPath = null;

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  private long prevTotalJiffies = 0;
  private long prevIdleJiffies = 0;
  private boolean hasPrevious = false;

  @Override
  public void initialize() {
    discoverCores();
    discoverHwmon();

    if (hwmonPath == null) {
      LOG.error("CPU hwmon (k10temp) not found");
      status = CollectorStatus.DEGRADED;
    } else if (tempInputPath == null) {
      LOG.error("CPU temperature sensor (Tctl) not found in {}", hwmonPath);
      status = CollectorStatus.DEGRADED;
    } else {
      status = CollectorStatus.OK;
    }

    LOG.info("CpuCollector initialized: {} cores, status={}", coreIds.size(), status);
  }

  private void discoverCores() {
    coreIds.clear();
    try {
      Path cpuDir = Paths.get(SYS_CPU_PATH);
      DirectoryStream<Path> stream = Files.newDirectoryStream(cpuDir, "cpu[0-9]*");
      for (Path entry : stream) {
        String name = entry.getFileName().toString();
        if (name.startsWith("cpu") && Files.isDirectory(entry)) {
          try {
            int id = Integer.parseInt(name.substring(3));
            coreIds.add(id);
          } catch (NumberFormatException e) {
            // skip non-cpu directories
          }
        }
      }
      coreIds.sort(Integer::compareTo);
    } catch (IOException e) {
      LOG.error("Failed to discover CPU cores: {}", e.getMessage());
    }
  }

  private void discoverHwmon() {
    try {
      Path hwmonDir = Paths.get(HWMON_PATH);
      if (!Files.exists(hwmonDir)) {
        return;
      }

      DirectoryStream<Path> stream = Files.newDirectoryStream(hwmonDir, "hwmon*");
      for (Path hwmon : stream) {
        Path nameFile = hwmon.resolve("name");
        if (Files.exists(nameFile)) {
          String name = Files.readString(nameFile).trim();
          if ("k10temp".equals(name)) {
            hwmonPath = hwmon.toString();
            findTempSensor(hwmon);
            return;
          }
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to discover hwmon: {}", e.getMessage());
    }
  }

  private void findTempSensor(Path hwmon) {
    try {
      DirectoryStream<Path> stream = Files.newDirectoryStream(hwmon, "temp*_label");
      for (Path labelFile : stream) {
        String label = Files.readString(labelFile).trim();
        if (label.contains("Tctl")) {
          String num = labelFile.getFileName().toString().split("_")[0];
          tempInputPath = hwmon.resolve(num + "_input").toString();
          return;
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to find Tctl sensor: {}", e.getMessage());
    }
  }

  @Override
  public Optional<CpuMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    try {
      double temperature = readTemperature();
      double load = calculateLoad();
      List<Double> frequencies = readFrequencies();

      return Optional.of(new CpuMetrics(temperature, load, frequencies));
    } catch (Exception e) {
      LOG.error("Failed to collect CPU metrics: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private double readTemperature() {
    if (tempInputPath == null) {
      return NO_TEMP;
    }
    try {
      String content = Files.readString(Paths.get(tempInputPath)).trim();
      return Double.parseDouble(content) / 1000.0;
    } catch (Exception e) {
      LOG.debug("Failed to read temperature: {}", e.getMessage());
      return NO_TEMP;
    }
  }

  private double calculateLoad() {
    long[] jiffies = readProcStatJiffies();
    long total = jiffies[0];
    long idle = jiffies[1];

    if (!hasPrevious) {
      prevTotalJiffies = total;
      prevIdleJiffies = idle;
      hasPrevious = true;
      return 0.0;
    }

    prevTotalJiffies = total;
    prevIdleJiffies = idle;

    if (total <= prevTotalJiffies) {
      return 0.0;
    }

    long totalDelta = total - prevTotalJiffies;
    long idleDelta = idle - prevIdleJiffies;

    return Math.min(100.0, ((double) (totalDelta - idleDelta) / totalDelta) * 100.0);
  }

  private long[] readProcStatJiffies() {
    long total = 0;
    long idle = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(PROC_STAT))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("cpu ")) {
          String[] parts = line.split("\\s+");
          if (parts.length >= 5) {
            for (int i = 1; i < parts.length; i++) {
              try {
                long val = Long.parseLong(parts[i]);
                total += val;
                if (i == 4) {
                  idle = val;
                }
              } catch (NumberFormatException e) {
                // skip
              }
            }
          }
          break;
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to read /proc/stat: {}", e.getMessage());
    }

    return new long[]{total, idle};
  }

  private List<Double> readFrequencies() {
    List<Double> freqs = new ArrayList<>();
    for (int coreId : coreIds) {
      double ghz = readCoreFreq(coreId);
      freqs.add(ghz);
    }
    return freqs;
  }

  private double readCoreFreq(int coreId) {
    try {
      Path freqFile = Paths.get(SYS_CPU_PATH, "cpu" + coreId, "cpufreq/scaling_cur_freq");
      if (Files.exists(freqFile)) {
        String content = Files.readString(freqFile).trim();
        return Double.parseDouble(content) / 1_000_000.0;
      }
    } catch (Exception e) {
      LOG.debug("Failed to read freq for cpu{}: {}", coreId, e.getMessage());
    }
    return 0.0;
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "CPU";
  }

  public List<Integer> getCoreIds() {
    return List.copyOf(coreIds);
  }
}