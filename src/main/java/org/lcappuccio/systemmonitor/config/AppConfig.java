package org.lcappuccio.systemmonitor.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application configuration loaded from bundled defaults and an optional user config file.
 *
 * <p>Loading order:
 * <ol>
 *   <li>Bundled {@code config.properties} from classpath (always present)</li>
 *   <li>User config at {@code ~/.config/linux-system-monitor/config.properties} (optional)</li>
 * </ol>
 *
 * <p>If the user config is absent a warning is logged and bundled defaults are used.
 * If a property value is invalid an error is logged and the bundled default is used.
 */
public final class AppConfig {

  private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
  private static final String BUNDLED_CONFIG = "/config.properties";
  private static final String USER_CONFIG_PATH =
      System.getProperty("user.home") + "/.config/linux-system-monitor/config.properties";

  private final int historySize;
  private final double tickSeconds;

  private final String netInterface;
  private final String gpuDrmPath;
  private final String diskSataDevice;
  private final String networkSpeedUnit;
  private final List<String> fsMountpoints;
  private final int pollIntervalDefault;
  private final int pollIntervalFilesystem;
  private final int pollIntervalDiskTemp;

  private final String colorCpu;
  private final String colorGpu;
  private final String colorVram;
  private final String colorNvme;
  private final String colorSata;
  private final String colorMemoryUsed;
  private final String colorSwapUsed;
  private final List<String> colorCpuClocks;

  private final boolean chartCpuEnabled;
  private final boolean chartLoadEnabled;
  private final boolean chartMemoryEnabled;
  private final boolean chartFrequencyEnabled;

  private AppConfig(Properties props) {

    this.historySize = parseInt(props, "history.size", 25);
    this.tickSeconds = Double.parseDouble(props.getProperty("tick.seconds", "2"));

    this.netInterface = props.getProperty("net.interface", "enp9s0");
    this.gpuDrmPath = props.getProperty("gpu.drm.path", "/sys/class/drm/card1");
    this.diskSataDevice = props.getProperty("disk.sata.device", "/dev/sda");
    this.networkSpeedUnit = props.getProperty("network.speed.unit", "Kbps");
    this.fsMountpoints = Arrays.asList(props.getProperty("fs.mountpoints", "/,/home,/data")
        .split(","));

    this.pollIntervalDefault = parseInt(props, "poll.interval.default", 2);
    this.pollIntervalFilesystem = parseInt(props, "poll.interval.filesystem", 60);
    this.pollIntervalDiskTemp = parseInt(props, "poll.interval.disk.temp", 15);

    this.colorCpu = props.getProperty("chart.color.cpu", "#0A6FC2");
    this.colorGpu = props.getProperty("chart.color.gpu", "#F44336");
    this.colorVram = props.getProperty("chart.color.vram", "#FF9800");
    this.colorNvme = props.getProperty("chart.color.nvme", "#9E9E9E");
    this.colorSata = props.getProperty("chart.color.sata", "#607D8B");
    this.colorMemoryUsed = props.getProperty("chart.color.memory.used", "#2EB82E");
    this.colorSwapUsed = props.getProperty("chart.color.swap.used", "#FFCCFF");
    this.colorCpuClocks = Arrays.asList(props.getProperty("chart.color.cpu.clocks",
        "#0A305C,#0D3C73,#0F488A,#1254A1,#1461B8,#176DCF,#176DCF,#3086E8,#4794EB,"
            + "#5EA1ED,#75AEF0,#8CBCF2,#A3C9F5,#BAD7F7,#D1E4FA,#E8F2FC").split(","));

    this.chartCpuEnabled = Boolean.parseBoolean(props.getProperty(
        "chart.group.temperature.enabled", "true"));
    this.chartLoadEnabled = Boolean.parseBoolean(props.getProperty(
        "chart.group.load.enabled", "true"));
    this.chartMemoryEnabled = Boolean.parseBoolean(props.getProperty(
        "chart.group.memory.enabled", "true"));
    this.chartFrequencyEnabled = Boolean.parseBoolean(props.getProperty(
        "chart.group.frequencies.enabled", "true"));
  }

  /**
   * Loads configuration from bundled defaults overlaid with the user config file if present.
   *
   * @return a fully populated {@link AppConfig} instance
   */
  public static AppConfig load() {
    Properties props = loadBundledDefaults();
    overlayUserConfig(props);
    return new AppConfig(props);
  }

  private static Properties loadBundledDefaults() {
    Properties props = new Properties();
    try (InputStream in = AppConfig.class.getResourceAsStream(BUNDLED_CONFIG)) {
      if (in == null) {
        LOG.warn("Bundled config not found on classpath: {}", BUNDLED_CONFIG);
      } else {
        props.load(in);
        LOG.info("Loaded bundled defaults from {}", BUNDLED_CONFIG);
      }
    } catch (IOException e) {
      LOG.error("Failed to load bundled config: {}", e.getMessage());
    }
    return props;
  }

  private static void overlayUserConfig(Properties props) {
    var userConfig = Paths.get(USER_CONFIG_PATH);
    if (!Files.exists(userConfig)) {
      LOG.warn("User config not found at {}, using bundled defaults", USER_CONFIG_PATH);
      return;
    }
    try (InputStream in = Files.newInputStream(userConfig)) {
      props.load(in);
      LOG.info("Loaded user config from {}", USER_CONFIG_PATH);
    } catch (IOException e) {
      LOG.warn("Failed to read user config {}, using bundled defaults: {}",
          USER_CONFIG_PATH, e.getMessage());
    }
  }

  private static int parseInt(Properties props, String key, int fallback) {
    String raw = props.getProperty(key);
    if (raw == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      LOG.error("Invalid integer for '{}': '{}', using fallback {}", key, raw, fallback);
      return fallback;
    }
  }

  /**
   * Returns the configured history size for charts.
   *
   * @return the value
   */
  public int getHistorySize() {
    return historySize;
  }

  /**
   * Returns the configured tick for charts in seconds.
   *
   * @return the value
   */
  public Double getTickSeconds() {
    return tickSeconds;
  }

  /**
   * Returns the network interface name, e.g. {@code enp9s0}.
   *
   * @return network interface name.
   */
  public String getNetInterface() {
    return netInterface;
  }

  /**
   * Returns the DRM sysfs path for the GPU, e.g. {@code /sys/class/drm/card1}.
   *
   * @return GPU DRM path.
   */
  public String getGpuDrmPath() {
    return gpuDrmPath;
  }

  /**
   * Returns the SATA disk device path, e.g. {@code /dev/sda}.
   *
   * @return SATA device path.
   */
  public String getDiskSataDevice() {
    return diskSataDevice;
  }

  /**
   * Returns the list of filesystem mount points to monitor.
   *
   * @return list of mount point paths.
   */
  public List<String> getFsMountpoints() {
    return fsMountpoints;
  }

  /**
   * Returns the default polling interval in seconds.
   *
   * @return polling interval in seconds.
   */
  public int getPollIntervalDefault() {
    return pollIntervalDefault;
  }

  /**
   * Returns the filesystem polling interval in seconds.
   *
   * @return filesystem polling interval in seconds.
   */
  public int getPollIntervalFilesystem() {
    return pollIntervalFilesystem;
  }

  /**
   * Returns the disk temperature polling interval in seconds.
   *
   * @return disk temperature polling interval in seconds.
   */
  public int getPollIntervalDiskTemp() {
    return pollIntervalDiskTemp;
  }

  /**
   * Returns the selected network speed for charts.
   *
   * @return network metric to be used in network chart.
   */
  public String getNetworkSpeedUnit() {
    return networkSpeedUnit;
  }

  /**
   * Returns the selected cpu colour for charts.
   *
   * @return the value.
   */
  public String getColorCpu() {
    return colorCpu;
  }

  /**
   * Returns the selected gpu colour for charts.
   *
   * @return the value.
   */
  public String getColorGpu() {
    return colorGpu;
  }

  /**
   * Returns the selected vram colour for charts.
   *
   * @return the value.
   */
  public String getColorVram() {
    return colorVram;
  }

  /**
   * Returns the selected nvme colour for charts.
   *
   * @return the value.
   */
  public String getColorNvme() {
    return colorNvme;
  }

  /**
   * Returns the selected sata colour for charts.
   *
   * @return the value.
   */
  public String getColorSata() {
    return colorSata;
  }

  /**
   * Returns the selected memory used for charts.
   *
   * @return the value.
   */
  public String getColorMemoryUsed() {
    return colorMemoryUsed;
  }

  /**
   * Returns the selected swap used for charts.
   *
   * @return the value.
   */
  public String getColorSwapUsed() {
    return colorSwapUsed;
  }

  /**
   * Returns the selected cpu clock colours for charts.
   *
   * @return the value.
   */
  public List<String> getColorCpuClocks() {
    return colorCpuClocks;
  }

  /**
   * Returns the selected cpu charts enable flag.
   *
   * @return the value.
   */
  public boolean isChartCpuEnabled() {
    return chartCpuEnabled;
  }

  /**
   * Returns the selected load charts enable flag.
   *
   * @return the value.
   */
  public boolean isChartLoadEnabled() {
    return chartLoadEnabled;
  }

  /**
   * Returns the selected memory charts enable flag.
   *
   * @return the value.
   */
  public boolean isChartMemoryEnabled() {
    return chartMemoryEnabled;
  }

  /**
   * Returns the selected cpu frequencies charts enable flag.
   *
   * @return the value.
   */
  public boolean isChartFrequencyEnabled() {
    return chartFrequencyEnabled;
  }
}