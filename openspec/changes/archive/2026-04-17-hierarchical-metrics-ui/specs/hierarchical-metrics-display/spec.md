## ADDED Requirements

### Requirement: Hierarchical metrics display
The system SHALL display system metrics in a tree structure with expandable hardware nodes, allowing users to collapse and expand groups of related metrics.

#### Scenario: Initial display shows all nodes expanded
- **WHEN** the application starts
- **THEN** all hardware nodes are expanded by default
- **AND** all metric rows are visible under their parent nodes

#### Scenario: User can collapse a hardware node
- **WHEN** user clicks the disclosure triangle next to a hardware node
- **THEN** all child metrics under that node are hidden
- **AND** the node displays an expandable indicator

#### Scenario: User can expand a collapsed hardware node
- **WHEN** user clicks the disclosure triangle next to a collapsed hardware node
- **AND** the node has hidden children
- **THEN** all child metrics become visible
- **AND** the node displays a collapsible indicator

### Requirement: CPU model name display
The system SHALL display the CPU model name (e.g., "AMD Ryzen 7 5700X") as the parent node for CPU metrics, replacing the generic "CPU" label.

#### Scenario: CPU model is successfully discovered
- **WHEN** the CPU collector successfully reads the model name from /proc/cpuinfo
- **AND** initialize() is called
- **THEN** the CPU parent node displays the full model name
- **AND** all CPU metrics (Temperature, Load, Core N) appear as children

#### Scenario: CPU model cannot be discovered
- **WHEN** /proc/cpuinfo is unavailable or parsing fails
- **AND** initialize() is called
- **THEN** the CPU parent node displays "CPU" as fallback
- **AND** all CPU metrics still appear as children

### Requirement: GPU model name display
The system SHALL display the GPU model name (e.g., "AMD Radeon RX 9070") as the parent node for GPU metrics, replacing the generic "GPU" label.

#### Scenario: GPU model is successfully discovered
- **WHEN** the GPU collector successfully reads the model from DRM sysfs or hwmon
- **AND** initialize() is called
- **THEN** the GPU parent node displays the full model name
- **AND** all GPU metrics (Temperature, Load, VRAM Used, Power, Fan) appear as children

#### Scenario: GPU model cannot be discovered
- **WHEN** DRM sysfs and hwmon are unavailable
- **AND** initialize() is called
- **THEN** the GPU parent node displays "GPU" as fallback
- **AND** all GPU metrics still appear as children

### Requirement: Metric updates work with tree structure
The system SHALL continue to update metric values in real-time while using the tree structure.

#### Scenario: Poller updates a metric value
- **WHEN** the PollerService collects new metric data
- **AND** Platform.runLater() is called with the update
- **THEN** the value cell in the TreeTableView updates immediately
- **AND** the parent node selection state is unchanged

#### Scenario: User selects a metric row
- **WHEN** user clicks on any metric row (child node)
- **THEN** the ChartPanel updates to display that metric's time-series data
- **AND** the behavior matches the previous TableView selection

### Requirement: Layout proportions maintained
The system SHALL maintain similar column width proportions as the previous TableView.

#### Scenario: Window is resized
- **WHEN** the user resizes the window horizontally
- **THEN** the three columns resize proportionally (60% metric, 30% value, 10% section)
- **AND** the tree structure remains functional at all window sizes

### Requirement: Theme compatibility
The system SHALL support both dark and light themes with the hierarchical display.

#### Scenario: Dark theme is enabled
- **WHEN** config.getUiTheme() returns "dark"
- **THEN** the TreeTableView uses dark theme CSS
- **AND** all node colors, text colors, and backgrounds match the existing dark theme
- **AND** disclosure triangles are visible and functional

#### Scenario: Light theme is enabled
- **WHEN** config.getUiTheme() returns anything other than "dark"
- **THEN** the TreeTableView uses light theme (default JavaFX)
- **AND** all node colors, text colors, and backgrounds match the existing light theme