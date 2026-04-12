package org.lcappuccio.systemmonitor.collectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Optional;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.model.NetworkMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects network interface metrics including IP address, link speed, and transfer rates.
 */
public class NetworkCollector implements Collector<NetworkMetrics> {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkCollector.class);
  private static final String PROC_NET_DEV = "/proc/net/dev";
  private static final String SYS_NET_PATH = "/sys/class/net/";

  private final String interfaceName;

  private CollectorStatus status = CollectorStatus.UNAVAILABLE;

  private long previousRxBytes = 0;
  private long previousTxBytes = 0;
  private long previousTimestamp = 0;
  private boolean hasPrevious = false;

  public NetworkCollector(AppConfig config) {
    this.interfaceName = config.getNetInterface();
  }

  @Override
  public void initialize() {
    if (!interfaceExists()) {
      LOG.error("Network interface {} not found", interfaceName);
      status = CollectorStatus.UNAVAILABLE;
      return;
    }

    status = CollectorStatus.OK;
    LOG.info("NetworkCollector initialized for interface {}", interfaceName);
  }

  private boolean interfaceExists() {
    try {
      NetworkInterface ni = NetworkInterface.getByName(interfaceName);
      return ni != null;
    } catch (IOException e) {
      LOG.error("Failed to check interface {}: {}", interfaceName, e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<NetworkMetrics> collect() {
    if (status == CollectorStatus.UNAVAILABLE) {
      return Optional.empty();
    }

    try {
      long[] rates = getTransferRates();
      long currentTime = System.currentTimeMillis();

      long uploadRate = 0;
      long downloadRate = 0;

      if (hasPrevious) {
        long timeDeltaMs = currentTime - previousTimestamp;
        if (timeDeltaMs > 0) {
          long rxDelta = rates[0] - previousRxBytes;
          long txDelta = rates[1] - previousTxBytes;
          downloadRate = (rxDelta * 1000) / timeDeltaMs;
          uploadRate = (txDelta * 1000) / timeDeltaMs;
        }
      }

      previousRxBytes = rates[0];
      previousTxBytes = rates[1];
      previousTimestamp = currentTime;
      hasPrevious = true;

      String ipAddress = getIpAddress();
      int linkSpeed = getLinkSpeed();

      return Optional.of(new NetworkMetrics(
          ipAddress,
          linkSpeed,
          uploadRate,
          downloadRate));
    } catch (IOException e) {
      LOG.error("Failed to collect network metrics: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private String getIpAddress() {
    try {
      NetworkInterface ni = NetworkInterface.getByName(interfaceName);
      if (ni == null) {
        return "N/A";
      }

      Enumeration<InetAddress> addresses = ni.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress addr = addresses.nextElement();
        if (addr instanceof java.net.Inet4Address) {
          return addr.getHostAddress();
        }
      }
      return "N/A";
    } catch (IOException e) {
      LOG.warn("Failed to get IP address: {}", e.getMessage());
      return "N/A";
    }
  }

  private int getLinkSpeed() {
    var path = Paths.get(SYS_NET_PATH, interfaceName, "speed");
    try {
      String content = Files.readString(path).trim();
      return Integer.parseInt(content);
    } catch (IOException e) {
      LOG.debug("Failed to read link speed for {}: {}", interfaceName, e.getMessage());
      return 0;
    }
  }

  private long[] getTransferRates() throws IOException {
    long rxBytes = 0;
    long txBytes = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(PROC_NET_DEV))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(interfaceName + ":")) {
          String[] parts = line.trim().split("\\s+");
          if (parts.length >= 11) {
            rxBytes = Long.parseLong(parts[1]);
            txBytes = Long.parseLong(parts[9]);
          }
          break;
        }
      }
    }

    return new long[]{rxBytes, txBytes};
  }

  @Override
  public CollectorStatus getStatus() {
    return status;
  }

  @Override
  public String getName() {
    return "Network";
  }
}