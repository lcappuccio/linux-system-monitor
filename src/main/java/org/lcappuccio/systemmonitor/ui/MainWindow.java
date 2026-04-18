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
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
 *   <li>Left: {@link TreeTableView} of {@link MetricRow} instances with tree nodes</li>
 *   <li>Right: chart panel</li>
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

    this.rootItem = new TreeItem<>(new MetricRow("System", "Metrics", ""));

    populateRows(config);

    chartPanel = new ChartPanel(rows, config);

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
    metricCol.setCellFactory(col -> new TreeTableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTreeTableRow() == null
            || getTreeTableRow().getItem() == null) {
          setText(null);
          setGraphic(null);
          return;
        }
        MetricRow row = getTreeTableRow().getItem();
        setText(row.getMetric());
        setGraphic(null);
      }
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

    metricCol.prefWidthProperty().bind(treeTable.widthProperty().multiply(0.40));
    valueCol.prefWidthProperty().bind(treeTable.widthProperty().multiply(0.60));

    treeTable.setPlaceholder(new javafx.scene.control.Label("No data available"));

    return treeTable;
  }

  private void populateRows(AppConfig config) {
    MetricRow cpuNode = new MetricRow(
        "CPU", "CPU", "", true, null,
        config.getColorCpu());
    TreeItem<MetricRow> cpuTreeItem = new TreeItem<>(cpuNode);
    rootItem.getChildren().add(cpuTreeItem);

    MetricRow cpuTempRow = new MetricRow(
        "CPU", "Temperature", "—", false, "CPU");
    rows.add(cpuTempRow);
    cpuTreeItem.getChildren().add(new TreeItem<>(cpuTempRow));

    MetricRow cpuLoadRow = new MetricRow(
        "CPU", "Load", "—", false, "CPU");
    rows.add(cpuLoadRow);
    cpuTreeItem.getChildren().add(new TreeItem<>(cpuLoadRow));

    List<Integer> coreIds = cpuCollector.getCoreIds();
    for (int i = 0; i < coreIds.size(); i++) {
      MetricRow coreRow = new MetricRow(
          "CPU", "Core " + i, "—", false, "CPU");
      rows.add(coreRow);
      cpuTreeItem.getChildren().add(new TreeItem<>(coreRow));
    }

    MetricRow gpuNode = new MetricRow(
        "GPU", "GPU", "", true, null,
        config.getColorGpu());
    TreeItem<MetricRow> gpuTreeItem = new TreeItem<>(gpuNode);
    rootItem.getChildren().add(gpuTreeItem);

    MetricRow gpuTempRow = new MetricRow(
        "GPU", "Temperature", "—", false, "GPU");
    rows.add(gpuTempRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuTempRow));

    MetricRow gpuLoadRow = new MetricRow(
        "GPU", "Load", "—", false, "GPU");
    rows.add(gpuLoadRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuLoadRow));

    MetricRow gpuVramUsedRow = new MetricRow(
        "GPU", "VRAM Used", "—", false, "GPU", config.getColorVram());
    rows.add(gpuVramUsedRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuVramUsedRow));

    MetricRow gpuVramTempRow = new MetricRow(
        "GPU", "VRAM Temperature", "—", false, "GPU",
        config.getColorVram());
    rows.add(gpuVramTempRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuVramTempRow));

    MetricRow gpuVramLoadRow = new MetricRow(
        "GPU", "VRAM Load", "—", false, "GPU", config.getColorVram());
    rows.add(gpuVramLoadRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuVramLoadRow));

    MetricRow gpuPowerRow = new MetricRow(
        "GPU", "Power", "—", false, "GPU");
    rows.add(gpuPowerRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuPowerRow));

    MetricRow gpuFanRow = new MetricRow(
        "GPU", "Fan", "—", false, "GPU");
    rows.add(gpuFanRow);
    gpuTreeItem.getChildren().add(new TreeItem<>(gpuFanRow));

    MetricRow memNode = new MetricRow(
        "Memory", "Memory", "", true, null);
    TreeItem<MetricRow> memTreeItem = new TreeItem<>(memNode);
    rootItem.getChildren().add(memTreeItem);

    MetricRow memUsedRow = new MetricRow(
        "Memory", "Used", "—", false, "Memory", config.getColorMemoryUsed());
    rows.add(memUsedRow);
    memTreeItem.getChildren().add(new TreeItem<>(memUsedRow));

    MetricRow memSwapRow = new MetricRow(
        "Memory", "Swap Used", "—", false, "Memory", config.getColorSwapUsed());
    rows.add(memSwapRow);
    memTreeItem.getChildren().add(new TreeItem<>(memSwapRow));

    MetricRow nvmeNode = new MetricRow(
        "Disks", "NVMe", "", true, null,
        config.getColorNvme());
    TreeItem<MetricRow> nvmeTreeItem = new TreeItem<>(nvmeNode);
    rootItem.getChildren().add(nvmeTreeItem);

    MetricRow nvmeTempRow = new MetricRow(
        "Disks", "NVMe Temperature", "—", false, "Disks");
    rows.add(nvmeTempRow);
    nvmeTreeItem.getChildren().add(new TreeItem<>(nvmeTempRow));

    MetricRow ssdNode = new MetricRow(
        "Disks", "SSD", "", true, null,
        config.getColorSata());
    TreeItem<MetricRow> ssdTreeItem = new TreeItem<>(ssdNode);
    rootItem.getChildren().add(ssdTreeItem);

    MetricRow ssdTempRow = new MetricRow(
        "Disks", "SSD Temperature", "—", false, "Disks");
    rows.add(ssdTempRow);
    ssdTreeItem.getChildren().add(new TreeItem<>(ssdTempRow));

    MetricRow fsNode = new MetricRow(
        "Filesystems", "Filesystems", "", true, null, null);
    TreeItem<MetricRow> fsTreeItem = new TreeItem<>(fsNode);
    rootItem.getChildren().add(fsTreeItem);

    for (String mount : config.getFsMountpoints()) {
      MetricRow fsRow = new MetricRow(
          "Filesystems", mount, "—", false, "Filesystems");
      rows.add(fsRow);
      fsTreeItem.getChildren().add(new TreeItem<>(fsRow));
    }

    MetricRow netNode = new MetricRow(
        "Network", "Network", "", true, null, null);
    TreeItem<MetricRow> netTreeItem = new TreeItem<>(netNode);
    rootItem.getChildren().add(netTreeItem);

    MetricRow netIpRow = new MetricRow(
        "Network", "IP Address", "—", false, "Network");
    rows.add(netIpRow);
    netTreeItem.getChildren().add(new TreeItem<>(netIpRow));

    MetricRow netSpeedRow = new MetricRow(
        "Network", "Link Speed", "—", false, "Network");
    rows.add(netSpeedRow);
    netTreeItem.getChildren().add(new TreeItem<>(netSpeedRow));

    MetricRow netUpRow = new MetricRow("Network", "Upload", "—", false, "Network");
    rows.add(netUpRow);
    netTreeItem.getChildren().add(new TreeItem<>(netUpRow));

    MetricRow netDownRow = new MetricRow(
        "Network", "Download", "—", false, "Network");
    rows.add(netDownRow);
    netTreeItem.getChildren().add(new TreeItem<>(netDownRow));
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