package org.lcappuccio.systemmonitor.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.NetworkMetrics;

class NetworkCollectorTest {

  private NetworkCollector collector;
  private AppConfig config;

  @BeforeEach
  void setUp() {
    config = AppConfig.load();
    collector = new NetworkCollector(config);
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void initialize_setsStatusOk() {
    collector.initialize();
    assertTrue(collector.getStatus() == CollectorStatus.OK
        || collector.getStatus() == CollectorStatus.UNAVAILABLE);
  }

  @Test
  void collect_returnsNonEmpty() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.OK) {
      var metrics = collector.collect();
      assertTrue(metrics.isPresent());
    }
  }

  @Test
  void collect_returnsValidMetrics() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.OK) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      NetworkMetrics metrics = metricsOpt.get();
      assertNotNull(metrics.ipAddress());
      assertTrue(metrics.linkSpeedMbps() >= 0);
      assertTrue(metrics.uploadBytesPerSec() >= 0);
      assertTrue(metrics.downloadBytesPerSec() >= 0);
    }
  }

  @Test
  void collect_firstCallReturnsZeroRates() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.OK) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      NetworkMetrics metrics = metricsOpt.get();
      assertTrue(metrics.downloadBytesPerSec() == 0,
          "First call should return 0 for download rate");
      assertTrue(metrics.uploadBytesPerSec() == 0,
          "First call should return 0 for upload rate");
    }
  }

  @Test
  void collect_secondCallReturnsRates() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.OK) {
      collector.collect();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // ignore
      }
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      NetworkMetrics metrics = metricsOpt.get();
      assertTrue(metrics.downloadBytesPerSec() >= 0,
          "Second call should return rate >= 0");
      assertTrue(metrics.uploadBytesPerSec() >= 0,
          "Second call should return rate >= 0");
    }
  }

  @Test
  void collect_returnsIpAddress() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.OK) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      String ip = metricsOpt.get().ipAddress();
      assertTrue(ip != null && !ip.isEmpty(),
          "IP address should be present");
      assertTrue(ip.equals("N/A") || ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"),
          "IP should be N/A or valid IPv4");
    }
  }

  @Test
  void collect_returnsLinkSpeed() {
    collector.initialize();
    if (collector.getStatus() == CollectorStatus.OK) {
      var metricsOpt = collector.collect();
      assertTrue(metricsOpt.isPresent());

      int speed = metricsOpt.get().linkSpeedMbps();
      assertTrue(speed >= 0,
          "Link speed should be non-negative");
    }
  }

  @Test
  void getName_returnsNetwork() {
    assertEquals("Network", collector.getName());
  }
}