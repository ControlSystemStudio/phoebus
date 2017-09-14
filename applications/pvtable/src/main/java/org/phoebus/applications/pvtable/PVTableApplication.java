/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.persistence.PVTableAutosavePersistence;
import org.phoebus.applications.pvtable.persistence.PVTablePersistence;
import org.phoebus.applications.pvtable.persistence.PVTableXMLPersistence;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.jobs.JobManager;

import javafx.application.Platform;
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

    public static final String NAME = "PV Table";

    @Override
    public String getName()
    {
        return NAME;
    }

//    @Override
//    public boolean canOpenResource(String resource)
//    {
//        return resource.endsWith(PVTableXMLPersistence.FILE_EXTENSION) ||
//               resource.endsWith(PVTableAutosavePersistence.FILE_EXTENSION);
//    }

    @Override
    public AppInstance create()
    {
        final PVTableInstance instance = new PVTableInstance();

        // XXX Eventually remove the demo content
        final PVTableModel model = instance.getModel();
        final List<ProcessVariable> pvs = new ArrayList<>();
        for (int i=1; i<=6; ++i)
        {
            model.addItem("# Local");
            model.addItem("loc://x(42)");
            model.addItem("loc://pick<VEnum>(1, \"A\", \"B\", \"C\")");
            model.addItem("# Sim");
            model.addItem("sim://sine");
            model.addItem("sim://ramp");
            model.addItem("#");
        }
        model.addItem("DTL_LLRF:IOC1:Load");

        instance.start();
    }

    @Override
    public AppInstance create(final String... resources)
    {
        // Load files in background job
        JobManager.schedule("Load PV Table", monitor ->
        {
            monitor.beginTask("Load", resources.length);
            for (String resource : resources)
            {
                final PVTableInstance instance = new PVTableInstance();
                final File file = new File(resource);
                try
                {
                    final PVTableModel model = instance.getModel();
                    PVTablePersistence.forFilename(file.toString()).read(model, new FileInputStream(file));

                    // On success, start UI
                    Platform.runLater(() -> instance.start());
                }
                catch (Exception ex)
                {
                    final String message = "Cannot open PV Table\n" + resource;
                    logger.log(Level.WARNING, message, ex);
                    ExceptionDetailsErrorDialog.openError(NAME, message, ex);
                }

                monitor.worked(1);
            }
        });
    }

    /** @param name Name of the icon
     *  @return Image for icon
     */
    public static Image getIcon(final String name)
    {
        return new Image(PVTableApplication.class.getResourceAsStream("/icons/" + name));
    }

    /**
     * 
     */
    @Override
    public List<String> supportedFileExtentions() 
    {
        return Arrays.asList(PVTableXMLPersistence.FILE_EXTENSION, PVTableAutosavePersistence.FILE_EXTENSION);
    }
}
