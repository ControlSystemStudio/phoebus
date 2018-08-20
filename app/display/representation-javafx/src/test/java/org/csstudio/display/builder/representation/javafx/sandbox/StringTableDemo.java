/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.util.Arrays;
import java.util.List;

import org.phoebus.ui.javafx.StringTable;
import org.phoebus.ui.javafx.StringTableListener;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** {@link StringTable} demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringTableDemo extends Application
{
    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        // Example data
        final List<String> headers = Arrays.asList("Left", "Options", "Bool", "Right");
        // Data lacks one element to demonstrate log message.
        // Table still "works"
        final List<List<String>> data = Arrays.asList(
                Arrays.asList("One", "Two", "true"/*, "missing" */),
                Arrays.asList("Uno", "Due", "false", "Tres"));

        // Table
        final StringTable table = new StringTable(true);
        table.setHeaders(headers);
        table.setColumnOptions(1, Arrays.asList("Two", "Due", "Zwo", "1+2-1"));
        table.setColumnOptions(2, StringTable.BOOLEAN_OPTIONS);
        table.setData(data);
        table.setBackgroundColor(Color.PINK);
        table.setTextColor(Color.GREEN);
        table.setFont(Font.font("Liberation Serif", 12.0));

        // Table looks OK by default, but combo box will need more space
        table.setColumnWidth(1, 100);

        table.setListener(new StringTableListener()
        {
            @Override
            public void tableChanged(final StringTable table)
            {
                System.out.println("Table headers and data changed");
            }

            @Override
            public void dataChanged(final StringTable table)
            {
                System.out.println("Data changed");
            }

            @Override
            public void selectionChanged(final StringTable table, final int[] rows, final int[] cols)
            {
                System.out.println("Selection: rows " + Arrays.toString(rows) + ", cols " + Arrays.toString(cols));
            }
        });

        // Example scene
        final Label label = new Label("Demo:");

        final Button new_headers = new Button("New Headers");
        new_headers.setOnAction(event ->
        {
            table.setHeaders(Arrays.asList("One", "Other"));
            table.setColumnOptions(0, null);
            table.setColumnOptions(1, Arrays.asList("Two", "Due", "Zwo", "1+2-1"));
        });

        final Button new_data = new Button("New Data");
        new_data.setOnAction(event ->
        {
            table.setHeaders(Arrays.asList("A", "B"));
            table.setData(Arrays.asList(
                    Arrays.asList("A 1", "B 1"),
                    Arrays.asList("A 2", "B 2")));
        });

        final Button set_color = new Button("Set Color");
        set_color.setOnAction(event ->
        {
            table.setCellColors(Arrays.asList(Arrays.asList(null, Color.ORANGE),
                                              Arrays.asList(null, null, Color.BLUEVIOLET)));
        });

        final CheckBox sel_row = new CheckBox("Select rows");
        sel_row.setOnAction(event ->
        {
        	table.setRowSelectionMode(sel_row.isSelected());
        });

        final BorderPane layout = new BorderPane();
        layout.setTop(label);
        layout.setCenter(table);
        layout.setRight(new VBox(10, new_headers, new_data, set_color, sel_row));

        final Scene scene = new Scene(layout, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Table Demo");
        stage.setOnCloseRequest(event ->
        {   // Fetch data from table view
            System.out.println(table.getHeaders());
            for (List<String> row : table.getData())
                System.out.println(row);

            System.out.println("Original data:");
            for (List<String> row : data)
                System.out.println(row);
        });
        stage.show();
    }
}
