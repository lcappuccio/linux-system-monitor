package org.lcappuccio.systemmonitor.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
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
  private static final double DIVIDER_POSITION = 0.30;

  private final ChartPanel chartPanel;
  private final Label heapLabel = new Label();
  private final ObservableList<MetricRow> rows;
  private final PollerService pollerService;
  private final SplitPane root;
  private final CpuCollector cpuCollector;
  private final GpuCollector gpuCollector;
  private final DiskCollector diskCollector;
  private final TreeItem<MetricRow> rootItem;

  private Timeline heapTimer;

  /**
   * Constructs the main window with the given application configuration.
   *
   * @param config the loaded application configuration
   */
  public MainWindow(AppConfig config) {
    LOG.info("Building main window");
    this.rows = FXCollections.observableArrayList();

    this.cpuCollector = new CpuCollector();
    this.cpuCollector.initialize();

    this.gpuCollector = new GpuCollector(config);
    this.gpuCollector.initialize();

    this.diskCollector = new DiskCollector(config);
    this.diskCollector.initialize();

    HardwareNames hardwareNames = HardwareNames.fromCollectors(
        cpuCollector, gpuCollector, diskCollector);

    this.rootItem = new TreeItem<>(new MetricRow("System", "Metrics", ""));

    populateRows(config);

    chartPanel = new ChartPanel(rows, config, hardwareNames);

    TreeTableView<MetricRow> treeTable = buildTreeTable();

    BorderPane leftPane = new BorderPane();
    leftPane.setCenter(treeTable);
    leftPane.setBottom(buildStatusBar(config));

    root = new SplitPane(leftPane, chartPanel.getRoot());
    root.setOrientation(Orientation.HORIZONTAL);
    root.setDividerPositions(DIVIDER_POSITION);

    if ("dark".equals(config.getUiTheme())) {
      root.getStylesheets().add(Objects.requireNonNull(getClass()
          .getResource("/dark.css")).toExternalForm());
    }

    startHeapMonitor();

    this.pollerService = createPollerService(config);
    this.pollerService.start();
  }

  private PollerService createPollerService(AppConfig config) {
    List<Collector<?>> defaultCollectors = new ArrayList<>();
    defaultCollectors.add(cpuCollector);
    defaultCollectors.add(new MemoryCollector());
    defaultCollectors.add(gpuCollector);
    defaultCollectors.add(new NetworkCollector(config));

    List<Collector<?>> filesystemCollectors = new ArrayList<>();
    filesystemCollectors.add(new FileSystemCollector(config));

    List<Collector<?>> diskTempCollectors = new ArrayList<>();
    diskTempCollectors.add(diskCollector);

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
    if (chartPanel != null) {
      chartPanel.shutdown();
    }
    if (heapTimer != null) {
      heapTimer.stop();
    }
  }

  private TreeTableView<MetricRow> buildTreeTable() {
    TreeTableColumn<MetricRow, String> metricCol = new TreeTableColumn<>("Metric");
    metricCol.setCellValueFactory(cell -> {
      TreeItem<MetricRow> treeItem = cell.getValue();
      if (treeItem == null || treeItem.getValue() == null) {
        return new javafx.beans.property.SimpleStringProperty("");
      }
      return new javafx.beans.property.SimpleStringProperty(treeItem.getValue().getMetric());
    });
    metricCol.setPrefWidth(300);

    TreeTableColumn<MetricRow, String> valueCol = new TreeTableColumn<>("Value");
    valueCol.setCellValueFactory(cell -> {
      TreeItem<MetricRow> treeItem = cell.getValue();
      if (treeItem == null || treeItem.getValue() == null) {
        return new javafx.beans.property.SimpleStringProperty("");
      }
      MetricRow row = treeItem.getValue();
      if (row.isHardwareNode()) {
        return new javafx.beans.property.SimpleStringProperty("");
      }
      return row.valueProperty();
    });
    valueCol.setPrefWidth(120);

    TreeTableView<MetricRow> treeTable = new TreeTableView<>();
    treeTable.getColumns().add(metricCol);
    treeTable.getColumns().add(valueCol);
    treeTable.setRoot(rootItem);
    treeTable.setShowRoot(false);
    treeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

    metricCol.prefWidthProperty().bind(treeTable.widthProperty().multiply(0.70));
    valueCol.prefWidthProperty().bind(treeTable.widthProperty().multiply(0.30));

    treeTable.setPlaceholder(new javafx.scene.control.Label("No data available"));

    return treeTable;
  }

  private void populateRows(AppConfig config) {
    // CPU
    String cpuModelName = cpuCollector.getModelName();
    MetricRow cpuNode = new MetricRow("CPU", cpuModelName, "", true, null);
    TreeItem<MetricRow> cpuTreeItem = new TreeItem<>(cpuNode);
    rootItem.getChildren().add(cpuTreeItem);

    MetricRow cpuTempRow = new MetricRow("CPU", "Temperature", "—", false, cpuModelName);
    rows.add(cpuTempRow);
    cpuTreeItem.getChildren().add(new TreeItem<>(cpuTempRow));

    MetricRow cpuLoadRow = new MetricRow("CPU", "Load", "—", false, cpuModelName);
    rows.add(cpuLoadRow);
    cpuTreeItem.getChildren().add(new TreeItem<>(cpuLoadRow));

    List<Integer> coreIds = cpuCollector.getCoreIds();
    for (int i = 0; i < coreIds.size(); i++) {
      MetricRow coreRow = new MetricRow("CPU", "Core " + i, "—", false, cpuModelName);
      rows.add(coreRow);
      cpuTreeItem.getChildren().add(new TreeItem<>(coreRow));
    }

    // GPU
    String gpuModelName = gpuCollector.getModelName();
    // GPU node with model name
    MetricRow gpuNode = new MetricRow("GPU", gpuModelName, "", true, null);
    TreeItem<MetricRow> gpuTreeItem = new TreeItem<>(gpuNode);
    rootItem.getChildren().add(gpuTreeItem);

    MetricRow gpuTempRow = new MetricRow("GPU", "Temperature", "—", false, gpuModelName);
    rows.add(gpuTempRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuTempRow));

    MetricRow gpuLoadRow = new MetricRow("GPU", "Load", "—", false, gpuModelName);
    rows.add(gpuLoadRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuLoadRow));

    MetricRow gpuVramUsedRow = new MetricRow("GPU", "VRAM Used", "—", false, gpuModelName);
    rows.add(gpuVramUsedRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuVramUsedRow));

    MetricRow gpuVramTempRow = new MetricRow("GPU", "VRAM Temperature", "—", false, gpuModelName);
    rows.add(gpuVramTempRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuVramTempRow));

    MetricRow gpuVramLoadRow = new MetricRow("GPU", "VRAM Load", "—", false, gpuModelName);
    rows.add(gpuVramLoadRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuVramLoadRow));

    MetricRow gpuPowerRow = new MetricRow("GPU", "Power", "—", false, gpuModelName);
    rows.add(gpuPowerRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuPowerRow));

    MetricRow gpuFanRow = new MetricRow("GPU", "Fan", "—", false, gpuModelName);
    rows.add(gpuFanRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuFanRow));

    // Memory node
    MetricRow memNode = new MetricRow("Memory", "Memory", "", true, null);
    TreeItem<MetricRow> memTreeItem = new TreeItem<>(memNode);
    rootItem.getChildren().add(memTreeItem);

    MetricRow memUsedRow = new MetricRow("Memory", "Used", "—", false, "Memory");
    rows.add(memUsedRow);
    memTreeItem.getChildren().add(new TreeItem<>(memUsedRow));

    MetricRow memSwapRow = new MetricRow("Memory", "Swap Used", "—", false, "Memory");
    rows.add(memSwapRow);
    memTreeItem.getChildren().add(new TreeItem<>(memSwapRow));

    // Disks node - with NVMe and SSD as children
    String nvmeModelName = diskCollector.getNvmeModelName();
    MetricRow disksNode = new MetricRow("Disks", "Disks", "", true, null);
    TreeItem<MetricRow> disksTreeItem = new TreeItem<>(disksNode);
    rootItem.getChildren().add(disksTreeItem);

    MetricRow nvmeNode = new MetricRow("Disks", nvmeModelName, "", true, null);
    TreeItem<MetricRow> nvmeTreeItem = new TreeItem<>(nvmeNode);
    disksTreeItem.getChildren().add(nvmeTreeItem);

    MetricRow nvmeTempRow = new MetricRow("Disks", "NVMe Temperature", "—", false, nvmeModelName);
    rows.add(nvmeTempRow);
    nvmeTreeItem.getChildren().add(new TreeItem<>(nvmeTempRow));

    String sataModelName = diskCollector.getSataModelName();
    MetricRow ssdNode = new MetricRow("Disks", sataModelName, "", true, null);
    TreeItem<MetricRow> ssdTreeItem = new TreeItem<>(ssdNode);
    disksTreeItem.getChildren().add(ssdTreeItem);

    MetricRow ssdTempRow = new MetricRow("Disks", "SSD Temperature", "—", false, sataModelName);
    rows.add(ssdTempRow);
    ssdTreeItem.getChildren().add(new TreeItem<>(ssdTempRow));

    // Filesystems node
    MetricRow fsNode = new MetricRow("Filesystems", "Filesystems", "", true, null);
    TreeItem<MetricRow> fsTreeItem = new TreeItem<>(fsNode);
    rootItem.getChildren().add(fsTreeItem);

    for (String mount : config.getFsMountpoints()) {
      MetricRow fsRow = new MetricRow("Filesystems", mount, "—", false, "Filesystems");
      rows.add(fsRow);
      fsTreeItem.getChildren().add(new TreeItem<>(fsRow));
    }

    // Network node
    MetricRow netNode = new MetricRow("Network", "Network", "", true, null);
    TreeItem<MetricRow> netTreeItem = new TreeItem<>(netNode);
    rootItem.getChildren().add(netTreeItem);

    MetricRow netIpRow = new MetricRow("Network", "IP Address", "—", false, "Network");
    rows.add(netIpRow);
    netTreeItem.getChildren().add(new TreeItem<>(netIpRow));

    MetricRow netSpeedRow = new MetricRow("Network", "Link Speed", "—", false, "Network");
    rows.add(netSpeedRow);
    netTreeItem.getChildren().add(new TreeItem<>(netSpeedRow));

    MetricRow netUpRow = new MetricRow("Network", "Upload", "—", false, "Network");
    rows.add(netUpRow);
    netTreeItem.getChildren().add(new TreeItem<>(netUpRow));

    MetricRow netDownRow = new MetricRow("Network", "Download", "—", false, "Network");
    rows.add(netDownRow);
    netTreeItem.getChildren().add(new TreeItem<>(netDownRow));
  }

  private List<Integer> discoverCpuCores() {
    return cpuCollector.getCoreIds();
  }

  private javafx.scene.layout.HBox buildStatusBar(AppConfig appConfig) {
    if ("dark".equals(appConfig.getUiTheme())) {
      heapLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #BBBBBB; -fx-padding: 2 6 2 6;");
    } else {
      heapLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #0E0E0E; -fx-padding: 2 6 2 6;");
    }
    updateHeapLabel();
    javafx.scene.layout.HBox bar = new javafx.scene.layout.HBox(heapLabel);
    bar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
    return bar;
  }

  private void startHeapMonitor() {
    heapTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> updateHeapLabel()));
    heapTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
    heapTimer.play();
  }

  private void updateHeapLabel() {
    Runtime rt = Runtime.getRuntime();
    long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    long max = rt.maxMemory() / (1024 * 1024);
    heapLabel.setText("JVM Heap: " + used + " MB / " + max + " MB");
  }
}