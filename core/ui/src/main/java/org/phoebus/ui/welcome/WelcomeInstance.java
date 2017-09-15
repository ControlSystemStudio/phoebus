/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.welcome;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/** Welcome Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WelcomeInstance implements AppInstance
{
    /** At most one instance */
    static WelcomeInstance INSTANCE = null;

    private final AppDescriptor app;

    private final DockItem tab;

    WelcomeInstance(final AppDescriptor app)
    {
        this.app = app;

        tab = new DockItem(this, createContent());
        DockPane.getActiveDockPane().addTab(tab);

        // Track when instance is closed
        tab.addClosedNotification(() -> INSTANCE = null);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }


    private Node createContent()
    {
        final Label info = new Label("Welcome to Phoebus!\n\n" +
                                     "Try pushing the buttons in the toolbar");
        return new BorderPane(info);
    }

    void raise()
    {
        tab.select();
    }
}
