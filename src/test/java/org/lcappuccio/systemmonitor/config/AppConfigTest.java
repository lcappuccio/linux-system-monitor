package org.lcappuccio.systemmonitor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppConfigTest {

  @Test
  void load_returnsNonNullInstance() {
    AppConfig config = AppConfig.load();
    assertNotNull(config);
  }

  @Test
  void load_returnsBundledDefaults() {
    AppConfig config = AppConfig.load();
    assertEquals("test0", config.getNetInterface());
    assertEquals("/sys/test", config.getGpuDrmPath());
    assertEquals("/dev/test", config.getDiskSataDevice());
    assertEquals(99, config.getPollIntervalDefault());
    assertEquals(88, config.getPollIntervalFilesystem());
    assertEquals(77, config.getPollIntervalDiskTemp());
  }

  @Test
  void load_parsesFsMountpoints() {
    AppConfig config = AppConfig.load();
    assertNotNull(config.getFsMountpoints());
    assertEquals(2, config.getFsMountpoints().size());
    assertEquals("/", config.getFsMountpoints().get(0));
    assertEquals("/tmp", config.getFsMountpoints().get(1));
  }

  @Test
  void getters_returnExpectedValues() {
    AppConfig config = AppConfig.load();
    assertEquals("test0", config.getNetInterface());
    assertEquals("/sys/test", config.getGpuDrmPath());
    assertEquals("/dev/test", config.getDiskSataDevice());
  }

  @Test
  void pollIntervals_haveValidValues() {
    AppConfig config = AppConfig.load();
    assertFalse(config.getPollIntervalDefault() <= 0);
    assertFalse(config.getPollIntervalFilesystem() <= 0);
    assertFalse(config.getPollIntervalDiskTemp() <= 0);
  }

  @Test
  void networkSpeed_hasValidValues() {
    AppConfig config = AppConfig.load();
    assertEquals("Kbps", config.getNetworkSpeedUnit());
  }

  @Test
  void colorGetters_returnExpectedValues() {
    AppConfig config = AppConfig.load();
    assertEquals("#0A6FC2", config.getColorCpu());
    assertEquals("#F44336", config.getColorGpu());
    assertEquals("#FF9800", config.getColorVram());
    assertEquals("#9E9E9E", config.getColorNvme());
    assertEquals("#607D8B", config.getColorSata());
    assertEquals("#2EB82E", config.getColorMemoryUsed());
    assertEquals("#FFCCFF", config.getColorSwapUsed());
  }

  @Test
  void colorCpuClocks_returnsExpectedList() {
    AppConfig config = AppConfig.load();
    var cpuClocks = config.getColorCpuClocks();
    assertNotNull(cpuClocks);
    assertFalse(cpuClocks.isEmpty());
    assertEquals("#0A305C", cpuClocks.get(0));
    assertEquals("#0D3C73", cpuClocks.get(1));
    assertEquals("#0F488A", cpuClocks.get(2));
    // Test that it parses the comma-separated string correctly
    assertEquals(16, cpuClocks.size());
  }

  @Test
  void historySize_returnsExpectedValue() {
    AppConfig config = AppConfig.load();
    assertEquals(30, config.getHistorySize());
  }

  @Test
  void tickSeconds_returnsExpectedValue() {
    AppConfig config = AppConfig.load();
    assertEquals(2, config.getTickSeconds());
  }

  @Test
  void isChartCpuEnabled_returnsExpectedValue() {
    AppConfig config = AppConfig.load();
    assertTrue(config.isChartCpuEnabled());
  }

  @Test
  void isChartLoadEnabled_returnsExpectedValue() {
    AppConfig config = AppConfig.load();
    assertTrue(config.isChartLoadEnabled());
  }

  @Test
  void isChartMemoryEnabled_returnsExpectedValue() {
    AppConfig config = AppConfig.load();
    assertTrue(config.isChartMemoryEnabled());
  }

  @Test
  void isChartFrequencyEnabled_returnsExpectedValue() {
    AppConfig config = AppConfig.load();
    assertFalse(config.isChartFrequencyEnabled());
  }

}