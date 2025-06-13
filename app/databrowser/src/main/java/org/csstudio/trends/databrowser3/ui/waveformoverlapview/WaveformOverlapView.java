package org.csstudio.trends.databrowser3.ui.waveformoverlapview;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.csstudio.javafx.rtplot.*;
import org.csstudio.javafx.rtplot.data.PlotDataSearch;
import org.csstudio.javafx.rtplot.internal.ToolbarHandler;
import org.csstudio.javafx.rtplot.internal.TraceImpl;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.*;
import org.csstudio.trends.databrowser3.ui.ToggleToolbarMenuItem;
import org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample.SampleAlgorithm;
import org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample.SampleAlgorithms;
import org.csstudio.trends.databrowser3.ui.waveformoverlapview.sample.SampleManager;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Panel for overlapping waveform (array) samples of the current model.
 * Supports multi-waveform comparison with various visualization settings.
 *
 * @author Mingtao Li
 * China Spallation Neutron Sources
 */
@SuppressWarnings("nls")
public class WaveformOverlapView extends VBox {
    // Configuration parameters
    private static final SampleAlgorithm DEFAULT_SAMPLE_ALGORITHM = SampleAlgorithms.getAlgorithmInstance(
            "GroupTypicalSampling");
    private static final int MAX_SAMPLES_FOR_CONFIG = 20;
    //If there are too many sample waveform datas, then the average over a smpling interval
    // should be used to improve loading performance.
    private static int showRepresentativeWaveformNumber = 100;
    // Model and data structures
    private final Model model;

    private final List<WaveformOverlapValueDataProvider> waveforms = Collections.synchronizedList(new ArrayList<>());
    // UI Components
    private final ExtendedMultiCheckboxCombo<ModelItem> itemsCombo =
            new ExtendedMultiCheckboxCombo<>(Messages.WaveformViewSelect);
    private final ComboBox<String> algorithmComboBox = new ComboBox<>();
    private final SampleManager sampleManager = new SampleManager(DEFAULT_SAMPLE_ALGORITHM);
    private final Map<String, Color> colorMap = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<Instant, double[]>> dataMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> yAxisMap = new ConcurrentHashMap<>();
    private boolean jetColormapEnabled = true; // Color mapping mode: true: Jet colormap, false:interpolated color
    private boolean yAxesAutoScaleEnabled = true;
    private TraceType useTraceType = TraceType.LINES_DIRECT;// Default TraceType mode
    private int maxRepresentiveWaveformNumber = 1000;
    private final Slider showSampleNumberSlider = new Slider(0, maxRepresentiveWaveformNumber,
            showRepresentativeWaveformNumber);
    private TraceConfig traceConfig;
    // Visualization settings
    private PointType usePointType = PointType.NONE;  // Default Line Style
    private LineStyle useLineStyle = LineStyle.SOLID;  // Default Line Style
    private int useLineWidth = 1; // Default Line Width;
    private int usePointSize = 1;  // Default Point Size;
    private boolean avgRemovedEnabled = false; //Avg/offset Removal

    private ScheduledExecutorService executorService;
    private ToolBar toolbar;
    private Button configButton;
    // Plot components
    private ToolbarHandler<Instant> toolbarHandler;
    // Track if the code is updating the selected items to avoid recursive events
    private boolean isUpdatingItems = false;
    // RTPlot for displaying waveforms
    private RTValuePlot plot;
    /**
     * Model listener to handle changes in the model
     */
    private final ModelListener model_listener = new ModelListener() {
        PauseTransition pauseTransition = new PauseTransition(Duration.seconds(5));

        {
            pauseTransition.setOnFinished(event -> {
                Platform.runLater(() -> {
                    try {
                        updateAll();
                    } catch (Exception e) {
                        Activator.logger.log(Level.SEVERE, "Refresh failed ", e);
                    }
                });
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
        public void changedTimerange() {
            pauseTransition.stop();
            pauseTransition.play();
        }
    };

    /**
     * Initialize the waveform overlap view with the given model.
     *
     * @param model The data model containing items with waveform samples
     */
    public WaveformOverlapView(final Model model) {
        this.model = model;
        initUI();
        model.addListener(model_listener);
        updateAvailableItems();
        setupWindowCloseHandler();
    }


    /**
     * Initializes all UI components and layout.
     * Creates top control panel, middle configuration panel,  bottom settings panel, and plot.
     */
    private void initUI() {
        initializeTopControlPanel();
        initializeAlgorithmSelectionPanel();
        initializeVisualizationSettingsPanel();
        initializePlotArea();
    }

    /**
     * Initializes the top control panel.
     * Contains item selection dropdown, refresh button, and periodic refresh checkbox.
     */
    private void initializeTopControlPanel() {
        final Label itemLabel = new Label(Messages.SampleView_Item);
        final Button refreshButton = new Button("Refresh Once");
        refreshButton.setTooltip(new Tooltip("Trigger a single data refresh"));
        refreshButton.setOnAction(event -> updateAll());

        final CheckBox periodicCheckBox = new CheckBox("Periodic Refresh: OFF");
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


        // Bind combo box width to available space
        itemsCombo.prefWidthProperty().bind(this.widthProperty().subtract(20).subtract(itemLabel.widthProperty()).multiply(0.8));
        itemsCombo.prefHeightProperty().bind(periodicCheckBox.heightProperty());
        // Item selection listener
        itemsCombo.setOnSelect(item -> {
            if (!isUpdatingItems) {
                Platform.runLater(() -> {
                    onItemSelected(item);
                });
            }
        });

        itemsCombo.setOnDeselect(item -> {
            if (!isUpdatingItems) {
                Platform.runLater(() -> {
                    onItemDeselected(item);
                });
            }
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox topRow = new HBox(5, itemLabel, itemsCombo, spacer, refreshButton, periodicCheckBox);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(5));
        getChildren().add(topRow);
    }

    /**
     * Initializes the algorithm selection panel.
     * Contains sampling algorithm dropdown and sampling count slider.
     */
    private void initializeAlgorithmSelectionPanel() {
        final Label algorithmLabel = new Label(" Sampling Algorithm: ");
        algorithmLabel.prefWidthProperty().bind(this.widthProperty().divide(14));
        algorithmLabel.maxWidthProperty().bind(this.widthProperty().divide(10));

        String[] algorithms = SampleAlgorithms.getAllDisplayNames();
        List<String> items = new ArrayList<>(Arrays.asList(algorithms));
        items.add("Without Sampling");
        algorithmComboBox.getItems().setAll(items);
        algorithmComboBox.getSelectionModel().selectFirst();
        algorithmComboBox.prefWidthProperty().bind(this.widthProperty().divide(8));
        algorithmComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return; // Exit early for null values

            if (newVal.contains("Without Sampling")) {
                // Show confirmation dialog and handle result
                boolean confirmed = showConfirmationDialog("Confirm Algorithm Change", "Switching to " + newVal +
                        "may impact performance.\n" + "It is very slow ro render with large datasets.\n" + "Are you " + "sure you want to proceed?");

                if (!confirmed) {
                    // User canceled, revert selection and exit
                    Platform.runLater(() -> algorithmComboBox.setValue(oldVal));
                    return;
                }
            }
            // Process algorithm change
            SampleAlgorithm algo = SampleAlgorithms.getAlgorithmInstance(newVal);
            sampleManager.setSampleAlgorithm(algo);
            updateAll();
        });

        final Label showSamplingNumberLabel = new Label("Show Sampling Count: ");
        showSamplingNumberLabel.setPadding(new Insets(5));
        showSamplingNumberLabel.prefWidthProperty().bind(this.widthProperty().divide(10));
        showSamplingNumberLabel.textProperty().bind(Bindings.format("Show Sampling Count: %d",
                showSampleNumberSlider.valueProperty().asObject().map(value -> value.intValue())));

        showSampleNumberSlider.setPadding(new Insets(5));
        showSampleNumberSlider.prefWidthProperty().bind(this.widthProperty().multiply(0.5));
        HBox.setHgrow(showSampleNumberSlider, Priority.ALWAYS);
        showSampleNumberSlider.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            String selectedAlgorithm = algorithmComboBox.getValue();
            return selectedAlgorithm == null || selectedAlgorithm.contains("Without Sampling");
        }, algorithmComboBox.valueProperty()));
        showSampleNumberSlider.valueProperty().addListener((obs, oldVal, newVal) -> showRepresentativeWaveformNumber
                = newVal.intValue());
        showSampleNumberSlider.setOnMouseReleased(event -> updateAll());
        final double SMALL_STEP = 1.0;
        final double LARGE_STEP = 10.0;
        final double MAX_STEP_MULTIPLIER = 5.0;
        final PauseTransition updateSampleNumberTransition
                = new PauseTransition(Duration.millis(500));
        updateSampleNumberTransition.setOnFinished(event -> {
            updateAll();
        });

        showSampleNumberSlider.setOnScroll(event -> {
            double currentValue = showSampleNumberSlider.getValue();
            double deltaY = event.getDeltaY();
            double step = (currentValue < 100) ? SMALL_STEP : LARGE_STEP;
            if (event.isShiftDown()) {
                step *= MAX_STEP_MULTIPLIER;
            }
            double newValue = (deltaY > 0)
                    ? Math.min(currentValue + step, showSampleNumberSlider.getMax())
                    : Math.max(currentValue - step, showSampleNumberSlider.getMin());
            showSampleNumberSlider.setValue(newValue);

            updateSampleNumberTransition.stop();
            updateSampleNumberTransition.play();
            event.consume();
        });
        final HBox middleRow = new HBox(5, algorithmLabel, algorithmComboBox, showSamplingNumberLabel,
                showSampleNumberSlider);
        middleRow.setAlignment(Pos.CENTER_LEFT);
        middleRow.setPadding(new Insets(5));
        getChildren().add(middleRow);

    }

    /**
     * Initializes the visualization settings panel.
     * Contains color mapping, trace type, line style, and other visualization parameter controls.
     */
    private void initializeVisualizationSettingsPanel() {
        final Label colorMapLabel = new Label("Select Color Map: "); // Color map selection
        colorMapLabel.setPadding(new Insets(5));

        ComboBox<String> colorMapComboBox = new ComboBox<>(FXCollections.observableArrayList(Messages.UseJet,
                Messages.UseInterpolate));
        colorMapComboBox.getSelectionModel().selectFirst();
        colorMapComboBox.prefWidthProperty().bind(this.widthProperty().divide(8));
//        colorMapComboBox.prefHeightProperty().bind(refreshButton.heightProperty());
        colorMapComboBox.setOnAction(event -> {
            String selectedValue = colorMapComboBox.getValue();
            if (selectedValue != null) {
                if (selectedValue.equalsIgnoreCase(Messages.UseInterpolate)) {
                    jetColormapEnabled = false;
                } else if (selectedValue.equalsIgnoreCase(Messages.UseJet)) {
                    jetColormapEnabled = true;
                } else {
                    throw new IllegalArgumentException("Invalid Color map: " + selectedValue);
                }
                updateAll();
            }
        });
        colorMapComboBox.getSelectionModel().selectFirst();

        final Label traceTypeLabel = new Label("Trace Type: ");// Trace Type selection
        traceTypeLabel.setPadding(new Insets(5));

        ComboBox<TraceType> traceTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(TraceType.values()));
        traceTypeComboBox.getSelectionModel().selectFirst();
        traceTypeComboBox.prefWidthProperty().bind(this.widthProperty().divide(16));
//        traceTypeComboBox.prefHeightProperty().bind(refreshButton.heightProperty());
        traceTypeComboBox.setOnAction(event -> {
            TraceType selectedValue = traceTypeComboBox.getValue();
            if (selectedValue != null) {
                useTraceType = selectedValue;
                Platform.runLater(() -> {
                    traceConfig.updateProperty(TraceProperty.TRACE_STYLE, selectedValue);
                });
            }
        });
        traceTypeComboBox.getSelectionModel().select(useTraceType);


        final Label lineStyleLabel = new Label("Line Style: "); // Line style selection
        lineStyleLabel.setPadding(new Insets(5));

        ComboBox<LineStyle> lineStyleComboBox = new ComboBox<>(FXCollections.observableArrayList(LineStyle.values()));
        lineStyleComboBox.getSelectionModel().selectFirst();
        lineStyleComboBox.prefWidthProperty().bind(this.widthProperty().divide(16));
//        lineStyleComboBox.prefHeightProperty().bind(refreshButton.heightProperty());
        lineStyleComboBox.setOnAction(event -> {
            LineStyle selectedValue = lineStyleComboBox.getValue();
            if (selectedValue != null) {
                useLineStyle = selectedValue;
                traceConfig.updateProperty(TraceProperty.LINE_STYLE, useLineStyle);
            }
        });
        lineStyleComboBox.getSelectionModel().select(useLineStyle);


        Slider lineWidthSlider = new Slider(0, 10, 1);// line Width selection
        lineWidthSlider.setPadding(new Insets(5));
        lineWidthSlider.prefWidthProperty().bind(this.widthProperty().divide(8));
        lineWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            useLineWidth = newVal.intValue();
            if (Math.abs(useLineWidth) < 0.001) {
                lineStyleComboBox.setValue(LineStyle.SOLID);
                lineStyleComboBox.setDisable(true);
            } else {
                lineStyleComboBox.setDisable(false);
            }
        });
        lineWidthSlider.setOnMouseReleased(event -> traceConfig.updateProperty(TraceProperty.LINE_WIDTH, useLineWidth));
        final double STEP = 1.0;
        final PauseTransition updateLineWidthTransition
                = new PauseTransition(Duration.millis(500));
        updateLineWidthTransition.setOnFinished(event -> traceConfig.updateProperty(TraceProperty.LINE_WIDTH,
                useLineWidth));

        lineWidthSlider.setOnScroll(event -> {
            double currentValue = lineWidthSlider.getValue();
            double deltaY = event.getDeltaY();
            double newValue = (deltaY > 0)
                    ? Math.min(currentValue + STEP, lineWidthSlider.getMax())
                    : Math.max(currentValue - STEP, lineWidthSlider.getMin());
            lineWidthSlider.setValue(newValue);
            updateLineWidthTransition.stop();
            updateLineWidthTransition.play();
            event.consume();
        });
        final Label lineWidthLabel = new Label("Line Width: ");
        lineWidthLabel.setPadding(new Insets(5));
        lineWidthLabel.prefWidthProperty().bind(this.widthProperty().divide(20));
        lineWidthLabel.textProperty().bind(Bindings.format("Line Width: %d",
                lineWidthSlider.valueProperty().asObject().map(value -> value.intValue())));


        Slider pointSizeSlider = new Slider(0, 10, 1);// point size selection
        pointSizeSlider.setPadding(new Insets(5));
        pointSizeSlider.prefWidthProperty().bind(this.widthProperty().divide(8));
        pointSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> usePointSize = newVal.intValue());
        pointSizeSlider.setOnMouseReleased(event -> traceConfig.updateProperty(TraceProperty.POINT_SIZE, usePointSize));
        final PauseTransition updatepointSizeTransition
                = new PauseTransition(Duration.millis(500));
        updatepointSizeTransition.setOnFinished(event -> traceConfig.updateProperty(TraceProperty.POINT_SIZE,
                usePointSize));
        pointSizeSlider.setOnScroll(event -> {
            double currentValue = pointSizeSlider.getValue();
            double deltaY = event.getDeltaY();
            double newValue = (deltaY > 0)
                    ? Math.min(currentValue + STEP, pointSizeSlider.getMax())
                    : Math.max(currentValue - STEP, pointSizeSlider.getMin());
            pointSizeSlider.setValue(newValue);
            updatepointSizeTransition.stop();
            updatepointSizeTransition.play();
            event.consume();
        });
        final Label pointSizeLabel = new Label("Point Size: ");
        pointSizeLabel.setPadding(new Insets(5));
        pointSizeLabel.prefWidthProperty().bind(this.widthProperty().divide(20));
        pointSizeLabel.textProperty().bind(Bindings.format("Point Size: %d",
                pointSizeSlider.valueProperty().asObject().map(value -> value.intValue())));


        final Label pointTypeLabel = new Label("Point Type: ");  // Line style selection
        pointTypeLabel.setPadding(new Insets(5));

        ComboBox<PointType> pointTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(PointType.values()));
        pointTypeComboBox.getSelectionModel().selectFirst();
        pointTypeComboBox.prefWidthProperty().bind(this.widthProperty().divide(16));
//        pointTypeComboBox.prefHeightProperty().bind(refreshButton.heightProperty());
        pointTypeComboBox.setOnAction(event -> {
            PointType selectedValue = pointTypeComboBox.getValue();
            if (selectedValue != null) {
                usePointType = selectedValue;
                Platform.runLater(() -> {
                    traceConfig.updateProperty(TraceProperty.POINT_TYPE, usePointType);
                });
            }
        });
        pointTypeComboBox.getSelectionModel().select(usePointType);


        CheckBox removalAverageCheckBox = new CheckBox("Avg Removal?"); // Removal the offset or
        // the avg of the waveform samples, to see the flatuations between different samples.
        removalAverageCheckBox.setSelected(false);
        removalAverageCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            avgRemovedEnabled = newVal;
            Platform.runLater(() -> {
                updateAll();
            });
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox bottomRow = new HBox(5, colorMapLabel, colorMapComboBox, traceTypeLabel, traceTypeComboBox,
                lineWidthLabel, lineWidthSlider, lineStyleLabel, lineStyleComboBox, pointSizeLabel, pointSizeSlider,
                pointTypeLabel, pointTypeComboBox, spacer, removalAverageCheckBox);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setPadding(new Insets(5));
        getChildren().add(bottomRow);

    }

    /**
     * Creates and configures the main plot area.
     * Sets up axes, toolbar, and initial visualization settings.
     */
    private void initializePlotArea() {
        // Initialize plot
        plot = new RTValuePlot(true);
        plot.getXAxis().setName(Messages.WaveformIndex);
        plot.getXAxis().setAutoscale(true);
        plot.removeYAxis(0);
        /**
         *Configure Y-axis and toolbar
         *
         * If the number of waveforms shown in graph iws very large, there is great difficulty to
         * open the ConfigurationDialog to change the value axis's auto-scale property.
         * If the numer excedds MAX_SAMPLES_FOR_CONFIG, the configButton is disabled. If not, then enabled. This is
         * done by reflection.
         * Consequently, a new toggle button should be added to change this property.
         */

        try {
            Field toolbarHandlerField = plot.getClass().getSuperclass().getDeclaredField("toolbar");
            toolbarHandlerField.setAccessible(true);
            toolbarHandler = (ToolbarHandler<Instant>) toolbarHandlerField.get(plot);
            toolbar = toolbarHandler.getToolBar();
            configButton = (Button) toolbar.getItems().get(0);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // Add scale enable button to toolbar

        final CheckBox scaleCheckBox = new CheckBox("Y Auto-Scale: OFF");
        scaleCheckBox.setTooltip(new Tooltip("Enable/disable Y Auto-Scale"));
        scaleCheckBox.setGraphic(Activator.getIcon("scale"));
        scaleCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            yAxesAutoScaleEnabled = isSelected;
            scaleCheckBox.setText(isSelected ? "Y Auto-Scale: ON" : "Y Auto-Scale: OFF");
            plot.getYAxes().stream().forEach(yAxis -> yAxis.setAutoscale(isSelected));

        });


        scaleCheckBox.setSelected(true);
        toolbar.getItems().add(scaleCheckBox);


        plot.showLegend(false);
        traceConfig = new TraceConfig(plot);
        createContextMenu();


        // Assemble layout
        VBox.setVgrow(plot, Priority.ALWAYS);
        getChildren().add(plot);
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
                Optional.ofNullable(oldScene.getWindow()).ifPresent(w -> w.setOnCloseRequest(null));
                oldScene.windowProperty().removeListener(windowChangeListener);
            }
            if (newScene != null) {
                Optional.ofNullable(newScene.getWindow()).ifPresent(w -> w.setOnCloseRequest(closeHandler));
                newScene.windowProperty().addListener(windowChangeListener);
            }
        });
    }

    /**
     * Cleans up resources when the window is closed.
     * Stops any ongoing refresh tasks, removes model listeners,
     * and clears plot data to prevent memory leaks.
     */
    private void cleanupResources() {
        System.out.println("Cleaning up resources before window closes...");
        stopRefresh();

        if (model != null) {
            model.removeListener(model_listener);
        }
        synchronized (dataMap) {
            dataMap.clear();
        }
        waveforms.clear();
        if (plot != null) {
            plot.getTraces().forEach(plot::removeTrace);
            while (!plot.getYAxes().isEmpty())
                plot.removeYAxis(0);
        }
    }


    /**
     * Update the list of available items in the combo box
     */
    private void updateAvailableItems() {
        // Save currently selected items
        final List<ModelItem> previouslySelected = new ArrayList<>(itemsCombo.getSelectedOptions());

        for (ModelItem item : previouslySelected) {
            System.out.println(item.getDisplayName());
        }
        isUpdatingItems = true;

        // Get all items from the model
        final List<ModelItem> new_items = model.getItems();
        itemsCombo.setOptions(new_items);

        // Re-select those that were selected and are still valid
        previouslySelected.retainAll(new_items);
        itemsCombo.selectOptions(previouslySelected);

        isUpdatingItems = false;
        previouslySelected.forEach(this::onItemSelected);

    }

    /**
     * Handles item deselection event.
     * Removes all waveforms associated with the deselected item from the plot.
     *
     * @param selectedModelItem The deselected model item
     */
    private void onItemDeselected(final ModelItem selectedModelItem) {
        String itemName = selectedModelItem.getDisplayName();
        YAxis<Double> yAxisToRemove = null;
        int index = -1;
        for (YAxis<Double> yAxis : plot.getYAxes()) {
            if (yAxis.getName().equalsIgnoreCase(itemName)) {
                yAxisToRemove = yAxis;
                index = plot.getYAxes().indexOf(yAxis);
                break;
            }
        }
        if (null == yAxisToRemove)
            return;
        String yAxisName = yAxisToRemove.getName();
        List<Trace<Double>> tracesToRemove = new ArrayList<>();

        for (Trace<Double> trace : plot.getTraces()) {
            TraceImpl<Double> impl = (TraceImpl<Double>) trace;
            int n = trace.getYAxis();
            if (n == index) {
                tracesToRemove.add(trace);
            } else if (n > index) {
                int newIndex = n - 1;
                impl.setYAxis(newIndex);
            }
        }
        final int n = index;
        yAxisMap.replaceAll((key, value) -> value != null && value > n ? value - 1 : value);
        tracesToRemove.forEach(trace -> {
            plot.removeTrace(trace);

        });

        plot.removeYAxis(index);


    }

    /**
     * Handles item selection event.
     * Loads data for the selected item and updates the plot display.
     *
     * @param selectedModelItem The selected model item
     */
    private void onItemSelected(final ModelItem selectedModelItem) {
        PlotSamples samples = selectedModelItem.getSamples();
        String name = selectedModelItem.getName();
        try {
            updateData(selectedModelItem);
        } catch (IllegalArgumentException e) {
            showDataLoadingWarning(selectedModelItem, e.getMessage());
            Activator.logger.log(Level.WARNING, "Error loading data for: " + name + " with reason: " + e.getMessage());

            return;
        }

        YAxis<Double> yAxis = plot.addYAxis(selectedModelItem.getDisplayName());
        yAxis.setAutoscale(yAxesAutoScaleEnabled);
        yAxis.useAxisName(true);
        yAxis.useTraceNames(false);
        int index = plot.getYAxes().indexOf(yAxis);

        yAxisMap.put(name, index);
        // Get samples from the selected item


        Color baseColor = selectedModelItem.getPaintColor();
        colorMap.put(name, baseColor);

        updateSelectedWaveformDisplay(selectedModelItem);


    }

    /**
     * Updates data for the specified item.
     * Retrieves sample data from the model and converts it to internal data structures.
     *
     * @param selectedModelItem The model item to update data for
     */
    private void updateData(final ModelItem selectedModelItem) {
        PlotSamples samples = selectedModelItem.getSamples();
        String name = selectedModelItem.getName();
        PlotSample[] sampleArray = getSampleArray(samples);
        if (sampleArray == null)
            return;
        // Convert to time-indexed data map
        ConcurrentHashMap<Instant, double[]> arrayData;
        try {
            arrayData = getArrayData(sampleArray);
        } catch (IllegalArgumentException e) {
            showDataLoadingWarning(selectedModelItem, e.getMessage());
            Activator.logger.log(Level.WARNING, "Error loading data for: " + name + " with reason: " + e.getMessage());
            throw new IllegalArgumentException("Error loading data for: " + name);
        }
        dataMap.put(name, arrayData);

    }

    /**
     * Updates display for all selected items.
     * Reloads and renders waveform data for all selected items.
     */
    private void updateAll() {
        final List<ModelItem> selected = new ArrayList<>(itemsCombo.getSelectedOptions());
        selected.forEach(this::updateData);
        selected.forEach(this::updateSelectedWaveformDisplay);
    }

    /**
     * Renders the waveforms on the plot with current settings.
     * Applies color mapping, sampling, and visualization parameters.
     */
    private void updateSelectedWaveformDisplay(ModelItem selectedModelItem) {
        String itemName = selectedModelItem.getName();
        if (itemName.isBlank() || !dataMap.containsKey(itemName))
            return;


        Activator.thread_pool.execute(() -> {
            int index = yAxisMap.get(itemName);
            Color baseColor = colorMap.get(itemName);
            ConcurrentHashMap<Instant, double[]> arrayData = dataMap.get(itemName);
            List<Map.Entry<Instant, double[]>> entryList;
            synchronized (arrayData) {
                entryList = new ArrayList<>(arrayData.entrySet());
            }
            entryList.sort(Map.Entry.comparingByKey());
            LinkedHashMap<Instant, double[]> threadSafeCopy = new LinkedHashMap<>(entryList.size());
            entryList.forEach(entry -> threadSafeCopy.put(entry.getKey(), entry.getValue()));

            LinkedHashMap<Instant, double[]> sampledArray = sampleManager.hasAlgorithm() ?
                    sampleManager.applyCurrentFilter(threadSafeCopy, showRepresentativeWaveformNumber) : threadSafeCopy;
            double[][] resultArray = sampledArray.values().stream().toArray(double[][]::new);
            double[] avg = avgRemovedEnabled ? calculateAverageParallel(resultArray) : null;
            Map.Entry<Instant, double[]>[] entries = sampledArray.entrySet().toArray(new Map.Entry[0]);

            IntStream.range(0, entries.length).forEach(i -> {
                Map.Entry<Instant, double[]> entry = entries[i];
                Instant pos = entry.getKey();
                String traceName = pos.toString();
                WaveformOverlapValueDataProvider waveform = avg == null ? new WaveformOverlapValueDataProvider() :
                        new WaveformOverlapValueDataProvider(avg);
                waveform.setValue(entry.getValue());
                // calculate trace color
                double fraction = (double) i / entries.length;
                Color traceColor = jetColormapEnabled ? JetColorTable.getColor(fraction) :
                        baseColor.interpolate(Color.WHITE, 1 - fraction);

                Optional<Trace<Double>> result =
                        StreamSupport.stream(plot.getTraces().spliterator(), true).filter(trace -> trace instanceof TraceImpl && trace.getName().equalsIgnoreCase(traceName)).findAny();

                result.ifPresentOrElse(element -> {
                    TraceImpl<Double> impl = (TraceImpl<Double>) element;

                    impl.updateData(waveform);
                    if (!Objects.equals(impl.getColor(), traceColor))
                        impl.setColor(traceColor);
                }, () -> {
                    TraceImpl<Double> addTrace = (TraceImpl<Double>) plot.addTrace(traceName, null, waveform,
                            traceColor, useTraceType, useLineWidth, useLineStyle, usePointType, usePointSize, index);
                });

            });
            List<String> posSetToRetain =
                    sampledArray.keySet().stream().sorted().map(Instant::toString).collect(Collectors.toList());

// Remove traces that:
// 1. Belong to the specified Y-axis
// 2. Have names not present in the retention set
            final int targetYAxis = index;
            StreamSupport.stream(plot.getTraces().spliterator(), true).filter(TraceImpl.class::isInstance).map(TraceImpl.class::cast).filter(impl -> impl.getYAxis() == targetYAxis).filter(impl -> {
                String traceName = impl.getName();
                return traceName != null && !posSetToRetain.contains(traceName);
            }).forEach(impl -> {
                plot.removeTrace(impl);
            }); // Remove filtered traces


            // If trace number is larger than MAX_SAMPLES_FOR_CONFIG, the configuration dialog is difficult to open,
            // so the config button should be disabled.
            long traceCount = StreamSupport.stream(plot.getTraces().spliterator(), false).count();
            configButton.setDisable(traceCount > MAX_SAMPLES_FOR_CONFIG);
            Platform.runLater(() -> {
                plot.requestUpdate();
            });
        });

    }

    /**
     * Retrieves an array of plot samples within the current time range of the model.
     * <p>
     * This method filters samples from the provided PlotSamples collection
     * to include only those that fall within the current time range specified by the model.
     *
     * @param samples The collection of plot samples to filter
     * @return An array of plot samples within the time range, or null if no samples are found
     */
    private PlotSample[] getSampleArray(PlotSamples samples) {
        ObservableList<PlotSample> samplesInRange = FXCollections.observableArrayList();
        Lock lock = samples.getLock();
        boolean acquired = false;
        try {
            acquired = lock.tryLock(2, TimeUnit.SECONDS);
            if (!acquired) {
                Activator.logger.log(Level.WARNING, "Failed to acquire lock within 2 seconds");
                return new PlotSample[0];
            }

            TimeRelativeInterval timeRelativeInterval = model.getTimerange();
            TimeInterval timeInterval = timeRelativeInterval.toAbsoluteInterval();
            PlotDataSearch plotDataSearch = new PlotDataSearch();
            int startIndex = plotDataSearch.findSampleLessOrEqual(samples, timeInterval.getStart());
            int endIndex = plotDataSearch.findSampleGreaterOrEqual(samples, timeInterval.getEnd());
            if (startIndex == -1) {
                startIndex = 0;
                if (endIndex == -1) {
                    return null; // No samples in time range
                }
            }
            IntStream.rangeClosed(startIndex, endIndex)
                    .mapToObj(samples::get).filter(sample -> !(sample.getVType() instanceof VString))
                    .forEach(samplesInRange::add);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Activator.logger.log(Level.SEVERE, String.format("Thread %s interrupted during sample fetch",
                    Thread.currentThread().getName()), ex);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
        PlotSample[] array = samplesInRange.toArray(new PlotSample[0]);
        return array;
    }


    /**
     * Processes an array of PlotSample objects in parallel to build a mapping
     * from timestamps (Instants) to their corresponding double arrays.
     *
     * @param samples Array of plot samples. Must not be null or empty.
     * @return A concurrent map where keys are timestamps and values are numerical arrays.
     * @throws IllegalArgumentException if samples is null, empty, or contains non-VNumberArray data.
     */
    private ConcurrentHashMap<Instant, double[]> getArrayData(PlotSample[] samples) {
        // Validate input to prevent NPE
        Objects.requireNonNull(samples, "Input samples array cannot be null");


        if (samples.length == 0) {
            throw new IllegalArgumentException("Input samples array cannot be empty");
        }

        // Check all samples are of VNumberArray type
        boolean allValid = Arrays.stream(samples).allMatch(sample -> sample.getVType() instanceof VNumberArray);

        if (!allValid) {
            throw new IllegalArgumentException("Samples contain non-VNumberArray data");
        }
        // ConcurrentHashMap to safely collect results from parallel processing
        ConcurrentHashMap<Instant, double[]> arrayMap = new ConcurrentHashMap<>();

        // Dynamically choose parallel stream based on data size
        Stream<PlotSample> stream = samples.length > 100 ? Arrays.stream(samples).parallel() : Arrays.stream(samples);


        // Process each sample in parallel, mapping timestamp to its data array
        stream.parallel().forEach(sample -> {
            Instant position = sample.getPosition();
            VType vType = sample.getVType();
            ListNumber listNumber = getListNumber(vType);
            // Compute and store the data array only if the timestamp is new
            arrayMap.computeIfAbsent(position, pos -> fillValueArray(listNumber));
        });

        return arrayMap;
    }

    /**
     * Calculates the average values of each column in a 2D array in parallel.
     *
     * @param valueArrays a 2D array where each inner array represents a row of values.
     *                    All inner arrays must have the same length.
     * @return an array containing the average value of each column.
     * @throws IllegalArgumentException if the input array is null, empty,
     *                                  or contains sub-arrays of different lengths.
     */
    private double[] calculateAverageParallel(double[][] valueArrays) {
        // Validate input to prevent NPE and ensure consistency
        if (valueArrays == null || valueArrays.length == 0) {
            return new double[0]; // Return empty array for edge cases
        }

        int numRows = valueArrays.length;
        int numCols = valueArrays[0].length;

        // Check all rows have consistent length to avoid ArrayIndexOutOfBoundsException
        for (double[] row : valueArrays) {
            if (row == null || row.length != numCols) {
                throw new IllegalArgumentException("All sub-arrays must have the same length");
            }
        }

        double[] avg = new double[numCols];

        // Utilize parallel stream to compute averages column-wise
        // This leverages multi-core CPUs for improved performance on large datasets
        Arrays.parallelSetAll(avg, j -> {
            double sum = 0.0;
            for (double[] row : valueArrays) {
                sum += row[j];
            }
            return sum / numRows; // Compute average after summing to reduce floating-point errors
        });

        return avg;
    }

    /**
     * Retrieves a ListNumber from the given VType object.
     *
     * @param type the VType instance to process. Cannot be null.
     * @return a ListNumber containing numerical data.
     * If the input is a VNumberArray, returns its internal data;
     * otherwise, converts the value to a double and wraps it in a ListNumber.
     * @throws IllegalArgumentException if the input type is null or cannot be converted to a
     *                                  double.
     */
    private ListNumber getListNumber(VType type) {
        // Validate input to prevent NPE and ensure type compatibility
        if (type == null) {
            throw new IllegalArgumentException("Input VType cannot be null");
        }

        // Directly return the data if it's already a VNumberArray
        if (type instanceof VNumberArray) {
            return ((VNumberArray) type).getData();
        }

        // Convert other VType implementations to double and wrap in ListNumber
        try {
            double value = VTypeHelper.toDouble(type);
            return ArrayDouble.of(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported VType: " + type.getClass().getName(), e);
        }
    }

    /**
     * Converts a ListNumber to a double array.
     *
     * @param listNumber the ListNumber to convert. Cannot be null.
     * @return a double array containing the values from the ListNumber.
     * @throws NullPointerException  if listNumber is null.
     * @throws NumberFormatException if any value cannot be converted to a double.
     */
    private double[] fillValueArray(ListNumber listNumber) {
        // Validate input to prevent NullPointerException
        if (listNumber == null) {
            throw new NullPointerException("listNumber cannot be null");
        }
        int size = listNumber.size();
        double[] result = new double[size];
        // Fill the array with values from ListNumber
        // Using IntStream for potential performance improvement and readability
        IntStream.range(0, size).forEach(i -> result[i] = listNumber.getDouble(i));
        return result;
    }


    /**
     * Creates the plot context menu.
     * Configures right-click menu options including toolbar toggle, print, and save snapshot.
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
     * Displays a warning dialog when data loading fails for a specific model item.
     *
     * @param item    The model item for which data loading failed.
     * @param message The error message to display.
     */
    private void showDataLoadingWarning(ModelItem item, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Waveform Data Loading Failed");
        alert.setHeaderText("Failed to load: " + item.getDisplayName());
        alert.setContentText("Reason: " + message);
        alert.initOwner(getScene().getWindow());
        alert.show();
    }

    /**
     * Displays a confirmation dialog with the specified title and message.
     *
     * @param title   The title of the confirmation dialog
     * @param message The message to display in the dialog
     * @return true if the user clicks OK, false otherwise
     */
    private boolean showConfirmationDialog(String title, String message) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(title);
        dialog.setHeaderText(null); // Simplify dialog layout
        dialog.setContentText(message);

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Starts periodic data refresh.
     * Creates a daemon thread that repeatedly updates all waveform data every 5 seconds.
     * If a refresh task is already running, this method does nothing.
     */
    private void startRefresh() {
        if (executorService != null && !executorService.isShutdown())
            return;

        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "WaveformRefreshThread");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> {
            try {
                Platform.runLater(this::updateAll);
            } catch (Exception e) {
                Activator.logger.log(Level.WARNING, "Refresh task failed", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Stops the periodic data refresh task.
     * Attempts to gracefully shut down the refresh executor, waiting up to 1 second.
     * If the executor does not terminate in time, forces shutdown.
     * Resets the executor service reference to null after shutdown.
     */
    private void stopRefresh() {
        if (executorService != null) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS))
                    executorService.shutdownNow();
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                executorService = null;
            }
        }
    }


}
