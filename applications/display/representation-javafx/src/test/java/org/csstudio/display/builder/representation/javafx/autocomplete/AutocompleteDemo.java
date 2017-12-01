/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.autocomplete;

import org.csstudio.display.builder.representation.javafx.AutocompleteMenu;
import org.csstudio.display.builder.representation.javafx.AutocompleteMenuUpdater;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/** Demo of {@link AutocompleteMenu}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutocompleteDemo  extends Application
{
    private static class PVNameCompletion extends AutocompletionService implements AutocompleteMenuUpdater
    {
        private final AutocompleteMenu menu;

        public PVNameCompletion(final AutocompleteMenu menu)
        {
            super(SimPVAutocompletion.INSTANCE);
            this.menu = menu;
        }

        @Override
        public void requestEntries(final String content)
        {
            lookup(content, (name, entries) -> menu.setResults(name, entries));
         }

        @Override
        public void updateHistory(final String entry)
        {
            addToHistory(entry);
        }
    }

    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        final AutocompleteMenu menu = new AutocompleteMenu();
        menu.setUpdater(new PVNameCompletion(menu));

        // Text field that uses completion
        final TextField text = new TextField();
        menu.attachField(text);

        // GUI layout
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        final HBox box = new HBox(5, new Label("PV Name"), text);
        box.setAlignment(Pos.CENTER);
        final BorderPane layout = new BorderPane(box);
        final Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.show();
    }
}
