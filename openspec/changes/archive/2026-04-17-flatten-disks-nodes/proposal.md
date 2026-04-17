## Why

The hierarchical metrics UI currently has an unnecessary "Disks" category node between the root and individual disk devices. This extra level adds visual clutter without providing meaningful organization—users want to see their specific drives (e.g., "Samsung SSD 970 EVO Plus 500GB") directly at the top level, not nested under a generic "Disks" label.

## What Changes

- Remove the "Disks" parent node from the TreeTableView hierarchy
- Promote NVMe and SATA disk nodes to be direct children of the root (same level as CPU, GPU, Memory, Filesystems, Network)
- Preserve the device model names as the node labels (e.g., "Samsung SSD 970 EVO Plus 500GB")
- Keep disk temperature metrics as children under each device node

## Capabilities

### New Capabilities
(none - this is a UI restructuring, no new functionality)

### Modified Capabilities
- `hierarchical-metrics-display`: Update tree structure to flatten disk devices

## Impact

- **UI**: Modify `MainWindow.populateRows()` to add disk TreeItems directly to root instead of under "Disks" parent
- **MetricRow**: No changes needed
- **PollerService**: No changes needed (keys unchanged)