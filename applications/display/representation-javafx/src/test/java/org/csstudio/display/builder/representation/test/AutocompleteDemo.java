/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private static class DemoUpdater implements AutocompleteMenuUpdater
    {
        private static final ExecutorService pool = Executors.newCachedThreadPool();

        private final AutocompleteMenu menu;
        private final LinkedList<String> history = new LinkedList<>();
        private Future<?> ongoing_lookup;

        public DemoUpdater(final AutocompleteMenu menu)
        {
            this.menu = menu;
        }

        @Override
        public void requestEntries(final String content)
        {
            // Will always be called on UI thread, so OK to get/check/update ongoing lookup
            if (ongoing_lookup != null)
                ongoing_lookup.cancel(true);
            ongoing_lookup = pool.submit(() -> lookup(content));

        }

        private void lookup(final String content)
        {
            try
            {
                System.out.println(">>> Lookup '" + content + "' on " + Thread.currentThread());
                Thread.sleep(1000);

                final List<String> matches = new ArrayList<>();
                synchronized (history)
                {
                    for (String item : history)
                        if (item.contains(content))
                            matches.add(item);
                }

                if (! Thread.currentThread().isInterrupted())
                {
                    System.out.println("<<< Lookup '" + content + "' Done");
                    menu.setResults("History", matches);
                }
                else
                    System.out.println("xxx Lookup '" + content + "' cancelled");

            }
            catch (InterruptedException ex)
            {
                // Ignore
                System.out.println("XXX Lookup '" + content + "' interrupted");
            }
        }

        @Override
        public void updateHistory(final String entry)
        {
            System.out.println("Add to history: '" + entry + "' on " + Thread.currentThread());
            // Assert that most recent entry is as top of the list
            // by first removing a potential existing entry,
            // then adding to top
            synchronized (history)
            {
                history.remove(entry);
                history.addFirst(entry);
            }
        }
    }

    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        final AutocompleteMenu completion = new AutocompleteMenu();
        completion.setUpdater(new DemoUpdater(completion));

        // Text field that uses completion
        final TextField text = new TextField();
        completion.attachField(text);

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
