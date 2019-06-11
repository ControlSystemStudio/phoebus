/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.util.List;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/** Docking demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockingDemo extends ApplicationWrapper
{
    public static void main(String[] args)
    {
        for (String prop : List.of("java.specification.name",
                                   "java.specification.vendor",
                                   "java.specification.version",
                                   "java.home",
                                   "java.runtime.name",
                                   "java.runtime.version"))
            System.out.println(prop + " = " + System.getProperty(prop));

        launch(DockingDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        // Add dock items to the original stage
        final DockItem tab1 = new DockItem("Tab 1");
        final BorderPane layout = new BorderPane();
        layout.setTop(new Label("Top"));
        layout.setCenter(new Label("Tab that indicates resize behavior"));
        layout.setBottom(new Label("Bottom"));
        tab1.setContent(layout);

        final DockItem tab2 = new DockItem("Tab 2");
        tab2.setContent(new Rectangle(500, 500, Color.RED));

        // The DockPane is added to a stage by 'configuring' it.
        // Initial tabs can be provided right away
        DockPane tabs = DockStage.configureStage(stage, tab1, tab2);
        stage.setX(100);

        // .. or tabs are added later
        final DockItem tab3 = new DockItem("Tab 3");
        tab3.setContent(new Rectangle(500, 500, Color.GRAY));
        tabs.addTab(tab3);

        // Create another stage with more dock items
        final DockItem tab4 = new DockItem("Tab 4");
        tab4.setContent(new Rectangle(500, 500, Color.YELLOW));

        final DockItem tab5 = new DockItem("Tab 5");
        tab5.setContent(new Rectangle(500, 500, Color.BISQUE));

        final Stage other = new Stage();
        DockStage.configureStage(other, tab4, tab5);
        other.setX(600);

        stage.show();
        other.show();
    }
}
