/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.javafx.rtplot.util.RGBFactory;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/** Property panel
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PropertyPanel extends TabPane
{
    private static final String PROPERTY_TAB = "property_tab";

    /** Table cell where the value(!) is a ColorPicker */
    static class ColorTableCell<S> extends TableCell<S, ColorPicker>
    {
        /** Helper for CellValueFactory to create the picker for a color
         *  Cell value factory should then use picker.setOnAction() to
         *  handle edits.
         *  @param color
         *  @return
         */
        static ColorPicker createPicker(final Color color)
        {
            final ColorPicker picker = new ColorPicker(color);
            picker.getCustomColors().setAll(RGBFactory.PALETTE);
            picker.setStyle("-fx-color-label-visible: false ;");
            return picker;
        }

        // The color_column's CellValueFactory already provides a ColorPicker
        // Cell simply shows it
        @Override
        protected void updateItem(final ColorPicker picker, final boolean empty)
        {
            super.updateItem(picker, empty);
            setGraphic(empty ? null : picker);
        }
    }


    public PropertyPanel(final Model model, final UndoableActionManager undo)
    {
        final Tab traces = new TracesTab(model, undo);
        final Tab time_axis = new TimeAxisTab(model, undo);
        final Tab value_axes = new AxesTab(model, undo);
        final Tab misc = new MiscTab(model, undo);
        final Tab statistics = new StatisticsTab(model);
        getTabs().setAll(traces, time_axis, value_axes, misc, statistics);
        for (Tab tab : getTabs())
            tab.setClosable(false);
    }

    /** Add a tool tip to table column
     *
     *  <p>Needs to add the tool tip to each table cell in that column,
     *  which is done by wrapping the cell factory.
     *  This means that the table cell factory must be set before
     *  calling this method.
     *
     *  @param col {@link TableColumn}. Table cell factory must be set!
     *  @param text Text to show in tooltip
     */
    static <S, T> void addTooltip(final TableColumn<S, T> col, final String text)
    {
        final Callback<TableColumn<S,T>, TableCell<S,T>>  orig = col.getCellFactory();
        col.setCellFactory(c ->
        {
            final TableCell<S, T> cell = orig.call(c);
            cell.setTooltip(new Tooltip(text));
            return cell;
        });
    }

    public void restore(final Memento memento)
    {
        memento.getNumber(PROPERTY_TAB).ifPresent(tab -> getSelectionModel().select(tab.intValue()));
    }

    public void save(final Memento memento)
    {
        memento.setNumber(PROPERTY_TAB, getSelectionModel().getSelectedIndex());
    }
}
