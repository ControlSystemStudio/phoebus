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
@SuppressWarnings("nls")
public class StatusBar extends HBox
{
    private static final StatusBar instance = new StatusBar();

    private StatusBar()
    {
        super(5, ToolbarHelper.createSpring());
    }

    public static StatusBar getInstance()
    {
        return instance;
    }

    public void addItem(final Node item)
    {
        getChildren().add(item);
    }
}
