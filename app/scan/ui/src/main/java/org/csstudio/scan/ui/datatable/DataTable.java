/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.ui.ScanDataReader;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

/** Table display of logged scan data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataTable extends StackPane
{
    private static final SimpleStringProperty EMPTY =  new SimpleStringProperty("");
    private ObservableList<DataRow> rows = FXCollections.observableArrayList();
    private final TableView<DataRow> table = new TableView<>(rows);
    private ScanDataReader reader;

    public DataTable(final ScanClient scan_client, final long scan_id)
    {
        final TableColumn<DataRow, String> col = new TableColumn<>("Time");
        col.setCellValueFactory(cell -> cell.getValue().getDataValue(0));
        table.getColumns().add(col);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No data for Scan " + scan_id));

        getChildren().setAll(table);

        reader = new ScanDataReader(scan_client, this::update);
        reader.setScanId(scan_id);
    }

    private void update(final ScanData data)
    {
        final ScanDataIterator iterator = new ScanDataIterator(data);

        Platform.runLater(() -> updateTable(iterator));
    }

    private void updateTable(final ScanDataIterator iterator)
    {
        // A previous data set could have been for "Time, ypos"
        // while the new one is for "Time, xpos, ypos".
        // So not only is a new column added, the data that used to be in the
        // second column moved to the 3rd one.
        // --> If the column count changes, re-populate all rows.
        final ObservableList<TableColumn<DataRow, ?>> columns = table.getColumns();
        if (columns.size() != iterator.getDevices().size() + 1)
            rows.clear();

        // 'Time' column is already present
        // Create or update columns for devices
        int i = 1;
        for (String device : iterator.getDevices())
        {
            if (columns.size() <= i)
            {
                final TableColumn<DataRow, String> col = new TableColumn<>(device);
                final int col_index = i;
                
                col.setCellFactory(c -> new DataCell(col_index));

                col.setCellValueFactory(cell ->
                {
                    final DataRow row = cell.getValue();
                    if (col_index < row.size())
                    {
                        return row.getDataValue(col_index);
                    }
                    return EMPTY;
                });
                columns.add(col);
            }
            else
                columns.get(i).setText(device);
            ++i;
        }

        // Data rows
        i = -1;
        while (iterator.hasNext())
        {
            ++i;
            // Keep existing rows
            if (i < rows.size())
                continue;

            rows.add(new DataRow(iterator.getTimestamp(), iterator.getSamples()));
        }
    }

    /** Should be called to stop the reader (in case it's still running) */
    public void dispose()
    {
        reader.shutdown();
    }
}
