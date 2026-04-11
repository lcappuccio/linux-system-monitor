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
 * Application configuration loaded from {@code ~/.config/linux-system-monitor/config.properties}.
 *
 * <p>If the file is absent, built-in defaults are used and a warning is logged.
 * If a property is missing, its default value is used silently.
 */
public final class AppConfig {

  private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
  private static final String CONFIG_PATH =
      System.getProperty("user.home") + "/.config/linux-system-monitor/config.properties";

  // Defaults
  private static final String DEFAULT_NET_INTERFACE = "enp9s0";
  private static final String DEFAULT_GPU_DRM_PATH = "/sys/class/drm/card1";
  private static final String DEFAULT_DISK_SATA_DEVICE = "/dev/sda";
  private static final String DEFAULT_FS_MOUNTPOINTS = "/,/home,/data";
  private static final int DEFAULT_POLL_INTERVAL = 2;
  private static final int DEFAULT_POLL_INTERVAL_FS = 60;
  private static final int DEFAULT_POLL_INTERVAL_DISK_TEMP = 15;

  private final String netInterface;
  private final String gpuDrmPath;
  private final String diskSataDevice;
  private final List<String> fsMountpoints;
  private final int pollIntervalDefault;
  private final int pollIntervalFilesystem;
  private final int pollIntervalDiskTemp;

  private AppConfig(Properties props) {
    this.netInterface = props.getProperty("net.interface", DEFAULT_NET_INTERFACE);
    this.gpuDrmPath = props.getProperty("gpu.drm.path", DEFAULT_GPU_DRM_PATH);
    this.diskSataDevice = props.getProperty("disk.sata.device", DEFAULT_DISK_SATA_DEVICE);
    this.fsMountpoints = Arrays.asList(
        props.getProperty("fs.mountpoints", DEFAULT_FS_MOUNTPOINTS).split(","));
    this.pollIntervalDefault =
        parseInt(props, "poll.interval.default", DEFAULT_POLL_INTERVAL);
    this.pollIntervalFilesystem =
        parseInt(props, "poll.interval.filesystem", DEFAULT_POLL_INTERVAL_FS);
    this.pollIntervalDiskTemp =
        parseInt(props, "poll.interval.disk.temp", DEFAULT_POLL_INTERVAL_DISK_TEMP);
  }

  /**
   * Loads configuration from the default path, falling back to built-in defaults if absent.
   *
   * @return a fully populated {@link AppConfig} instance
   */
  public static AppConfig load() {
    var props = new Properties();
    var configFile = Paths.get(CONFIG_PATH);
    if (Files.exists(configFile)) {
      try (InputStream in = Files.newInputStream(configFile)) {
        props.load(in);
        LOG.info("Loaded configuration from {}", CONFIG_PATH);
      } catch (IOException e) {
        LOG.warn("Failed to read config file {}, using defaults: {}", CONFIG_PATH, e.getMessage());
      }
    } else {
      LOG.warn("Config file not found at {}, using built-in defaults", CONFIG_PATH);
    }
    return new AppConfig(props);
  }

  private static int parseInt(Properties props, String key, int defaultValue) {
    var raw = props.getProperty(key);
    if (raw == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      LOG.warn("Invalid integer value for '{}': '{}', using default {}", key, raw, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Returns the network interface name.
   *
   * @return network interface name, e.g. {@code enp9s0}
   */
  public String getNetInterface() {
    return netInterface;
  }

  /**
   * Returns the DRM sysfs path for the GPU.
   *
   * @return DRM sysfs path for the GPU, e.g. {@code /sys/class/drm/card1}
   */
  public String getGpuDrmPath() {
    return gpuDrmPath;
  }

  /**
   * Returns the SATA disk device path.
   *
   * @return SATA disk device path, e.g. {@code /dev/sda}
   */
  public String getDiskSataDevice() {
    return diskSataDevice;
  }

  /**
   * Returns the list of filesystem mount points to monitor.
   *
   * @return list of filesystem mount points to monitor
   */
  public List<String> getFsMountpoints() {
    return fsMountpoints;
  }

  /**
   * Returns the default polling interval in seconds.
   *
   * @return default polling interval in seconds
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
}