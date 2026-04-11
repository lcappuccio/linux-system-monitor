# linux-system-monitor

A JavaFX desktop application for monitoring Linux hardware metrics in real time.
Displays CPU, GPU, memory, storage, filesystem, and network statistics in a live-updating UI.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=lcappuccio_linux-system-monitor&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lcappuccio_linux-system-monitor)

## Requirements

- JDK 21+
- Maven 3.9+
- AMD GPU with `amdgpu` kernel driver
- `lm-sensors` installed and configured
- `smartctl` and `nvme-cli` installed, with the following sudoers rules:

```
leo ALL=(ALL) NOPASSWD: /usr/sbin/nvme
leo ALL=(ALL) NOPASSWD: /usr/sbin/smartctl
```

## Build

```bash
mvn clean package
```

## Run

```bash
# Development
mvn javafx:run

# From fat jar
java -jar target/linux-system-monitor-0.1.0-SNAPSHOT.jar
```

## Monitored Metrics

| Section | Metrics |
|---|---|
| CPU | Temperature, load, per-core frequency |
| Memory | Used/total RAM, used/total swap |
| GPU | Temperature, load, VRAM used/total, VRAM temp, VRAM load, power (PPT) |
| Disks | NVMe temperature, SATA SSD temperature |
| Filesystems | Used/free/total for `/`, `/home`, `/data` |
| Network | LAN IP, link speed, upload/download rate |

## Configuration

On first run, create `~/.config/linux-system-monitor/config.properties`:

```properties
net.interface=enp9s0
gpu.drm.path=/sys/class/drm/card1
disk.sata.device=/dev/sda
fs.mountpoints=/,/home,/data
poll.interval.default=2
poll.interval.filesystem=60
poll.interval.disk.temp=15
```

If the file is absent, the application starts with built-in defaults and logs a warning.
If a configured device or path does not exist, the affected collector is skipped and an error
is logged — the rest of the application continues normally.

## Fault Tolerance

*TODO*

## License

This project is licensed under the GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.