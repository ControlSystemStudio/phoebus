/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.web;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.application.PhoebusApplication;

import javafx.application.Platform;

/** Application that opens resource in external(!) web browser
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WebBrowserApplication implements AppResourceDescriptor
{
    private static final List<String> EXTENSIONS = List.of(".html");

    @Override
    public String getName()
    {
        return "web";
    }

    @Override
    public String getDisplayName()
    {
        return Messages.WebBrowser;
    }

    @Override
    public URL getIconURL()
    {
        return WebBrowserApplication.class.getResource("/icons/web.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return EXTENSIONS;
    }

    @Override
    public AppInstance create()
    {
        Platform.runLater(() -> PhoebusApplication.INSTANCE.getHostServices().showDocument(""));
        return null;
    }

    @Override
    public AppInstance create(final URI resource)
    {
        // In case the resource included the application hint, remove it
        final String url = resource.toString()
                                   .replace("?app=web&", "?")
                                   .replace("&app=web", "")
                                   .replace("?app=web", "");
        Platform.runLater(() -> PhoebusApplication.INSTANCE.getHostServices().showDocument(url));
        return null;
    }
}
