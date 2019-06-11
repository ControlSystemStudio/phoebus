/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/** Autocompletion demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutocompleteMenuDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.FINE);
        for (Handler handler : logger.getHandlers())
            handler.setLevel(logger.getLevel());

        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);

        Label label = new Label("PV 1:");
        TextField text = new TextField();
        GridPane.setHgrow(text, Priority.ALWAYS);
        PVAutocompleteMenu.INSTANCE.attachField(text);
        layout.add(label, 0, 0);
        layout.add(text, 1, 0);

        label = new Label("PV 2:");
        text = new TextField();
        GridPane.setHgrow(text, Priority.ALWAYS);
        PVAutocompleteMenu.INSTANCE.attachField(text);
        layout.add(label, 0, 1);
        layout.add(text, 1, 1);

        final Scene scene = new Scene(layout, 300, 200);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(AutocompleteMenuDemo.class, args);
    }
}
