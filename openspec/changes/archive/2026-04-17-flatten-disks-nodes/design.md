## Context

The hierarchical metrics UI was implemented in a recent change, replacing the flat TableView with a TreeTableView. The current tree structure is:

```
System
├── CPU (AMD Ryzen 7 5700X)
│   ├── Temperature
│   ├── Load
│   └── Core 0...
├── GPU (AMD Radeon RX 9070)
│   ├── Temperature
│   ├── Load
│   └── VRAM Used...
├── Memory
│   ├── Used
│   └── Swap Used
├── Disks                    ← Unnecessary intermediate node
│   ├── Samsung SSD 970...
│   │   └── Temperature
│   └── WDC WD10...
│       └── Temperature
├── Filesystems
│   └── / (root)...
└── Network
    └── IP Address...
```

The "Disks" category node adds no value—users identify their drives by the model name (e.g., "Samsung SSD 970 EVO Plus"), not by a generic category.

## Goals / Non-Goals

**Goals:**
- Remove the "Disks" parent node from the tree hierarchy
- Promote disk device nodes to be direct children of root
- Preserve device model names and temperature metrics

**Non-Goals:**
- No changes to data collection (PollerService unchanged)
- No changes to chart functionality
- No new metrics or collectors

## Decisions

1. **Flatten disk nodes to root level** — Direct children of rootItem, same as CPU, GPU, Memory, etc.

   **Alternative considered:** Keep disks under a collapsible "Storage" category but rename it. Rejected because the category itself adds no value.

2. **Preserve temperature as child of each device** — Each disk device node will have its temperature metric as a child, maintaining the same data display.

3. **Use existing getNvmeModelName() and getSataModelName()** — No new discovery needed, reuse existing collector methods.

## Risks / Trade-offs

- **Low risk change** — Only affects UI display, no backend changes
- **Minor visual change** — Users with muscle memory may need to re-find the disk nodes (now at top level instead of one click deeper)

## Migration Plan

This is a pure UI change, no migration needed. Simply modify MainWindow.populateRows() to add disk TreeItems directly to rootItem.