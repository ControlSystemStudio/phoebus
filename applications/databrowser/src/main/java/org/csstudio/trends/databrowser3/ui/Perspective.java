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
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.properties.PropertyPanel;
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
@SuppressWarnings("nls")
public class Perspective extends SplitPane
{
    /** Memento tags */
    private static final String LEFT_RIGHT_SPLIT = "left_right_split",
                                PLOT_TABS_SPLIT = "plot_tabs_split",
                                SHOW_SEARCH = "show_search",
                                SHOW_PROPERTIES = "show_properties",
                                SHOW_EXPORT = "show_export";

    private final Model model = new Model();
    private final SearchView search = new SearchView(model);
    private final ModelBasedPlot plot = new ModelBasedPlot(true);
    private final Controller controller;
    private final TabPane left_tabs = new TabPane(),
                          bottom_tabs = new TabPane();
    private final SplitPane plot_and_tabs = new SplitPane(plot.getPlot(), bottom_tabs);
    private Tab search_tab, properties_tab, export_tab;

    public Perspective()
    {
        try
        {
            // TODO Remove dummy model items
            model.addAxis().setColor(Color.BLUE);
            model.addAxis();
            model.addItem(new PVItem("sim://sine(-10, 10, 0.1)", 0.0));
            final PVItem item = new PVItem("DTL_LLRF:IOC1:Load", 0.0);
            item.setDisplayName("CPU Load");
            String url = "jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=OFF)(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=snsappa.sns.ornl.gov)(PORT=1610))(ADDRESS=(PROTOCOL=TCP)(HOST=snsappb.sns.ornl.gov)(PORT=1610))(CONNECT_DATA=(SERVICE_NAME=prod_controls)))";
            item.addArchiveDataSource(new ArchiveDataSource(url, 0, "Accelerator"));

            model.addItem(item);
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.SEVERE, "Cannot fake content", ex);
        }

        search_tab = new Tab(Messages.Search, search);
        search_tab.setGraphic(Activator.getIcon("search"));
        search_tab.setOnClosed(event -> autoMinimize(left_tabs, this, 0.0));
        left_tabs.getTabs().setAll(search_tab);

        properties_tab = new Tab("Properties", new PropertyPanel(model, plot.getPlot().getUndoableActionManager()));
        properties_tab.setGraphic(Activator.getIcon("properties"));
        properties_tab.setOnClosed(event -> autoMinimize(bottom_tabs, plot_and_tabs, 1.0));
        export_tab = new Tab("Export");
        export_tab.setGraphic(Activator.getIcon("export"));
        export_tab.setOnClosed(event -> autoMinimize(bottom_tabs, plot_and_tabs, 1.0));

        bottom_tabs.getTabs().setAll(properties_tab);

        plot_and_tabs.setOrientation(Orientation.VERTICAL);
        plot_and_tabs.setDividerPositions(0.8);

        getItems().setAll(left_tabs, plot_and_tabs);
        setDividerPositions(0.2);

        createContextMenu();


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
        final MenuItem show_search = new MenuItem(Messages.OpenSearchView, Activator.getIcon("search"));
        show_search.setOnAction(event -> showSearchTab());

        final MenuItem show_properties = new MenuItem(Messages.OpenPropertiesView, Activator.getIcon("properties"));
        show_properties.setOnAction(event -> showBottomTab(properties_tab));

        final MenuItem show_export = new MenuItem(Messages.OpenExportView, Activator.getIcon("export"));
        show_export.setOnAction(event -> showBottomTab(export_tab));

        // TODO Open Inspect Samples

        // TODO Open Waveform View

        final ContextMenu menu = new ContextMenu(show_search, show_properties, show_export);
        plot.getPlot().setOnContextMenuRequested(event ->
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY()));
    }

    /** If there are no tabs, minimize that part of the split pane
     *  @param tabs TabPane to check if it's empty
     *  @param pane SplitPane to adjust if there are no tabs
     *  @param pos Divider position to use if there are no tabs
     */
    private void autoMinimize(final TabPane tabs, final SplitPane pane, final double pos)
    {
        if (tabs.getTabs().isEmpty())
            pane.setDividerPositions(pos);
    }

    /** Show search tab:
     *  Assert it's there, select it,
     *  make lower split pane large enough
     */
    private void showSearchTab()
    {
        // If tab not on screen, add it
        if (! left_tabs.getTabs().contains(search_tab))
            left_tabs.getTabs().add(search_tab);

        // Select the requested tab
        left_tabs.getSelectionModel().select(search_tab);

        // Assert that the tabs section is visible
        if (getDividers().get(0).getPosition() < 0.2)
            setDividerPositions(0.2);

        // If tab was just added, its header won't show
        // correctly unless we schedule a re-layout
        Platform.runLater(() -> layout() );
    }

    /** @param tab Tab to show:
     *             Assert it's there, select it,
     *             make lower split pane large enough
     */
    private void showBottomTab(final Tab tab)
    {
        // If tab not on screen, add it
        if (! bottom_tabs.getTabs().contains(tab))
            if (tab == properties_tab)
                bottom_tabs.getTabs().add(0, tab);
            else
                bottom_tabs.getTabs().add(tab);

        // Select the requested tab
        bottom_tabs.getSelectionModel().select(tab);

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
        memento.getBoolean(SHOW_SEARCH).ifPresent(show -> { if (! show) left_tabs.getTabs().remove(search_tab); });
        memento.getBoolean(SHOW_PROPERTIES).ifPresent(show -> { if (! show) bottom_tabs.getTabs().remove(properties_tab); });
        memento.getBoolean(SHOW_EXPORT).ifPresent(show -> { if (show) bottom_tabs.getTabs().add(export_tab); });
    }

    /** @param memento Where to store current settings */
    public void save(final Memento memento)
    {
        search.save(memento);
        memento.setNumber(LEFT_RIGHT_SPLIT, getDividers().get(0).getPosition());
        memento.setNumber(PLOT_TABS_SPLIT, plot_and_tabs.getDividers().get(0).getPosition());
        memento.setBoolean(SHOW_SEARCH, left_tabs.getTabs().contains(search_tab));
        memento.setBoolean(SHOW_PROPERTIES, bottom_tabs.getTabs().contains(properties_tab));
        memento.setBoolean(SHOW_EXPORT, bottom_tabs.getTabs().contains(export_tab));
    }

    public void dispose()
    {
        plot.dispose();
        controller.stop();
    }
}
