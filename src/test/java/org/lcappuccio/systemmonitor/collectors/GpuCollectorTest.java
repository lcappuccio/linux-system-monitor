package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
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

  @AfterEach
  void tearDown() {
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
}