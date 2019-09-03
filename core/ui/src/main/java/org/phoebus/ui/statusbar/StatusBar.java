/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.statusbar;

import org.phoebus.ui.Preferences;
import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
        super(5);
        setPadding(new Insets(0, 5, 5, 5));

        // Show User ID?
        if (Preferences.status_show_user)
            getChildren().add(new Label(System.getProperty("user.name")));

        // Filler between standard options and additions (update, progress, ...)
        getChildren().add(ToolbarHelper.createSpring());
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

    /** Remove item from the status bar
     *
     *  @param item Item to remove
     */
    public void removeItem(final Node item)
    {
        getChildren().remove(item);
    }
}
