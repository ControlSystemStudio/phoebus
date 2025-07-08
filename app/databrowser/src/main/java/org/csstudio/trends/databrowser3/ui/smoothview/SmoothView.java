package org.csstudio.trends.databrowser3.ui.smoothview;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import org.csstudio.javafx.rtplot.*;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataSearch;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;
import org.csstudio.javafx.rtplot.internal.MouseMode;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.*;
import org.csstudio.trends.databrowser3.ui.ToggleToolbarMenuItem;
import org.csstudio.trends.databrowser3.ui.smoothview.filter.BidirectionalFillerOutliers;
import org.csstudio.trends.databrowser3.ui.smoothview.filter.FilterAlgorithm;
import org.csstudio.trends.databrowser3.ui.smoothview.filter.FilterAlgorithms;
import org.csstudio.trends.databrowser3.ui.smoothview.filter.MovingStandardDeviation;
import org.epics.vtype.VString;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Panel for smoothing samples of a trace by applying various filters. Allows
 * users to select a data item, apply different filtering algorithms, and
 * visualize both the filtered signal and noise components.
 *
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
@SuppressWarnings("nls")
public class SmoothView extends VBox {
    private static final int MAX_WINDOW_SIZE = 1000;
    final CheckBox periodicCheckBox = new CheckBox();
    private final Model model;
    private final ComboBox<String> itemComboBox = new ComboBox<>();
    private final ComboBox<String> algorithmComboBox = new ComboBox<>();
    private final CheckBox showNoiseCheckBox = new CheckBox("Show Residual Signal");
    private final Label sampleCountLabel = new Label(Messages.SampleView_Count);
    private final RTTimePlot plot = new RTTimePlot(true);
    private final SmoothData filteredData = new SmoothData();
    private final SmoothData noiseData = new SmoothData();
    private final DoubleProperty lowerLimit = new SimpleDoubleProperty(Double.NaN);
    private final DoubleProperty upperLimit = new SimpleDoubleProperty(Double.NaN);
    private final ExecutorService backgroundExecutor;
    private ObservableList<PlotSample> samples;

    private FilterAlgorithm algo;
    private ScheduledExecutorService executorService;
    private int windowWidth = 1;
    private final Slider windowSizeSlider = new Slider(1, MAX_WINDOW_SIZE, windowWidth);
    private String selectedItemName = null;

    private final ModelListener modelListener = new ModelListener() {
        private PauseTransition pauseTransition = new PauseTransition(Duration.seconds(2));

        {
            pauseTransition.setOnFinished(event -> {
                if (selectedItemName != null) {
                    Platform.runLater(() -> {
                        try {
                            fetchAndProcessSamples(sample ->
                                    modifyLimit(sample));
                            processAndUpdatePlot(samples);
                        } catch (Exception e) {
                            Activator.logger.log(Level.SEVERE, "Refresh failed ", e);
                        }
                    });
                }
            });
        }

        @Override
        public void itemAdded(final ModelItem item) {
            updateAvailableItems();
        }

        @Override
        public void itemRemoved(final ModelItem item) {
            updateAvailableItems();
        }

        @Override
        public void changedItemLook(final ModelItem item) {
            updateAvailableItems();
        }

        @Override
        public void changedAnnotations() {
            // Not directly relevant to this view
        }

        @Override
        public void changedTimerange() {
            pauseTransition.stop();
            pauseTransition.play();
        }
    };
    private TraceType DEFAULT_TRACE_TYPE = TraceType.LINES_DIRECT;
    private boolean isRefreshing = false;
    private long refreshIntervalMs = 1000;
    private boolean isFirstRefreshingExecution = true;

    /**
     * Initialize the smoothing view panel with the provided model.
     *
     * @param model The data model containing plot items
     */
    public SmoothView(final Model model) {
        this.model = model;
        model.addListener(modelListener);
        backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SmoothView-Background");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((t1, e) -> {
                Activator.logger.log(Level.SEVERE, "Background thread error", e);
            });
            return t;
        });
        // Setup UI components
        initializeUI();

        // Load initial data
        updateAvailableItems();
//        fetchAndProcessSamples();
        setupWindowCloseHandler();


    }


    /**
     * Initialize UI components and layout
     */
    private void initializeUI() {
        createTopRow();
        createMiddleRow();
        createBottomRow();
        createPlot();
    }

    /**
     * Creates the top control panel row.
     * Contains item selection dropdown, valid data range input fields, refresh controls, and periodic refresh checkbox.
     */
    private void createTopRow() {
        // Top row: Item selection
        final Label itemLabel = new Label(Messages.SampleView_Item);
        final Button refreshButton = new Button("Refresh Once");
        refreshButton.setTooltip(new Tooltip("Trigger a single data refresh"));
        refreshButton.setOnAction(event -> Platform.runLater(() -> {
            fetchAndProcessSamples(samples -> {
                modifyLimit(samples);

            });
        }));


        periodicCheckBox.setText("Periodic Refresh: OFF");
        periodicCheckBox.setTooltip(new Tooltip("Enable/disable automatic periodic refreshing"));

        refreshButton.disableProperty().bind(periodicCheckBox.selectedProperty());
        periodicCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            periodicCheckBox.setText(isSelected ? "Periodic Refresh: ON" : "Periodic Refresh: OFF");
            if (isSelected) {
                startRefresh();
            } else {
                stopRefresh();
            }
        });

        itemComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(itemComboBox, Priority.ALWAYS);
        itemComboBox.prefHeightProperty().bind(refreshButton.heightProperty());
        itemComboBox.setOnAction(event -> {
            onItemSelected(itemComboBox.getValue());
        });
        final Label validValueRangeLabel = new Label("Valid Data Range: ");
        validValueRangeLabel.prefWidthProperty().bind(this.widthProperty().divide(14));

        validValueRangeLabel.setPadding(new Insets(5));
        StringConverter<Number> converter = new StringConverter<Number>() {
            private final DoubleStringConverter doubleConverter = new DoubleStringConverter();

            @Override
            public String toString(Number object) {
                return doubleConverter.toString(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                try {
                    double value = doubleConverter.fromString(string);
                    return value;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        };
        TextField lowerlimitTextField = new TextField();
        lowerlimitTextField.textProperty().bindBidirectional(lowerLimit, converter);
        lowerlimitTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isValidNumber(newValue))
                processAndUpdatePlot(samples);
            else
                lowerlimitTextField.setText(oldValue); // Rollback to previous valid value
        });
        Label hyphenLabel = new Label("-");
        TextField upperlimitTextField = new TextField();
        upperlimitTextField.textProperty().bindBidirectional(upperLimit, converter);
        upperlimitTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isValidNumber(newValue))
                processAndUpdatePlot(samples);
            else
                upperlimitTextField.setText(oldValue); // Rollback to previous valid value
        });


        final HBox topRow = new HBox(5, itemLabel, itemComboBox, validValueRangeLabel,
                lowerlimitTextField, hyphenLabel, upperlimitTextField, refreshButton, periodicCheckBox);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(5));
        getChildren().add(topRow);
    }

    /**
     * Creates the middle control panel row.
     * Contains sample count display, smoothing window width slider, algorithm selection dropdown, and noise
     * visibility toggle.
     */
    private void createMiddleRow() {

        // Middle row: Controls for window size, algorithm, and noise visibility
        sampleCountLabel.prefWidthProperty().bind(this.widthProperty().divide(12));
//        sampleCountLabel.setPadding(new Insets(5));
        final Label windowWidthLabel = new Label("Smoothing Window Width: " + windowWidth);
        windowWidthLabel.setPadding(new Insets(5));
        windowWidthLabel.prefWidthProperty().bind(this.widthProperty().divide(9));

        windowSizeSlider.setPadding(new Insets(5));
        windowSizeSlider.prefWidthProperty().bind(this.widthProperty().divide(8));
        HBox.setHgrow(windowSizeSlider, Priority.ALWAYS);
        windowSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            windowWidth = newVal.intValue();
            windowWidthLabel.setText("Smoothing Window Width: " + windowWidth);
        });
        windowSizeSlider.setOnMouseReleased(event -> {
            if (selectedItemName != null) {
                processAndUpdatePlot(samples);
            }
        });
        final double SMALL_STEP = 1.0;
        final double LARGE_STEP = 10.0;
        final double MAX_STEP_MULTIPLIER = 5.0;
        final PauseTransition updatePlotTransition
                = new PauseTransition(Duration.millis(500));
        updatePlotTransition.setOnFinished(event -> {
            if (selectedItemName != null && samples != null) {
                processAndUpdatePlot(samples);
            }
        });
        windowSizeSlider.setOnScroll(event -> {
            double currentValue = windowSizeSlider.getValue();
            double deltaY = event.getDeltaY();
            double step = (currentValue < 100) ? SMALL_STEP : LARGE_STEP;
            if (event.isShiftDown()) {
                step *= MAX_STEP_MULTIPLIER;
            }
            double newValue = (deltaY > 0)
                    ? Math.min(currentValue + step, windowSizeSlider.getMax())
                    : Math.max(currentValue - step, windowSizeSlider.getMin());


            windowSizeSlider.setValue(newValue);
            windowWidthLabel.setText("Smoothing Window Width: " + (int) newValue);

            updatePlotTransition.stop();
            updatePlotTransition.play();


            event.consume();
        });


        final Label algorithmLabel = new Label("Smoothing Algorithm: ");
        algorithmLabel.setPadding(new Insets(5));
        algorithmLabel.prefWidthProperty().bind(this.widthProperty().divide(12));

        String[] algorithms = FilterAlgorithms.getAllDisplayNames();
        algorithmComboBox.getItems().setAll(algorithms);

        algorithmComboBox.prefWidthProperty().bind(this.widthProperty().divide(10));
        algorithmComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                algo = FilterAlgorithms.getAlgorithmInstance(newVal);
                if (algo == null) {
                    Activator.logger.log(Level.SEVERE, "Invalid Algorithm: " + newVal);
                    return;
                }
                setNoiseVisible(showNoiseCheckBox.isSelected());
                if (selectedItemName != null) {
                    Platform.runLater(() -> {
                        processAndUpdatePlot(samples);
                    });
                }
            }
        });
        algorithmComboBox.getSelectionModel().select(0);

        Tooltip showNoiseTooltip = getTooltip();
        showNoiseCheckBox.setTooltip(showNoiseTooltip);
        showNoiseCheckBox.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String selectedAlgorithm = algorithmComboBox.getValue();
            long windowWidth = windowSizeSlider.valueProperty().longValue();
            return selectedAlgorithm == null || selectedAlgorithm.contains("Standard Deviation") || windowWidth < 2;
        }, algorithmComboBox.valueProperty(), windowSizeSlider.valueProperty()));
        showNoiseCheckBox.setSelected(false);
        showNoiseCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            setNoiseVisible(newVal);
        });

        final HBox middleRow = new HBox(5, sampleCountLabel, algorithmLabel, algorithmComboBox, windowWidthLabel,
                windowSizeSlider, showNoiseCheckBox);
        middleRow.setAlignment(Pos.CENTER_LEFT);
        middleRow.setPadding(new Insets(5));
        getChildren().add(middleRow);
    }

    /**
     * Creates the bottom control panel row.
     * Contains trace type selection dropdown for configuring plot visualization.
     */
    private void createBottomRow() {
        final Label traceTypeLabel = new Label("Trace Type: ");// Trace Type selection
        traceTypeLabel.setPadding(new Insets(5));

        ComboBox<TraceType> traceTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(TraceType.values()));
        traceTypeComboBox.getSelectionModel().selectFirst();
        traceTypeComboBox.prefWidthProperty().bind(this.widthProperty().divide(16));
        traceTypeComboBox.prefHeightProperty().bind(itemComboBox.heightProperty());
        traceTypeComboBox.setOnAction(event -> {
            DEFAULT_TRACE_TYPE = traceTypeComboBox.getValue();
            if (DEFAULT_TRACE_TYPE != null) {
                Platform.runLater(() -> {
                    plot.getTraces().forEach(trace -> trace.setType(DEFAULT_TRACE_TYPE));
                    plot.requestUpdate();
                });
            }
        });
        traceTypeComboBox.getSelectionModel().select(TraceType.LINES_DIRECT);

        final HBox bottomRow = new HBox(5, traceTypeLabel, traceTypeComboBox);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(bottomRow);
    }

    /**
     * Initialize the RTTimePlot with appropriate settings
     */
    private void createPlot() {
        plot.showToolbar(true);
        plot.getXAxis().setGridVisible(true);
        plot.getXAxis().setName("");

        // Setup Y axes
        plot.getYAxes().get(0).setGridVisible(true);
        plot.addYAxis("Noise");
        plot.getYAxes().get(1).setOnRight(true);

        plot.setMouseMode(MouseMode.PAN);
        plot.setScrolling(false);
        plot.setPadding(new Insets(0, 5, 5, 5));
        VBox.setVgrow(plot, Priority.ALWAYS);
        getChildren().add(plot);


        plot.addTrace("Smoothing", null, filteredData, Color.RED,
                DEFAULT_TRACE_TYPE, 3, LineStyle.SOLID, PointType.NONE, 0, 0);
        plot.addTrace("Noise", null, noiseData, Color.BLACK,
                DEFAULT_TRACE_TYPE, 3, LineStyle.SOLID, PointType.NONE, 0, 1);

        createContextMenu();
    }

    private Tooltip getTooltip() {
        String showNoiseTooltipText = "Show Residual Signal\n\n" +
                "Calculated as: Original Data - Filtered Data\n\n" +
                "Useful for: \n" +
                "- Identifying noise or outliers\n" +
                "- Analyzing filtering effectiveness\n" +
                "- Detecting sudden changes not captured by the filter\n\n" +
                "Disabled when:\n" +
                "- Algorithm is Standard Deviation\n" +
                "- Window size < 2";
        Tooltip showNoiseTooltip = new Tooltip(showNoiseTooltipText);
        return showNoiseTooltip;
    }


    /**
     * Create context menu for the plot
     */
    private void createContextMenu() {
        final ContextMenu menu = new ContextMenu();
        plot.setOnContextMenuRequested(event -> {
            menu.getItems().setAll(new ToggleToolbarMenuItem(plot), new SeparatorMenuItem(), new PrintAction(plot),
                    new SaveSnapshotAction(plot));
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Sets up the window close handler to ensure resources are cleaned up properly
     * when the window is closed. This method handles both initial window setup
     * and any changes to the window (e.g., when the view is moved between windows).
     */
    private void setupWindowCloseHandler() {
        final EventHandler<WindowEvent> closeHandler = event -> {
            System.out.println("Cleaning up resources for window: " + event.getSource());
            cleanupResources();
        };
        final ChangeListener<Window> windowChangeListener = (windowObs, oldWindow, newWindow) -> {
            if (oldWindow != null)
                oldWindow.setOnCloseRequest(null);

            if (newWindow != null)
                newWindow.setOnCloseRequest(closeHandler);
        };
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.windowProperty().removeListener(windowChangeListener);
                Optional.ofNullable(oldScene.getWindow()).ifPresent(w -> w.setOnCloseRequest(null));

            }
            if (newScene != null) {
                newScene.windowProperty().addListener(windowChangeListener);
                Optional.ofNullable(newScene.getWindow()).ifPresent(w -> w.setOnCloseRequest(closeHandler));

            }
        });
    }

    /**
     * Cleans up resources when the window is closed.
     * Stops any ongoing refresh tasks, removes model listeners,
     * and clears plot data to prevent memory leaks.
     */
    private void cleanupResources() {
        stopRefresh();

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (model != null) {
            model.removeListener(modelListener);
        }
        if (plot != null) {
            plot.getTraces().forEach(plot::removeTrace);
            while (!plot.getYAxes().isEmpty())
                plot.removeYAxis(0);
            filteredData.setData(Collections.emptyList());
            noiseData.setData(Collections.emptyList());
        }
        if (samples != null)
            samples.clear();
    }


    /**
     * Handle item selection from the combo box
     *
     * @param itemName Name of the selected item
     */
    private void onItemSelected(final String itemName) {
        if (itemName == null || itemName.equals(selectedItemName)) {
            return;
        }

        try {
            this.selectedItemName = itemName;
            fetchAndProcessSamples(samples -> {
                modifyLimit(samples);

            });

            ModelItem selectedItem = model.getItem(itemName);
            Color color = selectedItem.getPaintColor();
            updateTraceColor("Smoothing", color);
            setNoiseVisible(showNoiseCheckBox.isSelected());
        } catch (Exception e) {
            Activator.logger.log(Level.SEVERE, "Error on item selection: " + itemName, e);
        }

    }

    /**
     * Update the list of available items in the combo box
     */
    private void updateAvailableItems() {
        final List<String> itemNames = model.getItems().stream()
                .map(ModelItem::getName)
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            itemComboBox.getItems().setAll(itemNames);
            if (selectedItemName != null && itemNames.contains(selectedItemName)) {
                itemComboBox.setValue(selectedItemName);
            }
        });
    }

    /**
     * Fetch samples from the model and process them in the background
     */
    public CompletableFuture<Void> fetchAndProcessSamples(Consumer<ObservableList<PlotSample>> onComplete) {
        if (selectedItemName == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
                    final ModelItem item = model.getItem(selectedItemName);
                    if (item == null)
                        return null;

                    TimeInterval timeInterval = getTimeInterval();
                    updateXAxisRange(timeInterval);
                    ObservableList<PlotSample> sampleList = fetchSamplesInTimeInterval(item, timeInterval);
                    return sampleList;
                }, backgroundExecutor)
                .thenAcceptAsync(sampleList -> {
                    if (sampleList != null && !sampleList.isEmpty()) {
                        Platform.runLater(() -> {
                            this.samples = FXCollections.observableArrayList(sampleList);
                            if (onComplete != null)
                                onComplete.accept(sampleList);
                            plot.requestUpdate();
                        });
                    }
                }, backgroundExecutor)
                .exceptionally(ex -> {
                    Activator.logger.log(Level.SEVERE, "Failed to process samples", ex);
                    return null;
                });
    }


    /**
     * Fetch samples within a specific time interval for a given model item.
     *
     * @param item         The model item to fetch samples from
     * @param timeInterval The time interval to retrieve samples for
     * @return ObservableList of PlotSample within the specified time interval
     */
    private ObservableList<PlotSample> fetchSamplesInTimeInterval(ModelItem item, TimeInterval timeInterval) {
        PlotSamples itemSamples = item.getSamples();
        int[] indices = findSampleIndices(itemSamples, timeInterval);
        ObservableList<PlotSample> result = collectValidSamples(itemSamples, indices[0], indices[1]);
        return result;
    }

    /**
     * Get the current time interval from the model's timerange.
     *
     * @return TimeInterval representing the current time range
     */
    private TimeInterval getTimeInterval() {
        TimeRelativeInterval timeRelativeInterval = model.getTimerange();
        return timeRelativeInterval.toAbsoluteInterval();
    }

    /**
     * Find the start and end indices of samples within a time interval.
     *
     * @param samples      The PlotSamples collection to search
     * @param timeInterval The time interval to find samples for
     * @return An array containing start and end indices, or null if search fails
     */
    private int[] findSampleIndices(PlotSamples samples, TimeInterval timeInterval) {
        PlotDataSearch search = new PlotDataSearch();
        boolean lockAcquired = false;
        try {
            lockAcquired = samples.getLock().tryLock(2, TimeUnit.SECONDS);
            if (!lockAcquired) {
                Activator.logger.log(Level.WARNING, "Failed to acquire sample lock within timeout");
                return null;
            }
            int startIndex = search.findSampleLessOrEqual(samples, timeInterval.getStart());
            int endIndex = search.findSampleGreaterOrEqual(samples, timeInterval.getEnd());

            if (startIndex == -1) {
                startIndex = 0;
                if (endIndex == -1) {
                    endIndex = samples.size() - 1;
                }
            }
            return new int[]{startIndex, endIndex};
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Activator.logger.log(Level.WARNING, "Interrupted while accessing samples", ex);
        } catch (Exception ex) {
            Activator.logger.log(Level.SEVERE, "Error fetching samples", ex);
        } finally {
            if (lockAcquired) {
                samples.getLock().unlock();
            }
        }
        return null;
    }

    /**
     * Collect valid samples from the specified range, excluding string-type values.
     *
     * @param samples The PlotSamples collection to process
     * @param start   The start index of the sample range
     * @param end     The end index of the sample range
     * @return ObservableList of valid PlotSample objects
     */
    private ObservableList<PlotSample> collectValidSamples(PlotSamples samples, int start, int end) {
        ObservableList<PlotSample> result = FXCollections.observableArrayList();
        List<PlotSample> tempList = new ArrayList<>();
        boolean locked = false;
        try {

            locked = samples.getLock().tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                Activator.logger.log(Level.WARNING, "Failed to acquire lock for samples");
                return result;
            }


            for (int i = start; i <= end; i++) {
                PlotSample sample = samples.get(i);
                if (!(sample.getVType() instanceof VString)) {
                    tempList.add(sample);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Activator.logger.log(Level.WARNING, "Interrupted while processing samples", e);
        } catch (Exception e) {
            Activator.logger.log(Level.SEVERE, "Error processing samples", e);
        } finally {
            if (locked) {
                samples.getLock().unlock();
            }
        }
        if (!tempList.isEmpty()) {
            result.addAll(tempList);
        }
        return result;
    }


    private void updateXAxisRange(TimeInterval timeInterval) {
        Platform.runLater(() -> plot.getXAxis().setValueRange(timeInterval.getStart(), timeInterval.getEnd()));
    }


    /**
     * Get the max/min of the fetched samples
     *
     * @param samples List of plot samples to process
     */
    private void modifyLimit(final ObservableList<PlotSample> samples) {
        if (samples.isEmpty()) {
            return;
        }
        try {
            // Extract raw data and timestamps
            double[] rawSignal = samples.stream()
                    .mapToDouble(PlotSample::getValue)
                    .toArray();

            DoubleSummaryStatistics stats = Arrays.stream(rawSignal)
                    .filter(Double::isFinite)
                    .summaryStatistics();


            if (stats.getCount() > 0) {

                double range = stats.getMax() - stats.getMin();
                double margin = range * 0.05; // 5% margin

                lowerLimit.set(stats.getMin() - margin);
                upperLimit.set(stats.getMax() + margin);
            } else {

                String itemName = selectedItemName != null ? selectedItemName : "unknown";
                Activator.logger.log(Level.WARNING, "No valid data points for range calculation in " + itemName);
            }
        } catch (Exception ex) {
            // Log detailed error information
            String itemName = selectedItemName != null ? selectedItemName : "unknown";
            Activator.logger.log(Level.SEVERE, "Error calculating data range for item: " + itemName, ex);
        }
    }


    /**
     * Process the fetched samples and update the plot
     *
     * @param samples List of plot samples to process
     */
    private void processAndUpdatePlot(final ObservableList<PlotSample> samples) {

        if (samples.isEmpty() || algo == null)
            return;
        sampleCountLabel.setText(Messages.SampleView_Count + " " + samples.size());
        try {
            ProcessedData data = processData(samples);
            updatePlotData(data);
        } catch (Exception ex) {
            Activator.logger.log(Level.SEVERE, "Error processing samples", ex);
        }

        // Request plot update
        Platform.runLater(() -> {
            plot.requestUpdate();
            plot.stagger(true);
        });


    }

    /**
     * Process raw samples to generate filtered and noise signals.
     *
     * @param samples The raw samples to process
     * @return ProcessedData containing filtered and noise data
     */
    private ProcessedData processData(ObservableList<PlotSample> samples) {
        // Extract raw data and timestamps
        int size = samples.size();
        double[] rawSignal = new double[size];
        Instant[] timestamps = new Instant[size];

        // Extract data in a single pass for efficiency
        IntStream.range(0, size)
                .parallel()
                .forEach(i -> {
                    PlotSample sample = samples.get(i);
                    rawSignal[i] = sample.getValue();
                    timestamps[i] = sample.getPosition();
                });
        double[] fillOutliers = fillOutliers(rawSignal, lowerLimit.get(), upperLimit.get());
        double[] filteredSignal = algo.applyFilter(fillOutliers, windowWidth);

        // Calculate noise (difference between original and filtered)
        double[] noiseSignal = new double[size];
        for (int i = 0; i < size; i++) {
            noiseSignal[i] = rawSignal[i] - filteredSignal[i];
        }
        // Convert to plot data items
        List<PlotDataItem<Instant>> filteredDataList = new ArrayList<>(size);
        List<PlotDataItem<Instant>> noiseDataList = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            filteredDataList.add(new SimpleDataItem<>(timestamps[i], filteredSignal[i]));
            noiseDataList.add(new SimpleDataItem<>(timestamps[i], noiseSignal[i]));

        }
        return new ProcessedData(filteredDataList, noiseDataList);
    }

    /**
     * Update the plot data with processed filtered and noise signals.
     *
     * @param data ProcessedData containing filtered and noise data
     */
    private void updatePlotData(ProcessedData data) {
        Platform.runLater(() -> {
            filteredData.setData(data.filtered);
            noiseData.setData(data.noise);
        });
    }

    private double[] fillOutliers(double[] data, double lower, double upper) {
        if (Double.isNaN(lower) || Double.isNaN(upper)) {
            return data;
        }
        try {
            return BidirectionalFillerOutliers.fillOutliers(data, lower, upper);
        } catch (Exception e) {
            Activator.logger.log(Level.WARNING, "Outlier filling failed, returning raw data", e);
            return data;
        }
    }


    private void updateTraceColor(String traceName, Color color) {
        if (plot == null || color == null) {
            return;
        }

        Trace<Instant> trace = StreamSupport.stream(plot.getTraces().spliterator(), true)
                .filter(t -> traceName.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);
        if (trace != null) {
            trace.setColor(color);
        }
    }

    /**
     * Validates if the given text represents a valid number.
     * Supports:
     * - Positive/negative integers (e.g., 123, -456)
     * - Floating-point numbers (e.g., 1.23, -4.56)
     * - Scientific notation (e.g., 1.23e-4, 5E+6)
     *
     * @param text The text to validate
     * @return true if the text is a valid number, false otherwise
     */
    private boolean isValidNumber(String text) {
        if (text == null)
            return false;

        String trimmed = text.trim();
        if (trimmed.isEmpty())
            return false;

        try {
            double value = Double.parseDouble(trimmed);
            return !Double.isNaN(value) && !Double.isInfinite(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void startRefresh() {
        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Refresh-Scheduler");
            t.setDaemon(true);
            return t;
        });
        executorService.scheduleWithFixedDelay(() -> {
            Platform.runLater(() -> {
                try {
                    fetchAndProcessSamples(smaples -> {
                        if (isFirstRefreshingExecution) {
                            isFirstRefreshingExecution = false;
                            modifyLimit(samples);
                        }
                    });

                } catch (Exception e) {
                    Activator.logger.log(Level.SEVERE, "Refresh failed ", e);

                }
            });

        }, 0, refreshIntervalMs, TimeUnit.MILLISECONDS);
        isRefreshing = true;
        Activator.logger.log(Level.INFO, "Refresh start ");
    }

    private void stopRefresh() {
        if (!isRefreshing || executorService == null)
            return;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executorService = null;
        isRefreshing = false;
        isFirstRefreshingExecution = true;
        Activator.logger.log(Level.INFO, "Refresh stop ");
    }


    /**
     * Toggle visibility of the noise trace
     *
     * @param visible True to show noise, false to hide
     */
    private void setNoiseVisible(final boolean visible) {
        boolean showNoise = visible;
        if (algo instanceof MovingStandardDeviation) {
            showNoise = false;

        }
        YAxis<Instant> noiseAxis = plot.getYAxes().stream()
                .filter(axis -> "Noise".equalsIgnoreCase(axis.getName()))
                .findFirst()
                .orElse(null);

        if (noiseAxis != null)
            noiseAxis.setVisible(showNoise);


        Trace<Instant> noiseTrace = StreamSupport.stream(plot.getTraces().spliterator(), true)
                .filter(impl -> {
                    String traceName = impl.getName();
                    return traceName != null && traceName.equalsIgnoreCase("Noise");
                }).findAny().orElse(null);
        if (noiseTrace != null)
            noiseTrace.setVisible(showNoise);


    }

    /**
     * Container for processed plot data, holding filtered and noise signals.
     *
     * @param filtered List of filtered data points
     * @param noise    List of noise data points (raw - filtered)
     */
    private record ProcessedData(
            List<PlotDataItem<Instant>> filtered,
            List<PlotDataItem<Instant>> noise
    ) {
    }

}
