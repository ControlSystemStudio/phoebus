/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.config;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.converter.DefaultStringConverter;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.ui.tree.ValidatingTextFieldTableCell;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.UpdateThrottle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller for TitleDetailTable.fxml.
 *
 * @author Kay Kasemir (original), FXML refactor
 */
@SuppressWarnings("nls")
public class TitleDetailTableController {

    @SuppressWarnings("unused")
    @FXML
    private TableView<TitleDetail> table;
    @SuppressWarnings("unused")
    @FXML
    protected TableColumn<TitleDetail, String> titleColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TitleDetail, String> detailColumn;
    @SuppressWarnings("unused")
    @FXML
    private Node optionsRoot;
    @SuppressWarnings("unused")
    @FXML
    private Label titleLabel;

    @FXML
    protected TitleDetailToolbarController titleDetailToolbarViewController;

    private final ObservableList<TitleDetail> items = FXCollections.observableArrayList();

    /**
     * Performs initialization common for this controller and {@link TitleDetailDelayTableController}.
     */
    public void initialize() {

        titleDetailToolbarViewController.setTitleDetailTableController(this);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Title column
        titleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title));
        titleColumn.setCellFactory(ValidatingTextFieldTableCell.forTableColumn(new DefaultStringConverter()));

        final InvalidationListener item_selected = prop ->
                titleDetailToolbarViewController.setButtonStates(table.getSelectionModel().getSelectedCells().size());
        table.getSelectionModel().selectedIndexProperty().addListener(item_selected);
        // Apply initial state
        item_selected.invalidated(null);

        configure();

    }

    /**
     * Configures view for class specific items.
     */
    public void configure(){

        table.setItems(items);

        titleColumn.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetail(event.getNewValue(), items.get(row).detail));

            // Immediately move focus to the Detail column
            Platform.runLater(() ->
            {
                table.getSelectionModel().clearAndSelect(row);
                table.edit(row, table.getColumns().get(1));
            });
        });

        // Detail column  (newlines stored as \n, displayed as \\n)
        detailColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().detail.replace("\n", "\\n")));
        detailColumn.setCellFactory(
                ValidatingTextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        detailColumn.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetail(
                    items.get(row).title,
                    event.getNewValue().replace("\\n", "\n")));
        });
    }

    /**
     * Populate the table with an initial list of items.
     * The original list is not modified.
     *
     * @param initial_items Items to display initially.
     */
    public void setItems(final List<? extends TitleDetail> initial_items) {
        items.setAll(initial_items);
    }

    /**
     * @return Current items in the table (empty title+detail rows are removed).
     */
    public List<? extends TitleDetail> getItems() {
        items.removeIf(item -> item.title.isEmpty() && item.detail.isEmpty());
        return items;
    }

    /**
     * Adds a new row in the {@link TableView}
     */
    public void handleAdd() {
        items.add(new TitleDetail("", ""));

        // Start editing the Title cell of the new row after a short delay
        UpdateThrottle.TIMER.schedule(() ->
                        Platform.runLater(() ->
                        {
                            final int row = items.size() - 1;
                            table.getSelectionModel().clearAndSelect(row);
                            table.edit(row, table.getColumns().get(0));
                        }),
                200, TimeUnit.MILLISECONDS);
    }

    /**
     * Prepares selected table row for edit.
     */
    public void handleEdit() {
        final int row = table.getSelectionModel().getSelectedIndex();
        if (row < 0)
            return;

        final TitleDetail value = items.get(row);
        final MultiLineInputDialog dialog = new MultiLineInputDialog(value.detail);
        dialog.setTitle("Detail for '" + value.title + "'");
        DialogHelper.positionDialog(dialog, optionsRoot, -600, -100);
        dialog.showAndWait().ifPresent(details ->
                items.set(row, new TitleDetail(value.title, details)));
    }

    /**
     * Moves table row up
     */
    public void handleUp() {
        final List<Integer> idx =
                new ArrayList<>(table.getSelectionModel().getSelectedIndices());
        idx.sort((a, b) -> a - b);          // ascending

        for (int i : idx) {
            final TitleDetail item = items.remove(i);
            if (i > 0) {
                items.add(i - 1, item);
                table.getSelectionModel().clearAndSelect(i - 1);
            } else {
                // Roll-around: top item wraps to bottom
                items.add(item);
                table.getSelectionModel().clearAndSelect(items.size() - 1);
            }
        }
    }

    /**
     * Moves table row down
     */
    public void handleDown() {
        final List<Integer> idx =
                new ArrayList<>(table.getSelectionModel().getSelectedIndices());
        idx.sort((a, b) -> b - a);          // descending

        for (int i : idx) {
            final TitleDetail item = items.remove(i);
            if (i < items.size()) {
                items.add(i + 1, item);
                table.getSelectionModel().clearAndSelect(i + 1);
            } else {
                // Roll-around: bottom item wraps to top
                items.add(0, item);
                table.getSelectionModel().clearAndSelect(0);
            }
        }
    }

    /**
     * Deletes a row from the {@link TableView}
     */
    public void handleDelete() {
        final List<Integer> idx =
                new ArrayList<>(table.getSelectionModel().getSelectedIndices());
        idx.sort((a, b) -> b - a);          // descending — stable indices while removing

        for (int i : idx)
            items.remove(i);
    }

    /**
     * Sets the title for the section (guidance, displays...)
     * @param title Localized string for the title.
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
