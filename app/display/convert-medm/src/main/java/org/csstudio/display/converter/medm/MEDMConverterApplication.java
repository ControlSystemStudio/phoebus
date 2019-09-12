/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.medm;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockPane;

import javafx.stage.Stage;

/** Phoebus application for MEDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MEDMConverterApplication implements AppResourceDescriptor
{
    private static final List<String> FILE_EXTENSIONS = List.of("adl");
    public static final String NAME = "convert_medm";
    public static final String DISPLAY_NAME = "MEDM Converter";

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
        return DisplayModel.class.getResource("/icons/display.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return FILE_EXTENSIONS;
    }

    @Override
    public AppInstance create()
    {
        ExceptionDetailsErrorDialog.openError(DISPLAY_NAME, "Must be called with a file name", new Exception("No file name"));
        return null;
    }

    @Override
    public AppInstance create(final URI resource)
    {
        try
        {
            // Convert file
            final File input = ModelResourceUtil.getFile(resource);
            final File output = new File(input.getAbsolutePath().replace(".adl", ".bob"));
            new Converter(input, output);

            // On success, open in display editor, runtime, other editor
            ApplicationLauncherService.openFile(output, true, (Stage)DockPane.getActiveDockPane().getScene().getWindow());
            // On success, open the display editor
            // final AppResourceDescriptor editor = ApplicationService.findApplication("display_editor");
            // editor.create(output.toURI());
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(DISPLAY_NAME, "Failed to open " + resource, ex);
        }
        return null;
    }
}
