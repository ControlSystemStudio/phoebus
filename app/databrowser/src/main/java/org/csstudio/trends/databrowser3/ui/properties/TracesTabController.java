package org.csstudio.trends.databrowser3.ui.properties;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.converter.DefaultStringConverter;
import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.*;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.archive.vtype.DefaultVTypeFormat;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.AlertWithToggle;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.SecondsParser;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the traces tab.
 * Created from previous version of {@link TracesTab}.
 */
public class TracesTabController {

    @FXML
    private TableView<ModelItem> traceTable;
    @FXML
    private Label lowerPlaceholder;
    @FXML
    private VBox formulaPane;
    @FXML
    private TextField formulaText;
    @FXML
    private TableView<ArchiveDataSource> archivesTable;
    @FXML
    private TableColumn<ModelItem, Boolean> visibilityColumn;
    @FXML
    private TableColumn<ModelItem, String> itemNameColumn;
    @FXML
    private TableColumn<ModelItem, String> displayNameColumn;
    @FXML
    private TableColumn<ModelItem, ColorPicker> colorColumn;
    @FXML
    private TableColumn<ModelItem, String> cursorValueColumn;
    @FXML
    private TableColumn<ModelItem, String> scanPeriodColumn;
    @FXML
    private TableColumn<ModelItem, String> liveBufferSizeColumn;
    @FXML
    private TableColumn<ModelItem, String> axisColumn;
    @FXML
    private TableColumn<ModelItem, String> traceTypeColumn;
    @FXML
    private TableColumn<ModelItem, String> lineWidthColumn;
    @FXML
    private TableColumn<ModelItem, String> lineStyleColumn;
    @FXML
    private TableColumn<ModelItem, String> pointTypeColumn;
    @FXML
    private TableColumn<ModelItem, String> pointSizeColumn;
    @FXML
    private TableColumn<ModelItem, RequestType> requestTypeColumn;
    @FXML
    private TableColumn<ModelItem, String> waveformIndexColumn;
    @FXML
    private TableColumn<ModelItem, Boolean> alarmLimitsVisibilityColumn;
    @FXML
    private TableColumn<ArchiveDataSource, String> archiveNameColumn;
    @FXML
    private TableColumn<ArchiveDataSource, String> urlColumn;

    // Earlier version used
    //   trace_types = FXCollections.observableArrayList(TraceType.getDisplayNames());
    // and passed that to the UI.
    // But UI will listen to changes in observable array. The static list would then
    // keep receiving new listeners added to the UI, and thus keep the UI elements and
    // their data in memory.
    // Now keep only the trace & point types as plain list,
    // and UI creates a short-time observable list while running, which is then disposed as UI closes.
    private final List<String> traceTypes = List.of(TraceType.getDisplayNames());
    private final List<String> lineStyles = List.of(LineStyle.getDisplayNames());
    private final List<String> pointTypes = List.of(PointType.getDisplayNames());

    private final Model model;

    private final UndoableActionManager undoableActionManager;

    private final List<WeakReference<SelectedSampleProperty>> selectedSamples = new ArrayList<>();
    private final ObservableList<String> axisNames = FXCollections.observableArrayList();

    public TracesTabController(final Model model, final UndoableActionManager undoableActionManager) {
        this.model = model;
        this.undoableActionManager = undoableActionManager;
    }

    @FXML
    public void initialize() {

        visibilityColumn.setCellFactory(CheckBoxTableCell.forTableColumn(visibilityColumn));
        visibilityColumn.setCellValueFactory(cell -> {
            final SimpleBooleanProperty visibilityProperty = new SimpleBooleanProperty(cell.getValue().isVisible());
            // Update model when CheckBoxTableCell updates this property
            visibilityProperty.addListener((p, old, visible) -> {
                if (! visible  &&  Preferences.prompt_for_visibility)
                {
                    final AlertWithToggle dialog = new AlertWithToggle(Alert.AlertType.CONFIRMATION,
                            Messages.HideTraceWarningDetail,
                            ButtonType.YES, ButtonType.NO);
                    dialog.setTitle(Messages.HideTraceWarning);
                    dialog.getDialogPane().setMinSize(600, 350);
                    DialogHelper.positionDialog(dialog, traceTable, -600, -350);
                    final boolean doNotHide = dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES;
                    Preferences.setVisibilityPrompt(! dialog.isHideSelected());
                    if (doNotHide)
                    {   // Restore checkbox
                        Platform.runLater(() -> visibilityProperty.set(true)); // Yes, wrapping in Platform.runLater is necessary!
                        return;
                    }
                }
                new ChangeVisibilityCommand(undoableActionManager, cell.getValue(), visible);
            });
            return visibilityProperty;
        });
        PropertyPanel.addTooltip(visibilityColumn, Messages.TraceVisibilityTT);

        itemNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResolvedName()));
        itemNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        itemNameColumn.setOnEditCommit(event -> {
            try
            {
                new ChangeNameCommand(undoableActionManager, event.getRowValue(), event.getNewValue());
            }
            catch (Exception ex)
            {
                traceTable.refresh();
            }
        });
        PropertyPanel.addTooltip(itemNameColumn, Messages.ItemNameTT);

        displayNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResolvedDisplayName()));
        displayNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        displayNameColumn.setOnEditCommit(event ->
                new ChangeDisplayNameCommand(undoableActionManager, event.getRowValue(), event.getNewValue()));
        PropertyPanel.addTooltip(displayNameColumn, Messages.TraceDisplayNameTT);

        colorColumn.setCellValueFactory(cell -> {
            final Color color = cell.getValue().getPaintColor();
            final ColorPicker picker = PropertyPanel.ColorTableCell.createPicker(color);
            picker.setOnAction(event ->
                    new ChangeColorCommand(undoableActionManager, cell.getValue(), picker.getValue()));
            return new SimpleObjectProperty<>(picker);
        });
        colorColumn.setCellFactory(cell -> new PropertyPanel.ColorTableCell<>());
        PropertyPanel.addTooltip(colorColumn, Messages.ColorTT);

        // Selected sample
        cursorValueColumn.setCellValueFactory(cell ->
        {
            final SelectedSampleProperty prop = new SelectedSampleProperty(cell.getValue());
            selectedSamples.add(new WeakReference<>(prop));
            return prop;
        });
        PropertyPanel.addTooltip(cursorValueColumn, Messages.CursorValueTT);

        scanPeriodColumn.setCellValueFactory(cell ->
        {
            final ModelItem item = cell.getValue();
            if (item instanceof PVItem)
                return new SimpleStringProperty(Double.toString(((PVItem)item).getScanPeriod()));
            else
                return new SimpleStringProperty(Messages.NotApplicable);

        });
        scanPeriodColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        scanPeriodColumn.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            if (item instanceof PVItem)
            {
                try
                {
                    new ChangeSamplePeriodCommand(undoableActionManager, (PVItem)item, Double.parseDouble(event.getNewValue()));
                }
                catch (Exception e)
                {
                    traceTable.refresh();
                }
            }
        });
        PropertyPanel.addTooltip(scanPeriodColumn, Messages.ScanPeriodTT);

        liveBufferSizeColumn.setCellValueFactory(cell ->
        {
            final ModelItem item = cell.getValue();
            if (item instanceof PVItem)
                return new SimpleStringProperty(Integer.toString(((PVItem)item).getLiveCapacity()));
            else
                return new SimpleStringProperty(Messages.NotApplicable);

        });
        liveBufferSizeColumn.setCellFactory(c -> new TextFieldTableCell<>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String value, boolean empty)
            {
                super.updateItem(value, empty);
                if (empty)
                    this.setTooltip(null);
                else
                {
                    if (getTableRow() == null)
                        return;
                    final ModelItem item = getTableRow().getItem();
                    if (! (item instanceof PVItem))
                    {
                        this.setTooltip(null);
                        return;
                    }
                    // Dynamic Tooltip that shows time range for the buffer
                    final int size = ((PVItem) getTableRow().getItem()).getLiveCapacity();
                    final String span = SecondsParser.formatSeconds(size);
                    String text = MessageFormat.format(Messages.LiveBufferSizeInfoFmt, size, span);
                    this.setTooltip(new Tooltip(text));
                }
            }
        });
        liveBufferSizeColumn.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            if (item instanceof PVItem)
            {
                try
                {
                    new ChangeLiveCapacityCommand(undoableActionManager, (PVItem)item, Integer.parseInt(event.getNewValue()));
                }
                catch (Exception e)
                {
                    traceTable.refresh();
                }
            }
        });

        axisColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getAxis().getName()));
        axisColumn.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(axisNames));
        axisColumn.setOnEditCommit(event ->
        {
            final int index = axisNames.indexOf(event.getNewValue());
            final AxisConfig axis = event.getRowValue().getModel().get().getAxis(index);
            new ChangeAxisCommand(undoableActionManager, event.getRowValue(), axis);
        });
        axisColumn.setEditable(true);
        PropertyPanel.addTooltip(axisColumn, Messages.AxisTT);

        traceTypeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getTraceType().toString()));
        traceTypeColumn.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(FXCollections.observableArrayList(traceTypes)));
        traceTypeColumn.setOnEditCommit(event ->
        {
            final int index = traceTypes.indexOf(event.getNewValue());
            final TraceType type = TraceType.values()[index];
            new ChangeTraceTypeCommand(undoableActionManager, event.getRowValue(), type);
        });
        PropertyPanel.addTooltip(traceTypeColumn, Messages.TraceTypeTT);

        lineWidthColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Integer.toString(cell.getValue().getLineWidth())));
        lineWidthColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        lineWidthColumn.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            try
            {
                new ChangeLineWidthCommand(undoableActionManager, item, Integer.parseInt(event.getNewValue()));
            }
            catch (Exception e)
            {
                traceTable.refresh();
            }
        });
        PropertyPanel.addTooltip(lineWidthColumn, Messages.TraceLineWidthTT);

        lineStyleColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getLineStyle().toString()));
        lineStyleColumn.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(FXCollections.observableArrayList(lineStyles)));
        lineStyleColumn.setOnEditCommit(event -> {
            final int index = lineStyles.indexOf(event.getNewValue());
            final LineStyle style = LineStyle.values()[index];
            new ChangeLineStyleCommand(undoableActionManager, event.getRowValue(), style);
        });
        PropertyPanel.addTooltip(lineStyleColumn, Messages.TraceLineStyleTT);

        pointTypeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getPointType().toString()));
        pointTypeColumn.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(FXCollections.observableArrayList(pointTypes)));
        pointTypeColumn.setOnEditCommit(event ->
        {
            final int index = pointTypes.indexOf(event.getNewValue());
            final PointType type = PointType.values()[index];
            new ChangePointTypeCommand(undoableActionManager, event.getRowValue(), type);
        });
        PropertyPanel.addTooltip(pointTypeColumn, Messages.PointTypeTT);

        pointSizeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Integer.toString(cell.getValue().getPointSize())));
        pointSizeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        pointSizeColumn.setOnEditCommit(event -> {
            final ModelItem item = event.getRowValue();
            try
            {
                new ChangePointSizeCommand(undoableActionManager, item, Integer.parseInt(event.getNewValue()));
            }
            catch (Exception e)
            {
                traceTable.refresh();
            }
        });
        PropertyPanel.addTooltip(pointSizeColumn, Messages.PointSizeTT);

        requestTypeColumn.setCellValueFactory(cell ->
        {
            final RequestType type;
            if (cell.getValue() instanceof PVItem)
                type = ((PVItem)cell.getValue()).getRequestType();
            else
                type = null;
            return new SimpleObjectProperty<>(type);
        });
        requestTypeColumn.setCellFactory(cell -> new RequestTypeCell());

        waveformIndexColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Integer.toString(cell.getValue().getWaveformIndex())));
        waveformIndexColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveformIndexColumn.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            try
            {
                new ChangeWaveformIndexCommand(undoableActionManager, item, Integer.parseInt(event.getNewValue()));
            }
            catch (Exception e)
            {
                traceTable.refresh();
            }
        });
        PropertyPanel.addTooltip(waveformIndexColumn, Messages.WaveformIndexColTT);


        alarmLimitsVisibilityColumn.setCellFactory(modelItemBooleanTableColumn ->
                new AlarmVisibilityTableCell());

        alarmLimitsVisibilityColumn.setCellValueFactory(cell -> {
            final BooleanProperty visibilityProperty = new SimpleBooleanProperty(((PVItem)cell.getValue()).getShowAlarmLimits());
            visibilityProperty.addListener((p, old, visible) -> {
                PVItem pvItem = (PVItem)cell.getValue();
                pvItem.showAlarmLimits(visible);
            });
            return visibilityProperty;
        });


        traceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        traceTable.getColumns().forEach(c -> c.setSortable(false));

        archiveNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        urlColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUrl()));

        model.addListener(modelListener);
        updateFromModel();

        traceTable.getSelectionModel().getSelectedItems().addListener(selectionChanged);

        archivesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        createContextMenu();
        updateAxisNames();
    }

    private static class SelectedSampleProperty extends SimpleStringProperty
    {
        private final ModelItem item;

        SelectedSampleProperty(final ModelItem item)
        {
            this.item = item;
            update();
        }

        void update()
        {
            final Optional<PlotDataItem<Instant>> sample = item.getSelectedSample();
            if (sample.isPresent())
            {
                String text = DefaultVTypeFormat.get().format( ((PlotSample) sample.get()).getVType() );
                final String units = item.getUnits();
                if (units != null)
                    text = text + " " + units;
                set(text);
            }
            else
                set(Messages.NotApplicable);
        }
    }

    /** Table cell that shows RequestType */
    private class RequestTypeCell extends TableCell<ModelItem, RequestType>
    {
        final CheckBox button = new CheckBox();

        RequestTypeCell()
        {
            button.setTooltip(new Tooltip(Messages.RequestTypeTT));
        }

        @Override
        protected void updateItem(final RequestType value, final boolean empty)
        {
            super.updateItem(value, empty);
            if (empty  ||  getItem() == null)
                setGraphic(null);
            else
            {
                if (value == RequestType.OPTIMIZED)
                {
                    button.setSelected(true);
                    button.setText(Messages.Request_optimized);
                }
                else
                {
                    button.setSelected(false);
                    button.setText(Messages.Request_raw);
                }
                button.setOnAction(event ->
                {
                    final RequestType type = button.isSelected() ? RequestType.OPTIMIZED : RequestType.RAW;

                    if (type == RequestType.RAW  &&  Preferences.prompt_for_raw_data_request)
                    {
                        final AlertWithToggle dialog = new AlertWithToggle(Alert.AlertType.CONFIRMATION,
                                Messages.RequestTypeWarningDetail,
                                ButtonType.YES, ButtonType.NO);
                        dialog.setTitle(Messages.RequestTypeWarning);
                        dialog.getDialogPane().setMinSize(600, 350);
                        DialogHelper.positionDialog(dialog, traceTable, -600, -350);
                        final boolean optimize = dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES;
                        Preferences.setRawDataPrompt(! dialog.isHideSelected());
                        if (optimize)
                        {   // Restore checkbox
                            button.setSelected(true);
                            return;
                        }
                    }
                    new ChangeRequestTypeCommand(undoableActionManager, (PVItem)getTableRow().getItem(), type);
                });
                setGraphic(button);
            }
        }
    }

    /**
     * Custom {@link TableCell} for the Show Alarm Limits column. Idea is to hide it for non-{@link PVItem}s.
     */
    private static class AlarmVisibilityTableCell extends CheckBoxTableCell<ModelItem, Boolean> {

        @Override
        public void updateItem(final Boolean visible, final boolean empty) {
            super.updateItem(visible, empty);
            if(empty || getTableRow() == null){
                setGraphic(null);
                return;
            }

            ModelItem modelItem = getTableRow().getItem();
            if (modelItem instanceof FormulaItem) {
                // Showing alarm limits for formula items does not make sense...
                setGraphic(null);
            } else if (modelItem instanceof PVItem pvItem && (!pvItem.hasAlarmLimits() || pvItem.getRequestType().equals(RequestType.OPTIMIZED))) {
                // No alarm limits available, not RAW request
                setGraphic(null);
            }
        }
    }

    /** Update table if the model changes, for example via Un-do */
    private final ModelListener modelListener = new ModelListener()
    {
        @Override
        public void itemRemoved(final ModelItem item)
        {

            updateFromModel();
        }

        @Override
        public void itemAdded(final ModelItem item)
        {
            if(item instanceof AlarmLimitItem){
                return;
            }
            updateFromModel();
        }

        @Override
        public void changedItemVisibility(final ModelItem item)
        {
            traceTable.refresh();
        }

        @Override
        public void changedItemLook(final ModelItem item)
        {
            traceTable.refresh();
        }

        @Override
        public void changedItemDataConfig(final PVItem item, final boolean archive_invalid)
        {
            traceTable.refresh();
        }

        @Override
        public void changedAxis(final Optional<AxisConfig> axis)
        {
            if (traceTable.getEditingCell() != null)
            {
                Platform.runLater(() -> changedAxis(axis));
                return;
            }
            // In case an axis _name_ changed, this needs to be shown
            // in the "Axis" column.
            updateAxisNames();
        }

        @Override
        public void selectedSamplesChanged()
        {
            // Just trace_table.refresh() would 'work',
            // but much too heavy.
            // Instead, update the observable prop to item's value
            for (WeakReference<SelectedSampleProperty> ref : selectedSamples)
            {
                final SelectedSampleProperty prop = ref.get();
                if (prop != null)
                    prop.update();
            }
        }

        @Override
        public void itemHasAlarmLimits() {
            traceTable.refresh();
        }
    };

    private void updateFromModel()
    {
        traceTable.getItems().setAll(model.getItems().stream().filter(i -> !(i instanceof AlarmLimitItem)).collect(Collectors.toList()));
        updateAxisNames();
    }

    private void updateAxisNames()
    {
        final List<String> names = new ArrayList<>(model.getAxes().size());
        for (AxisConfig ai : model.getAxes())
            names.add(ai.getName());

        if (! names.equals(axisNames))
            axisNames.setAll(names);
    }

    /** Update archives or formula section when the selection model items change */
    final InvalidationListener selectionChanged = event ->
    {
        final ObservableList<ModelItem> items = traceTable.getSelectionModel().getSelectedItems();
        archivesTable.getItems().clear();
        // Note: Items in detail pane are set to the current value here.
        // They won't update if the model item changes, for example via undo/redo.
        if (items.size() == 1)
        {
            final ModelItem item = items.getFirst();
            if (item instanceof PVItem)
            {
                lowerPlaceholder.setVisible(false);
                archivesTable.setVisible(true);
                formulaPane.setVisible(false);
                archivesTable.getItems().setAll(((PVItem)item).getArchiveDataSources());
                formulaText.clear();
            }
            else if (item instanceof FormulaItem)
            {
                lowerPlaceholder.setVisible(false);
                archivesTable.setVisible(false);
                formulaPane.setVisible(true);
                archivesTable.getItems().clear();
                formulaText.setText(((FormulaItem)item).getExpression());
            }
        }
        else
        {
            lowerPlaceholder.setVisible(true);
            archivesTable.setVisible(false);
            formulaPane.setVisible(false);
            formulaText.clear();
        }
    };

    private void createContextMenu()
    {
        final MenuItem addPv = new AddPVorFormulaMenuItem(traceTable, model, undoableActionManager, false);
        final MenuItem addFormula = new AddPVorFormulaMenuItem(traceTable, model, undoableActionManager, true);
        final MenuItem addPvsFromClipboard = new AddPVsFromTheClipboardMenuItem(undoableActionManager, model, traceTable);
        final ContextMenu menu = new ContextMenu();

        traceTable.setOnContextMenuRequested(event ->
        {
            final ObservableList<MenuItem> items = menu.getItems();
            items.setAll(addPv, addFormula, addPvsFromClipboard);

            final List<ModelItem> selection = traceTable.getSelectionModel().getSelectedItems();

            if (selection.size() == 1)
                items.add(new MoveItemAction(model, undoableActionManager, selection.getFirst(), true));

            if (!selection.isEmpty())
                items.add(new DeleteItemsMenuItem(model, undoableActionManager, selection));

            if (selection.size() == 1)
                items.add(new MoveItemAction(model, undoableActionManager, selection.getFirst(), false));

            if (selection.size() > 1)
                items.add(new EditMultipleItemsAction(traceTable, model, undoableActionManager, selection));

            items.add(new SeparatorMenuItem());


            items.add(new ShowHideAllAction(model, undoableActionManager, true));
            items.add(new ShowHideAllAction(model, undoableActionManager, false));

            items.add(new SeparatorMenuItem());

            // Add PV-based entries
            final List<ProcessVariable> pvs = selection.stream()
                    .filter(item -> item instanceof PVItem)
                    .map(item -> new ProcessVariable(item.getResolvedName()))
                    .collect(Collectors.toList());
            if (!pvs.isEmpty())
            {
                SelectionService.getInstance().setSelection(this, pvs);
                ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(traceTable), menu);
            }

            menu.show(traceTable.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    @FXML
    public void handleFormula(){
        if (traceTable.getSelectionModel().getSelectedItem() instanceof FormulaItem formulaItem) {
            if (FormulaItemEditor.run(formulaText, formulaItem, undoableActionManager))
                Platform.runLater(() -> formulaText.setText(formulaItem.getExpression()));
        }
    }
}