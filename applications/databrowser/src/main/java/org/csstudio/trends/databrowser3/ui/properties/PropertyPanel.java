/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;


import java.time.Instant;
import java.util.Optional;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.phoebus.archive.vtype.DefaultVTypeFormat;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.TimestampFormats;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;

/** Property panel
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertyPanel extends TabPane
{
    private static Tab traces = new Tab(Messages.TracesTab);
    private static Tab time_axis = new Tab(Messages.TimeAxis);
    private static Tab value_axes = new Tab(Messages.ValueAxes);
    private static Tab misc = new Tab(Messages.Miscellaneous);

    private static final ObservableList<String> trace_types = FXCollections.observableArrayList(TraceType.getDisplayNames());
    private static final ObservableList<String> point_types = FXCollections.observableArrayList(PointType.getDisplayNames());
    private static final ObservableList<String> request_types = FXCollections.observableArrayList(Messages.Request_raw, Messages.Request_optimized);

    private final UndoableActionManager undo;
    private final TableView<ModelItem> trace_table = new TableView<>();
    private final ObservableList<String> axis_names = FXCollections.observableArrayList();

    /** Prompt for the 'hide trace' warning'? */
    private static boolean prompt_for_not_visible = true;

    /** Prompt for the 'raw request' warning? */
    private static boolean prompt_for_raw_data_request = true;

    /** Update table if the model changes, for example via Un-do */
    private final ModelListener model_listener = new ModelListener()
    {
        @Override
        public void itemRemoved(final ModelItem item)
        {
            trace_table.refresh();
        }

        @Override
        public void itemAdded(final ModelItem item)
        {
            trace_table.refresh();
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
        public void changedItemDataConfig(final PVItem item)
        {
            trace_table.refresh();
        }

        @Override
        public void changedAxis(final Optional<AxisConfig> axis)
        {
            // In case an axis _name_ changed, this needs to be shown
            // in the "Axis" column.
            trace_table.refresh();
        }
    };



    /** Table cell that shows ColorPicker */
    private static class ColorTableCell extends TableCell<ModelItem, ColorPicker>
    {
        // The color_column's CellValueFactory already turned the ColorSection into ColorPicker
        // Show place the picker in the cell
        @Override
        protected void updateItem(final ColorPicker picker, final boolean empty)
        {
            super.updateItem(picker, empty);
            setGraphic(empty ? null : picker);
        }
    }


    /** Table cell that shows RequestType */
    private class RequestTypeCell extends TableCell<ModelItem, RequestType>
    {
        final CheckBox button = new CheckBox();

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

                    if (type == RequestType.RAW  &&  prompt_for_raw_data_request)
                    {
                        final Alert dialog = new Alert(AlertType.CONFIRMATION,
                                Messages.RequestTypeWarningDetail,
                                ButtonType.YES, ButtonType.NO);
                        dialog.setHeaderText(Messages.RequestTypeWarning);
                        dialog.setResizable(true);
                        dialog.getDialogPane().setMinSize(600, 350);
                        DialogHelper.positionDialog(dialog, trace_table, -600, -350);
                        if (dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
                        {   // Restore checkbox
                            button.setSelected(true);
                            return;
                        }
                        prompt_for_raw_data_request = false;
                    }
                    new ChangeRequestTypeCommand(undo, (PVItem)getTableRow().getItem(), type);
                });
                setGraphic(button);
            }
        }
    }

    public PropertyPanel(final Model model, final UndoableActionManager undo)
    {
        super(traces, time_axis, value_axes, misc);
        this.undo = undo;

        for (Tab tab : getTabs())
            tab.setClosable(false);

        createTracesTab();

        setModel(model);
    }
    private void createTracesTab()
    {
        // Top: Traces
        createTracesTabItemPanel();

        // Bottom: Archives for selected trace
        final Node archives = new Label("TODO Archives");

        final SplitPane top_bottom = new SplitPane(trace_table, archives);
        top_bottom.setOrientation(Orientation.VERTICAL);
        top_bottom.setDividerPositions(0.6);
        traces.setContent(top_bottom);
    }

    private void createTracesTabItemPanel()
    {
        // Visible Column ----------
        TableColumn<ModelItem, Boolean> vis_col = new TableColumn<>(Messages.TraceVisibility);
        vis_col.setCellValueFactory(cell ->
        {
            final SimpleBooleanProperty vis_property = new SimpleBooleanProperty(cell.getValue().isVisible());
            // Update model when CheckBoxTableCell updates this property
            vis_property.addListener((p, old, visible) ->
            {
                if (! visible  &&  prompt_for_not_visible)
                {
                    final Alert dialog = new Alert(AlertType.CONFIRMATION,
                                                    Messages.HideTraceWarningDetail,
                                                    ButtonType.YES, ButtonType.NO);
                    dialog.setHeaderText(Messages.HideTraceWarning);
                    dialog.setResizable(true);
                    dialog.getDialogPane().setMinSize(600, 350);
                    DialogHelper.positionDialog(dialog, trace_table, -600, -350);
                    if (dialog.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
                    {   // Restore checkbox
                        vis_property.set(true);
                        return;
                    }
                    prompt_for_not_visible = false;
                }
                new ChangeVisibilityCommand(undo, cell.getValue(), visible);
            });
            return vis_property;
        });
        vis_col.setCellFactory(CheckBoxTableCell.forTableColumn(vis_col));
        trace_table.getColumns().add(vis_col);

        // Trace PV/Formula Column ----------
        TableColumn<ModelItem, String> col = new TableColumn<>(Messages.ItemName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        // TODO Add PV Name completion
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
        trace_table.getColumns().add(col);

        // Display Name Column ----------
        col = new TableColumn<>(Messages.TraceDisplayName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayName()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            new ChangeDisplayNameCommand(undo, event.getRowValue(), event.getNewValue());
        });
        col.setEditable(true);
        trace_table.getColumns().add(col);

        // Color Column ----------
        TableColumn<ModelItem, ColorPicker> color_col = new TableColumn<>(Messages.Color);
        color_col.setCellValueFactory(cell ->
        {
            final Color color = cell.getValue().getPaintColor();
            final ColorPicker picker = new ColorPicker(color);
            picker.setStyle("-fx-color-label-visible: false ;");
            picker.setOnAction(event ->
                new ChangeColorCommand(undo, cell.getValue(), picker.getValue()));
            return new SimpleObjectProperty<>(picker);
        });
        color_col.setCellFactory(cell -> new ColorTableCell());
        trace_table.getColumns().add(color_col);

        // Selected sample time stamp and value
        col = new TableColumn<>(Messages.CursorTimestamp);
        col.setCellValueFactory(cell ->
        {
            final Optional<PlotDataItem<Instant>> sample = cell.getValue().getSelectedSample();
            final String text = sample.isPresent()
                              ? TimestampFormats.MILLI_FORMAT.format(sample.get().getPosition())
                              : Messages.NotApplicable;
            return new SimpleStringProperty(text);
        });
        trace_table.getColumns().add(col);
        col = new TableColumn<>(Messages.CursorValue);
        col.setCellValueFactory(cell ->
        {
            final ModelItem item = cell.getValue();
            final Optional<PlotDataItem<Instant>> sample = item.getSelectedSample();
            String text;
            if (sample.isPresent())
            {

                text = DefaultVTypeFormat.get().format( ((PlotSample) sample.get()).getVType() );
                final String units = item.getUnits();
                if (units != null)
                    text = text + " " + units;
            }
            else
                text = Messages.NotApplicable;
            return new SimpleStringProperty(text);
        });
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
        col.setCellFactory(TextFieldTableCell.forTableColumn());
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
        trace_table.getColumns().add(col);

        // Trace Type Column ----------
        col = new TableColumn<>(Messages.TraceType);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getTraceType().toString()));
        col.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(trace_types));
        col.setOnEditCommit(event ->
        {
            final int index = trace_types.indexOf(event.getNewValue());
            final TraceType type = TraceType.values()[index];
            new ChangeTraceTypeCommand(undo, event.getRowValue(), type);
        });
        col.setEditable(true);
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
        trace_table.getColumns().add(col);

        // Point Type Column ----------
        col = new TableColumn<>(Messages.PointType);
        col.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getPointType().toString()));
        col.setCellFactory(cell -> new DirectChoiceBoxTableCell<>(point_types));
        col.setOnEditCommit(event ->
        {
            final int index = point_types.indexOf(event.getNewValue());
            final PointType type = PointType.values()[index];
            new ChangePointTypeCommand(undo, event.getRowValue(), type);
        });
        col.setEditable(true);
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
        col = new TableColumn<>(Messages.WaveformIndex);
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
        trace_table.getColumns().add(col);




        trace_table.setEditable(true);


        // TODO Cursor value update
        // TODO Add tool tips
    }


    private void setModel(final Model model)
    {
        // TODO Replace initial population from model with model listener
        for (ModelItem item : model.getItems())
            trace_table.getItems().add(item);

        axis_names.clear();
        for (AxisConfig ai : model.getAxes())
            axis_names.add(ai.getName());

        model.addListener(model_listener);
    }
}
