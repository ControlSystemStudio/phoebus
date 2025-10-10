/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;
import org.phoebus.ui.web.WebBrowserApplication;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import static org.phoebus.ui.application.PhoebusApplication.logger;

/** Menu entry to open help
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenHelp implements MenuEntry
{
    @Override
    public String getName()
    {
        return Messages.Help;
    }

    @Override
    public String getMenuPath()
    {
        return Messages.HelpContentMenuPath;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(getClass(), "/icons/help.png");
    }

    @Override
    public Void call()
    {
        try {
            URI helpLocationURI = new URI(determineHelpLocation());
            WebBrowserApplication webBrowserApplication = new WebBrowserApplication();
            webBrowserApplication.create(helpLocationURI);
        } catch (URISyntaxException uriSyntaxException) {
            throw new RuntimeException(uriSyntaxException);
        }
        return null;
    }

    public static String determineHelpLocation()
    {
        if (!Preferences.documentation_location.isEmpty()) {
            String suffix = Preferences.documentation_location.endsWith("/") ? "index.html" : "/index.html";
            return Preferences.documentation_location + suffix;
        }

        final File phoenix_install = Locations.install();

        // The distribution includes a lib/ and a doc/ folder.
        // Check for the doc/index.html
        File loc = new File(phoenix_install, "doc/index.html");
        if (loc.exists())
            return loc.toURI().toString();

        // During development,
        // product is started from IDE as ....../git/phoebus/phoebus-product.
        // Check for copy of docs in      ....../git/phoebus/docs/build/html
        loc = new File(phoenix_install, "docs");
        if (loc.exists())
        {
            loc = new File(loc, "build/html/index.html");
            if (loc.exists())
                return loc.toURI().toString();
            logger.log(Level.WARNING, "Found phoebus-doc repository, but no build/html/index.html. Run 'make html'");
        }

        // Fall back to online copy of the manual
        return "https://control-system-studio.readthedocs.io";
    }
}
