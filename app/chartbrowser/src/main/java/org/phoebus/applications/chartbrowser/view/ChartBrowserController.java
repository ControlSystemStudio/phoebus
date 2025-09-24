/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.view;

import com.influxdb.query.FluxRecord;
import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.CrosshairIndicator;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.XValueIndicator;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.utils.Tuple;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.epics.util.array.ListDouble;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.applications.chartbrowser.ChartBrowserApp;
import org.phoebus.applications.chartbrowser.model.PVTableEntry;
import org.phoebus.applications.chartbrowser.model.StatisticsTableEntry;
import org.phoebus.applications.chartbrowser.persistence.ChartBrowserPersistence;
import org.phoebus.applications.chartbrowser.view.cells.BufferSizeTableCell;
import org.phoebus.applications.chartbrowser.view.cells.CheckBoxTableCell;
import org.phoebus.applications.chartbrowser.view.cells.StringInputTableCell;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.reader.influx2.InfluxArchiveReader;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.influx2.InfluxDB_Preferences;
import org.phoebus.ui.javafx.ImageCache;

/**
 * Controller for the Chart Browser application.
 * This controller manages the creation and update of charts, handles PV data (live and archive),
 * and provides UI interactions such as zooming, table updates, tooltips, and archive data retrieval.
 *
 */
public class ChartBrowserController {
    private static final int MAX_POINTS_PER_DATASET = 100000;
    private static final int MAX_TOTAL_POINTS = 500000;
    private static final int DEFAULT_BUFFER_SIZE = 5000;
    private static final int ZOOM_DEBOUNCE_MS = 500;
    private static final int STATS_UPDATE_INTERVAL_MS = 5000;
    private static final String ARCHIVED_PREFIX = "archived/";
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter
        .ofPattern("yyyy‑MM‑dd HH:mm:ss.SSS")
        .withZone(UTC_ZONE);

    @FXML private BorderPane chartContainer;
    @FXML private TextField txtPVName;
    @FXML private TableView<PVTableEntry> pvTable;
    @FXML private TableColumn<PVTableEntry, String> nameColumn;
    @FXML private TableColumn<PVTableEntry, Boolean> archiveColumn;
    @FXML private TableColumn<PVTableEntry, Boolean> rawDataColumn;
    @FXML private TableColumn<PVTableEntry, Integer> bufferSizeColumn;
    @FXML private TableColumn<PVTableEntry, String> meanValueColumn;
    @FXML private Button removePVButton;
    @FXML private TabPane tabPane;
    @FXML private TableView<StatisticsTableEntry> statsTable;
    @FXML private TableColumn<StatisticsTableEntry, String> statsPVNameColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsSampleCountColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsMeanColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsMedianColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsStdDevColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsMinColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsMaxColumn;
    @FXML private TableColumn<StatisticsTableEntry, Number> statsSumColumn;
    @FXML private Button refreshStatsButton;
    @FXML private ToggleButton toggleCrosshair;
    @FXML private ToggleButton toggleCloseTabPane;
    @FXML private Button btnLoadPlt;
    @FXML private Button btnSavePlt;

    private XYChart chart;
    private final Map<String, DoubleDataSet> dataSets = new HashMap<>();
    private final Map<String, List<XValueIndicator>> markers = new HashMap<>();
    private final Map<String, PV> pvInstances = new HashMap<>();
    private final Map<String, Disposable> pvSubscriptions = new HashMap<>();
    private final Map<String, List<Long>> timeStamps = new HashMap<>();
    private final ObservableList<PVTableEntry> pvList = FXCollections.observableArrayList();
    private final ObservableList<StatisticsTableEntry> statsList = FXCollections.observableArrayList();

    private final AtomicBoolean isZoomRefreshing = new AtomicBoolean(false);
    private final AtomicBoolean statsTimerRunning = new AtomicBoolean(false);
    private final AtomicInteger pendingArchiveRequests = new AtomicInteger(0);
    private volatile boolean isShuttingDown = false;
    private boolean zoomListenerAdded = false;

    private final ExecutorService archiveDataExecutor = Executors.newFixedThreadPool(4);
    private final Map<String, Timer> timers = new HashMap<>();
    private final Object zoomLock = new Object();

    private CrosshairIndicator crosshairIndicator;
    private ImageView upImageView;
    private ImageView downImageView;

    private static final Logger logger = Logger.getLogger(ChartBrowserController.class.getPackageName());
    private final Map<String, Long> lastTaskScheduled = new HashMap<>();
    private static final Map<String, String> lastPVStringVal = new HashMap<>();

    private volatile long lastZoomUpdateTime = 0;
    private volatile boolean zoomUpdatePending = false;
    private final Object zoomUpdateLock = new Object();
    private Timer zoomDebounceTimer;

    /**
     * Initializes the controller
     */
    public void initialize() {
        initializeUI();
        initializeChart();
        initializeTables();
        initializeKeyboardShortcuts();
    }

    private void initializeUI() {
        Image crosshairImage = ImageCache.getImage(ChartBrowserApp.class, "/icons/crosshair.png");
        Image upImage = ImageCache.getImage(ChartBrowserApp.class, "/icons/up.png");
        Image downImage = ImageCache.getImage(ChartBrowserApp.class, "/icons/down.png");

        ImageView crosshairImageView = createImageView(crosshairImage);
        upImageView = createImageView(upImage);
        downImageView = createImageView(downImage);

        toggleCrosshair.setGraphic(crosshairImageView);
        toggleCloseTabPane.setGraphic(downImageView);

        toggleCrosshair.setSelected(false);
        toggleCrosshair.setOnAction(evt -> toggleCrosshair());

        btnSavePlt.setOnAction(evt -> onSavePlt());
        btnLoadPlt.setOnAction(evt -> onLoadPlt());
    }

    private ImageView createImageView(Image image) {
        ImageView view = new ImageView(image);
        view.setFitHeight(16);
        view.setFitWidth(16);
        return view;
    }

    private void initializeChart() {
        DefaultNumericAxis xAxis = createTimeAxis();
        DefaultNumericAxis yAxis = createYAxis();

        chart = new XYChart(xAxis, yAxis);
        chart.setAnimated(false);
        chart.getPlugins().addAll(
            new Zoomer(),
            new EditAxis(),
            new ParameterMeasurements()
        );

        crosshairIndicator = createCrosshairIndicator();

        chart.setOnZoom(zoomEvent -> scheduleTask("zoom", ZOOM_DEBOUNCE_MS, this::updateDataForZoomLevel));

        chartContainer.setCenter(chart);
        chart.prefWidthProperty().bind(chartContainer.widthProperty());
        chart.prefHeightProperty().bind(chartContainer.heightProperty());
    }

    private DefaultNumericAxis createTimeAxis() {
        DefaultNumericAxis xAxis = new DefaultNumericAxis("");
        xAxis.setAnimated(false);
        xAxis.setTimeAxis(true);
        xAxis.setTickLabelFormatter(new TimeAxisFormatter());

        ZonedDateTime now = ZonedDateTime.now(UTC_ZONE);
        double endEpochSeconds = toEpochSeconds(now);
        double startEpochSeconds = endEpochSeconds - 3600d;

        xAxis.setMin(startEpochSeconds);
        xAxis.setMax(endEpochSeconds);
        xAxis.setAutoRanging(false);

        return xAxis;
    }

    private DefaultNumericAxis createYAxis() {
        DefaultNumericAxis yAxis = new DefaultNumericAxis("");
        yAxis.setAnimated(false);
        yAxis.setMin(-10.0);
        yAxis.setMax(10.0);
        yAxis.setAutoRanging(false);
        return yAxis;
    }

    private void initializeTables() {
        initializePVTable();
        initializeStatisticsTable();
    }

    private void initializePVTable() {
        pvTable.setItems(pvList);

        nameColumn.setCellValueFactory(data -> data.getValue().pvNameProperty());
        archiveColumn.setCellValueFactory(data -> data.getValue().useArchiveProperty());
        archiveColumn.setCellFactory(column -> new CheckBoxTableCell());

        CheckBox allArchive = new CheckBox();
        allArchive.setOnAction(e -> {
            boolean selected = allArchive.isSelected();
            pvList.forEach(pv -> pv.useArchiveProperty().set(selected));
            pvTable.refresh();
        });
        archiveColumn.setGraphic(allArchive);

        rawDataColumn.setCellValueFactory(data -> data.getValue().useRawDataProperty());
        rawDataColumn.setCellFactory(column -> new CheckBoxTableCell());
        bufferSizeColumn.setCellValueFactory(data -> data.getValue().bufferSizeProperty());
        bufferSizeColumn.setCellFactory(column -> new BufferSizeTableCell());
        meanValueColumn.setCellValueFactory(data -> data.getValue().meanValueProperty());
        meanValueColumn.setCellFactory(column -> new StringInputTableCell("1s"));

        pvTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldSelection, newSelection) ->
                removePVButton.setDisable(newSelection == null));

        pvTable.setRowFactory(tv -> createPVTableRow());
    }

    private TableRow<PVTableEntry> createPVTableRow() {
        TableRow<PVTableEntry> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                PVTableEntry entry = row.getItem();
                if (entry != null) {
                    removePV(entry.getPvName());
                }
            }
        });
        return row;
    }

    private void initializeStatisticsTable() {
        statsTable.setItems(statsList);

        statsPVNameColumn.setCellValueFactory(data -> data.getValue().pvNameProperty());
        configureNumberColumn(statsSampleCountColumn, "%d");
        configureNumberColumn(statsMeanColumn, "%.6f");
        configureNumberColumn(statsMedianColumn, "%.6f");
        configureNumberColumn(statsStdDevColumn, "%.6f");
        configureNumberColumn(statsMinColumn, "%.6f");
        configureNumberColumn(statsMaxColumn, "%.6f");
        configureNumberColumn(statsSumColumn, "%.6f");

        refreshStatsButton.setOnAction(event -> refreshStatistics());

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "statsTab".equals(newTab.getId())) {
                startStatisticsTimer();
            } else {
                stopStatisticsTimer();
            }
        });

        startStatisticsTimer();
    }

    private void configureNumberColumn(TableColumn<StatisticsTableEntry, Number> column,
        String format) {
        column.setCellValueFactory(data -> {
            String propName = column.getId().replace("stats", "").toLowerCase();

            return switch (propName) {
                case "samplecountcolumn" -> data.getValue().sampleCountProperty();
                case "meancolumn" -> data.getValue().meanProperty();
                case "mediancolumn" -> data.getValue().medianProperty();
                case "stddevcolumn" -> data.getValue().stdDevProperty();
                case "mincolumn" -> data.getValue().minValueProperty();
                case "maxcolumn" -> data.getValue().maxValueProperty();
                case "sumcolumn" -> data.getValue().sumProperty();
                default -> null;
            };
        });

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format(format, item));
                }
            }
        });
    }

    private void initializeKeyboardShortcuts() {
        chartContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                    this::onSavePlt
                );
            }
        });
    }

    @FXML
    public void onPVFieldAction() throws Exception {
        addPVFromField();
    }

    @FXML
    public void onAddPVAction() throws Exception {
        addPVFromField();
    }

    private void addPVFromField() throws Exception {
        String pvName = txtPVName.getText();

        if (pvName != null && !pvName.trim().isEmpty()) {
            setPVName(pvName);
        }
    }

    @FXML
    public void onRemovePVAction() {
        PVTableEntry selected = pvTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            removePV(selected.getPvName());
        }
    }

    @FXML
    public void onToggleCloseTabPaneAction(ActionEvent ignored) {
        boolean closed = toggleCloseTabPane.isSelected();
        tabPane.setVisible(!closed);
        tabPane.setManaged(!closed);
        toggleCloseTabPane.setGraphic(closed ? upImageView : downImageView);
    }

    public void setPVName(String pvName) throws Exception {
        if (pvName == null || pvName.trim().isEmpty() || pvExists(pvName)) {
            return;
        }

        if (txtPVName != null) {
            txtPVName.setText(pvName);
        }

        createPVEntry(pvName, false, false, DEFAULT_BUFFER_SIZE);
    }

    private boolean pvExists(String pvName) {
        return pvList.stream().anyMatch(entry -> entry.getPvName().equals(pvName));
    }

    private void createPVEntry(String name, boolean useArchive, boolean useRawData, int bufferSize) throws Exception {
        PVTableEntry entry = new PVTableEntry(name);
        entry.setUseArchive(useArchive);
        entry.setUseRawData(useRawData);
        entry.setBufferSize(bufferSize);

        pvList.add(entry);
        entry.useArchiveProperty().addListener((obs, oldVal, newVal) -> updateArchive(newVal, entry));

        DoubleDataSet dataSet = new DoubleDataSet(name);
        dataSets.put(name, dataSet);
        timeStamps.put(name, new ArrayList<>());
        chart.getDatasets().add(dataSet);

        PV pv = PVPool.getPV(name);
        pvInstances.put(name, pv);

        Disposable subscription = pv.onValueEvent().subscribe(
            value -> handlePVValue(name, value),
            Throwable::printStackTrace
        );

        pvSubscriptions.put(name, subscription);

        if (useArchive) {
            resetArchivedSeries(entry);
            loadArchiveSeries(entry);
            setupZoomListener();
            scheduleLiveDataResume(List.of(entry));
        }
    }

    private void handlePVValue(String pvName, VType value) {
        try {
            if (value instanceof VDouble vDouble) {
                handleScalarValue(pvName, vDouble);
            } else if (value instanceof VDoubleArray array) {
                handleArrayValue(pvName, array);
            } else if (value instanceof VString str) {
                handleStringValue(pvName, str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleScalarValue(String pvName, VDouble vDouble) {
        double yValue = vDouble.getValue();
        if (Double.isNaN(yValue) || Double.isInfinite(yValue)) {
            return;
        }

        Instant timestamp = VTypeHelper.getTimestamp(vDouble);
        double timeInSeconds = timestamp.toEpochMilli() / 1000.0;

        Platform.runLater(() -> addUniqueDataPoint(pvName, dataSets.get(pvName), timeInSeconds, yValue));
    }

    private void handleArrayValue(String pvName, VDoubleArray array) {
        ListDouble data = array.getData();
        int n = data.size();
        if (n == 0) return;

        Instant timestamp = VTypeHelper.getTimestamp(array);
        double baseTime = timestamp.toEpochMilli() / 1000.0;

        double[] xs = new double[n];
        double[] ys = new double[n];

        for (int i = 0; i < n; i++) {
            xs[i] = baseTime + i;
            ys[i] = data.getDouble(i);
        }

        Platform.runLater(() -> updateArrayData(pvName, xs, ys));
    }

    public void handleStringValue(String pvName, VString pvVal) {
        if (lastPVStringVal.get(pvName) == null) {
            lastPVStringVal.put(pvName, pvVal.getValue());
            return;
        }

        if (!lastPVStringVal.get(pvName).equals(pvVal.getValue())) {
            Instant timestamp = VTypeHelper.getTimestamp(pvVal);
            double timeInSeconds = timestamp.getEpochSecond() + timestamp.getNano() / 1_000_000_000d;

            if (isInVisibleRange(timeInSeconds)) {
                Platform.runLater(() -> {
                    XValueIndicator xMarker = new XValueIndicator(
                        chart.getXAxis(),
                        timeInSeconds
                    );
                    xMarker.setText(pvVal.getValue());
                    xMarker.setLabelPosition(0.05);
                    xMarker.setEditable(false);

                    markers.computeIfAbsent(pvName, k -> new ArrayList<>());
                    markers.get(pvName).add(xMarker);
                    chart.getPlugins().add(xMarker);
                });
            }

            lastPVStringVal.replace(pvName, pvVal.getValue());
        }
    }

    private boolean isInVisibleRange(double timeInSeconds) {
        DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();
        double minTime = xAxis.getMin();
        double maxTime = xAxis.getMax();

        if (Double.isNaN(minTime) || Double.isNaN(maxTime) || Double.isNaN(timeInSeconds)) {
            return false;
        }

        double timeRange = maxTime - minTime;
        double margin = Math.max(timeRange * 0.05, 1.0);

        return timeInSeconds >= (minTime - margin) && timeInSeconds <= (maxTime + margin);
    }

    private void updateArrayData(String pvName, double[] xs, double[] ys) {
        DoubleDataSet ds = dataSets.get(pvName);
        if (ds != null) {
            ds.clearData();
            ds.add(xs, ys);

            List<Long> timestamps = timeStamps.get(pvName);
            if (timestamps != null) {
                timestamps.clear();
                for (double x : xs) {
                    timestamps.add((long)(x * 1000));
                }
            }
        }
    }

    private void addUniqueDataPoint(String pvName, DoubleDataSet dataSet, double timestamp, double value) {
        if (dataSet == null) return;

        List<Long> timestamps = timeStamps.computeIfAbsent(pvName, k -> new ArrayList<>());
        long timeMillis = (long) (timestamp * 1000);

        int existingIndex = findTimestampIndex(timestamps, timeMillis);

        if (existingIndex >= 0) {
            if (Math.abs(dataSet.get(1, existingIndex) - value) > 1e-10) {
                dataSet.set(existingIndex, timestamp, value);
            }
        } else {
            if (dataSet.getDataCount() == 0) {
                enableAutoRanging();
            }

            dataSet.add(timestamp, value);
            timestamps.add(timeMillis);

            trimDataSet(pvName, dataSet, timestamps);
        }

        Platform.runLater(() -> chart.requestLayout());
    }

    private int findTimestampIndex(List<Long> timestamps, long timeMillis) {
        for (int i = 0; i < timestamps.size(); i++) {
            if (timestamps.get(i) == timeMillis) {
                return i;
            }
        }
        return -1;
    }

    private void enableAutoRanging() {
        chart.getXAxis().setAutoRanging(true);
        chart.getYAxis().setAutoRanging(true);
    }

    private void trimDataSet(String pvName, DoubleDataSet dataSet, List<Long> timestamps) {
        int maxSize = getBufferSize(pvName);

        while (dataSet.getDataCount() > maxSize) {
            dataSet.remove(0);
            if (!timestamps.isEmpty()) {
                timestamps.remove(0);
            }
        }
    }

    private int getBufferSize(String pvName) {
        return pvList.stream()
            .filter(entry -> entry.getPvName().equals(pvName))
            .findFirst()
            .map(PVTableEntry::getBufferSize)
            .orElse(DEFAULT_BUFFER_SIZE);
    }

    private void updateArchive(boolean state, PVTableEntry entry) {
        if (state) {
            List<PVTableEntry> archivedPVs = pvList.stream()
                .filter(PVTableEntry::isUseArchive)
                .toList();

            resetArchivedSeries(entry);
            loadArchiveSeries(entry);

            setupZoomListener();
            scheduleLiveDataResume(archivedPVs);
        } else {
            resetArchivedSeries(entry);
            removeArchivedData(entry);
        }
    }

    private void removeArchivedData(PVTableEntry entry) {
        String archivedKey = buildArchiveKey(entry.getPvName());

        Optional.ofNullable(pvSubscriptions.remove(archivedKey))
            .ifPresent(Disposable::dispose);

        Optional.ofNullable(dataSets.remove(archivedKey))
            .ifPresent(ds -> chart.getDatasets().remove(ds));

        timeStamps.remove(archivedKey);


        List<XValueIndicator> markersList = markers.get(archivedKey);

        if (markersList != null) {
            markersList.forEach((marker) -> chart.getPlugins().remove(marker));
            markers.remove(archivedKey);
        }
    }

    private void resetArchivedSeries(PVTableEntry row) {
        String key = buildArchiveKey(row.getPvName());

        Optional.ofNullable(pvSubscriptions.remove(key))
            .ifPresent(Disposable::dispose);
        Optional.ofNullable(dataSets.get(key))
            .ifPresent(DoubleDataSet::clearData);
        Optional.ofNullable(timeStamps.get(key))
            .ifPresent(List::clear);

        clearMarkersForPV(key);
    }

    private void loadArchiveSeries(PVTableEntry row) {
        ParsedPVName parsed = parsePVName(row.getPvName());
        if (parsed == null) {
            return;
        }

        String key = buildArchiveKey(row.getPvName());
        Instant start = getStartInstant();
        Instant end = getEndInstant();

        pendingArchiveRequests.incrementAndGet();
        archiveDataExecutor.submit(() -> fetchAndDisplayArchiveData(
            parsed, key, start, end, row.getBufferSize(), row
        ));
    }


    private void fetchAndDisplayArchiveData(ParsedPVName parsed, String key,
        Instant start, Instant end,
        int points, PVTableEntry row) {
        try {
            logger.log(Level.INFO, String.format("Fetching archive data for %s (bucket: %s, pv: %s) from %s to %s",
                key, parsed.bucket, parsed.pvName, start, end));

            List<VType> archiveData = fetchArchiveData(
                parsed.bucket, parsed.pvName, start, end, points, row
            );

            logger.log(Level.INFO, String.format("Retrieved %d archive points for %s",
                archiveData.size(), key));

            boolean usedFallback = false;

            if (archiveData.isEmpty()) {
                logger.log(Level.WARNING, String.format("No archive data found for %s in time range, trying fallback", key));
                archiveData = fetchFallbackArchiveData(parsed.bucket, parsed.pvName, row);
                usedFallback = !archiveData.isEmpty();

                if (usedFallback) {
                    logger.log(Level.INFO, String.format("Fallback retrieved %d points for %s",
                        archiveData.size(), key));
                }
            }

            final boolean finalUsedFallback = usedFallback;
            final List<VType> finalArchiveData = archiveData;

            Platform.runLater(() -> {
                try {
                    if (!finalArchiveData.isEmpty()) {
                        displayArchiveData(key, finalArchiveData,
                            Math.min(row.getBufferSize(), MAX_POINTS_PER_DATASET));

                        if (finalUsedFallback) {
                            updateChartTimeRangeForFallbackData(finalArchiveData);
                        }
                    }
                } finally {
                    pendingArchiveRequests.decrementAndGet();
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Error fetching archive data for %s: %s",
                key, e.getMessage()), e);
            Platform.runLater(() -> {
                showArchiveError(row.getPvName(), e);
                pendingArchiveRequests.decrementAndGet();
            });
        }
    }

    private List<VType> fetchFallbackArchiveData(String bucket, String pvName, PVTableEntry row) {
        List<VType> fallbackData = new ArrayList<>();

        try (InfluxArchiveReader reader = createArchiveReader(bucket)) {
            FluxRecord lastPoint = reader.getLastPoint("influx://" + bucket + "/" + pvName);

            if (lastPoint != null && lastPoint.getTime() != null) {
                Instant lastPointTime = lastPoint.getTime();
                Instant fallbackStart = lastPointTime.minus(Duration.ofHours(1));

                fallbackData = fetchArchiveData(bucket, pvName, fallbackStart, lastPointTime,
                    row.getBufferSize(), row);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error fetching fallback data for " + pvName, e);
        }

        return fallbackData;
    }

    private void updateChartTimeRangeForFallbackData(List<VType> fallbackData) {
        if (fallbackData.isEmpty()) return;

        Instant minTime = null;
        Instant maxTime = null;

        for (VType value : fallbackData) {
            Instant timestamp = VTypeHelper.getTimestamp(value);
            if (timestamp != null) {
                if (minTime == null || timestamp.isBefore(minTime)) {
                    minTime = timestamp;
                }
                if (maxTime == null || timestamp.isAfter(maxTime)) {
                    maxTime = timestamp;
                }
            }
        }

        if (minTime != null && maxTime != null) {
            final Instant finalMinTime = minTime;
            final Instant finalMaxTime = maxTime;

            Platform.runLater(() -> {
                DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();
                double startSeconds = finalMinTime.toEpochMilli() / 1000.0;
                double endSeconds = finalMaxTime.toEpochMilli() / 1000.0;

                double margin = (endSeconds - startSeconds) * 0.05;

                xAxis.setAutoRanging(false);
                xAxis.setMin(startSeconds - margin);
                xAxis.setMax(endSeconds + margin);
            });
        }
    }

    private List<VType> fetchArchiveData(String bucket, String pv, Instant start,
        Instant end, int points, PVTableEntry row) {
        List<VType> data = new ArrayList<>();

        try (InfluxArchiveReader reader = createArchiveReader(bucket)) {
            ValueIterator it = createValueIterator(reader, bucket + "/" + pv, start, end, points, row);

            System.out.println("aaa");

            int maxPointsToFetch = Math.min(points, row.getBufferSize());
            int fetchedCount = 0;

            while (it.hasNext() && fetchedCount < maxPointsToFetch) {
                VType value = it.next();
                if (value instanceof VNumber || value instanceof VString) {
                    data.add(value);
                    fetchedCount++;
                }
            }
        } catch (Exception ex) {
            showArchiveError(pv, ex);
        }

        return data;
    }

    private ValueIterator createValueIterator(InfluxArchiveReader reader, String pv,
        Instant start, Instant end, int points,
        PVTableEntry row) throws Exception {

        FluxRecord lastPoint = reader.getLastPoint(pv);

        if (lastPoint != null && lastPoint.getValue() instanceof String) {
            return reader.getRawValues(pv, start, end);
        }

        if (row.isUseRawData()) {
            String mean = row.getMeanValue();
            return mean.isEmpty() ?
                reader.getRawValues(pv, start, end) :
                reader.getRawValues(pv, start, end, mean);
        } else {
            return reader.getOptimizedValues(pv, start, end, points);
        }
    }

    private void displayArchiveData(String key, List<VType> data, int bufferSize) {
        if (data.isEmpty()) {
            logger.log(Level.WARNING, String.format("No data to display for %s", key));
            return;
        }

        if (data.get(0) instanceof VString) {
            clearMarkersForPV(key);
            data.forEach(point -> handleStringValue(key, (VString) point));
            return;
        }

        DoubleDataSet ds = getOrCreateDataSet(key);

        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        List<Long> ts = new ArrayList<>();

        int maxPoints = Math.min(data.size(), bufferSize);
        extractDataPoints(data, maxPoints, xs, ys, ts);

        if (!xs.isEmpty()) {
            updateDataSet(ds, xs, ys, ts, key);
            scheduleChartUpdate();
        } else {
            logger.log(Level.WARNING, String.format("No valid data points extracted for %s", key));
        }
    }

    private void clearMarkersForPV(String pvName) {
        List<XValueIndicator> markersList = markers.get(pvName);
        if (markersList != null) {
            markersList.forEach(marker -> chart.getPlugins().remove(marker));
            markersList.clear();
        }
    }

    private DoubleDataSet getOrCreateDataSet(String key) {
        return dataSets.computeIfAbsent(key, k -> {
            DoubleDataSet ds = new DoubleDataSet(k);
            timeStamps.put(k, new ArrayList<>());
            chart.getDatasets().add(ds);
            return ds;
        });
    }

    private void extractDataPoints(List<VType> data, int maxPoints,
        List<Double> xs, List<Double> ys, List<Long> ts) {
        for (int i = 0; i < maxPoints; i++) {
            VType value = data.get(i);
            if (value instanceof VNumber num) {
                Instant t = VTypeHelper.getTimestamp(value);
                xs.add(t.toEpochMilli() / 1_000d);
                ys.add(num.getValue().doubleValue());
                ts.add(t.toEpochMilli());
            }
        }
    }

    private void updateDataSet(DoubleDataSet ds, List<Double> xs, List<Double> ys,
        List<Long> ts, String key) {
        if (xs.size() > MAX_POINTS_PER_DATASET) {
            decimateData(xs, ys, ts);
        }

        ds.clearData();
        ds.add(toDoubleArray(xs), toDoubleArray(ys));

        List<Long> timestamps = timeStamps.get(key);
        timestamps.clear();
        timestamps.addAll(ts);
    }

    private void decimateData(List<Double> xs, List<Double> ys, List<Long> ts) {
        int decimationFactor = xs.size() / MAX_POINTS_PER_DATASET;
        List<Double> decimatedXs = new ArrayList<>();
        List<Double> decimatedYs = new ArrayList<>();
        List<Long> decimatedTs = new ArrayList<>();

        for (int i = 0; i < xs.size(); i += decimationFactor) {
            decimatedXs.add(xs.get(i));
            decimatedYs.add(ys.get(i));
            decimatedTs.add(ts.get(i));
        }

        xs.clear();
        xs.addAll(decimatedXs);
        ys.clear();
        ys.addAll(decimatedYs);
        ts.clear();
        ts.addAll(decimatedTs);
    }

    private double[] toDoubleArray(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    @FXML
    public void refreshStatistics() {
        archiveDataExecutor.submit(() -> {
            try {
                List<StatisticsTableEntry> newStats = new ArrayList<>();

                pvList.stream()
                    .map(PVTableEntry::getPvName)
                    .flatMap(pv -> buildStatsFromDataset(pv).stream())
                    .forEach(newStats::add);

                pvList.stream()
                    .filter(PVTableEntry::isUseArchive)
                    .flatMap(entry -> buildStatsFromArchive(entry).stream())
                    .forEach(newStats::add);

                dataSets.keySet().stream()
                    .filter(key -> key.startsWith(ARCHIVED_PREFIX))
                    .filter(key -> newStats.stream().noneMatch(s -> s.getPvName().equals(key)))
                    .flatMap(key -> buildStatsFromDataset(key).stream())
                    .forEach(newStats::add);

                Platform.runLater(() -> {
                    statsList.clear();
                    statsList.addAll(newStats);
                });

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error calculating statistics", e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Statistics Error");
                    alert.setHeaderText("Error calculating statistics");
                    alert.setContentText("Unable to calculate statistics: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    private Optional<StatisticsTableEntry> buildStatsFromDataset(String key) {
        DoubleDataSet ds = dataSets.get(key);
        if (ds == null || ds.getDataCount() == 0) {
            return Optional.empty();
        }

        double[] values = IntStream.range(0, ds.getDataCount())
            .mapToDouble(i -> ds.get(1, i))
            .toArray();

        return Optional.of(createStatisticsEntry(key, values));
    }

    private Optional<StatisticsTableEntry> buildStatsFromArchive(PVTableEntry row) {
        ParsedPVName parsed = parsePVName(row.getPvName());
        if (parsed == null) return Optional.empty();

        String pvNameForStats = row.getPvName();
        Instant start = getStartInstant();
        Instant end = getEndInstant();

        try (InfluxArchiveReader reader = createArchiveReader(parsed.bucket)) {
            Map<String, Double> stats = reader.getStatistics(pvNameForStats, start, end);

            if (stats == null || stats.getOrDefault("count", 0d) == 0d) {
                return Optional.empty();
            }

            String key = ARCHIVED_PREFIX + row.getPvName();
            return Optional.of(createStatisticsEntryFromMap(key, stats));

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error calculating statistics for " + row.getPvName(), ex);
            return Optional.empty();
        }
    }

    private StatisticsTableEntry createStatisticsEntry(String key, double[] values) {
        DoubleSummaryStatistics stats = Arrays.stream(values).summaryStatistics();

        StatisticsTableEntry entry = new StatisticsTableEntry(key);
        entry.setSampleCount((int) stats.getCount());
        entry.setSum(stats.getSum());
        entry.setMinValue(stats.getMin());
        entry.setMaxValue(stats.getMax());
        entry.setMean(stats.getAverage());
        entry.setStdDev(calculateStdDev(values, stats.getAverage()));
        entry.setMedian(calculateMedian(values));

        return entry;
    }

    private StatisticsTableEntry createStatisticsEntryFromMap(String key, Map<String, Double> stats) {
        StatisticsTableEntry entry = new StatisticsTableEntry(key);
        entry.setSampleCount(stats.get("count").intValue());
        entry.setSum(stats.get("sum"));
        entry.setMean(stats.get("mean"));
        entry.setStdDev(stats.get("stdDev"));
        entry.setMinValue(stats.get("min"));
        entry.setMaxValue(stats.get("max"));
        entry.setMedian(stats.get("median"));
        return entry;
    }

    private double calculateStdDev(double[] values, double mean) {
        double variance = Arrays.stream(values)
            .map(v -> (v - mean) * (v - mean))
            .sum() / values.length;
        return Math.sqrt(variance);
    }

    private double calculateMedian(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int n = sorted.length;
        return n % 2 == 0 ?
            (sorted[n/2 - 1] + sorted[n/2]) / 2 :
            sorted[n/2];
    }

    private void toggleCrosshair() {
        if (toggleCrosshair.isSelected()) {
            chart.getPlugins().add(crosshairIndicator);
        } else {
            chart.getPlugins().remove(crosshairIndicator);
        }
    }

    private CrosshairIndicator createCrosshairIndicator() {
        return new CrosshairIndicator() {
            @Override
            protected String formatData(final Chart chart, final Tuple<Number, Number> data) {
                double xMouse = data.getXValue().doubleValue();

                DataPoint closest = findClosestDataPoint(xMouse);

                if (closest != null) {
                    String ts = formatTimestamp(closest.x);
                    return String.format(Locale.US, "Value: %.6f%nTime: %s", closest.y, ts);
                }
                return "";
            }
        };
    }

    private DataPoint findClosestDataPoint(double xMouse) {
        double minDelta = Double.MAX_VALUE;
        DataPoint closest = null;

        for (DoubleDataSet ds : dataSets.values()) {
            for (int i = 0; i < ds.getDataCount(); i++) {
                double xVal = ds.get(0, i);
                double delta = Math.abs(xVal - xMouse);

                if (delta < minDelta) {
                    minDelta = delta;
                    closest = new DataPoint(xVal, ds.get(1, i));
                }
            }
        }

        return closest;
    }

    private String formatTimestamp(double epochSeconds) {
        long secPart = (long) epochSeconds;
        long nanoPart = Math.round((epochSeconds - secPart) * 1_000_000_000);
        Instant instant = Instant.ofEpochSecond(secPart, nanoPart);
        return TIMESTAMP_FORMAT.format(instant.atZone(UTC_ZONE));
    }

    @FXML
    private void onSavePlt() {
        FileChooser chooser = createFileChooser("Save chartbrowser");
        File file = chooser.showSaveDialog(chartContainer.getScene().getWindow());

        if (file != null) {
            try {
                ChartBrowserPersistence.save(this, file);
            } catch (Exception ex) {
                showError("Error while saving file", ex);
            }
        }
    }

    @FXML
    private void onLoadPlt() {
        FileChooser chooser = createFileChooser("Load ChartBrowser");
        File file = chooser.showOpenDialog(chartContainer.getScene().getWindow());

        if (file != null) {
            try {
                ChartBrowserPersistence.load(this, file);
            } catch (Exception ex) {
                showError("Error while loading plt file", ex);
            }
        }
    }

    private FileChooser createFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("ChartBrowser (*.plt)", "*.plt")
        );
        return chooser;
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void showArchiveError(String pv, Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Archive Data Error");
            alert.setHeaderText("Error retrieving archive data for " + pv);
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        });
    }

    private void setupZoomListener() {
        if (!zoomListenerAdded && !chart.getPlugins().isEmpty() && chart.getPlugins().get(0) instanceof Zoomer zoomer) {
            zoomListenerAdded = true;

            zoomer.setZoomScrollFilter(scrollEvent -> {
                scheduleZoomUpdate();
                return true;
            });

            zoomer.zoomStackDeque().addListener(
                (ListChangeListener<Map<Axis, Zoomer.ZoomState>>) change -> scheduleZoomUpdate());

            DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();

            xAxis.minProperty().addListener((obs, oldVal, newVal) -> {
                if (!isZoomRefreshing.get()) {
                    scheduleZoomUpdate();
                }
            });

            xAxis.maxProperty().addListener((obs, oldVal, newVal) -> {
                if (!isZoomRefreshing.get()) {
                    scheduleZoomUpdate();
                }
            });
        }
    }

    private void scheduleZoomUpdate() {
        synchronized (zoomUpdateLock) {
            zoomUpdatePending = true;

            if (zoomDebounceTimer != null) {
                zoomDebounceTimer.cancel();
            }

            zoomDebounceTimer = new Timer("zoom-debounce", true);
            zoomDebounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (zoomUpdateLock) {
                        if (zoomUpdatePending) {
                            zoomUpdatePending = false;
                            updateDataForZoomLevel();
                        }
                    }
                }
            }, ZOOM_DEBOUNCE_MS);
        }
    }

    private void updateDataForZoomLevel() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastZoomUpdateTime < ZOOM_DEBOUNCE_MS) {
            return;
        }
        lastZoomUpdateTime = currentTime;

        if (!isZoomRefreshing.compareAndSet(false, true)) {
            return;
        }

        try {
            synchronized (zoomLock) {
                updateDataForZoomLevelInternal();
            }
        } finally {
            isZoomRefreshing.set(false);
        }
    }

    private void updateDataForZoomLevelInternal() {
        List<PVTableEntry> archivedPVs = pvList.stream()
            .filter(PVTableEntry::isUseArchive)
            .toList();

        if (archivedPVs.isEmpty()) {
            return;
        }

        if (pendingArchiveRequests.get() > 3) {
            scheduleTask("zoom-retry", 200, this::updateDataForZoomLevel);
            return;
        }

        DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();
        double zoomMin = xAxis.getMin();
        double zoomMax = xAxis.getMax();

        if (Double.isNaN(zoomMin) || Double.isNaN(zoomMax)) {
            return;
        }

        if (Math.abs(zoomMax - zoomMin) < 1e-6) {
            return;
        }

        Instant zoomStart = toInstant(Math.min(zoomMin, zoomMax));
        Instant zoomEnd = toInstant(Math.max(zoomMin, zoomMax));

        if (isSimilarTimeRange(zoomStart, zoomEnd)) {
            return;
        }

        updateLastTimeRange(zoomStart, zoomEnd);

        processArchivedPVsSequentially(archivedPVs, zoomStart, zoomEnd);
    }

    private Instant lastRangeStart = null;
    private Instant lastRangeEnd = null;

    private boolean isSimilarTimeRange(Instant start, Instant end) {
        if (lastRangeStart == null || lastRangeEnd == null) {
            return false;
        }

        Duration rangeDuration = Duration.between(start, end);
        Duration threshold = rangeDuration.dividedBy(20);

        return Math.abs(Duration.between(lastRangeStart, start).toMillis()) < threshold.toMillis() &&
            Math.abs(Duration.between(lastRangeEnd, end).toMillis()) < threshold.toMillis();
    }

    private void updateLastTimeRange(Instant start, Instant end) {
        lastRangeStart = start;
        lastRangeEnd = end;
    }

    private void processArchivedPVsSequentially(List<PVTableEntry> pvs, Instant start, Instant end) {
        if (pvs.isEmpty() || isShuttingDown) {
            return;
        }

        PVTableEntry firstPV = pvs.get(0);
        int pointCount = calculateZoomPointCount(firstPV, start, end);
        processZoomUpdate(firstPV, start, end, pointCount);

        if (pvs.size() > 1) {
            List<PVTableEntry> remaining = pvs.subList(1, pvs.size());
            scheduleTask("sequential-pv-update", 50, () ->
                processArchivedPVsSequentially(remaining, start, end));
        }
    }

    private void processZoomUpdate(PVTableEntry entry, Instant start, Instant end, int pointCount) {
        ParsedPVName parsed = parsePVName(entry.getPvName());
        if (parsed == null) {
            logger.log(Level.WARNING, "Could not parse PV name: " + entry.getPvName());
            return;
        }

        String archivedKey = buildArchiveKey(entry.getPvName());

        if (isDataRangeSufficient(archivedKey, start, end)) {
            logger.log(Level.FINE, "Data range already sufficient for " + archivedKey);
            return;
        }

        pendingArchiveRequests.incrementAndGet();

        archiveDataExecutor.submit(() -> {
            try {
                int adjustedPointCount = Math.max(pointCount, 100);

                List<VType> zoomData = fetchArchiveData(
                    parsed.bucket, parsed.pvName, start, end, adjustedPointCount, entry);

                Platform.runLater(() -> {
                    try {
                        mergeArchiveData(archivedKey, zoomData, Math.min(entry.getBufferSize(), MAX_POINTS_PER_DATASET));
                        chart.requestLayout();
                    } finally {
                        pendingArchiveRequests.decrementAndGet();
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error updating zoom data for PV: " + entry.getPvName(), e);
                pendingArchiveRequests.decrementAndGet();
            }
        });
    }

    private boolean isDataRangeSufficient(String key, Instant start, Instant end) {
        DoubleDataSet ds = dataSets.get(key);
        if (ds == null || ds.getDataCount() == 0) {
            return false;
        }

        double startSeconds = start.toEpochMilli() / 1000.0;
        double endSeconds = end.toEpochMilli() / 1000.0;

        double firstTime = ds.get(0, 0);
        double lastTime = ds.get(0, ds.getDataCount() - 1);

        double coverage = (Math.min(lastTime, endSeconds) - Math.max(firstTime, startSeconds)) /
            (endSeconds - startSeconds);

        return coverage > 0.9;
    }

    private void mergeArchiveData(String key, List<VType> newData, int bufferSize) {
        if (newData.isEmpty()) {
            return;
        }

        DoubleDataSet ds = getOrCreateDataSet(key);
        List<Long> timestamps = timeStamps.get(key);

        List<Double> newXs = new ArrayList<>();
        List<Double> newYs = new ArrayList<>();

        for (VType value : newData) {
            if (value instanceof VNumber num) {
                Instant t = VTypeHelper.getTimestamp(value);
                double timeSeconds = t.toEpochMilli() / 1000.0;

                if (!timestamps.contains(t.toEpochMilli())) {
                    newXs.add(timeSeconds);
                    newYs.add(num.getValue().doubleValue());
                }
            }
        }

        if (!newXs.isEmpty()) {
            for (int i = 0; i < newXs.size(); i++) {
                ds.add(newXs.get(i), newYs.get(i));
                timestamps.add((long)(newXs.get(i) * 1000));
            }

            sortDataSet(ds, timestamps);

            while (ds.getDataCount() > bufferSize) {
                ds.remove(0);
                if (!timestamps.isEmpty()) {
                    timestamps.remove(0);
                }
            }
        }
    }

    private void sortDataSet(DoubleDataSet ds, List<Long> timestamps) {
        if (ds.getDataCount() <= 1) return;

        double[] xs = new double[ds.getDataCount()];
        double[] ys = new double[ds.getDataCount()];

        for (int i = 0; i < ds.getDataCount(); i++) {
            xs[i] = ds.get(0, i);
            ys[i] = ds.get(1, i);
        }

        Integer[] indices = IntStream.range(0, xs.length).boxed().toArray(Integer[]::new);
        Arrays.sort(indices, Comparator.comparingDouble(i -> xs[i]));

        double[] sortedXs = new double[xs.length];
        double[] sortedYs = new double[ys.length];
        List<Long> sortedTimestamps = new ArrayList<>();

        for (int i = 0; i < indices.length; i++) {
            sortedXs[i] = xs[indices[i]];
            sortedYs[i] = ys[indices[i]];
            sortedTimestamps.add((long)(sortedXs[i] * 1000));
        }

        ds.clearData();
        ds.add(sortedXs, sortedYs);
        timestamps.clear();
        timestamps.addAll(sortedTimestamps);
    }

    private int calculateZoomPointCount(PVTableEntry entry, Instant start, Instant end) {
        Duration timeRange = Duration.between(start, end);
        long totalSeconds = timeRange.getSeconds();

        if (entry.isUseRawData()) {
            return Math.min(entry.getBufferSize() * 3, MAX_POINTS_PER_DATASET);
        } else {
            int archivedPVCount = Math.max(1, (int) pvList.stream()
                .filter(PVTableEntry::isUseArchive)
                .count());

            int basePoints;
            if (totalSeconds < 60) {
                basePoints = Math.min(10000, (int) totalSeconds * 10);
            } else if (totalSeconds < 3600) {
                basePoints = Math.min(5000, (int) totalSeconds);
            } else if (totalSeconds < 86400) {
                basePoints = 3000;
            } else if (totalSeconds < 604800) {
                basePoints = 1500;
            } else {
                basePoints = 1000;
            }

            return Math.max(200, Math.min(basePoints, MAX_TOTAL_POINTS / archivedPVCount));
        }
    }

    private void scheduleLiveDataResume(List<PVTableEntry> archived) {
        scheduleTask("liveResume", 300, () -> Platform.runLater(() -> archived.stream()
            .filter(pv -> !pv.isUseArchive())
            .forEach(this::removeArchivedData)));
    }

    public void removePV(String pvName) {
        pvList.removeIf(entry -> entry.getPvName().equals(pvName));

        cleanupPV(pvName);

        String archivedKey = buildArchiveKey(pvName);
        cleanupPV(archivedKey);

        List<XValueIndicator> markersList = markers.get(pvName);

        if (markersList != null) {
            markersList.forEach((marker) -> chart.getPlugins().remove(marker));
            markers.remove(pvName);
        }
    }

    private void cleanupPV(String key) {
        Optional.ofNullable(pvSubscriptions.remove(key))
            .ifPresent(Disposable::dispose);

        Optional.ofNullable(dataSets.remove(key))
            .ifPresent(ds -> chart.getDatasets().remove(ds));

        timeStamps.remove(key);

        Optional.ofNullable(pvInstances.remove(key))
            .ifPresent(PVPool::releasePV);
    }

    public void clearAllPVs() {
        new ArrayList<>(pvTable.getItems()).forEach(item -> removePV(item.getPvName()));
        pvTable.refresh();
    }

    private void startStatisticsTimer() {
        if (statsTimerRunning.getAndSet(true)) return;

        archiveDataExecutor.submit(this::refreshStatistics);

        scheduleRepeatingTask("stats", STATS_UPDATE_INTERVAL_MS, STATS_UPDATE_INTERVAL_MS,
            this::refreshStatistics);
    }

    private void stopStatisticsTimer() {
        if (!statsTimerRunning.getAndSet(false)) return;
        cancelTask("stats");
    }

    private void scheduleChartUpdate() {
        scheduleTask("chartUpdate", 100, () -> Platform.runLater(() -> {
            if (!isShuttingDown) {
                chart.requestLayout();
            }
        }));
    }

    private void scheduleTask(String name, long delay, Runnable task) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastTaskScheduled.get(name);

        if (lastTime != null && (currentTime - lastTime) < delay / 2) {
            return;
        }

        lastTaskScheduled.put(name, currentTime);
        cancelTask(name);

        Timer timer = new Timer(name, true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, delay);

        timers.put(name, timer);
    }

    private void scheduleRepeatingTask(String name, long delay, long period, Runnable task) {
        cancelTask(name);

        Timer timer = new Timer(name, true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, delay, period);

        timers.put(name, timer);
    }

    private void cancelTask(String name) {
        Timer timer = timers.remove(name);
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    /**
     * Loads a .plt file programmatically
     * @param file the .plt file to load
     */
    public void loadPltFile(File file) {
        try {
            ChartBrowserPersistence.load(this, file);
        } catch (Exception ex) {
            showError("Error while loading plt file", ex);
        }
    }

    /**
     * Sets the time range for the chart
     * @param start start time
     * @param end end time
     */
    public void setTimeRange(Instant start, Instant end) {
        Platform.runLater(() -> {
            DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();
            double startSeconds = start.toEpochMilli() / 1000.0;
            double endSeconds = end.toEpochMilli() / 1000.0;

            xAxis.setAutoRanging(false);
            xAxis.setMin(startSeconds);
            xAxis.setMax(endSeconds);

            if (pvList.stream().anyMatch(PVTableEntry::isUseArchive)) {
                scheduleTask("timeRangeUpdate", 500, this::updateDataForZoomLevel);
            }
        });
    }

    public void shutdown() {
        isShuttingDown = true;

        if (zoomDebounceTimer != null) {
            zoomDebounceTimer.cancel();
        }

        archiveDataExecutor.shutdown();
        try {
            if (!archiveDataExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                archiveDataExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            archiveDataExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        timers.values().forEach(Timer::cancel);
        timers.clear();

        synchronized (zoomLock) {
            pvSubscriptions.values().forEach(Disposable::dispose);
            pvSubscriptions.clear();

            dataSets.values().forEach(ds -> chart.getDatasets().remove(ds));
            dataSets.clear();

            pvList.clear();
            timeStamps.clear();
            statsList.clear();
        }
    }

    public String getPVName() {
        return txtPVName.getText();
    }

    public TextField getPVField() {
        return txtPVName;
    }

    public List<PVTableEntry> getPvTableItems() {
        return new ArrayList<>(pvTable.getItems());
    }

    public Instant getStartTime() {
        return getStartInstant();
    }

    public Instant getEndTime() {
        return getEndInstant();
    }

    private String buildArchiveKey(String pvName) {
        String key = ARCHIVED_PREFIX + pvName;
        return key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
    }

    private Instant getStartInstant() {
        return toInstant(chart.getXAxis().getMin());
    }

    private Instant getEndInstant() {
        return toInstant(chart.getXAxis().getMax());
    }

    private Instant toInstant(double epochSeconds) {
        long secPart = (long) epochSeconds;
        long nanoPart = (long) ((epochSeconds % 1) * 1_000_000_000);
        return Instant.ofEpochSecond(secPart, nanoPart);
    }

    private double toEpochSeconds(ZonedDateTime zdt) {
        return zdt.toEpochSecond() + zdt.getNano() / 1_000_000_000d;
    }

    private InfluxArchiveReader createArchiveReader(String bucket) {
        String url = "influx://" + InfluxDB_Preferences.getHost() + ":" + InfluxDB_Preferences.getPort();

        if (bucket != null && !bucket.trim().isEmpty()) {
            return new InfluxArchiveReader(url, bucket);
        } else {
            return new InfluxArchiveReader(url);
        }
    }

    /**
     * Parses a PV name to extract bucket and measurement information.
     * Supports PV name formats with mandatory bucket:
     * - influx2://bucket/measurement (specific bucket, no specific field)
     * - influx2://bucket/measurement/field (specific bucket, specific field)
     *
     * @param pvName the PV name to parse
     * @return ParsedPVName containing bucket and measurement, or null if parsing fails
     */
    private ParsedPVName parsePVName(String pvName) {
        if (pvName == null || pvName.trim().isEmpty()) {
            return null;
        }

        String cleanName = pvName;
        if (pvName.startsWith("influx://")) {
            cleanName = pvName.replace("influx://", "");
        } else if (pvName.startsWith("influx2://")) {
            cleanName = pvName.replace("influx2://", "");
        }

        String[] parts = cleanName.split("/");

        if (parts.length < 2) {
            logger.log(Level.WARNING, "Invalid InfluxDB PV format (missing bucket): " + pvName);
            return null;
        }

        String resolvedBucket = parts[0];
        String resolvedMeasurement;
        String resolvedField = null;

        try {
            switch (parts.length) {
                case 2:
                    resolvedMeasurement = parts[1];
                    break;
                case 3:
                    resolvedMeasurement = parts[1];
                    resolvedField = Objects.equals(parts[2], "null") ? null : parts[2];
                    break;
                default:
                    logger.log(Level.WARNING, "Invalid InfluxDB PV format (too many parts): " + pvName);
                    return null;
            }

            String finalPvName = resolvedMeasurement;
            if (resolvedField != null) {
                finalPvName += "/" + resolvedField;
            }

            return new ParsedPVName(resolvedBucket, finalPvName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error parsing PV name: " + pvName, e);
            return null;
        }
    }


    public void addPV(String name, boolean useArchive, boolean useRawData, int bufferSize) {
        if (pvExists(name)) return;

        try {
            createPVEntry(name, useArchive, useRawData, bufferSize);
            pvTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            removePV(name);
        }
    }

    private record DataPoint(double x, double y) {

    }

    private record ParsedPVName(String bucket, String pvName) {
        String getFullPath() {
            return (!bucket.isEmpty() ? bucket + "/" : "") + pvName;
        }
    }

    private class TimeAxisFormatter extends StringConverter<Number> {
        @Override
        public String toString(Number epochSeconds) {
            double secs = epochSeconds.doubleValue();
            return formatTimestamp(secs);
        }

        @Override
        public Number fromString(String string) {
            return null;
        }
    }
}
