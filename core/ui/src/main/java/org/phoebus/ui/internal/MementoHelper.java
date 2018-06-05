/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.internal;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.framework.persistence.XMLMementoTree;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.docking.SplitDock;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Stage;

/** Helper for persisting UI to/from memento
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MementoHelper
{
    private static final String FIXED = "fixed";
    private static final String FULLSCREEN = "fullscreen";
    private static final String HEIGHT = "height";
    private static final String HORIZONTAL = "horizontal";
    private static final String INPUT_URI = "input_uri";
    private static final String MAXIMIZED = "maximized";
    private static final String MINIMIZED = "minimized";
    private static final String PANE = "pane";
    private static final String POS = "pos";
    private static final String SELECTED = "selected";
    private static final String SPLIT = "split";
    private static final String WIDTH = "width";
    private static final String X = "x";
    private static final String Y = "y";

    /** Memento keys */
    private static final String LAST_OPENED_FILE = "last_opened_file",
                                DEFAULT_APPLICATION = "default_application",
                                SHOW_TABS = "show_tabs";

    /** Save state of Stage to memento
     *  @param memento
     *  @param stage
     */
    public static void saveStage(final MementoTree memento, final Stage stage)
    {
        final MementoTree stage_memento = memento.createChild(DockStage.getID(stage));
        stage_memento.setNumber(X, stage.getX());
        stage_memento.setNumber(Y, stage.getY());
        stage_memento.setNumber(WIDTH, stage.getWidth());
        stage_memento.setNumber(HEIGHT, stage.getHeight());
        if (stage.isFullScreen())
            stage_memento.setBoolean(FULLSCREEN, true);
        else if (stage.isMaximized())
            stage_memento.setBoolean(MAXIMIZED, true);
        else if (stage.isIconified())
            stage_memento.setBoolean(MINIMIZED, true);

        final Node node = DockStage.getPaneOrSplit(stage);
        savePaneOrSplit(stage_memento, node);
    }

    /** Persist each stage (window) and its tabs
     * @param memento */
    public static void saveStages(final MementoTree memento)
    {
        for (final Stage stage : DockStage.getDockStages())
           saveStage(memento, stage);
    }

    /** @param memento
     *  @param item DockPane or SplitDock
     */
    private static void savePaneOrSplit(final MementoTree memento, final Node node)
    {
        if (node instanceof DockPane)
        {
            final DockPane pane = (DockPane) node;
            final MementoTree pane_memento = memento.createChild(PANE);
            if (pane.isFixed())
                pane_memento.setBoolean(FIXED, true);
            pane_memento.setNumber(SELECTED, pane.getSelectionModel().getSelectedIndex());
            for (final DockItem item : pane.getDockItems())
            {
                saveDockItem(pane_memento, item);
            }
        }
        else if (node instanceof SplitDock)
        {
            final SplitDock split = (SplitDock) node;
            final MementoTree split_memento = memento.createChild(SPLIT);
            split_memento.setNumber(POS, split.getDividerPosition());
            if (! split.isHorizontal())
                split_memento.setBoolean(HORIZONTAL, false);
            for (final Node sub : split.getItems())
                savePaneOrSplit(split_memento, sub);
        }
        else
            logger.log(Level.WARNING, "Cannot persist " + node);
    }

    /** Save state of a DockItem
     *
     *  <p>Store the application name, optionally the resource (input),
     *  and then allow the application itself to store details.
     *
     *  @param memento
     *  @param item
     */
    private static void saveDockItem(final MementoTree memento, final DockItem item)
    {
        final AppInstance application = item.getApplication();
        if (application == null  ||  application.isTransient())
            return;

        final MementoTree item_memento = memento.getChild(item.getID());
        item_memento.setString(DockItem.KEY_APPLICATION, application.getAppDescriptor().getName());

        if (item instanceof DockItemWithInput)
        {
            final URI input = ((DockItemWithInput)item).getInput();
            if (input != null)
                item_memento.setString(INPUT_URI, input.toString());
        }

        try
        {
            application.save(item_memento);
        }
        catch (final Throwable ex)
        {
            logger.log(Level.SEVERE, "Application " + application.getAppDescriptor().getDisplayName() + " memento error", ex);
        }
    }

    /** Restore state of Stage from memento
     *  @param memento
     *  @param stage
     *  @return <code>true</code> if any tab item was restored
     */
    public static boolean restoreStage(final MementoTree stage_memento, final Stage stage)
    {
        // Closest to atomically setting size and location with minimal flicker: hide, update, show
        stage.hide();
        stage_memento.getNumber(X).ifPresent(num -> stage.setX(num.doubleValue()));
        stage_memento.getNumber(Y).ifPresent(num -> stage.setY(num.doubleValue()));
        stage_memento.getNumber(WIDTH).ifPresent(num -> stage.setWidth(num.doubleValue()));
        stage_memento.getNumber(HEIGHT).ifPresent(num -> stage.setHeight(num.doubleValue()));
        stage.show();
        stage_memento.getBoolean(FULLSCREEN).ifPresent(flag -> stage.setFullScreen(flag));
        stage_memento.getBoolean(MAXIMIZED).ifPresent(flag -> stage.setMaximized(flag));
        stage_memento.getBoolean(MINIMIZED).ifPresent(flag -> stage.setIconified(flag));

        // Initial pane of the new stage
        final DockPane pane = DockStage.getDockPanes(stage).get(0);
        // <pane> content or <split>
        final List<MementoTree> children = stage_memento.getChildren();
        if (children.size() != 1)
        {
            logger.log(Level.WARNING, "Expect single <pane> or <split>, got " + children);
            return false;
        }
        return restorePaneOrSplit(pane, children.get(0));
    }

    /** @param pane Initial, empty pane that might be filled with items or split
     *  @param children
     *  @return <code>true</code> if any tab item was restored
     */
    private static boolean restorePaneOrSplit(final DockPane pane, final MementoTree content)
    {
        boolean any = false;
        if (content.getName().equals(PANE))
        {   // Fill given pane with tabs
            for (final MementoTree item_memento : content.getChildren())
                any |= restoreDockItem(item_memento, pane);
            // Maybe select a specific tab
            // If the new tab is inside a SplitDock,
            // it won't actually exist until the content of the SplitDock's SplitPane
            // is rendered on the next UI tick, resulting in a NullPointerException
            // deep inside JFX calling TabPane$TabPaneSelectionModel.select
            // By deferring to a later UI tick, the tab selection succeeds
            content.getNumber(SELECTED).ifPresent(index -> Platform.runLater(() -> pane.getSelectionModel().select(index.intValue())));

            // If pane is 'fixed', mark as such _after_ all items have been restored
            // to prevent changes from now on
            content.getBoolean(FIXED).ifPresent(fixed -> Platform.runLater(() -> pane.setFixed(fixed)));
        }
        else if (content.getName().equals(SPLIT))
        {   // Split the original pane
            final SplitDock split = pane.split(content.getBoolean(HORIZONTAL).orElse(true));

            // Fill split's items
            final List<Node> first_second_pane = split.getItems();
            final List<MementoTree> first_second_mememto = content.getChildren();
            any |= restorePaneOrSplit((DockPane) first_second_pane.get(0), first_second_mememto.get(0));
            any |= restorePaneOrSplit((DockPane) first_second_pane.get(1), first_second_mememto.get(1));

            // The divider position needs to be set at the end, after the complete scene has been restored.
            // Otherwise the SplitPane self-adjusts the position when the sub-elements are rendered,
            // replacing a position set now.
            content.getNumber(POS).ifPresent(num -> Platform.runLater(() -> split.setDividerPosition(num.doubleValue())));
        }
        else
            logger.log(Level.WARNING, "Expect <pane> or <split>, got " + content);
        return any;
    }

    /** Restore a DockItem and AppInstance from memento
     *
     *  <p>Create the {@link AppInstance} from the application name
     *  stored in memento.
     *  For resource-based application, check for saved resource (input),
     *  then allow application to restore details.
     *
     *  @param item_memento
     *  @param pane
     *  @return <code>true</code> if a tab was restored
     */
    private static boolean restoreDockItem(final MementoTree item_memento, final DockPane pane)
    {
        final String app_id = item_memento.getString(DockItem.KEY_APPLICATION).orElse(null);

        if (app_id == null)
            return false;

        final AppDescriptor app = ApplicationService.findApplication(app_id);
        if (app == null)
        {
            logger.log(Level.WARNING, "No application found to restore " + app_id);
            return false;
        }

        // If dock item was restored within a SplitDock,
        // its Scene is only set on a later UI tick when the SplitPane is rendered.
        // Defer restoring the application so that DockPane.autoHideTabs can locate the tab header
        Platform.runLater(() -> restoreApplication(item_memento, pane, app));

        return true;
    }

    private static void restoreApplication(final MementoTree item_memento, final DockPane pane, final AppDescriptor app)
    {
        DockPane.setActiveDockPane(pane);
        final AppInstance instance;
        if (app instanceof AppResourceDescriptor)
        {
            final AppResourceDescriptor app_res = (AppResourceDescriptor) app;
            final String input = item_memento.getString(INPUT_URI).orElse(null);
            instance = input == null
                    ? app_res.create()
                    : app_res.create(URI.create(input));
        }
        else
            instance = app.create();

        instance.restore(item_memento);
    }

    /**
     * <p> Write all the current stages to a memento file.
     * @param memento_file
     * @param last_opened_file
     * @param default_application
     *  */
    public static void saveState(final File memento_file, final File last_opened_file, final String default_application)
    {
        PhoebusApplication.logger.log(Level.INFO, "Persisting state to " + memento_file);
        try
        {
            final XMLMementoTree memento = XMLMementoTree.create();

            // Persist global settings
            if (last_opened_file != null)
                memento.setString(LAST_OPENED_FILE, last_opened_file.toString());
            if (default_application != null)
                memento.setString(DEFAULT_APPLICATION, default_application);
            memento.setBoolean(SHOW_TABS, DockPane.isAlwaysShowingTabs());

            // Persist each stage (window) and its tabs
            saveStages(memento);

            // Write the memento file
            if (!memento_file.getParentFile().exists())
                memento_file.getParentFile().mkdirs();
            memento.write(new FileOutputStream(memento_file));
        }
        catch (final Exception ex)
        {
            PhoebusApplication.logger.log(Level.WARNING, "Error writing saved state to " + memento_file, ex);
        }
    }

    /** <p> Close a DockPane or SplitDock and all tabs held within.
     * @param node
     * @return <code>true</code> if all the tabs close successfully.*/
    public static boolean closePaneOrSplit(Node node)
    {
        if (node instanceof DockPane)
        {
            final DockPane pane = (DockPane) node;
        	final List<DockItem> items = pane.getDockItems();
            for (final DockItem item : items)
            {
                if (! closeDockItem(item))
                    return false;
            }
        }
        else if (node instanceof SplitDock)
        {
            final SplitDock split = (SplitDock) node;
            
            // Altering size of list we are iterating over.
            // Cannot rely on ...getItems.size() to provide fixed value.
            // Cannot rely on foreach or for loop iterators.
            // Read size once and request the first node a fixed number of times.
            int size = split.getItems().size();
            for (int i = 0; i < size; i++)
            {
            	if (! closePaneOrSplit(split.getItems().get(0)))
            		return false;
            }
        }
        else
        {
            logger.log(Level.WARNING, "Cannot close " + node);
            return false;
        }

        return true;
    }

    /** <p> Close a dock item.
     * @param item
     * @return <code>true</code> if the dock item closed.
     * */
    private static boolean closeDockItem(DockItem item)
    {
    	System.out.println("Closing " + item.toString());
        if (! item.close())
            return false;
        return true;
    }
}
