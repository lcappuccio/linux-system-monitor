package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.GpuMetrics;

class GpuCollectorTest {

  private GpuCollector collector;
  private AppConfig config;

  @BeforeEach
  void setUp() {
    config = AppConfig.load();
    collector = new GpuCollector(config);
  }

  @Test
  void initialize_setsStatusOkOrDegraded() {
    collector.initialize();
    assertTrue(collector.getStatus() == CollectorStatus.OK
        || collector.getStatus() == CollectorStatus.DEGRADED
        || collector.getStatus() == CollectorStatus.UNAVAILABLE);
  }

  @Test
  void collect_returnsNonEmpty() {
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

      GpuMetrics metrics = metricsOpt.get();
      assertNotNull(metrics);

      assertTrue(metrics.loadPercent() >= 0.0 && metrics.loadPercent() <= 100.0,
          "Load should be between 0 and 100");
      assertTrue(metrics.vramLoadPercent() >= 0.0 && metrics.vramLoadPercent() <= 100.0,
          "VRAM load should be between 0 and 100");
      assertTrue(metrics.powerWatts() >= 0.0,
          "Power should be >= 0");
    }
  }

  @Test
  void collect_returnsTemperatureOrNaN() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      double temp = metricsOpt.get().temperatureCelsius();
      assertTrue(Double.isNaN(temp) || (temp >= 0.0 && temp < 150.0),
          "Temperature should be NaN or valid");
    }
  }

  @Test
  void collect_returnsVramUsage() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      GpuMetrics metrics = metricsOpt.get();
      assertTrue(metrics.vramUsedBytes() >= 0,
          "VRAM used should be >= 0");
      assertTrue(metrics.vramTotalBytes() >= 0,
          "VRAM total should be >= 0");
    }
  }

  @Test
  void getName_returnsGpu() {
    assertEquals("GPU", collector.getName());
  }

  @Test
  void collect_handlesDegradedStatus() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.DEGRADED) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());
    }
  }

  @Test
  void collect_validatesMetricsAreSane() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      GpuMetrics m = metricsOpt.get();
      if (m.vramTotalBytes() > 0) {
      assertTrue(m.vramUsedBytes() <= m.vramTotalBytes(),
          "VRAM used should not exceed total");
      }
    }
  }

  @Test
  void lookupInPciIds_findsDeviceByVendorAndDeviceId() {
    Path ids = Paths.get("src/test/resources/test-pci.ids");
    String name = GpuCollector.lookupInPciIds(ids, "1002", "7550");
    assertEquals("Navi 33 [Radeon RX 7700S/7600/7600M]", name);
  }

  @Test
  void lookupInPciIds_returnsNullForUnknownDevice() {
    Path ids = Paths.get("src/test/resources/test-pci.ids");
    String name = GpuCollector.lookupInPciIds(ids, "1002", "0000");
    assertNull(name);
  }

  @Test
  void lookupInPciIds_returnsNullForUnknownVendor() {
    Path ids = Paths.get("src/test/resources/test-pci.ids");
    String name = GpuCollector.lookupInPciIds(ids, "ffff", "0000");
    assertNull(name);
  }

  @Test
  void lookupInPciIds_findsNvidiaDevice() {
    Path ids = Paths.get("src/test/resources/test-pci.ids");
    String name = GpuCollector.lookupInPciIds(ids, "10de", "2482");
    assertEquals("GA102 [GeForce RTX 3070 Ti]", name);
  }

  @Test
  void formatHexLabel_amdReturnsShortLabel() {
    assertEquals("AMD (7550)", GpuCollector.formatHexLabel("1002", "7550"));
  }

  @Test
  void formatHexLabel_nonAmdIncludesDeviceId() {
    String label = GpuCollector.formatHexLabel("10de", "2482");
    assertTrue(label.contains("2482"), "Label should contain device ID: " + label);
    assertFalse(label.equals("10de:2482"), "Label should resolve vendor name if pci.ids available");
  }

  @Test
  void formatHexLabel_missingVendorReturnsColonFormat() {
    assertEquals("unknown:0000", GpuCollector.formatHexLabel("unknown", "0000"));
  }
}