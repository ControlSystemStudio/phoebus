/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
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
        DockItemWithInput.ALL_FILES,
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
    public URL getIconURL()
    {
        return getClass().getResource("/icons/pvtable.png");
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
    public PVTableInstance create(final URI resource)
    {
        PVTableInstance instance = null;

        // Handles pv or file/http resource
        try
        {
            final List<String> pvs = ResourceParser.parsePVs(resource);
            if (pvs.size() > 0)
            {
                instance = create();
                for (String pv : pvs)
                    instance.getModel().addItem(pv);
            }
            else
            {
                // Check for existing instance with that input
                final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, resource);
                if (existing != null)
                {   // Found one, raise it
                    instance = existing.getApplication();
                    instance.raise();
                }
                else
                {   // Nothing found, create new one
                    instance = create();
                    instance.loadResource(resource);
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PV Table cannot open '" + resource + "'", ex);
        }
        return instance;
    }

    /** @param name Name of the icon
     *  @return Image for icon
     */
    public static Image getIcon(final String name)
    {
        return new Image(PVTableApplication.class.getResourceAsStream("/icons/" + name));
    }
}
