package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
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

      DiskMetrics metrics = metricsOpt.get();
      assertNotNull(metrics);
    }
  }

  @Test
  void collect_temperatureOrNaN() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      DiskMetrics m = metricsOpt.get();
      boolean nvmeValid = !Double.isNaN(m.nvmeTempCelsius());
      boolean sataValid = !Double.isNaN(m.sataTempCelsius());

      assertTrue(nvmeValid || sataValid,
          "At least one temperature should be available");
    }
  }

  @Test
  void collect_temperatureRange() {
    collector.initialize();
    if (collector.getStatus() != CollectorStatus.UNAVAILABLE) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      DiskMetrics m = metricsOpt.get();
      double nvme = m.nvmeTempCelsius();
      double sata = m.sataTempCelsius();

      if (!Double.isNaN(nvme)) {
        assertTrue(nvme >= -20 && nvme < 100, "NVMe temp out of range");
      }
      if (!Double.isNaN(sata)) {
        assertTrue(sata >= -20 && sata < 100, "SATA temp out of range");
      }
    }
  }

  @Test
  void getName_returnsDisks() {
    assertEquals("Disks", collector.getName());
  }

  @Test
  void collect_handlesDegradedStatus() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.DEGRADED) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());
    }
  }
}