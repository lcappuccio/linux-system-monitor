package org.lcappuccio.systemmonitor.collectors;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.lcappuccio.systemmonitor.model.DiskMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskCollector implements Collector<DiskMetrics> {

    private static final Logger LOG = LoggerFactory.getLogger(DiskCollector.class);
    private static final String HWMON_PATH = "/sys/class/hwmon";
    private static final String BLOCK_PATH = "/sys/block";
    private static final double NO_TEMP = Double.NaN;

    private final Map<String, String> diskTempPaths = new LinkedHashMap<>();
    private CollectorStatus status = CollectorStatus.UNAVAILABLE;

    public DiskCollector() {
    }

    @Override
    public void initialize() {
        discoverDrives();

        if (diskTempPaths.isEmpty()) {
            LOG.error("DiskCollector: no storage devices found");
            status = CollectorStatus.UNAVAILABLE;
        } else {
            status = CollectorStatus.OK;
        }

        LOG.info("DiskCollector initialized: status={}, disks={}", status, diskTempPaths.size());
    }

    private void discoverDrives() {
        Path hwmonDir = Paths.get(HWMON_PATH);
        if (!Files.exists(hwmonDir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(hwmonDir, "hwmon*")) {
            for (Path hwmon : stream) {
                Path nameFile = hwmon.resolve("name");
                if (!Files.exists(nameFile)) continue;

                String name = Files.readString(nameFile).trim();
                Path tempFile = hwmon.resolve("temp1_input");
                if (!Files.exists(tempFile)) continue;

                switch (name) {
                    case "nvme" -> discoverNvmeDisk(hwmon, tempFile);
                    case "drivetemp" -> discoverDriveTempDisk(hwmon, tempFile);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to discover disk hwmon devices: {}", e.getMessage());
        }
    }

    private void discoverNvmeDisk(Path hwmon, Path tempFile) {
        String model = discoverNvmeModel();
        if (model == null) model = "nvme0n1";
        diskTempPaths.put(model, tempFile.toString());
        LOG.info("Discovered NVMe disk: {} (temp={})", model, tempFile);
    }

    private void discoverDriveTempDisk(Path hwmon, Path tempFile) {
        String model = discoverDriveTempModel(hwmon);
        if (model == null) model = "sdX";
        diskTempPaths.put(model, tempFile.toString());
        LOG.info("Discovered SATA disk via drivetemp: {} (temp={})", model, tempFile);
    }

    private String discoverDriveTempModel(Path hwmon) {
        try {
            Path deviceLink = hwmon.resolve("device");
            if (!Files.exists(deviceLink)) return null;

            Path scsiDevicePath = deviceLink.toRealPath();

            Path blockDir = Paths.get(BLOCK_PATH);
            if (!Files.exists(blockDir)) return null;

            try (Stream<Path> entries = Files.list(blockDir)) {
                Optional<Path> match = entries
                    .filter(p -> p.getFileName().toString().startsWith("sd"))
                    .filter(p -> {
                        try {
                            Path devLink = p.resolve("device");
                            return Files.exists(devLink) && devLink.toRealPath().equals(scsiDevicePath);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst();

                if (match.isPresent()) {
                    Path modelFile = match.get().resolve("device/model");
                    if (Files.exists(modelFile)) {
                        return Files.readString(modelFile).trim();
                    }
                    return match.get().getFileName().toString();
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to discover drivetemp model: {}", e.getMessage());
        }
        return null;
    }

    private String discoverNvmeModel() {
        return discoverNvmeModel(Paths.get(BLOCK_PATH));
    }

    static String discoverNvmeModel(Path blockRoot) {
        try (Stream<Path> entries = Files.list(blockRoot)) {
            Optional<Path> nvmeBlock = entries
                .filter(p -> p.getFileName().toString().startsWith("nvme"))
                .findFirst();
            if (nvmeBlock.isPresent()) {
                Path modelFile = nvmeBlock.get().resolve("device/model");
                if (Files.exists(modelFile)) {
                    return Files.readString(modelFile).trim();
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to read NVMe model name: {}", e.getMessage());
        }
        return null;
    }

    public List<String> getDiskLabels() {
        return List.copyOf(diskTempPaths.keySet());
    }

    @Override
    public Optional<DiskMetrics> collect() {
        if (status == CollectorStatus.UNAVAILABLE || diskTempPaths.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Double> temps = new LinkedHashMap<>(diskTempPaths.size());
        for (var entry : diskTempPaths.entrySet()) {
            temps.put(entry.getKey(), readTemp(entry.getValue()));
        }
        return Optional.of(new DiskMetrics(temps));
    }

    private double readTemp(String tempPath) {
        if (tempPath == null) return NO_TEMP;
        try {
            String content = Files.readString(Paths.get(tempPath)).trim();
            return Double.parseDouble(content) / 1000.0;
        } catch (IOException | NumberFormatException e) {
            LOG.error("Failed to read disk temp from {}: {}", tempPath, e.getMessage());
            return NO_TEMP;
        }
    }

    @Override
    public CollectorStatus getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return "Disks";
    }
}
