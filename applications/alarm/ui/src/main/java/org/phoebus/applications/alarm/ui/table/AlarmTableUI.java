/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import java.util.ArrayList;
import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmContextMenuHelper;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.tree.ConfigureComponentAction;
import org.phoebus.framework.persistence.Memento;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/** Alarm Table UI
 *
 *  <p>Show list of active and acknowledged alarms.
 *
 *  <p>While implemented as {@link BorderPane},
 *  only the methods defined in here should be called
 *  to interact with it.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableUI extends BorderPane
{
    // TODO Share the AlarmClient for given configuration between table, tree, area
    // TODO AlarmTableApplication, AlarmTableInstance
    // TODO Toolbar? to acknowledge/ un-ack, select by name
    // TODO Context menu for alarm guidance, PV actions
    // TODO Maintenance mode?
    // TODO Limit number of rows (was 2500)

    private final AlarmClient client;

    // Sorting:
    //
    // TableView supports sorting as a default when user clicks on columns.
    //
    // LinkedColumnSorter updates the requested sort in the 'other' table.
    // Wrapping the raw data into a SortedList persists the sort order
    // when elements are added/removed in the original data.
    // https://stackoverflow.com/questions/34889111/how-to-sort-a-tableview-programmatically
    //
    // Adding a callback to the observableArrayList instructs the list to also
    // trigger a re-sort when properties of the existing rows change.
    // https://rterp.wordpress.com/2015/05/08/automatically-sort-a-javafx-tableview/
    private final ObservableList<AlarmInfoRow> active_rows = FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);
    private final ObservableList<AlarmInfoRow> acknowledged_rows = FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);

    private final SplitPane split;

    private final TableView<AlarmInfoRow> active = createTable(active_rows, true);
    private final TableView<AlarmInfoRow> acknowledged = createTable(acknowledged_rows, false);


    /** Table cell that shows a Severity */
    private class SeverityLevelCell extends TableCell<AlarmInfoRow, SeverityLevel>
    {
        @Override
        protected void updateItem(final SeverityLevel item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
            {
                setText("");
                setTextFill(Color.BLACK);
            }
            else
            {
                setText(item.toString());
                setTextFill(AlarmUI.getColor(item));
            }
        }
    }

    public AlarmTableUI(final AlarmClient client)
    {
        this.client = client;

        // When user resizes columns, update them in the 'other' table
        active.setColumnResizePolicy(new LinkedColumnResize(active, acknowledged));
        acknowledged.setColumnResizePolicy(new LinkedColumnResize(acknowledged, active));

        // When user sorts column, apply the same to the 'other' table
        active.getSortOrder().addListener(new LinkedColumnSorter(active, acknowledged));
        acknowledged.getSortOrder().addListener(new LinkedColumnSorter(acknowledged, active));

        split = new SplitPane(active, acknowledged);
        split.setOrientation(Orientation.VERTICAL);

        setCenter(split);
    }

    private TableView<AlarmInfoRow> createTable(final ObservableList<AlarmInfoRow> rows,
                                                final boolean active)
    {
        final SortedList<AlarmInfoRow> sorted = new SortedList<>(rows);
        final TableView<AlarmInfoRow> table = new TableView<>(sorted);

        // Ensure that the sorted rows are always updated as the column sorting
        // of the TableView is changed by the user clicking on table headers.
        sorted.comparatorProperty().bind(table.comparatorProperty());

        TableColumn<AlarmInfoRow, String> col = new TableColumn<>("PV");
        col.setPrefWidth(240);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().pv);

        table.getColumns().add(col);

        col = new TableColumn<>("Description");
        col.setPrefWidth(400);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().description);
        table.getColumns().add(col);

        TableColumn<AlarmInfoRow, SeverityLevel> sevcol = new TableColumn<>("Alarm Severity");
        sevcol.setPrefWidth(130);
        sevcol.setReorderable(false);
        sevcol.setCellValueFactory(cell -> cell.getValue().severity);
        sevcol.setCellFactory(c -> new SeverityLevelCell());
        table.getColumns().add(sevcol);

        col = new TableColumn<>("Alarm Status");
        col.setPrefWidth(130);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().status);
        table.getColumns().add(col);

        col = new TableColumn<>("Alarm Time");
        col.setPrefWidth(200);
        col.setReorderable(false);
        table.getColumns().add(col);

        col = new TableColumn<>("Alarm Value");
        col.setPrefWidth(100);
        col.setReorderable(false);
        table.getColumns().add(col);

        sevcol = new TableColumn<>("PV Severity");
        sevcol.setPrefWidth(130);
        sevcol.setReorderable(false);
        sevcol.setCellValueFactory(cell -> cell.getValue().pv_severity);
        sevcol.setCellFactory(c -> new SeverityLevelCell());
        table.getColumns().add(sevcol);

        col = new TableColumn<>("PV Status");
        col.setPrefWidth(130);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().pv_status);
        table.getColumns().add(col);

        table.setPlaceholder(new Label(active ? "No active alarms" : "No acknowledged alarms"));

        createContextMenu(table, active);

        // Double-click to acknowledge or un-acknowledge
        table.setRowFactory(tv ->
        {
            final TableRow<AlarmInfoRow> row = new TableRow<>();
            row.setOnMouseClicked(event ->
            {
                if (event.getClickCount() == 2  &&  !row.isEmpty())
                    client.acknowledge(row.getItem().item, active);
            });
            return row;
        });

        return table;
    }

    private void createContextMenu(final TableView<AlarmInfoRow> table, final boolean active)
    {
        final ContextMenu menu = new ContextMenu();
        table.setOnContextMenuRequested(event ->
        {
            final ObservableList<MenuItem> menu_items = menu.getItems();
            menu_items.clear();

            final List<AlarmTreeItem<?>> selection = new ArrayList<>();
            for (AlarmInfoRow row : table.getSelectionModel().getSelectedItems())
                selection.add(row.item);

            // Add guidance etc.
            new AlarmContextMenuHelper().addSupportedEntries(table, client, menu_items, selection);
            if (menu_items.size() > 0)
                menu_items.add(new SeparatorMenuItem());

            if (AlarmUI.mayConfigure()  &&   selection.size() == 1)
                menu_items.add(new ConfigureComponentAction(table, client, selection.get(0)));

            // TODO Add context menu actions for "PV"
            menu.show(table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    void restore(final Memento memento)
    {
        int i = 0;
        for (TableColumn<AlarmInfoRow, ?> col : active.getColumns())
            memento.getNumber("COL" + i++).ifPresent(wid -> col.setPrefWidth(wid.doubleValue()));

        memento.getNumber("POS").ifPresent(pos -> split.setDividerPositions(pos.doubleValue()));
    }

    void save(final Memento memento)
    {
        int i = 0;
        for (TableColumn<AlarmInfoRow, ?> col : active.getColumns())
            memento.setNumber("COL" + i++, col.getWidth());

        memento.setNumber("POS", split.getDividers().get(0).getPosition());
    }

    /** Update the alarm information to show
     *
     *  <p>The provided lists are not retained, but
     *  some of the AlarmInfoRow items may be added
     *  to the table's list of items.
     *
     *  @param active Active alarms
     *  @param acknowledged Acknowledged alarms
     */
    public void update(final List<AlarmInfoRow> active,
                       final List<AlarmInfoRow> acknowledged)
    {
        update(active_rows, active);
        update(acknowledged_rows, acknowledged);
    }

    /** Update existing list of items with new input
     *  @param items List to update
     *  @param input New input
     */
    private void update(final ObservableList<AlarmInfoRow> items, final List<AlarmInfoRow> input)
    {
        // Similar, but might trigger a full table redraw:
        // items.setAll(input);

        // Update content of common list entries
        int N = Math.min(items.size(), input.size());
        for (int i=0; i<N; ++i)
            items.get(i).copy(input.get(i));

        N = input.size();
        if (N > items.size())
        {
            // Additional elements if input is larger that existing list
            for (int i=items.size(); i<N; ++i)
                items.add(input.get(i));
        }
        else // Trim items, input has fewer elements
            items.remove(N, items.size());
    }
}
