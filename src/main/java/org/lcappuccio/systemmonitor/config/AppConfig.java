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

  private final String netInterface;
  private final String gpuDrmPath;
  private final String diskSataDevice;
  private final String networkSpeedUnit;
  private final List<String> fsMountpoints;
  private final int pollIntervalDefault;
  private final int pollIntervalFilesystem;
  private final int pollIntervalDiskTemp;

  private AppConfig(Properties props) {
    this.netInterface = props.getProperty("net.interface");
    this.gpuDrmPath = props.getProperty("gpu.drm.path");
    this.diskSataDevice = props.getProperty("disk.sata.device");
    this.networkSpeedUnit = props.getProperty("network.speed.unit");
    this.fsMountpoints = Arrays.asList(props.getProperty("fs.mountpoints").split(","));
    this.pollIntervalDefault = parseInt(props, "poll.interval.default", 2);
    this.pollIntervalFilesystem = parseInt(props, "poll.interval.filesystem", 60);
    this.pollIntervalDiskTemp = parseInt(props, "poll.interval.disk.temp", 15);
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
   * Returns the network interface name, e.g. {@code enp9s0}.
   *
   * @return network interface name
   */
  public String getNetInterface() {
    return netInterface;
  }

  /**
   * Returns the DRM sysfs path for the GPU, e.g. {@code /sys/class/drm/card1}.
   *
   * @return GPU DRM path
   */
  public String getGpuDrmPath() {
    return gpuDrmPath;
  }

  /**
   * Returns the SATA disk device path, e.g. {@code /dev/sda}.
   *
   * @return SATA device path
   */
  public String getDiskSataDevice() {
    return diskSataDevice;
  }

  /**
   * Returns the list of filesystem mount points to monitor.
   *
   * @return list of mount point paths
   */
  public List<String> getFsMountpoints() {
    return fsMountpoints;
  }

  /**
   * Returns the default polling interval in seconds.
   *
   * @return polling interval in seconds
   */
  public int getPollIntervalDefault() {
    return pollIntervalDefault;
  }

  /**
   * Returns the filesystem polling interval in seconds.
   *
   * @return filesystem polling interval in seconds
   */
  public int getPollIntervalFilesystem() {
    return pollIntervalFilesystem;
  }

  /**
   * Returns the disk temperature polling interval in seconds.
   *
   * @return disk temperature polling interval in seconds
   */
  public int getPollIntervalDiskTemp() {
    return pollIntervalDiskTemp;
  }

  /**
   * Returns the selected network speed for charts
   *
   * @return network metric to be used in network chart
   */
  public String getNetworkSpeedUnit() {return networkSpeedUnit;}
}