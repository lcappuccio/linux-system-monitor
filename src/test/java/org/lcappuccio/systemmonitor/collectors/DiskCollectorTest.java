package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
