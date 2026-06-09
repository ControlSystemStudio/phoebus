/*******************************************************************************
 * Copyright (c) 2018-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.config;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.util.converter.DefaultStringConverter;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.ui.tree.ValidatingTextFieldTableCell;
import org.phoebus.ui.javafx.UpdateThrottle;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * FXML controller for TitleDetailDelayTable.fxml.
 *
 * <p>All UI structure is declared in the FXML file. This controller wires up
 * cell factories, cell-value factories, event handlers, and button icons
 * (which cannot be expressed in plain FXML without a custom builder).
 *
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class TitleDetailDelayTableController extends TitleDetailTableController {
    // ── Option enum ────────────────────────────────────────────────────────────

    private enum ActionOption {
        /**
         * Send e-mail with alarm info
         */
        mailto,
        /**
         * Execute external command
         */
        cmd,
        /**
         * Update PV with severity
         */
        sevrpv,
        /**
         * Update PV with alarm-info text
         */
        infopv
    }

    @SuppressWarnings("unused")
    @FXML
    private TableView<TitleDetailDelay> table;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TitleDetailDelay, ActionOption> optionColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TitleDetailDelay, String> infoColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TitleDetailDelay, Integer> delayColumn;

    private final ObservableList<TitleDetailDelay> items = FXCollections.observableArrayList();

    /**
     * Configure table columns particular for this view.
     */
    @Override
    public void configure() {

        table.setItems(items);

        optionColumn.setCellFactory(ComboBoxTableCell.forTableColumn(ActionOption.values()));
        optionColumn.setCellValueFactory(
                cell -> new SimpleObjectProperty<>(getOptionFromDetail(cell.getValue())));
        optionColumn.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            final TitleDetailDelay updated = setOptionToDetail(items.get(row), event.getNewValue());
            items.set(row, updated);

            if (updated.hasDelay())
                Platform.runLater(() ->
                {
                    table.getSelectionModel().clearAndSelect(row);
                    table.edit(row, infoColumn);
                });
        });

        // ── Info (text-field sub-column under Detail) ──────────────────────────
        infoColumn.setCellValueFactory(cell -> new SimpleStringProperty(getInfoFromDetail(cell.getValue())));
        infoColumn.setCellFactory(ValidatingTextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        infoColumn.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            final TitleDetailDelay updated = setInfoToDetail(items.get(row), event.getNewValue());
            items.set(row, updated);

            if (updated.hasDelay())
                Platform.runLater(() ->
                {
                    table.getSelectionModel().clearAndSelect(row);
                    table.edit(row, delayColumn);
                });
        });

        // ── Delay (spinner column) ─────────────────────────────────────────────
        delayColumn.setCellValueFactory(
                cell -> new SimpleIntegerProperty(cell.getValue().delay).asObject());
        delayColumn.setCellFactory(column -> new DelayTableCell());
        delayColumn.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetailDelay(
                    items.get(row).title,
                    items.get(row).detail,
                    event.getNewValue()));
        });

        titleColumn.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetailDelay(
                    event.getNewValue(),
                    items.get(row).detail,
                    items.get(row).delay));

            // Auto-advance to the Option sub-column
            Platform.runLater(() ->
            {
                table.getSelectionModel().clearAndSelect(row);
                table.edit(row, optionColumn);
            });
        });
    }

    /**
     * @return current items, with empty title+detail rows removed
     */
    @Override
    public List<? extends TitleDetail> getItems() {
        items.removeIf(item -> item.title.isEmpty() && item.detail.isEmpty());
        return items;
    }


    // ── Detail helpers (unchanged logic) ──────────────────────────────────────

    private ActionOption getOptionFromDetail(final TitleDetailDelay tdd) {
        if (tdd == null) return null;
        final int sep = tdd.detail.indexOf(':');
        if (sep < 0) return ActionOption.mailto;
        try {
            return ActionOption.valueOf(tdd.detail.substring(0, sep));
        } catch (Exception e) {
            return ActionOption.mailto;
        }
    }

    private String getInfoFromDetail(final TitleDetailDelay tdd) {
        if (tdd == null) return "";
        final int sep = tdd.detail.indexOf(':');
        return sep < 0 ? "" : tdd.detail.substring(sep + 1);
    }

    private TitleDetailDelay setOptionToDetail(final TitleDetailDelay tdd, final ActionOption option) {
        if (tdd == null || option == null) return tdd;
        return new TitleDetailDelay(
                tdd.title,
                option + ":" + getInfoFromDetail(tdd),
                tdd.delay);
    }

    private TitleDetailDelay setInfoToDetail(final TitleDetailDelay tdd, final String info) {
        if (tdd == null || info == null) return tdd;
        return new TitleDetailDelay(
                tdd.title,
                getOptionFromDetail(tdd) + ":" + info.replace("\\n", "\n"),
                tdd.delay);
    }

    @Override
    public void handleAdd() {
        items.add(new TitleDetailDelay("", "", 0));

        // Trigger editing the title of new item
        UpdateThrottle.TIMER.schedule(() -> Platform.runLater(() ->
            {
                final int row = items.size() - 1;
                table.getSelectionModel().clearAndSelect(row);
                table.edit(row, table.getColumns().get(0));
            }), 200, TimeUnit.MILLISECONDS);
    }

    /**
     * Populate the table with an initial list of items.
     * The original list is not modified.
     *
     * @param initial_items Items to display initially.
     */
    @Override
    public void setItems(final List<? extends TitleDetail> initial_items) {
        items.setAll(initial_items.stream().map(i -> (TitleDetailDelay)i).toList());
    }

    // ── DelayTableCell (inner class – unchanged logic) ─────────────────────────

    /**
     * Custom {@link TableCell} for the Delay column.
     * Shows a {@link Spinner} and disables it for action types that have no delay.
     */
    private static class DelayTableCell extends TableCell<TitleDetailDelay, Integer> {
        private final Spinner<Integer> spinner;

        DelayTableCell() {
            this.spinner = new Spinner<>(0, 10_000, 1);
            this.spinner.setEditable(true);

            // Keep arrow buttons out of the tab order
            spinner.lookupAll(".increment-arrow-button, .decrement-arrow-button")
                    .forEach(node -> node.setFocusTraversable(false));

            spinner.valueProperty().addListener((obs, oldValue, newValue) ->
            {
                if (isEditing()) commitEdit(newValue);
            });

            // Validate on focus loss
            spinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) ->
            {
                if (!isNowFocused) {
                    final Integer current = spinner.getValue();
                    if (Objects.equals(current, getItem())) {
                        cancelEdit();
                        return;
                    }

                    if (!isEditing()) {
                        final TableView<TitleDetailDelay> tv = getTableView();
                        if (tv != null)
                            Platform.runLater(() ->
                            {
                                tv.getSelectionModel().clearAndSelect(getIndex());
                                tv.edit(getIndex(), getTableColumn());
                                commitEdit(current);
                            });
                        else
                            commitEdit(current);
                    } else
                        commitEdit(current);
                }
            });
        }

        @Override
        public void updateItem(final Integer item, final boolean empty) {
            super.updateItem(item, empty);

            if (empty
                    || getTableRow() == null
                    || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }

            final boolean hasDelay = getTableRow().getItem().hasDelay();
            spinner.setDisable(!hasDelay);
            spinner.getEditor().setStyle(
                    hasDelay ? "-fx-text-inner-color: black;"
                            : "-fx-text-inner-color: lightgray;");
            spinner.getValueFactory().setValue(item);
            setGraphic(spinner);

            Platform.runLater(() ->
            {
                if (isEditing()) {
                    spinner.getEditor().requestFocus();
                    spinner.getEditor().end();
                }
            });
        }
    }
}
