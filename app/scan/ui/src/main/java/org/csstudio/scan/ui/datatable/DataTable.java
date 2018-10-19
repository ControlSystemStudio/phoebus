/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import static org.csstudio.scan.ScanSystem.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.logging.Level;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.ui.ScanDataReader;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

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
        
        ContextMenu menu = new ContextMenu();
        
        MenuItem exportTable = new MenuItem("Export table to CSV");
        exportTable.setOnAction(event -> 
        {
            FileChooser file_chooser = new FileChooser();
            File csv_file = file_chooser.showSaveDialog(this.getScene().getWindow());
            
            if (null == csv_file)
                return;
            
            writeTableToCSV(csv_file);
        });
        
        MenuItem exportRawData = new MenuItem("Export raw data to CSV");
        exportRawData.setOnAction(event -> 
        {
            FileChooser file_chooser = new FileChooser();
            File csv_file = file_chooser.showSaveDialog(this.getScene().getWindow());
            
            if (null == csv_file)
                return;
            
            writeRawDataToCSV(csv_file);
        });
        
        menu.getItems().add(exportTable);
        menu.getItems().add(exportRawData);
        
        table.setContextMenu(menu);
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
    
    /** Write the tables contents to the passed file in the CSV format. */
    private void writeTableToCSV(final File csv_file)
    {
        writeToCSV(csv_file, false);
    }
    
    /** Write the tables contents to the passed file in the CSV format. */
    private void writeRawDataToCSV(final File csv_file)
    {
        writeToCSV(csv_file, true);
    }
    
    private void writeToCSV(final File csv_file, final boolean include_timestamps)
    {
        StringBuilder str_builder = new StringBuilder();
        int i, size;
        ObservableList<TableColumn<DataRow, ?>> cols = table.getColumns();
        
        size = cols.size();
        
        for (i = 0; i < size; i++)
        {
            TableColumn<DataRow, ?> col = cols.get(i);
            str_builder.append(col.getText());
            if (i != 0 && include_timestamps)
                str_builder.append(", " + col.getText() + " Timestamps");
            if (i != size - 1)
                str_builder.append(", ");
        }
        str_builder.append("\n");
        
        for (DataRow row : rows)
        {
            size = row.size();
            for (i = 0; i < size; i++)
            {
                str_builder.append(row.getDataValue(i).get());
                if (i != 0 && include_timestamps)
                    str_builder.append(", " + row.getDataTimestamp(i).get());
                if (i != size - 1)
                    str_builder.append(", ");
            }
            str_builder.append("\n");
        }
        
        try (PrintWriter writer = new PrintWriter(csv_file))
        {            
            writer.print(str_builder.toString());
        } 
        catch (FileNotFoundException ex)
        {
            final String output = include_timestamps ? "data" : "table";
            logger.log(Level.WARNING, "Failed to write " + output + " to CSV.", ex);
        }
    }
    
    /** Should be called to stop the reader (in case it's still running) */
    public void dispose()
    {
        reader.shutdown();
    }
}
