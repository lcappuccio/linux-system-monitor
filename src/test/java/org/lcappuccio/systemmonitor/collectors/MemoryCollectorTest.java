package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lcappuccio.systemmonitor.model.MemoryMetrics;

class MemoryCollectorTest {

  private MemoryCollector collector;

  @BeforeEach
  void setUp() {
    collector = new MemoryCollector();
  }

  @Test
  void initialize_setsStatusToOk() {
    collector.initialize();
    assertEquals(CollectorStatus.OK, collector.getStatus());
  }

  @Test
  void collect_returnsNonEmptyMetrics() {
    collector.initialize();
    var metrics = collector.collect();
    assertTrue(metrics.isPresent());
  }

  @Test
  void collect_returnsValidMemoryMetrics() {
    collector.initialize();
    var metricsOpt = collector.collect();
    assertTrue(metricsOpt.isPresent());
    MemoryMetrics metrics = metricsOpt.get();

    assertTrue(metrics.memUsedBytes() >= 0);
    assertTrue(metrics.memTotalBytes() > 0);
    assertTrue(metrics.swapTotalBytes() >= 0);
    assertTrue(metrics.swapUsedBytes() >= 0);
  }

  @Test
  void collect_memUsedIsLessThanTotal() {
    collector.initialize();
    var metricsOpt = collector.collect();
    assertTrue(metricsOpt.isPresent());
    MemoryMetrics metrics = metricsOpt.get();

    assertTrue(metrics.memUsedBytes() < metrics.memTotalBytes(),
        "Used memory should be less than total");
  }

  @Test
  void getName_returnsMemory() {
    assertEquals("Memory", collector.getName());
  }

  @Test
  void statusIsNotUnvailableAfterInit() {
    collector.initialize();
    assertNotNull(collector.getStatus());
  }
}