/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.util.logging.Level;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.search.SearchView;
import org.phoebus.framework.persistence.Memento;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;

/** Combined layout of all data browser components
 *  @author Kay Kasemir
 */
public class Perspective extends SplitPane
{
    /** Memento tags */
    private static final String LEFT_RIGHT_SPLIT = "left_right_split",
            PLOT_TABS_SPLIT = "plot_tabs_split",
            SHOW_PROPERTIES = "show_properties",
            SHOW_EXPORT = "show_export";

    private final SearchView search = new SearchView();
    private final Model model = new Model();
    private final ModelBasedPlot plot = new ModelBasedPlot(true);
    private final Controller controller;
    private final TabPane tabs = new TabPane();
    private final SplitPane plot_and_tabs = new SplitPane(plot.getPlot(), tabs);
    private Tab properties_tab, export_tab;

    public Perspective()
    {
        properties_tab = new Tab("Properties");
        properties_tab.setGraphic(Activator.getIcon("properties"));
        properties_tab.setOnClosed(event -> autoMinimizeTabs());
        export_tab = new Tab("Export");
        export_tab.setGraphic(Activator.getIcon("export"));
        export_tab.setOnClosed(event -> autoMinimizeTabs());

        tabs.getTabs().setAll(properties_tab);

        plot_and_tabs.setOrientation(Orientation.VERTICAL);
        plot_and_tabs.setDividerPositions(0.8);

        getItems().setAll(search, plot_and_tabs);
        setDividerPositions(0.2);

        createContextMenu();

        try
        {
            // TODO Remove dummy model items
            model.addAxis().setColor(Color.BLUE);
            model.addItem(new PVItem("sim://sine(-10, 10, 0.1)", 0.0));
            model.addItem(new PVItem("DTL_LLRF:IOC1:Load", 0.0));
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.SEVERE, "Cannot fake content", ex);
        }

        controller = new Controller(model, plot);
        try
        {
            controller.start();
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.SEVERE, "Cannot start data browser", ex);
        }
    }

    private void createContextMenu()
    {
        final MenuItem show_properties = new MenuItem(Messages.OpenPropertiesView, Activator.getIcon("properties"));
        show_properties.setOnAction(event -> showTab(properties_tab));

        final MenuItem show_export = new MenuItem(Messages.OpenExportView, Activator.getIcon("export"));
        show_export.setOnAction(event -> showTab(export_tab));

        // TODO Open Inspect Samples

        // TODO Open Waveform View

        final ContextMenu menu = new ContextMenu(show_properties, show_export);
        plot.getPlot().setOnContextMenuRequested(event ->
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY()));
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

    /** @param memento From where to restore previously saved settings */
    public void restore(final Memento memento)
    {
        search.restore(memento);
        memento.getNumber(LEFT_RIGHT_SPLIT).ifPresent(pos -> setDividerPositions(pos.floatValue()));
        memento.getNumber(PLOT_TABS_SPLIT).ifPresent(pos -> plot_and_tabs.setDividerPositions(pos.floatValue()));
        memento.getBoolean(SHOW_PROPERTIES).ifPresent(show -> { if (! show) tabs.getTabs().remove(properties_tab); });
        memento.getBoolean(SHOW_EXPORT).ifPresent(show -> { if (show) tabs.getTabs().add(export_tab); });
    }

    /** @param memento Where to store current settings */
    public void save(final Memento memento)
    {
        search.save(memento);
        memento.setNumber(LEFT_RIGHT_SPLIT, getDividers().get(0).getPosition());
        memento.setNumber(PLOT_TABS_SPLIT, plot_and_tabs.getDividers().get(0).getPosition());
        memento.setBoolean(SHOW_PROPERTIES, tabs.getTabs().contains(properties_tab));
        memento.setBoolean(SHOW_EXPORT, tabs.getTabs().contains(export_tab));
    }

    public void dispose()
    {
        plot.dispose();
        controller.stop();
    }
}
