/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.imports.SampleImportAction;
import org.csstudio.trends.databrowser3.imports.SampleImporters;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.export.ExportView;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.plot.PlotListener;
import org.csstudio.trends.databrowser3.ui.properties.AddPVorFormulaMenuItem;
import org.csstudio.trends.databrowser3.ui.properties.PropertyPanel;
import org.csstudio.trends.databrowser3.ui.properties.RemoveUnusedAxes;
import org.csstudio.trends.databrowser3.ui.sampleview.SampleView;
import org.csstudio.trends.databrowser3.ui.search.SearchView;
import org.csstudio.trends.databrowser3.ui.selection.DatabrowserSelection;
import org.csstudio.trends.databrowser3.ui.waveformview.WaveformView;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.logbook.ui.menu.SendLogbookAction;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.ui.spi.ContextMenuEntry;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

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
                                SHOW_EXPORT = "show_export",
                                SHOW_WAVEFORM = "show_waveform";

    private static final Preferences prefs = PhoebusPreferenceService.userNodeForClass(Perspective.class);

    private final Model model = new Model();
    private final ModelBasedPlot plot = new ModelBasedPlot(true);
    private SearchView search;
    private ExportView export = null;
    private SampleView inspect = null;
    private WaveformView waveform = null;

    private final Controller controller;
    private final TabPane left_tabs = new TabPane(),
                          bottom_tabs = new TabPane();
    private final SplitPane plot_and_tabs = new SplitPane(plot.getPlot(), bottom_tabs);
    private PropertyPanel property_panel;
    private Tab search_tab, properties_tab, export_tab, inspect_tab, waveform_tab = null;


    public Perspective(final boolean minimal)
    {
        property_panel = new PropertyPanel(model, plot.getPlot().getUndoableActionManager());
        properties_tab = new Tab(Messages.PropertiesTabName, property_panel);
        properties_tab.setGraphic(Activator.getIcon("properties"));
        properties_tab.setOnClosed(event ->
        {
            // Update pref that properties were last closed
            prefs.putBoolean(SHOW_PROPERTIES, false);
            autoMinimizeBottom();
        });

        if (! minimal)
        {
            // Check preferences
            if (prefs.getBoolean(SHOW_PROPERTIES, true))
                bottom_tabs.getTabs().setAll(properties_tab);
        }

        plot_and_tabs.setOrientation(Orientation.VERTICAL);
        plot_and_tabs.setDividerPositions(0.8);

        getItems().setAll(left_tabs, plot_and_tabs);
        setDividerPositions(0.2);

        createContextMenu();
        setupDrop();

        controller = new Controller(model, plot);
        try
        {
            controller.start();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot start data browser", ex);
        }

        // As pane is resized, assert that the minimzed left or bottom region stays minimized
        widthProperty().addListener(prop -> Platform.runLater(() -> autoMinimizeLeft()));
        heightProperty().addListener(prop -> Platform.runLater(() -> autoMinimizeBottom()));
    }

    /** @return {@link Model} */
    public Model getModel()
    {
        return model;
    }

    private void createContextMenu()
    {
        final UndoableActionManager undo = plot.getPlot().getUndoableActionManager();

        final List<MenuItem> add_data = new ArrayList<>();
        add_data.add(new AddPVorFormulaMenuItem(plot.getPlot(), model, undo, false));
        add_data.add(new AddPVorFormulaMenuItem(plot.getPlot(), model, undo, true));

        for (String type : SampleImporters.getTypes())
            add_data.add(new SampleImportAction(model, type, undo));

        final MenuItem show_search = new MenuItem(Messages.OpenSearchView, Activator.getIcon("search"));
        show_search.setOnAction(event -> showSearchTab());

        final MenuItem show_properties = new MenuItem(Messages.OpenPropertiesView, Activator.getIcon("properties"));
        show_properties.setOnAction(event ->
        {
            // Update pref that properties were last opened
            prefs.putBoolean(SHOW_PROPERTIES, true);
            showBottomTab(properties_tab);
        });

        final MenuItem show_export = new MenuItem(Messages.OpenExportView, Activator.getIcon("export"));
        show_export.setOnAction(event ->
        {
            createExportTab();
            showBottomTab(export_tab);
        });

        final MenuItem show_samples = new MenuItem(Messages.InspectSamples, Activator.getIcon("search"));
        show_samples.setOnAction(event ->
        {
            createInspectionTab();
            showBottomTab(inspect_tab);
        });

        final MenuItem show_waveform = new MenuItem(Messages.OpenWaveformView, Activator.getIcon("wavesample"));
        show_waveform.setOnAction(event ->
        {
            createWaveformTab();
            showBottomTab(waveform_tab);
        });
        final MenuItem refresh = new MenuItem(Messages.Refresh, Activator.getIcon("refresh_remote"));
        refresh.setOnAction(event -> controller.scheduleArchiveRetrieval());

        final ContextMenu menu = new ContextMenu();
        final ObservableList<MenuItem> items = menu.getItems();

        plot.getPlot().setOnContextMenuRequested(event ->
        {
            items.clear();
            items.add(new ToggleToolbarMenuItem(plot.getPlot()));
            items.add(new SeparatorMenuItem());
            items.addAll(add_data);

            items.add(new SeparatorMenuItem());
            items.add(new PrintAction(plot.getPlot()));
            items.add(new SaveSnapshotAction(plot.getPlot()));

            SelectionService.getInstance().setSelection(this, Arrays.asList(DatabrowserSelection.of(model, plot)));
            List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();
            supported.stream().forEach(action -> {
                MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
                menuItem.setOnAction((e) -> {
                    try {
                        action.call(plot.getPlot(), SelectionService.getInstance().getSelection());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Failed to exectute " + action.getName() + " from databrowser plot.", ex);
                    }
                });
                items.add(menuItem);
            });

            if (LogbookUiPreferences.is_supported)
                items.add(new SendLogbookAction(DockPane.getActiveDockPane(), Messages.ActionLogbookTitle, Messages.ActionLogbookBody, () ->  plot.getPlot().getImage()));
            if (model.getEmptyAxis().isPresent())
            {
                items.add(new SeparatorMenuItem());
                items.add(new RemoveUnusedAxes(model, undo));
            }
            items.addAll(new SeparatorMenuItem(), show_search, show_properties, show_export, show_samples, show_waveform, refresh);

            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private void createSearchTab()
    {
        if (search_tab == null)
        {
            search = new SearchView(model, plot.getPlot().getUndoableActionManager());
            search_tab = new Tab(Messages.Search, search);
            search_tab.setGraphic(Activator.getIcon("search"));
            search_tab.setOnClosed(event -> autoMinimizeLeft());
        }
    }

    private void createExportTab()
    {
        if (export_tab == null)
        {
            export = new ExportView(model);
            export_tab = new Tab(Messages.Export, export);
            export_tab.setGraphic(Activator.getIcon("export"));
            export_tab.setOnClosed(evt -> autoMinimizeBottom());
        }
    }

    private void createInspectionTab()
    {
        if (inspect_tab == null)
        {
            inspect = new SampleView(model);
            inspect_tab = new Tab(Messages.InspectSamples, inspect);
            inspect_tab.setGraphic(Activator.getIcon("search"));
            inspect_tab.setOnClosed(evt -> autoMinimizeBottom());
        }
    }

    private void createWaveformTab()
    {
        if (waveform_tab == null)
        {
            waveform = new WaveformView(model);
            waveform_tab = new Tab(Messages.OpenWaveformView, waveform);
            waveform_tab.setGraphic(Activator.getIcon("wavesample"));
            waveform_tab.setOnClosed(evt -> autoMinimizeBottom());
        }
    }

    private void setupDrop()
    {
        final Node node = plot.getPlot();
        node.setOnDragOver(event ->
        {
            final Dragboard db = event.getDragboard();
            if (db.hasString()  ||
                db.hasContent(SearchView.CHANNEL_INFOS))
            {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        node.setOnDragDropped(event ->
        {
            final PlotListener lst = plot.getListener();
            if (lst == null)
                return;
            final Dragboard db = event.getDragboard();

            // Notify lst in next UI tick...
            if (db.hasContent(SearchView.CHANNEL_INFOS))
            {
                @SuppressWarnings("unchecked")
                final List<ChannelInfo> channels = (List<ChannelInfo>) db.getContent(SearchView.CHANNEL_INFOS);
                final List<ProcessVariable> pvs = new ArrayList<>(channels);
                final List<ArchiveDataSource> archives = channels.stream().map(ChannelInfo::getArchiveDataSource).collect(Collectors.toList());
                Platform.runLater(() -> lst.droppedPVNames(pvs, archives));
            }
            else if (db.hasString())
            {
                final String dropped = db.getString();
                try
                {
                    final List<String> pvs = DroppedPVNameParser.parseDroppedPVs(dropped);
                    if (pvs.size() > 0)
                        Platform.runLater(() -> lst.droppedNames(pvs));
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot parse PV names from dropped text " + dropped, ex);
                }
            }
            else if (db.hasFiles())
            {
                for (File file : db.getFiles())
                    Platform.runLater(() -> lst.droppedFilename(file));
            }
            // .. and mark drop as completed ASAP so that cursor is reset.
            // Otherwise cursor would still suggest active drag while
            // we are for example opening a dialog to handle the dropped PVs.
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void autoMinimizeLeft()
    {
        autoMinimize(left_tabs, this, 0.0);
    }

    private void autoMinimizeBottom()
    {
        autoMinimize(bottom_tabs, plot_and_tabs, 1.0);
    }

    /** If there are no tabs, minimize that part of the split pane
     *  @param tabs TabPane to check if it's empty
     *  @param pane SplitPane to adjust if there are no tabs
     *  @param pos Divider position to use if there are no tabs
     */
    private void autoMinimize(final TabPane tabs, final SplitPane pane, final double pos)
    {
        if (tabs.getTabs().isEmpty())
            pane.getItems().remove(tabs);
    }

    /** Show search tab:
     *  Assert it's there, select it,
     *  make lower split pane large enough
     */
    private void showSearchTab()
    {
        createSearchTab();

        if (! getItems().contains(left_tabs))
        {
            getItems().add(0, left_tabs);
            setDividerPositions(0.2);
        }

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
        if (! plot_and_tabs.getItems().contains(bottom_tabs))
        {
            plot_and_tabs.getItems().add(bottom_tabs);
            plot_and_tabs.setDividerPositions(0.8);
        }
        if (plot_and_tabs.getDividers().get(0).getPosition() > 0.9)
            plot_and_tabs.setDividerPositions(0.8);

        // If tab was just added, its header won't show
        // correctly unless we schedule a re-layout
        Platform.runLater(() -> plot_and_tabs.layout() );
    }

    /** @param memento From where to restore previously saved settings */
    public void restore(final Memento memento)
    {
        property_panel.restore(memento);

        memento.getBoolean(SHOW_SEARCH).ifPresent(show ->
        {
            if (show)
            {
                createSearchTab();
                search.restore(memento);
                left_tabs.getTabs().add(search_tab);
            }
        });
        memento.getBoolean(SHOW_PROPERTIES).ifPresent(show -> { if (! show) bottom_tabs.getTabs().remove(properties_tab); });
        memento.getBoolean(SHOW_EXPORT).ifPresent(show ->
        {
            if (show)
            {
                createExportTab();
                export.restore(memento);
                bottom_tabs.getTabs().add(export_tab);
            }
        });
        memento.getBoolean(SHOW_WAVEFORM).ifPresent(show ->
        {
            if (show)
            {
                createWaveformTab();
                bottom_tabs.getTabs().add(waveform_tab);
            }
        });

        // Has no effect when run right now?
        Platform.runLater(() ->
        {
            memento.getNumber(LEFT_RIGHT_SPLIT).ifPresent(pos -> setDividerPositions(pos.floatValue()));
            memento.getNumber(PLOT_TABS_SPLIT).ifPresent(pos -> plot_and_tabs.setDividerPositions(pos.floatValue()));

            autoMinimizeLeft();
            autoMinimizeBottom();
        });
    }

    /** @param memento Where to store current settings */
    public void save(final Memento memento)
    {
        if (search != null)
            search.save(memento);

        property_panel.save(memento);

        if (export != null)
            export.save(memento);

        if (getDividers().size() > 0)
            memento.setNumber(LEFT_RIGHT_SPLIT, getDividers().get(0).getPosition());

        if (plot_and_tabs.getDividers().size() > 0)
            memento.setNumber(PLOT_TABS_SPLIT, plot_and_tabs.getDividers().get(0).getPosition());

        if (left_tabs.getTabs().contains(search_tab))
            memento.setBoolean(SHOW_SEARCH, true);

        if (! bottom_tabs.getTabs().contains(properties_tab))
            memento.setBoolean(SHOW_PROPERTIES, false);

        if (bottom_tabs.getTabs().contains(export_tab))
            memento.setBoolean(SHOW_EXPORT, true);

        if (bottom_tabs.getTabs().contains(waveform_tab))
            memento.setBoolean(SHOW_WAVEFORM, true);
    }

    public void dispose()
    {
        try
        {
            prefs.flush();
        }
        catch (BackingStoreException ex)
        {
            logger.log(Level.WARNING, "Unable to flush preferences", ex);
        }
        // Stop PVs etc. ASAP
        controller.stop();
        // Then dispose plot
        plot.dispose();
        // Not specifically disposing property_panel.
        // Their model listeners have been removed when controller stopped
        // and 'disposed' model.
    }
    
}
