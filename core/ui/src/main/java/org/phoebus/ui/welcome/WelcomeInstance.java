/*******************************************************************************
 * Copyright (c) 2017-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.welcome;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.net.URL;
import java.util.logging.Level;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

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

    private WebView browser;

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
        browser = new WebView();
        final WebEngine engine = browser.getEngine();

        String url = Preferences.welcome;
        if (url.isEmpty())
        {
            final URL resource = getClass().getResource("welcome.html");
            url = resource.toExternalForm();
        }
        logger.log(Level.CONFIG, "Welcome URL: " + url);
        engine.load(url);

        return browser;
    }

    void raise()
    {
        tab.select();
    }
}
