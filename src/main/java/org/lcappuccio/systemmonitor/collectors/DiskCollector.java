package org.lcappuccio.systemmonitor.collectors;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.lcappuccio.systemmonitor.model.DiskMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects storage device temperatures from hwmon.
 */
public class DiskCollector implements Collector<DiskMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(DiskCollector.class);
  private static final String HWMON_PATH = "/sys/class/hwmon";
  private static final double NO_TEMP = Double.NaN;

  private String nvmeTempPath = null;
  private String nvmeModel = null;

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  public DiskCollector() {
  }

  @Override
  public void initialize() {
    discoverNvme();

    if (nvmeModel == null) {
      LOG.error("DiskCollector: no storage devices found");
      status = CollectorStatus.UNAVAILABLE;
    } else {
      status = CollectorStatus.OK;
    }

    LOG.info("DiskCollector initialized: status={}, disk={}", status, nvmeModel);
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
            if (nvmeModel == null) {
              nvmeModel = "nvme0n1";
            }
            LOG.info("Discovered NVMe disk: {} (temp={})", nvmeModel, nvmeTempPath);
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

  /**
   * Returns the list of disk labels (model names) for UI row creation.
   *
   * @return list of disk labels, empty if none discovered
   */
  public List<String> getDiskLabels() {
    if (nvmeModel == null) {
      return List.of();
    }
    return List.of(nvmeModel);
  }

  @Override
  public Optional<DiskMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    double temp = readNvmeTemp();
    return Optional.of(new DiskMetrics(Collections.singletonMap(nvmeModel, temp)));
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

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Disks";
  }
}
