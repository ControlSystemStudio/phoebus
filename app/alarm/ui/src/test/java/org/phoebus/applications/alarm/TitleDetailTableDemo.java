/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.util.List;

import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.ui.tree.TitleDetailTable;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Demo of {@link TitleDetailTable}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TitleDetailTableDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final List<TitleDetail> items = List.of(
            new TitleDetail("Example 1", "Some detail"),
            new TitleDetail("Example 2", "Much\nmore\ndetail")
            );

        final TitleDetailTable table = new TitleDetailTable(items);

        final BorderPane layout = new BorderPane(table);

        final Button dump = new Button("Dump Items");
        dump.setOnAction(event ->
        {
            System.out.println("Items:");
            for (TitleDetail item : table.getItems())
                System.out.println(item);
            System.out.println();
        });
        layout.setBottom(dump);

        final Scene scene = new Scene(layout, 600, 800);
        stage.setTitle("TitleDetailTable Demo");
        stage.setScene(scene);
        stage.show();


    }

    public static void main(final String[] args)
    {
        launch(TitleDetailTableDemo.class, args);
    }
}
