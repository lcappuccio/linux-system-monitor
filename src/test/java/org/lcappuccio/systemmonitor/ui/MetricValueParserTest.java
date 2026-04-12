package org.lcappuccio.systemmonitor.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class MetricValueParserTest {

  // --- Sentinel values ---

  @Test
  void parse_null_returnsEmpty() {
    assertFalse(MetricValueParser.parse(null).isPresent());
  }

  @Test
  void parse_emptyString_returnsEmpty() {
    assertFalse(MetricValueParser.parse("").isPresent());
  }

  @Test
  void parse_dash_returnsEmpty() {
    assertFalse(MetricValueParser.parse("—").isPresent());
  }

  @Test
  void parse_notAvailable_returnsEmpty() {
    assertFalse(MetricValueParser.parse("N/A").isPresent());
  }

  // --- Temperature ---

  @Test
  void parse_temperatureCelsius_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("42.3°C");
    assertTrue(result.isPresent());
    assertEquals(42.3, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_temperatureWholeNumber_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("65°C");
    assertTrue(result.isPresent());
    assertEquals(65.0, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_negativeTemperature_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("-5.0°C");
    assertTrue(result.isPresent());
    assertEquals(-5.0, result.getAsDouble(), 0.001);
  }

  // --- Percentage ---

  @Test
  void parse_percentage_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("85.0%");
    assertTrue(result.isPresent());
    assertEquals(85.0, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_percentageWholeNumber_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("100%");
    assertTrue(result.isPresent());
    assertEquals(100.0, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_zeroPercent_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("0%");
    assertTrue(result.isPresent());
    assertEquals(0.0, result.getAsDouble(), 0.001);
  }

  // --- Frequency ---

  @Test
  void parse_gigahertz_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("3.60 GHz");
    assertTrue(result.isPresent());
    assertEquals(3.60, result.getAsDouble(), 0.001);
  }

  // --- Bytes / storage ---

  @Test
  void parse_gigabytes_returnsFirstValue() {
    OptionalDouble result = MetricValueParser.parse("1.24 GB");
    assertTrue(result.isPresent());
    assertEquals(1.24, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_megabytes_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("512.0 MB");
    assertTrue(result.isPresent());
    assertEquals(512.0, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_kilobytes_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("128.5 KB");
    assertTrue(result.isPresent());
    assertEquals(128.5, result.getAsDouble(), 0.001);
  }

  // --- Composite filesystem string ---

  @Test
  void parse_compositeFilesystem_returnsFirstToken() {
    OptionalDouble result = MetricValueParser.parse("32 / 55 / 91 GB");
    assertTrue(result.isPresent());
    assertEquals(32.0, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_compositeFilesystemDecimal_returnsFirstToken() {
    OptionalDouble result = MetricValueParser.parse("1.5 / 8.2 / 10.0 GB");
    assertTrue(result.isPresent());
    assertEquals(1.5, result.getAsDouble(), 0.001);
  }

  // --- Network rates ---

  @Test
  void parse_bytesPerSec_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("1024 B/s");
    assertTrue(result.isPresent());
    assertEquals(1024.0, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_kilobytesPerSec_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("12.5 KB/s");
    assertTrue(result.isPresent());
    assertEquals(12.5, result.getAsDouble(), 0.001);
  }

  @Test
  void parse_megabytesPerSec_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("5.2 MB/s");
    assertTrue(result.isPresent());
    assertEquals(5.2, result.getAsDouble(), 0.001);
  }

  // --- Link speed ---

  @Test
  void parse_megabitsPerSec_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("1000 Mbps");
    assertTrue(result.isPresent());
    assertEquals(1000.0, result.getAsDouble(), 0.001);
  }

  // --- Fan speed ---

  @Test
  void parse_rpm_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("2500 RPM");
    assertTrue(result.isPresent());
    assertEquals(2500.0, result.getAsDouble(), 0.001);
  }

  // --- Power ---

  @Test
  void parse_watts_returnsValue() {
    OptionalDouble result = MetricValueParser.parse("120.5 W");
    assertTrue(result.isPresent());
    assertEquals(120.5, result.getAsDouble(), 0.001);
  }

  // --- Non-numeric (should not be plottable) ---

  @Test
  void parse_whitespaceOnly_returnsEmpty() {
    assertFalse(MetricValueParser.parse("   ").isPresent());
  }
}