## Context

The application currently uses a flat `TableView` with three columns: Section, Metric, Value. Sections include CPU, Memory, GPU, Disks, Filesystems, Network. Each row displays a single metric value. The table lacks visual hierarchy—users cannot collapse related metrics or see hardware identity at a glance.

**Current Architecture:**
- `MainWindow` builds a `TableView<MetricRow>` with 3 columns
- `MetricRow` is a simple POJO with section, metric, value
- Rows are populated at startup via `populateRows(config)`
- PollerService updates values by iterating through the `ObservableList<MetricRow>`

**Constraints:**
- JavaFX 21 with no additional UI libraries
- Must maintain backward compatibility with existing chart integration
- Polling intervals remain unchanged (2s default, 60s filesystem, 15s disk temp)
- Dark/light theme support must continue working

## Goals / Non-Goals

**Goals:**
- Replace flat TableView with TreeTableView for hierarchical display
- Display CPU model name (e.g., "AMD Ryzen 7 5700X") as expandable parent node
- Display GPU model name (e.g., "AMD Radeon RX 9070") as expandable parent node
- Allow users to collapse/expand hardware groups
- Preserve all existing metric updates and chart functionality
- Maintain 60/30/40 column layout proportions

**Non-Goals:**
- Add search/filter functionality
- Change data collection intervals
- Modify chart panel behavior
- Add drag-and-drop reordering
- Implement multi-select

## Decisions

### 1. TreeTableView over custom collapsible solution
**Chosen:** Use JavaFX `TreeTableView` directly  
**Rationale:** Native JavaFX control with built-in expand/collapse, sorting, and cell rendering. No third-party libraries needed. Maintains accessibility features.

**Alternatives considered:**
- Manual `TreeView` with `HBox` rows: More code, less native behavior
- Accordion control: Only allows one section open at a time

### 2. Hardware model discovery via /proc/cpuinfo and lspci
**Chosen:** Read `/proc/cpuinfo` for CPU model; use hwmon or DRM sysfs for GPU model  
**Rationale:** `/proc/cpuinfo` is standard and reliable. GPU model can be derived from DRM device name (card0 → card1) or hwmon discovery.

**Alternatives considered:**
- Hardcode: Rejected per project rules
- lspci: Not always available, would require fallback

### 3. Extend MetricRow to support tree structure
**Chosen:** Add optional `TreeItem<MetricRow>` reference and hierarchy fields to MetricRow  
**Rationale:** Minimizes changes to existing update logic. PollerService can continue iterating rows while TreeTableView binds to TreeItem structure.

**Alternatives considered:**
- Separate model: Would require duplicating data or complex synchronization
- New class: Breaking change to PollerService contract

### 4. Two-level hierarchy: Hardware node → Metrics
**Chosen:** Parent nodes for hardware (CPU model, GPU model), children for individual metrics  
**Rationale:** Matches user mental model. Single level per hardware keeps tree shallow.

**Alternatives considered:**
- Three levels (section → hardware → metric): Too deep for 6 categories
- Flat with icons: Doesn't provide collapse benefit

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| TreeTableView performance with frequent updates | Medium | Use cell value factories, avoid full row refresh |
| Hardware model discovery fails on some systems | Low | Fall back to "CPU" / "GPU" labels |
| Breaking chart integration | High | Verify TreeTableView selection fires same events |
| Thread safety with TreeItem updates | Medium | Ensure all UI updates via Platform.runLater() |

## Open Questions

1. **Should Memory have a hardware node?** Current thinking: No—memory doesn't have a discoverable model name. Keep as "Memory" section header.
2. **How to handle dynamic filesystems?** If a filesystem is added/removed at runtime, should the tree update? Current: No—filesystem list is set at startup from config.
3. **Default expanded state?** Current thinking: All nodes expanded by default for immediate visibility.