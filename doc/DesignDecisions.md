# Design Decisions

## MetricRow

* section and metric are plain String fields, not properties — they never change after construction, no need for observable wrappers.

* Only value is a StringProperty — it's the only field the poller will update.

* Initial value is "—" by convention (caller's choice) — signals "not yet collected" without showing stale zeros.

* No JavaFX types leak into the constructor signature — section and metric are plain strings.

## MainWindow

* MainWindow owns the ObservableList<MetricRow> — the poller will receive a reference to it later to push updates.

* Filesystem rows are generated dynamically from config.getFsMountpoints() — no hardcoding.

* shutdown() is a stub now, will delegate to PollerService.shutdown() once wired.

* buildChartPlaceholder() will be replaced by ChartPanel in the last step.

* The SimpleStringProperty inline in sectionCol and metricCol cell factories is intentional — those values never change so no need to store them in the row model.

## PollerService

Package: org.lcappuccio.systemmonitor.poller
Concern: Scheduling and orchestration. Owns the ScheduledExecutorService, holds a list of Collector<?> instances, calls each on its configured interval, and pushes results to the UI via Platform.runLater().

### Design:

Constructor takes AppConfig and the ObservableList<MetricRow> from MainWindow
Three separate schedules: default (2s), filesystem (60s), disk temp (15s)
Each collector is called in a try/catch — exceptions are logged, never propagated
shutdown() calls executor.shutdownNow()
No knowledge of JavaFX beyond Platform.runLater()

### Display Format:

Byte values are formatted using formatBytesWhole() for integer display:
- < 1024: "X B"
- < 1024*1024: "X KB"  
- < 1024*1024*1024: "X MB"
- >= 1024*1024*1024: "X GB" (rounded using Math.round())

FileSystemCollectors shows "used / free / total", Memory shows individual values

## MemoryCollector

Package: org.lcappuccio.systemmonitor.collectors
Concern: Reads /proc/meminfo and returns a MemoryMetrics record.

### Design:

No hwmon discovery needed
Parses MemTotal, MemAvailable, SwapTotal, SwapFree lines
MemUsed = MemTotal - MemAvailable
SwapUsed = SwapTotal - SwapFree
All values in kB in /proc/meminfo, convert to bytes on read
initialize() checks /proc/meminfo is readable, sets status OK or UNAVAILABLE

## FileSystemCollector

Package: org.lcappuccio.systemmonitor.collectors
Concern: Reports used/free/total bytes per configured mount point using df command.

### Design:

Uses `df --block-size=1 <mount>` via ProcessBuilder to get accurate filesystem sizes.
Important: df outputs in **1-byte blocks** (not 1K blocks) - values are direct bytes, no multiplication needed.
java.nio.file.FileStore was initially used but had 12-20% discrepancy due to reserved blocks.

initialize() validates each mount point in config.getFsMountpoints() by running df
Mount points that don't exist at init time are skipped with ERROR log, status set to DEGRADED
Returns FileSystemMetrics with a Map<String, FileSystemUsage>

PollerService display format: "used / free / total" (e.g., "32 / 55 / 91 GB")
Values are rounded to whole integers using Math.round()

## NetworkCollector

Package: org.lcappuccio.systemmonitor.collectors
Concern: Reports IP address, link speed, and current upload/download rates for the configured interface.

### Design:

IP address: java.net.NetworkInterface.getByName(iface).getInetAddresses()
Link speed: read /sys/class/net/<iface>/speed as plain integer (Mbps)
Upload/download rates: delta calculation on /proc/net/dev between two consecutive calls — stores previous rxBytes/txBytes and previous timestamp as instance fields
First call after init returns 0 for rates (no previous sample yet)
initialize() checks interface exists in NetworkInterface.getNetworkInterfaces()

## CpuCollector

Package: org.lcappuccio.systemmonitor.collectors
Concern: Reports CPU temperature, overall load percentage, and per-core frequencies.

### Design:

Temperature: hwmon discovery at startup — scan /sys/class/hwmon/hwmon*/name for k10temp, then find temp*_label containing Tctl, read corresponding temp*_input (value in millidegrees, divide by 1000)
Load: delta on /proc/stat first line — (total - idle) / total * 100 between two reads. Stores previous jiffies as instance fields
Core frequencies: read /sys/devices/system/cpu/cpu<N>/cpufreq/scaling_cur_freq for even-numbered CPUs only (logical → physical core mapping for AMD). Value in kHz, convert to GHz
initialize() performs hwmon discovery, logs ERROR and sets DEGRADED if k10temp not found (frequencies and load still work)

## GpuCollector

Package: org.lcappuccio.systemmonitor.collectors
Concern: Reports all AMD GPU metrics from sysfs and hwmon.

### Design:

hwmon discovery: scan for chip name amdgpu, then:

Junction temp: temp*_label = junction → temp*_input
VRAM temp: temp*_label = mem → temp*_input
Power (PPT): power*_label = PPT → power*_input (microwatts, divide by 1,000,000)


Load: <gpuDrmPath>/device/gpu_busy_percent (plain integer)
VRAM load: <gpuDrmPath>/device/mem_busy_percent
VRAM used/total: <gpuDrmPath>/device/mem_info_vram_used and mem_info_vram_total (bytes)
initialize() validates DRM path exists and hwmon amdgpu is discoverable

## DiskCollector

Package: org.lcappuccio.systemmonitor.collectors
Concern: Reports NVMe and SATA SSD temperatures.

### Design:

NVMe temp: hwmon discovery — scan for chip name nvme, read temp1_input (Composite, millidegrees → °C). No sudo nvme needed
SATA temp: ProcessBuilder invoking sudo smartctl -A <diskSataDevice>, parse output line containing Temperature_Celsius, extract the 10th field
Exit code from smartctl must be checked before parsing — non-zero means device unavailable
initialize() discovers nvme hwmon path, checks SATA device path exists
If only one of the two is available, status is DEGRADED rather than UNAVAILABLE

## ChartPanel

Package: org.lcappuccio.systemmonitor.ui
Concern: Displays a live LineChart for each selected metric.

### Design:

MainWindow calls chartPanel.addSeries(metricRow) when a row is selected in the table
Each series is keyed by section + "." + metric
Rolling window of 60 samples per series stored in ArrayDeque<XYChart.Data>
When the deque is full, remove the oldest before adding the newest
update(String key, double value) called by PollerService via Platform.runLater()
Clicking a selected row a second time removes its series from the chart
Y-axis auto-ranges; X-axis shows relative time (sample index, not wall clock) for simplicity in v1