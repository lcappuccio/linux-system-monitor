# Design Decisions

## MetricRow

* section and metric are plain String fields, not properties — they never change after construction, no need for observable wrappers.

* Only value is a StringProperty — it's the only field the poller will update.

* Initial value is "—" by convention (caller's choice) — signals "not yet collected" without showing stale zeros.

* No JavaFX types leak into the constructor signature — section and metric are plain strings.

## MainWindow

* MainWindow owns the ObservableList<MetricRow> — the poller will receive a reference to it later to push updates.

* Filesystem rows are generated dynamically from config.getFsMountpoints() — no hardcoding.

* shutdown() is a stub now, will delegate to PollerService.shutdown() once wired.

* buildChartPlaceholder() will be replaced by ChartPanel in the last step.

* The SimpleStringProperty inline in sectionCol and metricCol cell factories is intentional — those values never change so no need to store them in the row model.