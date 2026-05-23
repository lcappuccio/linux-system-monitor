package org.lcappuccio.systemmonitor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiskMetricsTest {

  @Test
  void constructor_storesTemperatures() {
    Map<String, Double> temps = new LinkedHashMap<>();
    temps.put("Samsung SSD 970 EVO", 42.0);
    temps.put("WDC WD10EZEX", 38.5);

    DiskMetrics metrics = new DiskMetrics(temps);

    assertEquals(2, metrics.temperatures().size());
    assertEquals(42.0, metrics.temperatures().get("Samsung SSD 970 EVO"));
    assertEquals(38.5, metrics.temperatures().get("WDC WD10EZEX"));
  }

  @Test
  void constructor_returnsUnmodifiableMap() {
    Map<String, Double> temps = new LinkedHashMap<>();
    temps.put("nvme", 40.0);

    DiskMetrics metrics = new DiskMetrics(temps);

    assertThrows(UnsupportedOperationException.class,
        () -> metrics.temperatures().put("sda", 35.0));
  }

  @Test
  void constructor_preservesInsertionOrder() {
    Map<String, Double> temps = new LinkedHashMap<>();
    temps.put("Disk A", 30.0);
    temps.put("Disk B", 31.0);
    temps.put("Disk C", 32.0);

    DiskMetrics metrics = new DiskMetrics(temps);

    var entries = metrics.temperatures().entrySet().iterator();
    assertEquals("Disk A", entries.next().getKey());
    assertEquals("Disk B", entries.next().getKey());
    assertEquals("Disk C", entries.next().getKey());
  }

  @Test
  void constructor_handlesEmptyMap() {
    DiskMetrics metrics = new DiskMetrics(new LinkedHashMap<>());

    assertTrue(metrics.temperatures().isEmpty());
  }

  @Test
  void constructor_handlesNanValues() {
    Map<String, Double> temps = new LinkedHashMap<>();
    temps.put("Disk X", Double.NaN);

    DiskMetrics metrics = new DiskMetrics(temps);

    assertTrue(Double.isNaN(metrics.temperatures().get("Disk X")));
  }
}
