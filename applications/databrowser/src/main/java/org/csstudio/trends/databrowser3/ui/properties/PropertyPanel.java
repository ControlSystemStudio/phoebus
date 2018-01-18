/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;


import static org.csstudio.trends.databrowser3.Activator.logger;

import java.util.logging.Level;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
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
public class PropertyPanel extends TabPane
{
    private static Tab traces = new Tab(Messages.TracesTab);
    private static Tab time_axis = new Tab(Messages.TimeAxis);
    private static Tab value_axes = new Tab(Messages.ValueAxes);
    private static Tab misc = new Tab(Messages.Miscellaneous);

    private final TableView<ModelItem> trace_table = new TableView<>();


    public PropertyPanel(final Model model)
    {
        super(traces, time_axis, value_axes, misc);
        for (Tab tab : getTabs())
            tab.setClosable(false);

        createTracesTab();

        // TODO Replace initial population from model with model listener
        for (ModelItem item : model.getItems())
            trace_table.getItems().add(item);
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

    private void createTracesTabItemPanel()
    {
        // Visible Column ----------
        TableColumn<ModelItem, Boolean> vis_col = new TableColumn<>(Messages.TraceVisibility);
        vis_col.setCellValueFactory(cell ->
        {
            final SimpleBooleanProperty vis_property = new SimpleBooleanProperty(cell.getValue().isVisible());
            // Update model when CheckBoxTableCell updates this property
            vis_property.addListener((p, old, visible) -> cell.getValue().setVisible(visible));
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
                event.getRowValue().setName(event.getNewValue());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot change name of" + event.getRowValue() , ex);
            }
        });
        col.setEditable(true);
        trace_table.getColumns().add(col);

        // Display Name Column ----------
        col = new TableColumn<>(Messages.TraceDisplayName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayName()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event -> event.getRowValue().setDisplayName(event.getNewValue()));
        col.setEditable(true);
        trace_table.getColumns().add(col);

        // Color Column ----------
        TableColumn<ModelItem, ColorPicker> color_col = new TableColumn<>(Messages.Color);
        color_col.setCellValueFactory(cell ->
        {
            final Color color = cell.getValue().getPaintColor();
            final ColorPicker picker = new ColorPicker(color);
            picker.setStyle("-fx-color-label-visible: false ;");
            picker.setOnAction(event -> cell.getValue().setColor(picker.getValue()));
            return new SimpleObjectProperty<>(picker);
        });
        color_col.setCellFactory(cell -> new ColorTableCell());
        trace_table.getColumns().add(color_col);



        trace_table.setEditable(true);


        // TODO Auto-generated method stub
    }
}
