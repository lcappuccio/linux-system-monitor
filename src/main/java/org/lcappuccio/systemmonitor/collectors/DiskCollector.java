package org.lcappuccio.systemmonitor.collectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.DiskMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects temperature metrics for NVMe and SATA storage devices.
 *
 * <p>Discovers all available storage devices at startup, resolves their model names
 * from sysfs, and provides ordered disk labels for UI row creation.
 */
public class DiskCollector implements Collector<DiskMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(DiskCollector.class);
  private static final String HWMON_PATH = "/sys/class/hwmon";
  private static final double NO_TEMP = Double.NaN;

  private final List<String> sataDevices;
  private String nvmeTempPath = null;
  private String nvmeModel = null;

  private final List<DiskInfo> disks = new ArrayList<>();
  private final List<String> diskLabels = new ArrayList<>();

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  private record DiskInfo(String label, boolean isNvme, int index) {}

  public DiskCollector(AppConfig config) {
    this.sataDevices = config.getDiskSataDevices();
  }

  // test-only constructor
  DiskCollector(List<String> sataDevices) {
    this.sataDevices = sataDevices;
  }

  @Override
  public void initialize() {
    disks.clear();
    diskLabels.clear();
    discoverNvme();
    discoverSataDisks();

    if (disks.isEmpty()) {
      LOG.error("DiskCollector: no storage devices found");
      status = CollectorStatus.UNAVAILABLE;
    } else {
      status = CollectorStatus.OK;
    }

    LOG.info("DiskCollector initialized: status={}, disks={}", status, diskLabels);
  }

  private void discoverNvme() {
    Path hwmonDir = Paths.get(HWMON_PATH);
    if (!Files.exists(hwmonDir)) {
      return;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(hwmonDir, "hwmon*")) {
      for (Path hwmon : stream) {
        Path nameFile = hwmon.resolve("name");
        if (Files.exists(nameFile)) {
          String name = Files.readString(nameFile).trim();
          if ("nvme".equals(name)) {
            Path tempFile = hwmon.resolve("temp1_input");
            if (Files.exists(tempFile)) {
              nvmeTempPath = tempFile.toString();
            }
            nvmeModel = discoverNvmeModel();
            String label = nvmeModel != null ? nvmeModel : "nvme0n1";
            disks.add(new DiskInfo(label, true, 0));
            diskLabels.add(label);
            LOG.info("Discovered NVMe disk: {} (temp={})", label, nvmeTempPath);
            return;
          }
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to discover NVMe hwmon: {}", e.getMessage());
    }
  }

  private String discoverNvmeModel() {
    return discoverNvmeModel(Paths.get("/sys/block"));
  }

  static String discoverNvmeModel(Path blockRoot) {
    try (Stream<Path> entries = Files.list(blockRoot)) {
      Optional<Path> nvmeBlock = entries
          .filter(p -> p.getFileName().toString().startsWith("nvme"))
          .findFirst();
      if (nvmeBlock.isPresent()) {
        Path modelFile = nvmeBlock.get().resolve("device/model");
        if (Files.exists(modelFile)) {
          return Files.readString(modelFile).trim();
        }
      }
    } catch (IOException e) {
      LOG.warn("Failed to read NVMe model name: {}", e.getMessage());
    }
    return null;
  }

  private void discoverSataDisks() {
    for (int i = 0; i < sataDevices.size(); i++) {
      String device = sataDevices.get(i);
      if (device.startsWith("/dev/")) {
        if (Files.exists(Paths.get(device))) {
          String devName = device.substring("/dev/".length());
          String model = discoverSataModel(devName);
          String label = model != null ? model : devName;
          disks.add(new DiskInfo(label, false, i));
          diskLabels.add(label);
          LOG.info("Discovered SATA disk: {} ({})", label, device);
        } else {
          LOG.warn("SATA device {} not found, skipping", device);
        }
      } else {
        String fullPath = "/dev/" + device;
        if (Files.exists(Paths.get(fullPath))) {
          String model = discoverSataModel(device);
          String label = model != null ? model : device;
          disks.add(new DiskInfo(label, false, i));
          diskLabels.add(label);
          LOG.info("Discovered SATA disk: {} ({})", label, fullPath);
        } else {
          LOG.warn("SATA device {} not found, skipping", fullPath);
        }
      }
    }
  }

  private String discoverSataModel(String devName) {
    return discoverSataModel(devName, Paths.get("/sys/block"));
  }

  static String discoverSataModel(String devName, Path blockRoot) {
    Path modelFile = blockRoot.resolve(devName + "/device/model");
    if (Files.exists(modelFile)) {
      try {
        return Files.readString(modelFile).trim();
      } catch (IOException e) {
        LOG.warn("Failed to read model for {}: {}", devName, e.getMessage());
      }
    }
    return null;
  }

  /**
   * Returns the ordered list of disk labels (model names) for UI row creation.
   *
   * @return list of disk labels
   */
  public List<String> getDiskLabels() {
    return diskLabels;
  }

  @Override
  public Optional<DiskMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    Map<String, Double> temps = new LinkedHashMap<>();
    for (DiskInfo disk : disks) {
      double temp = disk.isNvme() ? readNvmeTemp() : readSataTemp(disk.index());
      temps.put(disk.label(), temp);
    }

    return Optional.of(new DiskMetrics(temps));
  }

  private double readNvmeTemp() {
    if (nvmeTempPath == null) {
      return NO_TEMP;
    }
    try {
      String content = Files.readString(Paths.get(nvmeTempPath)).trim();
      return Double.parseDouble(content) / 1000.0;
    } catch (IOException | NumberFormatException e) {
      LOG.error("Failed to read NVMe temp: {}", e.getMessage());
      return NO_TEMP;
    }
  }

  private double readSataTemp(int index) {
    String device = sataDevices.get(index);
    if (!device.startsWith("/dev/")) {
      device = "/dev/" + device;
    }

    Process process = null;
    try {
      process = new ProcessBuilder("sudo", "smartctl", "-A", device)
          .redirectErrorStream(true)
          .start();

      boolean exited = process.waitFor(5, TimeUnit.SECONDS);
      if (!exited) {
        LOG.error("smartctl timed out for {}", device);
        return NO_TEMP;
      }
      if (process.exitValue() != 0) {
        LOG.error("smartctl exit code {} for {}", process.exitValue(), device);
        return NO_TEMP;
      }

      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.contains("Airflow_Temperature_Cel")) {
            return parseSmartctlLine(line);
          }
        }
      }
    } catch (IOException | InterruptedException e) {
      LOG.error("Failed to read SATA temp for {}: {}", device, e.getMessage());
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }
    }
    return NO_TEMP;
  }

  static double parseSmartctlLine(String line) {
    String[] parts = line.trim().split("\\s+");
    if (parts.length >= 10) {
      try {
        return Double.parseDouble(parts[9]);
      } catch (NumberFormatException e) {
        LOG.error("Failed to parse temperature: {}", line);
      }
    }
    return NO_TEMP;
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Disks";
  }
}
