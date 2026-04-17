## MODIFIED Requirements

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

#### Scenario: Disk devices appear at root level
- **WHEN** the application starts
- **THEN** disk device nodes (NVMe, SATA) appear as direct children of root
- **AND** no intermediate "Disks" category node exists
- **AND** disk device nodes display model names (e.g., "Samsung SSD 970 EVO Plus 500GB")

#### Scenario: Disk temperature is child of device node
- **WHEN** a disk device node is expanded
- **THEN** the temperature metric appears as a child of that device node
- **AND** temperature displays as "Temperature" under the device model name