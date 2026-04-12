package org.lcappuccio.systemmonitor.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import org.lcappuccio.systemmonitor.collectors.Collector;
import org.lcappuccio.systemmonitor.collectors.CpuCollector;
import org.lcappuccio.systemmonitor.collectors.DiskCollector;
import org.lcappuccio.systemmonitor.collectors.FileSystemCollector;
import org.lcappuccio.systemmonitor.collectors.GpuCollector;
import org.lcappuccio.systemmonitor.collectors.MemoryCollector;
import org.lcappuccio.systemmonitor.collectors.NetworkCollector;
import org.lcappuccio.systemmonitor.config.AppConfig;
import org.lcappuccio.systemmonitor.poller.PollerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application window.
 *
 * <p>Composed of a horizontal {@link SplitPane}:
 * <ul>
 *   <li>Left: {@link TableView} of {@link MetricRow} instances grouped by section</li>
 *   <li>Right: chart panel placeholder (to be implemented as {@code ChartPanel})</li>
 * </ul>
 */
public class MainWindow {

  private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);
  private static final double DIVIDER_POSITION = 0.4;

  private final SplitPane root;
  private final ObservableList<MetricRow> rows;
  private final PollerService pollerService;

  /**
   * Constructs the main window with the given application configuration.
   *
   * @param config the loaded application configuration
   */
  public MainWindow(AppConfig config) {
    LOG.debug("Building main window");
    this.rows = FXCollections.observableArrayList();

    TableView<MetricRow> table = buildTable();
    StackPane chartPlaceholder = buildChartPlaceholder();

    root = new SplitPane(table, chartPlaceholder);
    root.setOrientation(Orientation.HORIZONTAL);
    root.setDividerPositions(DIVIDER_POSITION);

    populateRows(config);

    this.pollerService = createPollerService(config);
    this.pollerService.start();
  }

  private PollerService createPollerService(AppConfig config) {
    List<Collector<?>> defaultCollectors = new ArrayList<>();
    defaultCollectors.add(new CpuCollector());
    defaultCollectors.add(new MemoryCollector());
    defaultCollectors.add(new GpuCollector());
    defaultCollectors.add(new NetworkCollector(config));

    List<Collector<?>> filesystemCollectors = new ArrayList<>();
    filesystemCollectors.add(new FileSystemCollector(config));

    List<Collector<?>> diskTempCollectors = new ArrayList<>();
    diskTempCollectors.add(new DiskCollector());

    return new PollerService(config, rows, defaultCollectors, filesystemCollectors,
        diskTempCollectors);
  }

  /**
   * Returns the root node to be placed in a {@link javafx.scene.Scene}.
   *
   * @return the root {@link Parent}
   */
  public Parent getRoot() {
    return root;
  }

  /**
   * Shuts down background services owned by this window.
   *
   * <p>Called on window close. Delegates to PollerService.shutdown().
   */
  public void shutdown() {
    LOG.info("MainWindow shutdown");
    if (pollerService != null) {
      pollerService.shutdown();
    }
  }

  private TableView<MetricRow> buildTable() {
    TableColumn<MetricRow, String> sectionCol = new TableColumn<>("Section");
    sectionCol.setCellValueFactory(
        cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getSection()));
    sectionCol.setPrefWidth(120);

    TableColumn<MetricRow, String> metricCol = new TableColumn<>("Metric");
    metricCol.setCellValueFactory(
        cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getMetric()));
    metricCol.setPrefWidth(180);

    TableColumn<MetricRow, String> valueCol = new TableColumn<>("Value");
    valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
    valueCol.setPrefWidth(120);

    TableView<MetricRow> table = new TableView<>(rows);
    table.getColumns().addAll(sectionCol, metricCol, valueCol);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table.setPlaceholder(new javafx.scene.control.Label("No data available"));
    return table;
  }

  private StackPane buildChartPlaceholder() {
    var label = new javafx.scene.control.Label("Charts — select a metric from the table");
    var pane = new StackPane(label);
    return pane;
  }

  private void populateRows(AppConfig config) {
    List<Integer> coreIds = discoverCpuCores();

    rows.add(new MetricRow("CPU", "Temperature", "—"));
    rows.add(new MetricRow("CPU", "Load", "—"));

    for (int coreId : coreIds) {
      rows.add(new MetricRow("CPU", "Core " + coreId, "—"));
    }

    rows.addAll(
        // Memory
        new MetricRow("Memory", "Used", "—"),
        new MetricRow("Memory", "Swap Used", "—"),
        // GPU
        new MetricRow("GPU", "Temperature", "—"),
        new MetricRow("GPU", "Load", "—"),
        new MetricRow("GPU", "VRAM Used", "—"),
        new MetricRow("GPU", "VRAM Temperature", "—"),
        new MetricRow("GPU", "VRAM Load", "—"),
        new MetricRow("GPU", "Power", "—"),
        // Disks
        new MetricRow("Disks", "NVMe Temperature", "—"),
        new MetricRow("Disks", "SSD Temperature", "—")
    );

    // Filesystems — one row per configured mount point
    for (String mount : config.getFsMountpoints()) {
      rows.add(new MetricRow("Filesystems", mount, "—"));
    }

    // Network
    rows.addAll(
        new MetricRow("Network", "IP Address", "—"),
        new MetricRow("Network", "Link Speed", "—"),
        new MetricRow("Network", "Upload", "—"),
        new MetricRow("Network", "Download", "—")
    );
  }

  private List<Integer> discoverCpuCores() {
    CpuCollector tempCollector = new CpuCollector();
    tempCollector.initialize();
    return tempCollector.getCoreIds();
  }
}