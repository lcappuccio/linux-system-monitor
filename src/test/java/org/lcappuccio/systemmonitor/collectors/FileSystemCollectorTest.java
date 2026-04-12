package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.FileSystemMetrics;

class FileSystemCollectorTest {

  private FileSystemCollector collector;
  private AppConfig config;

  @BeforeEach
  void setUp() {
    config = AppConfig.load();
    collector = new FileSystemCollector(config);
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void initialize_setsStatusOk() {
    collector.initialize();
    assertTrue(collector.getStatus() == CollectorStatus.OK
        || collector.getStatus() == CollectorStatus.DEGRADED);
  }

  @Test
  void collect_returnsNonEmpty() {
    collector.initialize();
    var metrics = collector.collect();
    assertTrue(metrics.isPresent());
  }

  @Test
  void collect_returnsValidMetrics() {
    collector.initialize();
    var metricsOpt = collector.collect();
    assertTrue(metricsOpt.isPresent());

    FileSystemMetrics metrics = metricsOpt.get();
    assertNotNull(metrics.usage());

    assertTrue(metrics.usage().size() > 0);

    for (var entry : metrics.usage().entrySet()) {
      String mount = entry.getKey();
      FileSystemMetrics.FileSystemUsage usage = entry.getValue();

      assertTrue(usage.usedBytes() >= 0, "Used should be >= 0 for " + mount);
      assertTrue(usage.freeBytes() >= 0, "Free should be >= 0 for " + mount);
      assertTrue(usage.totalBytes() > 0, "Total should be > 0 for " + mount);
    }
  }

  @Test
  void collect_usedPlusFreeEqualsTotal() {
    collector.initialize();
    var metricsOpt = collector.collect();
    assertTrue(metricsOpt.isPresent());

    FileSystemMetrics metrics = metricsOpt.get();

    for (var entry : metrics.usage().entrySet()) {
      FileSystemMetrics.FileSystemUsage usage = entry.getValue();
      assertEquals(usage.totalBytes(),
          usage.usedBytes() + usage.freeBytes(),
          "Used + Free should equal Total for " + entry.getKey());
    }
  }

  @Test
  void getName_returnsFilesystems() {
    assertEquals("Filesystems", collector.getName());
  }
}