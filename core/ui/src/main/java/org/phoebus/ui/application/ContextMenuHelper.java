/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.net.URL;
import java.util.logging.Level;

import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Helper for selection-based additions to context menu
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuHelper
{
    /** Load icon from class resource
     *
     *  <p>Supports alternate resolution icons:
     *  Given "icon-name.png", it will
     *  actually load "icon-name@2x.png" or "icon-name@3x.png",
     *  if available,
     *  on high resolution displays.
     *
     *  @param clazz Class used to load the resource
     *  @param icon_path Path to icon resource
     *  @return Icon {@link Image} or <code>null</code> if not found
     */
    public static Image loadIcon(final Class<?> clazz, final String icon_path)
    {
        final URL resource = clazz.getResource(icon_path);
        if (resource == null)
        {
            logger.log(Level.WARNING, "Cannot open '" + icon_path + "' for " + clazz);
            return null;
        }
        return new Image(resource.toExternalForm());
    }

    /** Add entries suitable for the current selection
     *
     *  <p>Invoke inside 'setOnContextMenuRequested' handler,
     *  after adding application specific menu entries,
     *  to add entries based on the current selection.
     *
     *  @param parent_node Parent node, usually owner of the context menu
     *  @param menu Menu where selection-based entries will be added
     */
    public static void addSupportedEntries(final Node parent_node, final ContextMenu menu)
    {
        final Window window = parent_node.getScene().getWindow();
        if (! (window instanceof Stage))
        {
            logger.log(Level.WARNING, "Expected 'Stage' for context menu, got " + window);
            return;
        }
        final Stage stage = (Stage) window;
        // Assert that this window's dock pane is the active one.
        // (on Mac and Linux, invoking the context menu will not
        //  always activate the stage)
        DockStage.setActiveDockPane(stage);

        for (ContextMenuEntry<?> entry : ContextMenuService.getInstance().listSupportedContextMenuEntries())
        {
            final MenuItem item = new MenuItem(entry.getName());

            final Image icon = entry.getIcon();
            if (icon != null)
                item.setGraphic(new ImageView(icon));
            item.setOnAction(e ->
            {
                try
                {
                    entry.callWithSelection(SelectionService.getInstance().getSelection());
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot invoke context menu", ex);
                }
            });
            menu.getItems().add(item);
        }
    }
}
