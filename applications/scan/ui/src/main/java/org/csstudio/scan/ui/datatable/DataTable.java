/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

/** Table display of logged scan data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataTable extends StackPane
{
    private ObservableList<List<SimpleStringProperty>> rows = FXCollections.observableArrayList();
    private final TableView<List<SimpleStringProperty>> table = new TableView<>(rows);
    private final ScanDataReader reader;

    public DataTable(final ScanClient scan_client, final long scan_id)
    {
        TableColumn<List<SimpleStringProperty>, String> col = new TableColumn<>("Time");
        col.setCellValueFactory(cell -> cell.getValue().get(0));
        table.getColumns().add(col);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        getChildren().setAll(table);

        this.reader = new ScanDataReader(scan_client, scan_id, this::update, this::scanCompleted);
    }

    private void update(final ScanData data)
    {
        final ScanDataIterator iterator = new ScanDataIterator(data);

        Platform.runLater(() -> updateTable(iterator));
    }

    private void updateTable(final ScanDataIterator iterator)
    {
        // 'Time' column is already present
        // Create or update columns for devices
        int i = 1;
        final ObservableList<TableColumn<List<SimpleStringProperty>, ?>> columns = table.getColumns();
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
                    return new SimpleStringProperty("???");
                });
                columns.add(col);
            }
            else
                columns.get(i).setText(device);
            ++i;
        }

        // Append new data rows
        i = -1;
        while (iterator.hasNext())
        {
            ++i;
            final List<SimpleStringProperty> row = new ArrayList<>(columns.size());

            row.add(new SimpleStringProperty(TimestampFormats.formatCompactDateTime(iterator.getTimestamp())));

            for (ScanSample sample : iterator.getSamples())
            {
                final String value = sample == null
                        ?"-null-"
                        : Arrays.toString(sample.getValues());
                row.add(new SimpleStringProperty(value));
            }

            // Keep existing rows
            if (i < rows.size())
                continue;
            rows.add(row);
        }
    }

    private void scanCompleted()
    {
        System.out.println("Scan completed...");
    }
}
