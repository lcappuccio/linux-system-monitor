## Why

The current flat TableView UI makes it difficult to scan and understand relationships between related metrics. Users must mentally group CPU core frequencies, GPU VRAM metrics, and filesystem details. Additionally, the generic "CPU" and "GPU" section headers don't convey hardware identity—users want to see "AMD 5700X" and "AMD 9070" at a glance.

## What Changes

- Replace `TableView` with a `TreeTableView` for hierarchical, collapsible metric groups
- Add CPU model name (e.g., "AMD Ryzen 7 5700X") as the expandable header node
- Add GPU model name (e.g., "AMD Radeon RX 9070") as the expandable header node
- Group metrics under their hardware nodes: CPU → Temperature, Load, Core 0, Core 1...
- Group GPU metrics under the GPU node: GPU → Temperature, Load, VRAM Used, VRAM Total...
- Filesystems and Network remain as top-level collapsible nodes
- Preserve all existing metric updates and chart functionality
- Maintain dark/light theme compatibility

## Capabilities

### New Capabilities
- `hierarchical-metrics-display`: Tree-based expandable display of system metrics with hardware identification

## Impact

- **UI**: Replace `MainWindow.buildTable()` with `TreeTableView`; modify `MetricRow` to support tree structure
- **Collectors**: Extend `CpuCollector` and `GpuCollector` to expose model names
- **PollerService**: Update metric key generation to include hardware context
- No changes to chart panel or data collection intervals