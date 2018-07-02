/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;

/** Table for editing list of {@link TitleDetail}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TitleDetailTable extends BorderPane
{
    private final ObservableList<TitleDetail> items = FXCollections.observableArrayList();

    private final TableView<TitleDetail> table = new TableView<>(items);

    private final Button add = new Button("", ImageCache.getImageView(ImageCache.class, "/icons/add.png")),
                         edit = new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/edit.png")),
                         up =  new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/up.png")),
                         down = new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/down.png")),
                         delete = new Button("", ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));

    /** @param initial_items Initial items. Original list will remain unchanged */
    public TitleDetailTable(final List<TitleDetail> initial_items)
    {
        items.setAll(initial_items);

        createTable();
        createButtons();

        final VBox buttons = new VBox(5, add, edit, up, down, delete);

        setCenter(table);
        BorderPane.setMargin(buttons, new Insets(0, 5, 0, 5));
        setRight(buttons);
    }

    /** @return Items in table */
    public List<TitleDetail> getItems()
    {
        // Delete empty items
        items.removeIf(item -> item.title.isEmpty()  &&  item.detail.isEmpty());
        return items;
    }

    private void createTable()
    {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);
        table.setPlaceholder(new Label("none"));

        TableColumn<TitleDetail, String> col = new TableColumn<>("Title");
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title));
        col.setCellFactory(column -> new TextFieldTableCell<>(new DefaultStringConverter()));
        col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetail(event.getNewValue(), items.get(row).detail));

            // Trigger editing the detail
            UpdateThrottle.TIMER.schedule(() ->
                Platform.runLater(() ->
                {
                    table.getSelectionModel().clearAndSelect(row);
                    table.edit(row, table.getColumns().get(1));
                }),
                200, TimeUnit.MILLISECONDS);
        });
        col.setSortable(false);
        table.getColumns().add(col);

        col = new TableColumn<>("Detail");
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().detail.replace("\n", "\\n")));
        col.setCellFactory(column -> new TextFieldTableCell<>(new DefaultStringConverter()));
        col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetail(items.get(row).title, event.getNewValue().replace("\\n", "\n")));
        });
        col.setSortable(false);
        table.getColumns().add(col);
    }

    private void createButtons()
    {
        add.setTooltip(new Tooltip("Add a new table item."));
        add.setOnAction(event ->
        {
            items.add(new TitleDetail("", ""));

            // Trigger editing the title of new item
            UpdateThrottle.TIMER.schedule(() ->
                Platform.runLater(() ->
                {
                    final int row = items.size()-1;
                    table.getSelectionModel().clearAndSelect(row);
                    table.edit(row, table.getColumns().get(0));
                }),
                200, TimeUnit.MILLISECONDS);
        });

        edit.setTooltip(new Tooltip("Edit the detail field of table item."));
        edit.setOnAction(event ->
        {
            final int row = table.getSelectionModel().getSelectedIndex();

            final TitleDetail value = items.get(row);
            final MultiLineInputDialog dialog = new MultiLineInputDialog(value.detail);
            dialog.setTitle("Detail for '" + value.title + "'");
            DialogHelper.positionDialog(dialog, edit, -600, -100);
            dialog.showAndWait().ifPresent(details ->
            {
                items.set(row,  new TitleDetail(value.title, details));
            });
        });

        up.setTooltip(new Tooltip("Move table item up."));
        up.setOnAction(event ->
        {
            final List<Integer> idx = new ArrayList<>(table.getSelectionModel().getSelectedIndices());
            idx.sort((a,b) -> a - b);
            // Starting at top, move each item 'up'
            for (int i : idx)
            {
                final TitleDetail item = items.remove(i);
                // Roll around, item from top moves 'up' by adding back to end
                if (i > 0)
                {
                    items.add(i-1, item);
                    table.getSelectionModel().clearAndSelect(i-1);
                }
                else
                {
                    items.add(item);
                    table.getSelectionModel().clearAndSelect(items.size()-1);
                }
            }
        });

        down.setTooltip(new Tooltip("Move table item down."));
        down.setOnAction(event ->
        {
            final List<Integer> idx = new ArrayList<>(table.getSelectionModel().getSelectedIndices());
            // Descending sort
            idx.sort((a,b) -> b - a);
            // Starting at bottom, move each item 'down'
            for (int i : idx)
            {
                final TitleDetail item = items.remove(i);
                // Roll around, item from top moves 'up' by adding back to end
                if (i < items.size())
                {
                    items.add(i+1, item);
                    table.getSelectionModel().clearAndSelect(i+1);
                }
                else
                {
                    items.add(0, item);
                    table.getSelectionModel().clearAndSelect(0);
                }
            }
        });

        delete.setTooltip(new Tooltip("Delete selected table items."));
        delete.setOnAction(event ->
        {
            final List<Integer> idx = new ArrayList<>(table.getSelectionModel().getSelectedIndices());
            // Descending sort
            idx.sort((a,b) -> b - a);
            // Delete from the bottom of the list so that remaining item indices remain unchanged
            for (int i : idx)
                items.remove(i);
        });

        // Disable options when nothing is selected to edit, move or delete
        final InvalidationListener item_selected = prop ->
        {
            final int size = table.getSelectionModel().getSelectedCells().size();
            final boolean nothing = size <= 0;
            up.setDisable(nothing);
            edit.setDisable(size != 1);
            down.setDisable(nothing);
            delete.setDisable(nothing);
        };
        table.getSelectionModel().selectedIndexProperty().addListener(item_selected);
        // Initial enable/disable
        item_selected.invalidated(null);
    }
}
