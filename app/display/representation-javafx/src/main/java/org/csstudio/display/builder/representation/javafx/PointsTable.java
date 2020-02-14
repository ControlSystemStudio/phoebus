/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.csstudio.display.builder.model.properties.Point;
import org.csstudio.display.builder.model.properties.Points;
import org.phoebus.ui.javafx.EditCell;
import org.phoebus.ui.javafx.TableHelper;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.converter.NumberStringConverter;

/** Table editor for {@link Points}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PointsTable
{
    /** Data to show and edit */
    private final Points points;

    /** Adapt {@link Points} to an {@link ObservableList} as required by {@link TableView} */
    private static class PointsAdapter extends ModifiableObservableListBase<Point>
    {
        // get() returns a new instance of Point every time it's called.
        // For TableView to allow editing, the element type (Point)
        // needs to properly implement equals().
        private final Points points;

        public PointsAdapter(final Points points)
        {
            this.points = points;
        }

        @Override
        public Point get(final int index)
        {
            return points.get(index);
        }

        @Override
        public int size()
        {
            return points.size();
        }

        @Override
        protected void doAdd(final int index, final Point element)
        {
            points.insert(index, element.getX(), element.getY());
        }

        @Override
        protected Point doSet(final int index, final Point element)
        {
            final Point old = new Point(points.getX(index), points.getY(index));
            points.set(index, element.getX(), element.getY());
            return old;
        }

        @Override
        protected Point doRemove(final int index)
        {
            final Point removed = points.get(index);
            points.delete(index);
            return removed;
        }
    };

    /** @param points Points to show and edit */
    public PointsTable(final Points points)
    {
        this.points = points;
    }

    /** @return Top-level {@link Node} */
    public Node create()
    {
        // Layout:
        //
        // | table |  [Add]
        // | table |  [Remove]
        // | table |
        // | table |

        // Create table with editable columns for type Double (supported as Number)
        final TableColumn<Point, Number> x_col = new TableColumn<>(Messages.PointsTable_X);
        x_col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Point,Number>, ObservableValue<Number>>()
        {
            @Override
            public ObservableValue<Number> call(CellDataFeatures<Point, Number> param)
            {
                return new SimpleDoubleProperty(param.getValue().getX());
            }
        });
        x_col.setCellFactory(tableColumn -> new EditCell<>(new NumberStringConverter()));
        x_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            points.setX(row, event.getNewValue().doubleValue());
        });

        final TableColumn<Point, Number> y_col = new TableColumn<>(Messages.PointsTable_Y);
        y_col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Point,Number>, ObservableValue<Number>>()
        {
            @Override
            public ObservableValue<Number> call(CellDataFeatures<Point, Number> param)
            {
                return new SimpleDoubleProperty(param.getValue().getY());
            }
        });
        y_col.setCellFactory(tableColumn -> new EditCell<>(new NumberStringConverter()));
        y_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            points.setY(row, event.getNewValue().doubleValue());
        });

        final ObservableList<Point> data = new PointsAdapter(points);
        final TableView<Point> table = new TableView<>(data);
        table.getColumns().add(x_col);
        table.getColumns().add(y_col);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(Messages.PointsTable_Empty));
        table.setEditable(true);

        // Buttons
        final Button add = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(event -> data.add(new Point(0, 0)));

        final Button remove = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        remove.setMaxWidth(Double.MAX_VALUE);
        remove.setOnAction(event ->
        {
            final int sel = table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
                data.remove(sel);
        });

        final Button up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        up.setMaxWidth(Double.MAX_VALUE);
        up.setOnAction(event -> TableHelper.move_item_up(table, data));

        final Button down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        down.setMaxWidth(Double.MAX_VALUE);
        down.setOnAction(event -> TableHelper.move_item_down(table, data));

        final VBox buttons = new VBox(10, add, remove, up, down);
        final HBox content = new HBox(10, table, buttons);
        HBox.setHgrow(table, Priority.ALWAYS);
        return content;
    }
}
