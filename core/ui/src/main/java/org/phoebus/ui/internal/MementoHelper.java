/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.internal;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.net.URL;
import java.util.logging.Level;

import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import javafx.stage.Stage;

/** Helper for persisting UI to/from memento
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MementoHelper
{
    private static final String FULLSCREEN = "fullscreen";
    private static final String HEIGHT = "height";
    private static final String INPUT_URL = "input_url";
    private static final String MAXIMIZED = "maximized";
    private static final String MINIMIZED = "minimized";
    private static final String WIDTH = "width";
    private static final String X = "x";
    private static final String Y = "y";

    /** Save state of Stage to memento
     *  @param memento
     *  @param stage
     */
    public static void saveStage(final MementoTree memento, final Stage stage)
    {
        final MementoTree stage_memento = memento.getChild(DockStage.getID(stage));
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

        for (DockItem item : DockStage.getDockPane(stage).getDockItems())
            saveDockItem(stage_memento, item);
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
        final MementoTree item_memento = memento.getChild(item.getID());
        final AppInstance application = item.getApplication();
        if (application == null)
            return;
        item_memento.setString(DockItem.KEY_APPLICATION, application.getAppDescriptor().getName());

        if (item instanceof DockItemWithInput)
        {
            final URL input = ((DockItemWithInput)item).getInput();
            if (input != null)
                item_memento.setString(INPUT_URL, input.toString());
        }

        try
        {
            application.save(item_memento);
        }
        catch (Throwable ex)
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
        stage_memento.getNumber(X).ifPresent(num -> stage.setX(num.doubleValue()));
        stage_memento.getNumber(Y).ifPresent(num -> stage.setY(num.doubleValue()));
        stage_memento.getNumber(WIDTH).ifPresent(num -> stage.setWidth(num.doubleValue()));
        stage_memento.getNumber(HEIGHT).ifPresent(num -> stage.setHeight(num.doubleValue()));
        stage_memento.getBoolean(FULLSCREEN).ifPresent(flag -> stage.setFullScreen(flag));
        stage_memento.getBoolean(MAXIMIZED).ifPresent(flag -> stage.setMaximized(flag));
        stage_memento.getBoolean(MINIMIZED).ifPresent(flag -> stage.setIconified(flag));

        final DockPane pane = DockStage.getDockPane(stage);
        boolean any = false;
        for (MementoTree item_memento : stage_memento.getChildren())
            any |= restoreDockItem(item_memento, pane);
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

        DockPane.setActiveDockPane(pane);
        final AppInstance instance;
        if (app instanceof AppResourceDescriptor)
        {
            final AppResourceDescriptor app_res = (AppResourceDescriptor) app;
            final String input = item_memento.getString(INPUT_URL).orElse(null);
            instance = input == null
                    ? app_res.create()
                    : app_res.create(input);
        }
        else
            instance = app.create();

        instance.restore(item_memento);

        return true;
    }
}
