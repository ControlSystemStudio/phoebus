/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.logging.Level;

import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.ContextMenuEntry;
import org.phoebus.framework.workbench.ContextMenuService;
import org.phoebus.ui.docking.DockStage;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Helper for selection-based additions to context menu
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuHelper
{
    /** Add entries suitable for the current selection
     *
     *  <p>Invoke inside 'setOnContextMenuRequested' handler,
     *  after adding application specific menu entries,
     *  to add entries based on the current selection.
     *
     *  @param parent_node Parent node, usually owner of the context menu
     *  @param menu Menu where selection-based entries will be added
     */
    public static void addSupportedEntries(Node parent_node, ContextMenu menu)
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
            item.setOnAction(e ->
            {
                try
                {
                    entry.callWithSelection(stage, SelectionService.getInstance().getSelection());
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
