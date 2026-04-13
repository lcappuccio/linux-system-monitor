package org.lcappuccio.systemmonitor.model;

/**
 * Enumeration of metric keys used to identify system metrics across the application.
 *
 * <p>Provides a centralized set of string keys for metrics such as CPU, GPU, memory,
 * network, disk, and filesystem data. Each nested enum represents a category of metrics.
 */
public enum MetricKey {

  Memory,
  Network,
  CPU,
  GPU,
  Disks,
  Filesystems;

  /**
   * Constructs a metric key string by combining the enum name with a metric name.
   *
   * @param metric the specific metric identifier
   * @return the full key in the format "Section.Metric"
   */
  public String key(String metric) {
    return name() + "." + metric;
  }

  /**
   * Memory-related metric keys.
   */
  public enum Mem {
    USED("Used"),
    SWAP_USED("Swap Used");

    private final String metric;

    Mem(String metric) {
      this.metric = metric;
    }

    public String key() {
      return MetricKey.Memory.key(metric);
    }
  }

  /**
   * Network-related metric keys.
   */
  public enum Net {
    IP_ADDRESS("IP Address"),
    LINK_SPEED("Link Speed"),
    UPLOAD("Upload"),
    DOWNLOAD("Download");

    private final String metric;

    Net(String metric) {
      this.metric = metric;
    }

    public String key() {
      return MetricKey.Network.key(metric);
    }
  }

  /**
   * CPU-related metric keys.
   */
  public enum Cpu {
    TEMPERATURE("Temperature"),
    LOAD("Load");

    private final String metric;

    Cpu(String metric) {
      this.metric = metric;
    }

    public String key() {
      return MetricKey.CPU.key(metric);
    }
  }

  /**
   * GPU-related metric keys.
   */
  public enum Gpu {
    TEMPERATURE("Temperature"),
    LOAD("Load"),
    VRAM_USED("VRAM Used"),
    VRAM_TEMPERATURE("VRAM Temperature"),
    VRAM_LOAD("VRAM Load"),
    POWER("Power"),
    FAN("Fan");

    private final String metric;

    Gpu(String metric) {
      this.metric = metric;
    }

    public String key() {
      return MetricKey.GPU.key(metric);
    }
  }

  /**
   * Disk-related metric keys.
   */
  public enum Disk {
    NVME_TEMPERATURE("NVMe Temperature"),
    SSD_TEMPERATURE("SSD Temperature");

    private final String metric;

    Disk(String metric) {
      this.metric = metric;
    }

    public String key() {
      return MetricKey.Disks.key(metric);
    }
  }

  /**
   * Filesystem-related metric keys (dynamic, mount-point based).
   */
  public enum Filesystem {
    ;

    public static String key(String mount) {
      return MetricKey.Filesystems.key(mount);
    }
  }
}