package org.lcappuccio.systemmonitor.ui;

import java.util.OptionalDouble;

/**
 * Stateless utility for extracting a plottable numeric value from a
 * {@link MetricRow} display string.
 *
 * <p>All methods are static. This class cannot be instantiated.
 *
 * <p>Supported formats:
 * <ul>
 *   <li>{@code "42.3°C"} → {@code 42.3}</li>
 *   <li>{@code "85.0%"} → {@code 85.0}</li>
 *   <li>{@code "3.60 GHz"} → {@code 3.60}</li>
 *   <li>{@code "1.24 GB"} → {@code 1.24}</li>
 *   <li>{@code "1024 B/s"} → {@code 1024.0}</li>
 *   <li>{@code "2500 RPM"} → {@code 2500.0}</li>
 *   <li>{@code "1000 Mbps"} → {@code 1000.0}</li>
 *   <li>{@code "32 / 55 / 91 GB"} → {@code 32.0} (first numeric token, used bytes)</li>
 *   <li>{@code "—"} → empty (not yet collected)</li>
 *   <li>{@code "N/A"} → empty (unavailable)</li>
 *   <li>{@code "192.168.1.1"} → empty (non-numeric)</li>
 * </ul>
 */
public final class MetricValueParser {

  private MetricValueParser() {
    // utility class, no instances
  }

  /**
   * Parses the first numeric token from a metric display string.
   *
   * <p>Returns {@link OptionalDouble#empty()} for sentinel values ({@code "—"}, {@code "N/A"})
   * and for strings where no leading numeric token can be extracted.
   *
   * @param displayValue the display string from a {@link MetricRow}
   * @return an {@link OptionalDouble} containing the parsed value, or empty if not plottable
   */
  public static OptionalDouble parse(String displayValue) {
    if (displayValue == null) {
      return OptionalDouble.empty();
    }

    String trimmed = displayValue.trim();

    if (trimmed.isEmpty() || "—".equals(trimmed) || "N/A".equals(trimmed)) {
      return OptionalDouble.empty();
    }

    // Extract the first whitespace- or unit-delimited numeric token
    String numeric = extractFirstNumericToken(trimmed);
    if (numeric.isEmpty()) {
      return OptionalDouble.empty();
    }

    try {
      return OptionalDouble.of(Double.parseDouble(numeric));
    } catch (NumberFormatException e) {
      return OptionalDouble.empty();
    }
  }

  /**
   * Extracts the first numeric token (integer or decimal) from the given string.
   *
   * <p>Handles leading minus sign for negative values (e.g. sub-zero temperatures).
   *
   * @param value the trimmed display string
   * @return the first numeric substring, or empty string if none found
   */
  private static String extractFirstNumericToken(String value) {
    StringBuilder sb = new StringBuilder();
    boolean seenDigit = false;
    boolean seenDot = false;

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);

      if (c == '-' && sb.length() == 0 && i + 1 < value.length()
          && Character.isDigit(value.charAt(i + 1))) {
        sb.append(c);
      } else if (Character.isDigit(c)) {
        sb.append(c);
        seenDigit = true;
      } else if (c == '.' && seenDigit && !seenDot) {
        sb.append(c);
        seenDot = true;
      } else if (seenDigit) {
        // Stop at first non-numeric character after digits
        break;
      }
    }

    // Avoid returning a bare "-" or trailing dot
    String result = sb.toString();
    if (result.equals("-") || result.equals(".")) {
      return "";
    }
    return result;
  }
}