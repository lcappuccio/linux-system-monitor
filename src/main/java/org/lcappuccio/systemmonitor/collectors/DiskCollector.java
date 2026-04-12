package org.lcappuccio.systemmonitor.collectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.DiskMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects NVMe and SATA SSD temperature metrics.
 */
public class DiskCollector implements Collector<DiskMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(DiskCollector.class);
  private static final String HWMON_PATH = "/sys/class/hwmon";
  private static final double NO_TEMP = Double.NaN;

  private final AppConfig config;
  private final String sataDevice;
  private String nvmeHwmonPath = null;
  private String nvmeTempPath = null;

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  public DiskCollector(AppConfig config) {
    this.config = config;
    this.sataDevice = config.getDiskSataDevice();
  }

  @Override
  public void initialize() {
    discoverNvmeHwmon();
    boolean sataValid = checkSataDevice();

    if (nvmeHwmonPath == null && !sataValid) {
      LOG.error("DiskCollector: neither NVMe nor SATA available");
      status = CollectorStatus.UNAVAILABLE;
    } else if (nvmeHwmonPath == null || !sataValid) {
      LOG.warn("DiskCollector: only NVMe or SATA available, status=DEGRADED");
      status = CollectorStatus.DEGRADED;
    } else {
      status = CollectorStatus.OK;
    }

    LOG.info("DiskCollector initialized: status={}", status);
  }

  private void discoverNvmeHwmon() {
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
          if ("nvme".equals(name)) {
            nvmeHwmonPath = hwmon.toString();
            Path tempFile = hwmon.resolve("temp1_input");
            if (Files.exists(tempFile)) {
              nvmeTempPath = tempFile.toString();
            }
            return;
          }
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to discover NVMe hwmon: {}", e.getMessage());
    }
  }

  private boolean checkSataDevice() {
    if (sataDevice == null) {
      return false;
    }
    if (sataDevice.startsWith("/dev/")) {
      return Files.exists(Paths.get(sataDevice));
    }
    return Files.exists(Paths.get("/dev/" + sataDevice));
  }

  @Override
  public Optional<DiskMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    double nvmeTemp = readNvmeTemp();
    double sataTemp = readSataTemp();

    return Optional.of(new DiskMetrics(nvmeTemp, sataTemp));
  }

  private double readNvmeTemp() {
    if (nvmeTempPath == null) {
      return NO_TEMP;
    }
    try {
      String content = Files.readString(Paths.get(nvmeTempPath)).trim();
      return Double.parseDouble(content) / 1000.0;
    } catch (Exception e) {
      LOG.debug("Failed to read NVMe temp: {}", e.getMessage());
      return NO_TEMP;
    }
  }

  private double readSataTemp() {
    String device = sataDevice;
    if (!device.startsWith("/dev/")) {
      device = "/dev/" + device;
    }

    Process process = null;
    try {
      process = new ProcessBuilder("sudo", "smartctl", "-A", device)
          .redirectErrorStream(true)
          .start();

      boolean exited = process.waitFor(5, TimeUnit.SECONDS);
      if (!exited || process.exitValue() != 0) {
        LOG.debug("smartctl exit code: {}", process.exitValue());
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
    } catch (Exception e) {
      LOG.debug("Failed to read SATA temp: {}", e.getMessage());
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }
    }
    return NO_TEMP;
  }

  private double parseSmartctlLine(String line) {
    String[] parts = line.trim().split("\\s+");
    if (parts.length >= 10) {
      try {
        return Double.parseDouble(parts[9]);
      } catch (NumberFormatException e) {
        LOG.debug("Failed to parse temperature: {}", line);
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