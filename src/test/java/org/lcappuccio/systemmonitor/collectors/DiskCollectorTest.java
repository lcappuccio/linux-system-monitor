package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.DiskMetrics;

class DiskCollectorTest {

  private DiskCollector collector;
  private AppConfig config;

  @BeforeEach
  void setUp() {
    config = AppConfig.load();
    collector = new DiskCollector(config);
  }

  @Test
  void initialize_setsAcceptableStatus() {
    collector.initialize();
    assertTrue(collector.getStatus() == CollectorStatus.OK
        || collector.getStatus() == CollectorStatus.UNAVAILABLE);
  }

  @Test
  void collect_returnsNonEmptyWhenAvailable() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metrics = collector.collect();
      assertTrue(metrics.isPresent());
    }
  }

  @Test
  void collect_returnsValidMetrics() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      DiskMetrics metrics = metricsOpt.get();
      assertFalse(metrics.temperatures().isEmpty());
    }
  }

  @Test
  void collect_temperatureRange() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      DiskMetrics m = metricsOpt.get();
      for (double temp : m.temperatures().values()) {
        if (!Double.isNaN(temp)) {
          assertTrue(temp >= -20 && temp < 100, "Temp out of range: " + temp);
        }
      }
    }
  }

  @Test
  void getName_returnsDisks() {
    assertEquals("Disks", collector.getName());
  }

  @Test
  void getDiskLabels_returnsNonEmptyWhenAvailable() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      assertFalse(collector.getDiskLabels().isEmpty());
    }
  }

  @Test
  void parseSmartctlLine_validLine_returnsTemperature() {
    String line = "194 Temperature_Celsius     0x0022   062   053   000    Old_age   Always       -       42";
    double result = DiskCollector.parseSmartctlLine(line);
    assertEquals(42.0, result);
  }

  @Test
  void parseSmartctlLine_tooFewParts_returnsNaN() {
    String line = "194 Temperature_Celsius     0x0022   062";
    double result = DiskCollector.parseSmartctlLine(line);
    assertTrue(Double.isNaN(result));
  }

  @Test
  void parseSmartctlLine_nonNumericTemp_returnsNaN() {
    String line = "194 Temperature_Celsius     0x0022   062   053   000    Old_age   Always       -       N/A";
    double result = DiskCollector.parseSmartctlLine(line);
    assertTrue(Double.isNaN(result));
  }

  @Test
  void parseSmartctlLine_negativeTemperature() {
    String line = "194 Temperature_Celsius     0x0022   062   053   000    Old_age   Always       -       -5";
    double result = DiskCollector.parseSmartctlLine(line);
    assertEquals(-5.0, result);
  }

  @Test
  void discoverNvmeModel_noNvmeBlock_returnsNull(@TempDir Path tempDir) {
    // empty block directory — no nvme entries
    assertNull(DiskCollector.discoverNvmeModel(tempDir));
  }

  @Test
  void discoverNvmeModel_blockMissingModelFile_returnsNull(@TempDir Path tempDir) throws IOException {
    Path nvmeDir = tempDir.resolve("nvme0n1");
    Files.createDirectories(nvmeDir.resolve("device"));

    assertNull(DiskCollector.discoverNvmeModel(tempDir));
  }

  @Test
  void discoverNvmeModel_blockWithModel_returnsModel(@TempDir Path tempDir) throws IOException {
    Path deviceDir = tempDir.resolve("nvme0n1/device");
    Files.createDirectories(deviceDir);
    Files.writeString(deviceDir.resolve("model"), "Samsung SSD 970 EVO Plus");

    assertEquals("Samsung SSD 970 EVO Plus",
        DiskCollector.discoverNvmeModel(tempDir));
  }

  @Test
  void discoverNvmeModel_multipleNvmeBlocks_returnsFirst(@TempDir Path tempDir) throws IOException {
    Files.createDirectories(tempDir.resolve("nvme0n1/device"));
    Files.writeString(tempDir.resolve("nvme0n1/device/model"), "Disk One");
    Files.createDirectories(tempDir.resolve("nvme1n1/device"));
    Files.writeString(tempDir.resolve("nvme1n1/device/model"), "Disk Two");

    assertEquals("Disk One", DiskCollector.discoverNvmeModel(tempDir));
  }

  @Test
  void discoverSataModel_modelExists_returnsModel(@TempDir Path tempDir) throws IOException {
    Path deviceDir = tempDir.resolve("sda/device");
    Files.createDirectories(deviceDir);
    Files.writeString(deviceDir.resolve("model"), "WDC WD10EZEX-00WN4A0");

    assertEquals("WDC WD10EZEX-00WN4A0",
        DiskCollector.discoverSataModel("sda", tempDir));
  }

  @Test
  void discoverSataModel_noModelFile_returnsNull(@TempDir Path tempDir) {
    assertNull(DiskCollector.discoverSataModel("sdb", tempDir));
  }

  @Test
  void discoverSataModel_ioErrorOnRead_returnsNull(@TempDir Path tempDir) throws IOException {
    // model file exists but is a directory, causing read failure
    Files.createDirectories(tempDir.resolve("sdc/device/model"));

    assertNull(DiskCollector.discoverSataModel("sdc", tempDir));
  }

  @Test
  void listConstructor_storesDevices() {
    // just verify the test-only constructor doesn't crash
    DiskCollector collector = new DiskCollector(List.of("/dev/sda", "/dev/sdb"));
    assertEquals(CollectorStatus.UNAVAILABLE, collector.getStatus());
  }

  @Test
  void collect_returnsEmptyWhenUnavailable() {
    AppConfig cfg = AppConfig.load();
    DiskCollector unavailableCollector = new DiskCollector(cfg);
    assertEquals(CollectorStatus.UNAVAILABLE, unavailableCollector.getStatus());
    assertTrue(unavailableCollector.collect().isEmpty());
  }

  @Test
  void getDiskLabels_returnsEmptyBeforeInitialize() {
    assertTrue(collector.getDiskLabels().isEmpty());
  }


}
