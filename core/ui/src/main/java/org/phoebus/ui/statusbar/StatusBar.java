/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.statusbar;

import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.scene.Node;
import javafx.scene.layout.HBox;

/** Application status bar
 *  @author Kay Kasemir
 */
public class StatusBar extends HBox
{
    private static final StatusBar instance = new StatusBar();

    private StatusBar()
    {
        super(5, ToolbarHelper.createSpring());
    }

    /** @return Singleton instance */
    public static StatusBar getInstance()
    {
        return instance;
    }

    /** Add item to the status bar
     *
     *  <p>Caller may later update the item,
     *  for example make it invisible when
     *  not applicable.
     *
     *  @param item Item to add
     */
    // Note: There is currently no control over the item location
    public void addItem(final Node item)
    {
        getChildren().add(item);
    }
}
