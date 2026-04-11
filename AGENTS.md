# AGENTS.md

This file provides guidance for agentic coding tools (Claude Code, GitHub Copilot CLI, opencode, etc.).

## Project Summary

`linux-system-monitor` is a JavaFX desktop application that monitors Linux system hardware metrics
(CPU, GPU, memory, storage, network) and displays them in a live-updating flat-list UI with time-series charts.
It targets Ubuntu with AMD GPU hardware.

## Tech Stack

- **Language**: Java 21
- **UI**: JavaFX 21
- **Build**: Maven
- **Logging**: SLF4J + Logback
- **Testing**: JUnit 5

## Package Structure

```
org.lcappuccio.systemmonitor
├── Main.java                  # JavaFX Application entry point
├── ui/
│   ├── MainWindow.java        # SplitPane: left TableView + right chart panel
│   ├── MetricRow.java         # JavaFX observable model for a single metric
│   └── ChartPanel.java        # Live LineChart for selected metrics
├── collectors/
│   ├── CpuCollector.java      # /proc/stat, /sys/.../cpufreq, hwmon (k10temp)
│   ├── MemCollector.java      # /proc/meminfo
│   ├── GpuCollector.java      # /sys/class/drm/card1/..., hwmon (amdgpu)
│   ├── DiskCollector.java     # hwmon (nvme), sudo smartctl (sata)
│   ├── FsCollector.java       # java.nio.file.FileStore
│   └── NetCollector.java      # /proc/net/dev, /sys/class/net/enp9s0/...
└── poller/
    └── PollerService.java     # ScheduledExecutorService, Platform.runLater()
```

## Coding Rules

- All collectors read from sysfs/procfs or invoke external commands (`sudo smartctl`, `sudo nvme`).
  No JNI or native bindings.
- hwmon paths (`/sys/class/hwmon/hwmon*/`) must be **discovered at startup** by reading the `name` file,
  not hardcoded. Chip names to look for: `k10temp` (CPU), `amdgpu` (GPU), `nvme` (NVMe).
- All file reads must handle `IOException` gracefully — return `null` or a sentinel value, log the error,
  never crash the poller.
- Polling interval is **2 seconds** for all metrics except filesystems (60s) and storage temps (15s).
- UI updates must always go through `Platform.runLater()`.
- Rate metrics (CPU load, net speed) require **delta calculation** between two consecutive reads.
- Do not use `Thread.sleep()` loops — use `ScheduledExecutorService`.
- SSD temp requires `sudo smartctl -A /dev/sda`. NVMe temp is read from hwmon, not nvme-cli.
- GPU is AMD (amdgpu driver). No NVIDIA/NVML code.
- Network interface is `enp9s0`.

## Commands

```bash
# Build
mvn clean package

# Run (development)
mvn javafx:run

# Run tests
mvn test

# Code quality
mvn checkstyle:check sonar:sonar -Psonar
```

## Do Not

- Do not hardcode hwmon paths.
- Do not use `System.out.println` — use SLF4J logger.
- Do not block the JavaFX Application Thread.
- Do not add dependencies without discussing first.
- Do not target Windows or macOS.