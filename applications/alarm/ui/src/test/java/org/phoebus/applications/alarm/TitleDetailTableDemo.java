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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link TitleDetailTable}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TitleDetailTableDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final List<TitleDetail> items = List.of(
            new TitleDetail("Example 1", "Some detail"),
            new TitleDetail("Example 2", "Much\nmore\ndetail")
            );

        final TitleDetailTable table = new TitleDetailTable(items);

        final Scene scene = new Scene(table, 600, 800);
        stage.setTitle("TitleDetailTable Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}
