/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmConverterApplication implements AppResourceDescriptor
{
    private static final List<String> FILE_EXTENSIONS = List.of("edl");
    public static final String NAME = "convert_edm";
    public static final String DISPLAY_NAME = "EDM Converter";

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
        final Window window = DockPane.getActiveDockPane().getScene().getWindow();

        // Check colors.list
        if (ConverterPreferences.colors_list.isBlank())
        {
            ExceptionDetailsErrorDialog.openError("EDM Converter",
                "EDM colors.list is not defined.\nConfigure it in the converter settings.",
                new Exception("Need colors.list"));
            return null;
        }

        // Perform actual conversion in background thread
        JobManager.schedule("Convert " + resource, monitor ->
        {
            try
            {
                // Convert file
                final File input = ModelResourceUtil.getFile(resource);
                final File output = new File(input.getAbsolutePath().replace(".edl", ".bob"));
                logger.log(Level.INFO, "Converting " + input + " to " + output);
                EdmModel.reloadEdmColorFile(ConverterPreferences.colors_list, ModelResourceUtil.openResourceStream(ConverterPreferences.colors_list));
                new EdmConverter(input, null).write(output);

                // On success, open in display editor, runtime, other editor
                Platform.runLater(() ->
                    ApplicationLauncherService.openFile(output, true, (Stage)window));
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(DISPLAY_NAME, "Failed to convert " + resource, ex);
            }
        });
        return null;
    }
}
