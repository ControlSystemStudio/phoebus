/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFormatter;

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
    private ObservableList<List<SimpleStringProperty>> rows = FXCollections.observableArrayList();
    private final TableView<List<SimpleStringProperty>> table = new TableView<>(rows);

    public DataTable(final ScanClient scan_client, final long scan_id)
    {
        final TableColumn<List<SimpleStringProperty>, String> col = new TableColumn<>("Time");
        col.setCellValueFactory(cell -> cell.getValue().get(0));
        table.getColumns().add(col);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No data for Scan " + scan_id));

        getChildren().setAll(table);

        new ScanDataReader(scan_client, scan_id, this::update);
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
        final ObservableList<TableColumn<List<SimpleStringProperty>, ?>> columns = table.getColumns();
        if (columns.size() != iterator.getDevices().length + 1)
            rows.clear();

        // 'Time' column is already present
        // Create or update columns for devices
        int i = 1;
        for (String device : iterator.getDevices())
        {
            if (columns.size() <= i)
            {
                final TableColumn<List<SimpleStringProperty>, String> col = new TableColumn<>(device);
                final int col_index = i;
                col.setCellValueFactory(cell ->
                {
                    final List<SimpleStringProperty> row = cell.getValue();
                    if (col_index < row.size())
                        return row.get(col_index);
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

            // Add a new row
            final List<SimpleStringProperty> row = new ArrayList<>(columns.size());
            row.add(new SimpleStringProperty(ScanSampleFormatter.format(iterator.getTimestamp())));
            for (ScanSample sample : iterator.getSamples())
                row.add(new SimpleStringProperty(ScanSampleFormatter.asString(sample)));
            rows.add(row);
        }
    }
}
