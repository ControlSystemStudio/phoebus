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

import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;


/** Table for editing list of {@link TitleDetailDelay}
 *
 *  <p>Largely based of off {@link TitleDetailTable}
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class TitleDetailDelayTable extends BorderPane
{
    private enum Option_d {
        mailto, cmd, sevrpv
    };

    private final ObservableList<TitleDetailDelay> items = FXCollections.observableArrayList();

    private final TableView<TitleDetailDelay> table = new TableView<>(items);

    private final Button add = new Button("", ImageCache.getImageView(ImageCache.class, "/icons/add.png")),
                         edit = new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/edit.png")),
                         up =  new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/up.png")),
                         down = new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/down.png")),
                         delete = new Button("", ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));

    /** @param initial_items Initial items. Original list will remain unchanged */
    public TitleDetailDelayTable(final List<TitleDetailDelay> initial_items)
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
    public List<TitleDetailDelay> getItems()
    {
        // Delete empty items (delay ignored)
        items.removeIf(item -> item.title.isEmpty()  &&  item.detail.isEmpty());
        return items;
    }

    /** Table cell for 'delay'
     *  Disables for actions that don't use the delay
     */
    class DelayTableCell extends TableCell<TitleDetailDelay, Integer>
    {
        private final Spinner<Integer> spinner;
        
        public DelayTableCell()
        {
            this.spinner = new Spinner<>(0, 10000, 1);
            spinner.setEditable(true);
            this.spinner.valueProperty().addListener((observable, oldValue, newValue) -> commitEdit(newValue));
        }

        @Override
        public void updateItem(Integer item, boolean empty)
        {
            super.updateItem(item, empty);

            if (empty ||
                getTableRow() == null ||
                getTableRow().getItem() == null ||
                spinner == null) {
                setGraphic(null);
                return;
            }
            if (getTableRow().getItem().hasDelay())
            {
                spinner.setDisable(false);
                spinner.getEditor().setStyle("-fx-text-inner-color: black;");
                //spinner.getEditor().setTextFill(Color.BLACK);
            }
            else
            {
                spinner.setDisable(true);
                spinner.getEditor().setStyle("-fx-text-inner-color: lightgray;");
                //spinner.getEditor().setTextFill(Color.LIGHTGRAY);
            }
            
            this.spinner.getValueFactory().setValue(item);
            setGraphic(spinner);
        }
    }

    private void createTable()
    {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);
        table.setPlaceholder(new Label("none"));

        TableColumn<TitleDetailDelay, String> col = new TableColumn<>("Title");
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().title));
        col.setCellFactory(column -> new TextFieldTableCell<>(new DefaultStringConverter()));
        col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetailDelay(event.getNewValue(), items.get(row).detail, items.get(row).delay));

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
        col.setSortable(false);
        table.getColumns().add(col);

        // Use a combo box to specified the action
        TableColumn<TitleDetailDelay, Option_d> tmpOptionCol = new TableColumn<>("Option");
        tmpOptionCol.setCellFactory(ComboBoxTableCell.forTableColumn(Option_d.values()));
        tmpOptionCol
                .setCellValueFactory(cell -> new SimpleObjectProperty<Option_d>(getOptionFromDetail(cell.getValue())));
        tmpOptionCol.setOnEditCommit(edit -> {
            final int row = edit.getTablePosition().getRow();
            TitleDetailDelay tmpT = items.get(row);
            Option_d option = edit.getNewValue();
            TitleDetailDelay newTitleDetailDelay = setOptionToDetail(tmpT, option);
            items.set(row, newTitleDetailDelay);
            // Trigger editing the delay.
            if (newTitleDetailDelay.hasDelay())
                UpdateThrottle.TIMER.schedule(() -> Platform.runLater(() -> {
                    table.getSelectionModel().clearAndSelect(row);
                    table.edit(row, table.getColumns().get(2));
                }), 200, TimeUnit.MILLISECONDS);
        });
        tmpOptionCol.setEditable(true);
        col.getColumns().add(tmpOptionCol);

        // Use a textfield to set info for detail
        TableColumn<TitleDetailDelay, String> infoCol = new TableColumn<>("Info");
        infoCol.setCellValueFactory(cell -> new SimpleStringProperty(getInfoFromDetail(cell.getValue())));
        infoCol.setCellFactory(column -> new TextFieldTableCell<>(new DefaultStringConverter()));
        infoCol.setOnEditCommit(event -> {
            final int row = event.getTablePosition().getRow();
            TitleDetailDelay tmpT = items.get(row);
            String newInfo = event.getNewValue();
            TitleDetailDelay newTitleDetailDelay = setInfoToDetail(tmpT, newInfo);
            items.set(row, newTitleDetailDelay);
            // Trigger editing the delay.
            if (newTitleDetailDelay.hasDelay())
                UpdateThrottle.TIMER.schedule(() -> Platform.runLater(() -> {
                    table.getSelectionModel().clearAndSelect(row);
                    table.edit(row, table.getColumns().get(2));
                }), 200, TimeUnit.MILLISECONDS);
        });
        infoCol.setSortable(false);
        col.getColumns().add(infoCol);

        TableColumn<TitleDetailDelay, Integer> delayCol = new TableColumn<>("Delay");
        delayCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().delay).asObject());
        delayCol.setCellFactory(column -> new DelayTableCell());
        delayCol.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            items.set(row, new TitleDetailDelay(items.get(row).title, items.get(row).detail, event.getNewValue()));
        });
        delayCol.setSortable(false);
        table.getColumns().add(delayCol);
    }

    /**
     * This function extract the command option from detail "option:info"
     * 
     * @param titleDetailDelay
     * @return enum Option_d either mailto or cmd
     */
    private Option_d getOptionFromDetail(TitleDetailDelay titleDetailDelay) {
        Option_d option = null;
        String detail = titleDetailDelay != null ? titleDetailDelay.detail : null;
        String[] split = detail != null ? detail.split(":") : null;
        String optionString = split != null && split.length > 0 ? split[0] : null;
        try {
            option = optionString != null ? Option_d.valueOf(optionString) : Option_d.mailto;
        } catch (Exception e) {
            option = Option_d.mailto;
        }
        return option;
    }

    /**
     * This function extract the info from detail "option:info"
     * 
     * @param titleDetailDelay
     * @return information eg : mail or command
     */
    private String getInfoFromDetail(TitleDetailDelay titleDetailDelay) {
        String info = "";
        String detail = titleDetailDelay != null ? titleDetailDelay.detail : null;
        String[] split = detail != null ? detail.split(":") : null;
        info = split != null && split.length > 1 ? split[1] : "";
        return info;
    }

    /**
     * Create a new TitleDetailDelay from a given option
     * @param titleDetailDelay
     * @param option
     * @return new TitleDetailDelay
     */
    private TitleDetailDelay setOptionToDetail(TitleDetailDelay titleDetailDelay, Option_d option) {
        TitleDetailDelay newTitleDetailDelay = titleDetailDelay;
        if (titleDetailDelay != null && option != null) {
            String info = getInfoFromDetail(titleDetailDelay);
            String detail = option.toString() + ":" + info;
            newTitleDetailDelay = new TitleDetailDelay(titleDetailDelay.title, detail, titleDetailDelay.delay);
        }
        return newTitleDetailDelay;
    }

    /**
     * Create a new TitleDetailDelay from a given info
     * @param titleDetailDelay
     * @param option
     * @return new TitleDetailDelay
     */
    private TitleDetailDelay setInfoToDetail(TitleDetailDelay titleDetailDelay, String info) {
        TitleDetailDelay newTitleDetailDelay = titleDetailDelay;
        if (titleDetailDelay != null && info != null) {
            Option_d option = getOptionFromDetail(titleDetailDelay);
            String newInfo =   info.replace("\\n", "\n");
            String detail = option.toString() + ":" + newInfo;
            newTitleDetailDelay = new TitleDetailDelay(titleDetailDelay.title, detail, titleDetailDelay.delay);
        }
        return newTitleDetailDelay;
    }

    private void createButtons()
    {
        add.setTooltip(new Tooltip("Add a new table item."));
        add.setOnAction(event ->
        {
            items.add(new TitleDetailDelay("", "", 0));

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

            final TitleDetailDelay value = items.get(row);
            final MultiLineInputDialog dialog = new MultiLineInputDialog(value.detail);
            dialog.setTitle("Detail for '" + value.title + "'");
            DialogHelper.positionDialog(dialog, edit, -600, -100);
            dialog.showAndWait().ifPresent(details ->
            {
                items.set(row,  new TitleDetailDelay(value.title, details, value.delay));
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
                final TitleDetailDelay item = items.remove(i);
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
                final TitleDetailDelay item = items.remove(i);
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
