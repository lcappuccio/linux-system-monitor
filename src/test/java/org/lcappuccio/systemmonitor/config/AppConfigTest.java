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
    assertEquals("enp9s0", config.getNetInterface());
    assertEquals("/sys/class/drm/card1", config.getGpuDrmPath());
    assertEquals("/dev/sda", config.getDiskSataDevice());
    assertEquals(2, config.getPollIntervalDefault());
    assertEquals(60, config.getPollIntervalFilesystem());
    assertEquals(15, config.getPollIntervalDiskTemp());
  }

  @Test
  void load_parsesFsMountpoints() {
    AppConfig config = AppConfig.load();
    assertNotNull(config.getFsMountpoints());
    assertEquals(3, config.getFsMountpoints().size());
    assertEquals("/", config.getFsMountpoints().get(0));
    assertEquals("/home", config.getFsMountpoints().get(1));
    assertEquals("/data", config.getFsMountpoints().get(2));
  }

  @Test
  void getters_returnExpectedValues() {
    AppConfig config = AppConfig.load();
    assertEquals("enp9s0", config.getNetInterface());
    assertEquals("/sys/class/drm/card1", config.getGpuDrmPath());
    assertEquals("/dev/sda", config.getDiskSataDevice());
  }

  @Test
  void pollIntervals_haveValidValues() {
    AppConfig config = AppConfig.load();
    assertFalse(config.getPollIntervalDefault() <= 0);
    assertFalse(config.getPollIntervalFilesystem() <= 0);
    assertFalse(config.getPollIntervalDiskTemp() <= 0);
  }
}