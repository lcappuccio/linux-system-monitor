package org.lcappuccio.systemmonitor.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}