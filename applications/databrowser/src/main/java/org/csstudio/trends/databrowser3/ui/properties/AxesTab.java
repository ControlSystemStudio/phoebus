/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;

/** Property tab for axes
 *  @author Kay Kasemir
 */
public class AxesTab extends Tab
{
    private final Model model;
    private final UndoableActionManager undo;
    private ObservableList<AxisConfig> axes = FXCollections.observableArrayList();

    private final TableView<AxisConfig> axes_table = new TableView<>(axes);


    public AxesTab(Model model, UndoableActionManager undo)
    {
        super(Messages.ValueAxes);
        this.model = model;
        this.undo = undo;

        createAxesTable();

        setContent(axes_table);

        // TODO Model listener
        updateFromModel();
    }

    private void updateFromModel()
    {
        axes.setAll(model.getAxes());
    }

    private TableColumn<AxisConfig, Boolean> createCheckboxColumn(final String label,
            final Function<AxisConfig, Boolean> getter,
            final BiConsumer<AxisConfig, Boolean> setter)
    {
        final TableColumn<AxisConfig, Boolean> check_col = new TableColumn<>(label);
        check_col.setCellValueFactory(cell ->
        {
            final AxisConfig axis = cell.getValue();
            final BooleanProperty prop = new SimpleBooleanProperty(getter.apply(axis));
            prop.addListener((p, old, value) ->
            {
                final ChangeAxisConfigCommand command = new ChangeAxisConfigCommand(undo, axis);
                setter.accept(axis, value);
                command.rememberNewConfig();
            });
            return prop;
        });
        check_col.setCellFactory(CheckBoxTableCell.forTableColumn(check_col));
        return check_col;
    }

    private void createAxesTable()
    {
        axes_table.getColumns().add(createCheckboxColumn(Messages.AxisVisibility,
                AxisConfig::isVisible, AxisConfig::setVisible));

        TableColumn<AxisConfig, String> col = new TableColumn<>(Messages.ValueAxisName);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            final AxisConfig axis = event.getRowValue();
            final ChangeAxisConfigCommand command = new ChangeAxisConfigCommand(undo, axis);
            axis.setName(event.getNewValue());
            command.rememberNewConfig();
        });
        col.setEditable(true);
        axes_table.getColumns().add(col);

        axes_table.getColumns().add(createCheckboxColumn(Messages.UseAxisName,
                AxisConfig::isUsingAxisName, AxisConfig::useAxisName));

        axes_table.getColumns().add(createCheckboxColumn(Messages.UseTraceNames,
                AxisConfig::isUsingTraceNames, AxisConfig::useTraceNames));

        axes_table.getColumns().add(createCheckboxColumn(Messages.Grid,
                AxisConfig::isGridVisible, AxisConfig::setGridVisible));

        axes_table.getColumns().add(createCheckboxColumn(Messages.AxisOnRight,
                AxisConfig::isUsingAxisName, AxisConfig::setOnRight));

        // Color Column ----------
        TableColumn<AxisConfig, ColorPicker> color_col = new TableColumn<>(Messages.Color);
        color_col.setCellValueFactory(cell ->
        {
            final Color color = cell.getValue().getPaintColor();
            final ColorPicker picker = PropertyPanel.ColorTableCell.createPicker(color);
//            picker.setOnAction(event ->
//                new ChangeAxisColorCommand(undo, cell.getValue(), picker.getValue()));
            return new SimpleObjectProperty<>(picker);
        });
        color_col.setCellFactory(cell -> new PropertyPanel.ColorTableCell<>());
        PropertyPanel.addTooltip(color_col, Messages.ColorTT);
        axes_table.getColumns().add(color_col);

        col = new TableColumn<>(Messages.AxisMin);
        col.setCellValueFactory(cell -> new SimpleStringProperty(Double.toString(cell.getValue().getMin())));
        axes_table.getColumns().add(col);

        col = new TableColumn<>(Messages.AxisMax);
        col.setCellValueFactory(cell -> new SimpleStringProperty(Double.toString(cell.getValue().getMax())));
        axes_table.getColumns().add(col);

        axes_table.getColumns().add(createCheckboxColumn(Messages.AutoScale,
                AxisConfig::isAutoScale, AxisConfig::setAutoScale));

        axes_table.getColumns().add(createCheckboxColumn(Messages.LogScale,
                AxisConfig::isLogScale, AxisConfig::setLogScale));

        axes_table.setEditable(true);
    }
}
