/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.csstudio.trends.databrowser3.ui.search.SearchView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Demo of the {@link ModelBasedPlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SearchDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final SearchView search_view = new SearchView();

        final BorderPane layout = new BorderPane(search_view);
        final Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}
