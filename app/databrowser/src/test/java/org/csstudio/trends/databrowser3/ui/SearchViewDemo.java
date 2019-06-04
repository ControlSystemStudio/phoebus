/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.search.SearchView;
import org.phoebus.ui.javafx.ApplicationWrapper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Demo of the {@link ModelBasedPlot}
 *  @author Kay Kasemir
 */
public class SearchViewDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final SearchView search_view = new SearchView(new Model(), new UndoableActionManager(10));

        final BorderPane layout = new BorderPane(search_view);
        final Scene scene = new Scene(layout, 300, 900);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(SearchViewDemo.class, args);
    }
}
