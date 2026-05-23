package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lcappuccio.systemmonitor.model.DiskMetrics;

class DiskCollectorTest {

  private DiskCollector collector;

  @BeforeEach
  void setUp() {
    collector = new DiskCollector();
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
  void getDiskLabels_returnsEmptyBeforeInitialize() {
    assertTrue(collector.getDiskLabels().isEmpty());
  }

  @Test
  void discoverNvmeModel_noNvmeBlock_returnsNull(@TempDir Path tempDir) {
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
  void collect_returnsEmptyWhenUnavailable() {
    assertEquals(CollectorStatus.UNAVAILABLE, collector.getStatus());
    assertTrue(collector.collect().isEmpty());
  }
}
