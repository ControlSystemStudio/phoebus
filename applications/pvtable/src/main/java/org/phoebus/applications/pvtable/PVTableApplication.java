/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

import static org.phoebus.framework.util.ResourceParser.createAppURI;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.phoebus.applications.pvtable.persistence.PVTableAutosavePersistence;
import org.phoebus.applications.pvtable.persistence.PVTableXMLPersistence;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

import javafx.scene.image.Image;
import javafx.stage.FileChooser.ExtensionFilter;

/** PV Table Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableApplication implements AppResourceDescriptor
{

    public static final Logger logger = Logger.getLogger(PVTableApplication.class.getPackageName());

    static final ExtensionFilter[] file_extensions = new ExtensionFilter[]
    {
        new ExtensionFilter("All", "*.*"),
        new ExtensionFilter("PV Table", "*." + PVTableXMLPersistence.FILE_EXTENSION),
        new ExtensionFilter("Autosave", "*." + PVTableAutosavePersistence.FILE_EXTENSION)
    };

    public static final String NAME = "pv_table";
    public static final String DISPLAY_NAME = "PV Table";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return List.of(PVTableXMLPersistence.FILE_EXTENSION, PVTableAutosavePersistence.FILE_EXTENSION);
    }

    @Override
    public PVTableInstance create()
    {
        return new PVTableInstance(this);
    }

    @Override
    public PVTableInstance create(final String resource)
    {
        PVTableInstance instance = null;

        // Handles
        // -app pv_table
        // -app pv_table?pv=a&pv=b
        // -app pv_table?file=/some/file
        // but no mix of pv and file argument in one call
        final Map<String, List<String>> args = parseQueryArgs(createAppURI(resource));
        final List<String> pvs = args.get(ResourceParser.PV_ARG);
        final List<String> files = args.get(ResourceParser.FILE_ARG);
        if (pvs != null)
        {
            instance = create();
            for (String pv : pvs)
                instance.getModel().addItem(pv);
        }
        else if (files != null)
        {
            for (String file : files)
            {
                final URL input = ResourceParser.createResourceURL(file);
                // Check for existing instance with that input
                final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, input);
                if (existing != null)
                {   // Found one, raise it
                    instance = existing.getApplication();
                    instance.raise();
                }
                else
                {   // Nothing found, create new one
                    instance = create();
                    instance.loadResource(input);
                }
            }
        }
        else
            instance = create();
        return instance;
    }

    /** @param name Name of the icon
     *  @return Image for icon
     */
    public static Image getIcon(final String name)
    {
        return new Image(getIconStream(name));
    }

    /** @param name Name of the icon
     *  @return InputStream for icon
     */
    public static InputStream getIconStream(final String name)
    {
        return PVTableApplication.class.getResourceAsStream("/icons/" + name);
    }
}
