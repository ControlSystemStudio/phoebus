/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.search.SearchView;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Combined layout of all data browser components
 *  @author Kay Kasemir
 */
public class Perspective extends SplitPane
{
    private final SearchView search = new SearchView();
    private final ModelBasedPlot plot = new ModelBasedPlot(true);
    private final TabPane tabs = new TabPane();
    private final SplitPane plot_and_tabs = new SplitPane(plot.getPlot(), tabs);
    private Tab properties_tab, export_tab;

    public Perspective()
    {
        properties_tab = new Tab("Properties");
        properties_tab.setOnClosed(event -> autoMinimizeTabs());
        export_tab = new Tab("Export");
        export_tab.setOnClosed(event -> autoMinimizeTabs());

        tabs.getTabs().setAll(properties_tab);

        plot_and_tabs.setOrientation(Orientation.VERTICAL);
        plot_and_tabs.setDividerPositions(0.8);



        getItems().setAll(search, plot_and_tabs);
        setDividerPositions(0.2);

        createContextMenu();
    }

    private void createContextMenu()
    {
        final MenuItem show_properties = new MenuItem(Messages.OpenPropertiesView);
        show_properties.setOnAction(event -> showTab(properties_tab));

        final MenuItem show_export = new MenuItem(Messages.OpenExportView);
        show_export.setOnAction(event -> showTab(export_tab));


        final ContextMenu menu = new ContextMenu(show_properties, show_export);

        plot.getPlot().setOnContextMenuRequested(event ->
        {
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    /** If there are no tabs, minimize that part of the split pane */
    private void autoMinimizeTabs()
    {
        if (tabs.getTabs().isEmpty())
            plot_and_tabs.setDividerPositions(1.0);
    }

    /** @param tab Tab to show:
     *             Assert it's there, select it,
     *             make lower split pane large enough
     */
    private void showTab(final Tab tab)
    {
        // If tab not on screen, add it
        if (! tabs.getTabs().contains(tab))
            if (tab == properties_tab)
                tabs.getTabs().add(0, tab);
            else
                tabs.getTabs().add(tab);

        // Select the requested tab
        tabs.getSelectionModel().select(tab);

        // Assert that the tabs section is visible
        if (plot_and_tabs.getDividers().get(0).getPosition() > 0.9)
            plot_and_tabs.setDividerPositions(0.8);

        // If tab was just added, its header won't show
        // correctly unless we schedule a re-layout
        Platform.runLater(() -> plot_and_tabs.layout() );
    }
}
