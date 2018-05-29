/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import java.util.List;

import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
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
public class AlarmTable extends BorderPane
{
    // TODO Application, Instance
    // TODO Save column sizes in memento
    // TODO Toolbar? to acknowledge/ un-ack, select by name
    // TODO Context menu for alarm guidance, PV actions
    // TODO Maintenance mode?

    private final TableView<AlarmInfoRow> active = createTable(true);
    private final TableView<AlarmInfoRow> acknowledged = createTable(false);

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

    public AlarmTable()
    {
        // When user resizes columns, update them in the 'other' table
        active.setColumnResizePolicy(new LinkedColumnResize(active, acknowledged));
        acknowledged.setColumnResizePolicy(new LinkedColumnResize(acknowledged, active));

        // Table automatically shows scroll bars,
        // except when it's empty and columns exceed visible width
        // https://bugs.openjdk.java.net/browse/JDK-8089225
        final SplitPane tables = new SplitPane(active, acknowledged);
        tables.setOrientation(Orientation.VERTICAL);

        setCenter(tables);
    }

    private TableView<AlarmInfoRow> createTable(final boolean active)
    {
        final TableView<AlarmInfoRow> table = new TableView<>();

        TableColumn<AlarmInfoRow, String> col = new TableColumn<>("PV");
        col.setPrefWidth(240);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().pv);

        table.getColumns().add(col);

        col = new TableColumn<>("Description");
        col.setPrefWidth(400);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().pv);
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
        table.getColumns().add(col);

        col = new TableColumn<>("Alarm Time");
        col.setPrefWidth(200);
        col.setReorderable(false);
        table.getColumns().add(col);

        col = new TableColumn<>("Alarm Value");
        col.setPrefWidth(100);
        col.setReorderable(false);
        table.getColumns().add(col);

        col = new TableColumn<>("PV Severity");
        col.setPrefWidth(130);
        col.setReorderable(false);
        table.getColumns().add(col);

        col = new TableColumn<>("PV Status");
        col.setPrefWidth(130);
        col.setReorderable(false);
        table.getColumns().add(col);

        table.setPlaceholder(new Label(active ? "No active alarms" : "No acknowledged alarms"));

        return table;
    }

    /** Set alarm information to show
     *  @param active Active alarms
     *  @param acknowledged Acknowledged alarms
     */
    public void setAlarms(final List<AlarmInfoRow> active,
                          final List<AlarmInfoRow> acknowledged)
    {
        this.active.getItems().setAll(active);
        this.acknowledged.getItems().setAll(acknowledged);
    }
}
