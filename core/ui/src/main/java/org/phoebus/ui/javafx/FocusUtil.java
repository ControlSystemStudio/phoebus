/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Node;

/** Helper for handling focus
 *  @author Kay Kasemir
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
}
