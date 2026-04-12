# Multi-Chart Implementation Plan

## Overview

Replace the current single-metric `ChartPanel` with a fixed set of always-visible chart groups,
each displaying related metrics as multiple colored series. The left table remains purely
informational — row selection no longer controls charts.

---

## Chart Groups

| Chart | Series | Y Axis | Unit |
|---|---|---|---|
| Temperature | CPU, GPU junction, VRAM, NVMe, SSD | shared | °C |
| Load | CPU, GPU, VRAM | shared | % (0–100) |
| Memory | RAM used, Swap used | shared | GB |
| Frequencies | Core 0–7 | shared | GHz |

Network upload/download are excluded — too volatile and unit-dependent (already handled
via `network.speed.unit` config for the table). Filesystem and disk temps are excluded —
too slow-moving to be useful in a live chart.

---

## Color Scheme

Colors are tied to hardware component, not metric. Consistent across all charts.

| Component | Color | JavaFX CSS |
|---|---|---|
| CPU | Blue | `#2196F3` |
| GPU junction | Red | `#F44336` |
| VRAM | Orange | `#FF9800` |
| NVMe | Gray | `#9E9E9E` |
| SSD | Dark gray | `#607D8B` |
| RAM used | Blue | `#2196F3` |
| Swap used | Purple | `#9C27B0` |
| Core 0–7 | Blue shades | `#1565C0` → `#90CAF9` |

---

## Architecture Changes

### `ChartGroup` record (new)

```
org.lcappuccio.systemmonitor.ui.ChartGroup
```

Immutable descriptor for a chart group:

```java
public record ChartGroup(
    String title,
    List<String> metricKeys,   // e.g. ["CPU.Temperature", "GPU.Temperature"]
    List<String> seriesColors, // hex colors, same order as metricKeys
    List<String> seriesLabels  // display names, same order as metricKeys
) {}
```

### `ChartPanel` — rewrite

**Remove:**
- `toggle()` method
- `selectedKey` / `activeSeries` fields
- Single `LineChart` — replaced by multiple

**Add:**
- `List<ChartGroup> groups` — defined at construction, immutable
- One `LineChart<Number, Number>` per group — stored in a `List<LineChart>`
- One `Map<String, XYChart.Series<Number, Number>>` per chart — keyed by metric key
- `VBox` as root instead of `StackPane` — charts stacked vertically

**Retain:**
- `Map<String, ArrayDeque<Double>> history` — unchanged
- `Map<String, Double> lastKnownValue` — unchanged
- `ChangeListener` subscription logic — unchanged
- `Timeline` tick logic — unchanged, now updates all active series across all charts

### `MainWindow` — minor changes

- Remove table selection listener that called `chartPanel.toggle()`
- Table becomes purely informational — no interaction with charts
- `ChartPanel` constructor receives `ObservableList<MetricRow>` as before — no new dependencies

---

## ChartPanel Construction

```java
// Define groups at construction time
List<ChartGroup> groups = List.of(
    new ChartGroup(
        "Temperature (°C)",
        List.of("CPU.Temperature", "GPU.Temperature", "GPU.VRAM Temperature",
                "Disks.NVMe Temperature", "Disks.SSD Temperature"),
        List.of("#2196F3", "#F44336", "#FF9800", "#9E9E9E", "#607D8B"),
        List.of("CPU", "GPU", "VRAM", "NVMe", "SSD")
    ),
    new ChartGroup(
        "Load (%)",
        List.of("CPU.Load", "GPU.Load", "GPU.VRAM Load"),
        List.of("#2196F3", "#F44336", "#FF9800"),
        List.of("CPU", "GPU", "VRAM")
    ),
    new ChartGroup(
        "Memory (GB)",
        List.of("Memory.Used", "Memory.Swap Used"),
        List.of("#2196F3", "#9C27B0"),
        List.of("RAM", "Swap")
    ),
    new ChartGroup(
        "Frequencies (GHz)",
        List.of("CPU.Core 0", "CPU.Core 1", "CPU.Core 2", "CPU.Core 3",
                "CPU.Core 4", "CPU.Core 5", "CPU.Core 6", "CPU.Core 7"),
        List.of("#1565C0", "#1976D2", "#1E88E5", "#2196F3",
                "#42A5F5", "#64B5F6", "#90CAF9", "#BBDEFB"),
        List.of("Core 0", "Core 1", "Core 2", "Core 3",
                "Core 4", "Core 5", "Core 6", "Core 7")
    )
);
```

Note: CPU core count is dynamic. The frequency group should only add series for cores
that exist in `history` — check `history.containsKey(key)` before adding.

---

## `onTick()` changes

Current logic iterates all keys in `history`. With multiple charts, additionally:

```java
// After appending to deque, update the relevant series if it exists
XYChart.Series<Number, Number> series = seriesMap.get(key);
if (series != null) {
    boolean atCapacity = series.getData().size() >= HISTORY_SIZE;
    series.getData().add(new XYChart.Data<>(series.getData().size(), value));
    if (atCapacity) {
        series.getData().remove(0);
        for (int i = 0; i < series.getData().size(); i++) {
            series.getData().get(i).setXValue(i);
        }
    }
}
```

`seriesMap` is a flat `Map<String, XYChart.Series<Number, Number>>` across all charts —
keyed by metric key. Built at construction time from all groups.

---

## Color Application

JavaFX `LineChart` applies colors via CSS. The cleanest approach is to set the stroke
directly on the series node after the chart is added to the scene:

```java
// After chart.getData().add(series):
series.nodeProperty().addListener((obs, oldNode, newNode) -> {
    if (newNode != null) {
        newNode.setStyle("-fx-stroke: " + color + ";");
    }
});
// Also color the legend symbol:
series.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + color + ";");
```

Alternatively, inject a CSS stylesheet into the scene with pre-defined series color classes.
The `nodeProperty` listener approach is simpler and requires no external CSS file.

---

## Layout

```
VBox (right panel)
├── LineChart — Temperature
├── LineChart — Load
├── LineChart — Memory
└── LineChart — Frequencies
```

Each chart gets equal vertical space via `VBox.setVgrow(chart, Priority.ALWAYS)`.
`VBox` replaces the current `StackPane` root in `ChartPanel`.

Set on each chart:
```java
chart.setAnimated(false);
chart.setCreateSymbols(false);
chart.setLegendVisible(true);
chart.setMinHeight(150);
```

---

## Files to Change

| File | Change |
|---|---|
| `ChartPanel.java` | Full rewrite |
| `ChartGroup.java` | New record |
| `MainWindow.java` | Remove toggle listener from table |
| `DesignDecisions.md` | Update ChartPanel section |
| `AGENTS.md` | Add ChartGroup, update ChartPanel description |

## Files Unchanged

`MetricRow`, `MetricValueParser`, `PollerService`, all collectors, `AppConfig`, `Main` —
no changes required.

---

## Known Constraints

- CPU core count is discovered at runtime — frequency chart series count varies per machine.
  Build frequency group dynamically, skip missing core keys.
- `Double.NaN` values (unavailable sensors) must be filtered before adding to series —
  `MetricValueParser` already returns `OptionalDouble.empty()` for `"N/A"`, so the
  `lastKnownValue` map will simply have no entry for unavailable metrics.
  Series for unavailable metrics will have empty deques and render nothing — correct behavior.
- Y axis for Load chart should be fixed `0–100` rather than auto-ranging, to avoid
  misleading scale when load is low. Set `yAxis.setAutoRanging(false)`,
  `yAxis.setLowerBound(0)`, `yAxis.setUpperBound(100)`.
- Y axis for Temperature and Memory should remain auto-ranging.
- Y axis for Frequencies should remain auto-ranging.