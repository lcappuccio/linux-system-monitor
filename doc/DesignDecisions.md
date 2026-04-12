# Design Decisions

## MetricRow

- `section` and `metric` are plain `String` fields, not properties — they never change after
  construction, no need for observable wrappers.
- Only `value` is a `StringProperty` — it is the only field updated by `PollerService`.
- Initial value is `"—"` by convention — signals "not yet collected" without showing stale zeros.
- No JavaFX types leak into the constructor signature — section and metric are plain strings.

## MainWindow

- `MainWindow` owns the `ObservableList<MetricRow>` and passes it to both `ChartPanel` and
  `PollerService`.
- Construction order is strict and intentional:
  1. `populateRows()` — rows must exist before any subscriber attaches
  2. `new ChartPanel(rows)` — subscribes listeners to already-populated rows
  3. `buildTable()` — wires selection listener to `chartPanel.toggle()`
  4. `pollerService.start()` — data starts flowing last
- Filesystem rows are generated dynamically from `config.getFsMountpoints()` — no hardcoding.
- CPU core rows are generated dynamically by running `CpuCollector.initialize()` at startup
  to discover physical core count.
- `shutdown()` delegates to both `pollerService.shutdown()` and `chartPanel.shutdown()`.
- `SimpleStringProperty` inline in `sectionCol` and `metricCol` cell factories is intentional —
  those values never change so no need to store them in the row model.

## PollerService

**Package:** `org.lcappuccio.systemmonitor.poller`
**Concern:** Scheduling and orchestration. Owns the `ScheduledExecutorService`, holds a list
of `Collector<?>` instances, calls each on its configured interval, and pushes formatted string
results to `MetricRow` via `Platform.runLater()`.

### Design

- Constructor takes `AppConfig` and the `ObservableList<MetricRow>` from `MainWindow`.
- Three separate schedules: default (2s), filesystem (60s), disk temp (15s).
- Each collector is called in a try/catch — exceptions are logged, never propagated.
- `shutdown()` calls `executor.shutdownNow()` with a 5-second graceful timeout.
- No knowledge of `ChartPanel` — pushes only to `MetricRow`.

### Display Format

Byte values formatted using `formatBytesWhole()` for integer display:
- `< 1024` → `"X B"`
- `< 1024²` → `"X KB"`
- `< 1024³` → `"X MB"`
- `>= 1024³` → `"X GB"` (rounded using `Math.round()`)

`FileSystemCollector` displays `"used / free / total"` (e.g. `"32 / 55 / 91 GB"`).
Memory shows individual values. Network rates use `formatBytesPerSec()` with one decimal place.

## MetricValueParser

**Package:** `org.lcappuccio.systemmonitor.ui`
**Concern:** Pure stateless utility. Converts a `MetricRow` display string to an
`OptionalDouble` for charting. No instances, no state, no side effects.

### Design

- Single public method: `parse(String displayValue) → OptionalDouble`.
- Extracts the first numeric token (integer or decimal, with optional leading minus).
- Returns `OptionalDouble.empty()` for sentinels (`"—"`, `"N/A"`), null, empty string,
  and strings with no leading numeric token.
- For composite strings (e.g. `"32 / 55 / 91 GB"`), returns the first token (`32.0`) —
  representing the "used" value, which is the most meaningful for charting.
- Has no knowledge of units, sections, or metric semantics.

## ChartPanel

**Package:** `org.lcappuccio.systemmonitor.ui`
**Concern:** Maintains per-metric rolling history and displays a live `LineChart` for the
selected metric.

### Design

- Has **no knowledge of `PollerService`** or any collector. Its only input is
  `ObservableList<MetricRow>`.
- Maintains two maps keyed by `section.metric`:
  - `Map<String, ArrayDeque<Double>> history` — rolling window of 150 samples (5 minutes at 2s tick)
  - `Map<String, Double> lastKnownValue` — last successfully parsed value per metric
- A `ChangeListener` on each `MetricRow.valueProperty()` calls `MetricValueParser.parse()`
  and updates `lastKnownValue` when a parseable value arrives.
- An internal `Timeline` fires every 2 seconds (`TICK_SECONDS`) independently of `PollerService`.
  On each tick, `lastKnownValue` is appended to every metric's deque — including metrics whose
  underlying value has not changed. This ensures:
  - All deques grow at a uniform rate
  - Stable metrics (e.g. disk temps, filesystems) render as flat lines rather than sparse points
- On row selection, the chart is populated from the existing deque (full history immediately
  visible). On deselection, the series is removed but the deque keeps accumulating.
- Clicking a selected row a second time deselects it and clears the chart.
- `shutdown()` stops the `Timeline` — must be called from `MainWindow.shutdown()`.
- X axis shows elapsed seconds (`tickCount * TICK_SECONDS`). Y axis auto-ranges.
- `setAnimated(false)` and `setCreateSymbols(false)` for performance.

## MemoryCollector

**Package:** `org.lcappuccio.systemmonitor.collectors`
**Concern:** Reads `/proc/meminfo` and returns a `MemoryMetrics` record.

### Design

- No hwmon discovery needed.
- Parses `MemTotal`, `MemAvailable`, `SwapTotal`, `SwapFree` lines.
- `MemUsed = MemTotal - MemAvailable`, `SwapUsed = SwapTotal - SwapFree`.
- All values in kB in `/proc/meminfo`, converted to bytes on read.
- `initialize()` checks `/proc/meminfo` is readable, sets status `OK` or `UNAVAILABLE`.

## FileSystemCollector

**Package:** `org.lcappuccio.systemmonitor.collectors`
**Concern:** Reports used/free/total bytes per configured mount point.

### Design

- Uses `df -B1 <mount>` via `ProcessBuilder` to get accurate filesystem sizes in bytes.
- `java.nio.file.FileStore` was considered but rejected — it excludes reserved blocks,
  causing a 12–20% discrepancy vs. actual disk usage reported by the OS.
- `initialize()` validates each mount point by running `df` against it.
- Mount points that fail at init time are skipped with `ERROR` log, status set to `DEGRADED`.
- Returns `FileSystemMetrics` with a `Map<String, FileSystemUsage>`.

## NetworkCollector

**Package:** `org.lcappuccio.systemmonitor.collectors`
**Concern:** Reports IP address, link speed, and current upload/download rates.

### Design

- IP address: `java.net.NetworkInterface.getByName(iface).getInetAddresses()`, IPv4 only.
- Link speed: `/sys/class/net/<iface>/speed` as plain integer (Mbps).
- Upload/download rates: delta on `/proc/net/dev` between consecutive calls.
  Stores previous `rxBytes`/`txBytes` and timestamp as instance fields.
- First call returns `0` for rates — no previous sample yet.
- `initialize()` checks interface exists via `NetworkInterface.getNetworkInterfaces()`.

## CpuCollector

**Package:** `org.lcappuccio.systemmonitor.collectors`
**Concern:** Reports CPU temperature, overall load percentage, and per-core frequencies.

### Design

- **Temperature:** hwmon discovery at startup — scans `/sys/class/hwmon/hwmon*/name` for
  `k10temp`, finds `temp*_label` containing `Tctl`, reads corresponding `temp*_input`
  (millidegrees → °C).
- **Load:** delta on `/proc/stat` first line — `(total - idle) / total * 100` between reads.
  Stores previous jiffies as instance fields. Returns `0.0` on first call.
- **Core frequencies:** reads `/sys/devices/system/cpu/cpu<N>/cpufreq/scaling_cur_freq` for
  even-numbered CPUs only (logical → physical core mapping for AMD). Value in kHz → GHz.
- `initialize()` performs hwmon discovery. If `k10temp` not found, sets `DEGRADED` —
  load and frequency collection still work.
- `getCoreIds()` is public — used by `MainWindow` to generate the correct number of core rows
  at startup without duplicating discovery logic.

## GpuCollector

**Package:** `org.lcappuccio.systemmonitor.collectors`
**Concern:** Reports all AMD GPU metrics from sysfs and hwmon.

### Design

- **hwmon discovery:** scans for chip name `amdgpu`, then:
  - Junction temp: `temp*_label = junction` → `temp*_input`
  - VRAM temp: `temp*_label = mem` → `temp*_input`
  - Power (PPT): `power1_label = PPT` → `power1_average` (microwatts → watts)
  - Fan: `fan1_input` (RPM)
- **Load:** `<gpuDrmPath>/device/gpu_busy_percent`
- **VRAM load:** `<gpuDrmPath>/device/mem_busy_percent`
- **VRAM used/total:** `<gpuDrmPath>/device/mem_info_vram_used` and `mem_info_vram_total` (bytes)
- `initialize()` validates DRM path and hwmon `amdgpu` discoverability independently.
  If only one is available, status is `DEGRADED`.

## DiskCollector

**Package:** `org.lcappuccio.systemmonitor.collectors`
**Concern:** Reports NVMe and SATA SSD temperatures.

### Design

- **NVMe temp:** hwmon discovery — scans for chip name `nvme`, reads `temp1_input`
  (Composite, millidegrees → °C). No `sudo nvme` needed.
- **SATA temp:** `ProcessBuilder` invoking `sudo smartctl -A <diskSataDevice>`.
  Parses output line containing `Airflow_Temperature_Cel`, extracts the 10th field.
  Exit code must be `0` before parsing — non-zero means device unavailable.
  Process is always destroyed in a `finally` block.
- `initialize()` discovers NVMe hwmon path and checks SATA device path exists.
- If only one of the two is available, status is `DEGRADED` rather than `UNAVAILABLE`.