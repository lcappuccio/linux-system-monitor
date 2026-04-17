## 1. Add Model Name Discovery to Collectors

- [x] 1.1 Add getModelName() method to CpuCollector that reads from /proc/cpuinfo (model name line)
- [x] 1.2 Add getModelName() method to GpuCollector that reads from DRM sysfs (device/name) or hwmon
- [x] 1.3 Add fallback to "CPU" / "GPU" if model name cannot be discovered

## 2. Extend MetricRow for Tree Structure

- [x] 2.1 Add hierarchical fields to MetricRow: parentSection, isHardwareNode flag
- [x] 2.2 Add constructor variant that creates hardware node (parent) entries
- [x] 3.3 Ensure existing metric update logic still works with new fields

## 3. Replace TableView with TreeTableView in MainWindow

- [x] 3.1 Import TreeTableView, TreeTableColumn, TreeItem classes
- [x] 3.2 Replace buildTable() with buildTreeTable() method
- [x] 3.3 Create TreeItem hierarchy: hardware nodes as parents, metrics as children
- [x] 3.4 Configure column widths: 60% metric name, 30% value, 10% section (for collapsed view)

## 4. Update populateRows for Hierarchical Structure

- [x] 4.1 Modify populateRows to create hardware parent TreeItems first
- [x] 4.2 Add child MetricRows under each hardware node
- [x] 4.3 Keep Filesystems and Network as top-level collapsible nodes (no model name)

## 5. Verify Chart Integration

- [x] 5.1 Ensure TreeTableView selection fires same events as previous TableView
- [x] 5.2 Verify ChartPanel receives correct metric updates
- [x] 5.3 Test expand/collapse doesn't interfere with chart data flow

## 6. Test and Verify

- [x] 6.1 Run mvn clean package to verify compilation
- [x] 6.2 Run mvn test to verify all tests pass
- [x] 6.3 Verify dark/light theme works with TreeTableView
- [x] 6.4 Manual test: verify CPU shows "AMD Ryzen 7 5700X" (or fallback "CPU")
- [x] 6.5 Manual test: verify GPU shows model name or fallback "GPU"
- [x] 6.6 Manual test: verify collapse/expand works for all nodes