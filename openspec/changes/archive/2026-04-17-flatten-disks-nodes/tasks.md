## 1. Modify MainWindow.populateRows()

- [x] 1.1 Remove the "Disks" parent TreeItem creation
- [x] 1.2 Add NVMe disk TreeItem directly to rootItem
- [x] 1.3 Add SATA disk TreeItem directly to rootItem
- [x] 1.4 Verify disk temperature remains as child of each device node

## 2. Test and Verify

- [x] 2.1 Run mvn clean package to verify compilation
- [x] 2.2 Run mvn test to verify all tests pass
- [x] 2.3 Manual test: verify NVMe shows at root level with model name (e.g., "Samsung SSD 970 EVO Plus 500GB")
- [x] 2.4 Manual test: verify SATA shows at root level with model name
- [x] 2.5 Manual test: verify temperature is child of each disk device node