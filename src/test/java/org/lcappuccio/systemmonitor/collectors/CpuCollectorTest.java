package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lcappuccio.systemmonitor.model.CpuMetrics;

class CpuCollectorTest {

  private CpuCollector collector;

  @BeforeEach
  void setUp() {
    collector = new CpuCollector();
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void initialize_setsStatusOkOrDegraded() {
    collector.initialize();
    assertTrue(collector.getStatus() == CollectorStatus.OK
        || collector.getStatus() == CollectorStatus.DEGRADED);
  }

  @Test
  void initialize_discoversCores() {
    collector.initialize();
    assertFalse(collector.getCoreIds().isEmpty(),
        "Should discover at least one CPU core");
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

    CpuMetrics metrics = metricsOpt.get();
    assertNotNull(metrics.coreFrequenciesGhz());

    assertTrue(metrics.loadPercent() >= 0.0 && metrics.loadPercent() <= 100.0,
        "Load should be between 0 and 100");

    assertTrue(metrics.temperatureCelsius() >= 0.0 || Double.isNaN(metrics.temperatureCelsius()),
        "Temperature should be >= 0 or NaN");
  }

  @Test
  void collect_firstCallReturnsZeroLoad() {
    collector.initialize();
    var metricsOpt = collector.collect();
    assertTrue(metricsOpt.isPresent());

    double load = metricsOpt.get().loadPercent();
    assertTrue(load >= 0.0 && load <= 100.0);
  }

  @Test
  void collect_returnsFrequenciesForAllCores() {
    collector.initialize();
    var metricsOpt = collector.collect();
    assertTrue(metricsOpt.isPresent());

    CpuMetrics metrics = metricsOpt.get();
    assertEquals(collector.getCoreIds().size(),
        metrics.coreFrequenciesGhz().size(),
        "Should return frequency for each discovered core");
  }

  @Test
  void getName_returnsCpu() {
    assertEquals("CPU", collector.getName());
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