package org.lcappuccio.systemmonitor.ui;

/**
 * Holds display names for hardware components used across UI.
 * Provides single source of truth for names shown in tree and charts.
 *
 * @param cpuModel      CPU model name (e.g., "AMD Ryzen 7 5700X")
 * @param gpuModel     GPU model name (e.g., "AMD GPU (1002:7550)")
 * @param nvmeModel    NVMe model name (e.g., "Samsung SSD 970 EVO Plus")
 * @param sataModel    SATA/SSD model name
 */
public record HardwareNames(
    String cpuModel,
    String gpuModel,
    String nvmeModel,
    String sataModel
) {
  /**
   * Creates HardwareNames from initialized collectors.
   *
   * @param cpuCollector   the CPU collector
   * @param gpuCollector  the GPU collector
   * @param diskCollector the disk collector
   * @return HardwareNames with model names from all collectors
   */
  public static HardwareNames fromCollectors(
      org.lcappuccio.systemmonitor.collectors.CpuCollector cpuCollector,
      org.lcappuccio.systemmonitor.collectors.GpuCollector gpuCollector,
      org.lcappuccio.systemmonitor.collectors.DiskCollector diskCollector) {
    return new HardwareNames(
        cpuCollector.getModelName(),
        gpuCollector.getModelName(),
        diskCollector.getNvmeModelName(),
        diskCollector.getSataModelName()
    );
  }
}