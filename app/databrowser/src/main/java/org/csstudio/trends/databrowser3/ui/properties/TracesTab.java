/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.archive.vtype.DefaultVTypeFormat;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.AlertWithToggle;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.SecondsParser;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.converter.DefaultStringConverter;

/** Property tab for traces (items, archives)
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TracesTab extends Tab
{
    // Earlier version used
    //   trace_types = FXCollections.observableArrayList(TraceType.getDisplayNames());
    // and passed that to the UI.
    // But UI will listen to changes in observable array. The static list would then
    // keep receiving new listeners added to the UI, and thus keep the UI elements and
    // their data in memory.
    // Now keep only the trace & point types as plain list,
    // and UI creates a short-time observable list while running, which is then disposed as UI closes.
    private static final List<String> trace_types = List.of(TraceType.getDisplayNames());
    private static final List<String> line_styles = List.of(LineStyle.getDisplayNames());
    private static final List<String> point_types = List.of(PointType.getDisplayNames());

    private final Model model;

    private final UndoableActionManager undo;

    private final TableView<ModelItem> trace_table = new TableView<>();

    private Label lower_placeholder = new Label(Messages.SelectTrace);

    private final TableView<ArchiveDataSource> archives_table = new TableView<>();

    private Pane formula_pane;

    private final TextField formula_txt = new TextField();

    private final ObservableList<String> axis_names = FXCollections.observableArrayList();

    /** Wrapper that provides ModelItem's selected sample as observable */
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

    private final List<WeakReference<SelectedSampleProperty>> selected_samples = new ArrayList<>();

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
                        final AlertWithToggle dialog = new AlertWithToggle(AlertType.CONFIRMATION,
                                Messages.RequestTypeWarningDetail,
                                ButtonType.YES, ButtonType.NO);
                        dialog.setTitle(Messages.RequestTypeWarning);
                        dialog.getDialogPane().setMinSize(600, 350);
                        DialogHelper.positionDialog(dialog, trace_table, -600, -350);
                        final boolean optimize = dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES;
                        Preferences.setRawDataPrompt(! dialog.isHideSelected());
                        if (optimize)
                        {   // Restore checkbox
                            button.setSelected(true);
                            return;
                        }
                    }
                    new ChangeRequestTypeCommand(undo, (PVItem)getTableRow().getItem(), type);
                });
                setGraphic(button);
            }
        }
    }

    /** Update table if the model changes, for example via Un-do */
    private final ModelListener model_listener = new ModelListener()
    {
        @Override
        public void itemRemoved(final ModelItem item)
        {
            updateFromModel();
        }

        @Override
        public void itemAdded(final ModelItem item)
        {
            updateFromModel();
        }

        @Override
        public void changedItemVisibility(final ModelItem item)
        {
            trace_table.refresh();
        }

        @Override
        public void changedItemLook(final ModelItem item)
        {
            trace_table.refresh();
        }

        @Override
        public void changedItemDataConfig(final PVItem item, final boolean archive_invalid)
        {
            trace_table.refresh();
        }

        @Override
        public void changedAxis(final Optional<AxisConfig> axis)
        {
            if (trace_table.getEditingCell() != null)
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
            for (WeakReference<SelectedSampleProperty> ref : selected_samples)
            {
                final SelectedSampleProperty prop = ref.get();
                if (prop != null)
                    prop.update();
            }
        }
    };

    /** Update archives or formula section when the selection model items change */
    final InvalidationListener selection_changed = event ->
    {
        final ObservableList<ModelItem> items = trace_table.getSelectionModel().getSelectedItems();
        archives_table.getItems().clear();
        // Note: Items in detail pane are set to the current value here.
        // They won't update if the model item changes, for example via undo/redo.
        if (items.size() == 1)
        {
            final ModelItem item = items.get(0);
            if (item instanceof PVItem)
            {
                lower_placeholder.setVisible(false);
                archives_table.setVisible(true);
                formula_pane.setVisible(false);
                archives_table.getItems().setAll(((PVItem)item).getArchiveDataSources());
                formula_txt.clear();
            }
            else if (item instanceof FormulaItem)
            {
                lower_placeholder.setVisible(false);
                archives_table.setVisible(false);
                formula_pane.setVisible(true);
                archives_table.getItems().clear();
                formula_txt.setText(((FormulaItem)item).getExpression());
            }
        }
        else
        {
            lower_placeholder.setVisible(true);
            archives_table.setVisible(false);
            formula_pane.setVisible(false);
            formula_txt.clear();
            formula_txt.clear();
        }
    };

    TracesTab(final Model model, final UndoableActionManager undo)
    {
        super(Messages.TracesTab);
        this.model = model;
        this.undo = undo;

        // Top: Traces
        createTraceTable();

        // Bottom: Archives for selected trace
        createArchivesTable();
        createDetailSection();

        archives_table.setVisible(false);
        formula_pane.setVisible(false);

        final StackPane details = new StackPane(lower_placeholder, archives_table, formula_pane);

        trace_table.setPlaceholder(new Label(Messages.TraceTableEmpty));
        final SplitPane top_bottom = new SplitPane(trace_table, details);
        top_bottom.setOrientation(Orientation.VERTICAL);
        Platform.runLater(() -> top_bottom.setDividerPositions(0.7));

        setContent(top_bottom);

        createContextMenu();

        model.addListener(model_listener);
        updateFromModel();

        trace_table.getSelectionModel().getSelectedItems().addListener(selection_changed);
    }

    private void createDetailSection()
    {
        final Label label = new Label(Messages.FormulaLabel);
        HBox.setHgrow(formula_txt, Priority.ALWAYS);
        formula_txt.setEditable(false);
        formula_txt.setOnMousePressed(event ->
        {
            final ModelItem item = trace_table.getSelectionModel().getSelectedItem();
            if (item instanceof FormulaItem)
            {
                final FormulaItem fitem = (FormulaItem) item;
                if (FormulaItemEditor.run(formula_txt, fitem, undo))
                    Platform.runLater(() -> formula_txt.setText(fitem.getExpression()));
            }
        });
        final HBox row = new HBox(5, label, formula_txt);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(5));
        final Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);
        formula_pane = new VBox(row, filler);
    }

    private void updateFromModel()
    {
        trace_table.getItems().setAll(model.getItems());
        updateAxisNames();
    }

    private void updateAxisNames()
    {
        final List<String> names = new ArrayList<>(model.getAxes().size());
        for (AxisConfig ai : model.getAxes())
            names.add(ai.getName());

        if (! names.equals(axis_names))
            axis_names.setAll(names);
    }

    private void createTraceTable()
    {
        // Visible Column ----------
        TableColumn<ModelItem, Boolean> vis_col = new TableColumn<>(Messages.TraceVisibility);
        vis_col.setCellValueFactory(cell ->
        {
            final SimpleBooleanProperty vis_property = new SimpleBooleanProperty(cell.getValue().isVisible());
            // Update model when CheckBoxTableCell updates this property
            vis_property.addListener((p, old, visible) ->
            {
                if (! visible  &&  Preferences.prompt_for_visibility)
                {
                    final AlertWithToggle dialog = new AlertWithToggle(AlertType.CONFIRMATION,
                                                    Messages.HideTraceWarningDetail,
                                                    ButtonType.YES, ButtonType.NO);
                    dialog.setTitle(Messages.HideTraceWarning);
                    dialog.getDialogPane().setMinSize(600, 350);
                    DialogHelper.positionDialog(dialog, trace_table, -600, -350);
                    final boolean dont_hide = dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES;
                    Preferences.setVisibilityPrompt(! dialog.isHideSelected());
                    if (dont_hide)
                    {   // Restore checkbox
                        vis_property.set(true);
                        return;
                    }
                }
                new ChangeVisibilityCommand(undo, cell.getValue(), visible);
            });
            return vis_property;
        });
        vis_col.setCellFactory(CheckBoxTableCell.forTableColumn(vis_col));
        PropertyPanel.addTooltip(vis_col, Messages.TraceVisibilityTT);
        trace_table.getColumns().add(vis_col);

        // Trace PV/Formula Column ----------
        TableColumn<ModelItem, String> col = new TableColumn<>(Messages.ItemName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResolvedName()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            try
            {
                new ChangeNameCommand(undo, event.getRowValue(), event.getNewValue());
            }
            catch (Exception ex)
            {
                trace_table.refresh();
            }
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.ItemNameTT);
        trace_table.getColumns().add(col);

        // Display Name Column ----------
        col = new TableColumn<>(Messages.TraceDisplayName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResolvedDisplayName()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            new ChangeDisplayNameCommand(undo, event.getRowValue(), event.getNewValue());
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.TraceDisplayNameTT);
        trace_table.getColumns().add(col);

        // Color Column ----------
        TableColumn<ModelItem, ColorPicker> color_col = new TableColumn<>(Messages.Color);
        color_col.setCellValueFactory(cell ->
        {
            final Color color = cell.getValue().getPaintColor();
            final ColorPicker picker = PropertyPanel.ColorTableCell.createPicker(color);
            picker.setOnAction(event ->
                new ChangeColorCommand(undo, cell.getValue(), picker.getValue()));
            return new SimpleObjectProperty<>(picker);
        });
        color_col.setCellFactory(cell -> new PropertyPanel.ColorTableCell<>());
        PropertyPanel.addTooltip(color_col, Messages.ColorTT);
        trace_table.getColumns().add(color_col);

        // Selected sample
        col = new TableColumn<>(Messages.CursorValue);
        col.setCellValueFactory(cell ->
        {
            final SelectedSampleProperty prop = new SelectedSampleProperty(cell.getValue());
            selected_samples.add(new WeakReference<>(prop));
            return prop;
        });
        PropertyPanel.addTooltip(col, Messages.CursorValueTT);
        trace_table.getColumns().add(col);

        // Scan Period Column (only applies to PVItems) ----------
        col = new TableColumn<>(Messages.ScanPeriod);
        col.setCellValueFactory(cell ->
        {
            final ModelItem item = cell.getValue();
            if (item instanceof PVItem)
                return new SimpleStringProperty(Double.toString(((PVItem)item).getScanPeriod()));
            else
                return new SimpleStringProperty(Messages.NotApplicable);

        });
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            if (item instanceof PVItem)
            {
                try
                {
                    new ChangeSamplePeriodCommand(undo, (PVItem)item, Double.parseDouble(event.getNewValue()));
                }
                catch (Exception e)
                {
                    trace_table.refresh();
                }
            }
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.ScanPeriodTT);
        trace_table.getColumns().add(col);

        // Buffer size Column (only applies to PVItems) ----------
        col = new TableColumn<>(Messages.LiveSampleBufferSize);
        col.setCellValueFactory(cell ->
        {
            final ModelItem item = cell.getValue();
            if (item instanceof PVItem)
                return new SimpleStringProperty(Integer.toString(((PVItem)item).getLiveCapacity()));
            else
                return new SimpleStringProperty(Messages.NotApplicable);

        });
        col.setCellFactory(c ->
        {
            final TextFieldTableCell<ModelItem, String> cell = new TextFieldTableCell<>(new DefaultStringConverter())
            {
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
            };
            return cell;
        });
        col.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            if (item instanceof PVItem)
            {
                try
                {
                    new ChangeLiveCapacityCommand(undo, (PVItem)item, Integer.parseInt(event.getNewValue()));
                }
                catch (Exception e)
                {
                    trace_table.refresh();
                }
            }
        });
        col.setEditable(true);
        trace_table.getColumns().add(col);

        // Axis Column ----------
        col = new TableColumn<>(Messages.Axis);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getAxis().getName()));
        col.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(axis_names));
        col.setOnEditCommit(event ->
        {
            final int index = axis_names.indexOf(event.getNewValue());
            final AxisConfig axis = event.getRowValue().getModel().get().getAxis(index);
            new ChangeAxisCommand(undo, event.getRowValue(), axis);
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.AxisTT);
        trace_table.getColumns().add(col);

        // Trace Type Column ----------
        col = new TableColumn<>(Messages.TraceType);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getTraceType().toString()));
        col.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(FXCollections.observableArrayList(trace_types)));
        col.setOnEditCommit(event ->
        {
            final int index = trace_types.indexOf(event.getNewValue());
            final TraceType type = TraceType.values()[index];
            new ChangeTraceTypeCommand(undo, event.getRowValue(), type);
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.TraceTypeTT);
        trace_table.getColumns().add(col);

        // Line Width Column ----------
        col = new TableColumn<>(Messages.TraceLineWidth);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(Integer.toString(cell.getValue().getLineWidth())));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            try
            {
                new ChangeLineWidthCommand(undo, item, Integer.parseInt(event.getNewValue()));
            }
            catch (Exception e)
            {
                trace_table.refresh();
            }
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.TraceLineWidthTT);
        trace_table.getColumns().add(col);

        // Line Style Type Column ----------
        col = new TableColumn<>(Messages.TraceLineStyle);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getLineStyle().toString()));
        col.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(FXCollections.observableArrayList(line_styles)));
        col.setOnEditCommit(event ->
        {
            final int index = line_styles.indexOf(event.getNewValue());
            final LineStyle style = LineStyle.values()[index];
            new ChangeLineStyleCommand(undo, event.getRowValue(), style);
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.TraceLineStyleTT);
        trace_table.getColumns().add(col);

        // Point Type Column ----------
        col = new TableColumn<>(Messages.PointType);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getPointType().toString()));
        col.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(FXCollections.observableArrayList(point_types)));
        col.setOnEditCommit(event ->
        {
            final int index = point_types.indexOf(event.getNewValue());
            final PointType type = PointType.values()[index];
            new ChangePointTypeCommand(undo, event.getRowValue(), type);
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.PointTypeTT);
        trace_table.getColumns().add(col);

        // Point Size Column ----------
        col = new TableColumn<>(Messages.PointSize);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(Integer.toString(cell.getValue().getPointSize())));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            try
            {
                new ChangePointSizeCommand(undo, item, Integer.parseInt(event.getNewValue()));
            }
            catch (Exception e)
            {
                trace_table.refresh();
            }
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.PointSizeTT);
        trace_table.getColumns().add(col);

        // Request Type Column ----------
        final TableColumn<ModelItem, RequestType> req_col = new TableColumn<>(Messages.RequestType);
        req_col.setCellValueFactory(cell ->
        {
            final RequestType type;
            if (cell.getValue() instanceof PVItem)
                type = ((PVItem)cell.getValue()).getRequestType();
            else
                type = null;
            return new SimpleObjectProperty<>(type);
        });
        req_col.setCellFactory(cell -> new RequestTypeCell());
        trace_table.getColumns().add(req_col);

        // Waveform Index Column ----------
        col = new TableColumn<>(Messages.WaveformIndexCol);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(Integer.toString(cell.getValue().getWaveformIndex())));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            final ModelItem item = event.getRowValue();
            try
            {
                new ChangeWaveformIndexCommand(undo, item, Integer.parseInt(event.getNewValue()));
            }
            catch (Exception e)
            {
                trace_table.refresh();
            }
        });
        col.setEditable(true);
        PropertyPanel.addTooltip(col, Messages.WaveformIndexColTT);
        trace_table.getColumns().add(col);

        trace_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        trace_table.setEditable(true);
        trace_table.getColumns().forEach(c -> c.setSortable(false));
    }

    private void createArchivesTable()
    {
        archives_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        archives_table.setPlaceholder(new Label(Messages.ArchiveListGUI_NoArchives));

        // Archive Name Column ----------
        TableColumn<ArchiveDataSource, String> col = new TableColumn<>(Messages.ArchiveName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        archives_table.getColumns().add(col);

        // Archive Server URL Column ----------
        col = new TableColumn<>(Messages.URL);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUrl()));
        archives_table.getColumns().add(col);

        archives_table.getColumns().forEach(c -> c.setSortable(false));
    }

    private void createContextMenu()
    {
        final MenuItem add_pv = new AddPVorFormulaMenuItem(trace_table, model, undo, false);
        final MenuItem add_formula = new AddPVorFormulaMenuItem(trace_table, model, undo, true);
        final ContextMenu menu = new ContextMenu();

        trace_table.setOnContextMenuRequested(event ->
        {
            final ObservableList<MenuItem> items = menu.getItems();
            items.setAll(add_pv, add_formula);

            final List<ModelItem> selection = trace_table.getSelectionModel().getSelectedItems();

            if (selection.size() == 1)
                items.add(new MoveItemAction(model, undo, selection.get(0), true));

            if (selection.size() > 0)
                items.add(new DeleteItemsMenuItem(model, undo, selection));

            if (selection.size() == 1)
                items.add(new MoveItemAction(model, undo, selection.get(0), false));

            if (selection.size() > 1)
                items.add(new EditMultipleItemsAction(trace_table, model, undo, selection));

            items.add(new SeparatorMenuItem());

            // Add PV-based entries
            final List<ProcessVariable> pvs = selection.stream()
                                                       .filter(item -> item instanceof PVItem)
                                                       .map(item -> new ProcessVariable(item.getResolvedName()))
                                                       .collect(Collectors.toList());
            if (pvs.size() > 0)
            {
                SelectionService.getInstance().setSelection(this, pvs);
                ContextMenuHelper.addSupportedEntries(trace_table, menu);
            }

            menu.show(trace_table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }
}
