/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.docking.DockStage;

import java.util.logging.Level;

/** Helper for handling focus
 *  @author Kay Kasemir, Kunal Shroff, Abraham Wolk
 */
public class FocusUtil
{
    /** @param node Node from which to remove focus */
    public static void removeFocus(Node node)
    {
        // Cannot un-focus, can only focus on _other_ node.
        // --> Find the uppermost node and focus on that.
        Node parent = node.getParent();
        if (parent == null)
            return;
        while (parent.getParent() != null)
            parent = parent.getParent();
        parent.requestFocus();
    }

    /**
     * Create a Runnable which when called sets the focus on the first DockPane of the Stage hosting the provided Node
     * @param node A node
     * @return A Runnable to set the Focus on the first DockPane of the Stage which holds the Node
     */
    public static Runnable setFocusOn(final Node node){
        {
            Window window = node.getScene().getWindow();
            if (window instanceof Stage)
            {
                final Stage stage = (Stage) window;
                return () -> DockStage.setActiveDockStage(stage);
            } else
            {
                PhoebusApplication.logger.log(Level.WARNING, "Expected 'Stage' for context menu, got " + window);
                return () -> {
                };
            }
        }
    }
}
