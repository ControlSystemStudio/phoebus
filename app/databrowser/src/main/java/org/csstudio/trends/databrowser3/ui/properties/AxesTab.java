/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveRescale;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Property tab for axes
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AxesTab extends Tab
{
    private final Model model;

    private final UndoableActionManager undo;

    private ObservableList<AxisConfig> axes = FXCollections.observableArrayList();

    /** One toggle per {@link ArchiveRescale} option */
    private final ToggleGroup rescale = new ToggleGroup();

    private final TableView<AxisConfig> axes_table = new TableView<>(axes);

    /** Flag to prevent recursion when this tab updates the model and thus triggers the model_listener */
    private boolean updating = false;

    /** Update Tab when model changes (undo, ...) */
    private ModelListener model_listener = new ModelListener()
    {
        @Override
        public void changedArchiveRescale()
        {
            if (updating)
                return;
            rescale.selectToggle(rescale.getToggles().get(model.getArchiveRescale().ordinal()));
        }

        @Override
        public void changedAxis(final Optional<AxisConfig> axis)
        {
            if (updating)
                return;
            if (axis.isPresent())
                axes_table.refresh();
            else
                updateFromModel();
        }
    };


    AxesTab(final Model model, final UndoableActionManager undo)
    {
        super(Messages.ValueAxes);
        this.model = model;
        this.undo = undo;

        final Label label = new Label(Messages.ArchiveRescale_Label);
        final RadioButton rescale_none = new RadioButton(Messages.ArchiveRescale_NONE),
                          rescale_stagger = new RadioButton(Messages.ArchiveRescale_STAGGER);
        rescale_none.setToggleGroup(rescale);
        rescale_stagger.setToggleGroup(rescale);
        rescale_stagger.selectedProperty().addListener((p, o, stagger) ->
        {
            updating = true;
            new ChangeArchiveRescaleCommand(model, undo, stagger ? ArchiveRescale.STAGGER : ArchiveRescale.NONE);
            updating = false;
        });

        final HBox rescales = new HBox(5, label, rescale_none, rescale_stagger);
        rescales.setAlignment(Pos.CENTER_LEFT);
        rescales.setPadding(new Insets(5));

        createAxesTable();

        createContextMenu();

        setContent(new VBox(5, rescales, axes_table));

        updateFromModel();
        model.addListener(model_listener);
    }

    private void updateFromModel()
    {
        rescale.selectToggle(rescale.getToggles().get(model.getArchiveRescale().ordinal()));
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
                updating = true;
                setter.accept(axis, value);
                updating = false;
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
            updating = true;
            axis.setName(event.getNewValue());
            updating = false;
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
                AxisConfig::isOnRight, AxisConfig::setOnRight));

        // Color Column ----------
        TableColumn<AxisConfig, ColorPicker> color_col = new TableColumn<>(Messages.Color);
        color_col.setCellValueFactory(cell ->
        {
            final AxisConfig axis = cell.getValue();
            final ColorPicker picker = PropertyPanel.ColorTableCell.createPicker(axis.getPaintColor());
            picker.setOnAction(event ->
            {
                final ChangeAxisConfigCommand command = new ChangeAxisConfigCommand(undo, axis);
                updating = true;
                axis.setColor(picker.getValue());
                updating = false;
                command.rememberNewConfig();
            });
            return new SimpleObjectProperty<>(picker);
        });
        color_col.setCellFactory(cell -> new PropertyPanel.ColorTableCell<>());
        PropertyPanel.addTooltip(color_col, Messages.ColorTT);
        axes_table.getColumns().add(color_col);

        col = new TableColumn<>(Messages.AxisMin);
        col.setCellValueFactory(cell -> new SimpleStringProperty(Double.toString(cell.getValue().getMin())));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            updating = true;
            try
            {
                final AxisConfig axis = event.getRowValue();
                final ChangeAxisConfigCommand command = new ChangeAxisConfigCommand(undo, axis);
                axis.setRange(Double.parseDouble(event.getNewValue()), axis.getMax());
                command.rememberNewConfig();
            }
            catch (NumberFormatException ex)
            {
                // NOP, leave as is
            }
            updating = false;
        });
        col.setEditable(true);
        axes_table.getColumns().add(col);

        col = new TableColumn<>(Messages.AxisMax);
        col.setCellValueFactory(cell -> new SimpleStringProperty(Double.toString(cell.getValue().getMax())));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event ->
        {
            updating = true;
            try
            {
                final AxisConfig axis = event.getRowValue();
                final ChangeAxisConfigCommand command = new ChangeAxisConfigCommand(undo, axis);
                axis.setRange(axis.getMin(), Double.parseDouble(event.getNewValue()));
                command.rememberNewConfig();
            }
            catch (NumberFormatException ex)
            {
                // NOP, leave as is
            }
            updating = false;
        });
        col.setEditable(true);
        axes_table.getColumns().add(col);

        axes_table.getColumns().add(createCheckboxColumn(Messages.AutoScale,
                AxisConfig::isAutoScale, AxisConfig::setAutoScale));

        axes_table.getColumns().add(createCheckboxColumn(Messages.LogScale,
                AxisConfig::isLogScale, AxisConfig::setLogScale));

        axes_table.setEditable(true);
        axes_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        axes_table.getColumns().forEach(c -> c.setSortable(false));
    }

    private void createContextMenu()
    {
        final MenuItem add_axis = new MenuItem(Messages.AddAxis, Activator.getIcon("add"));
        add_axis.setOnAction(event -> new AddAxisCommand(undo, model));

        final ContextMenu menu = new ContextMenu();
        axes_table.setOnContextMenuRequested(event ->
        {
            final ObservableList<MenuItem> items = menu.getItems();
            items.setAll(add_axis);

            final List<AxisConfig> selection = axes_table.getSelectionModel().getSelectedItems();

            if (selection.size() > 0)
                items.add(new DeleteAxes(axes_table, model, undo, selection));

            if (model.getEmptyAxis().isPresent())
                items.add(new RemoveUnusedAxes(model, undo));

            menu.show(axes_table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }
}
