/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.prefs.Preferences;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.OpenFileDialog;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Phoebus application for EDM converter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EDMConverterApplication implements AppResourceDescriptor
{
    private static final List<String> FILE_EXTENSIONS = List.of("edl");
    public static final String NAME = "convert_edm";
    public static final String DISPLAY_NAME = "EDM Converter";

    private static String colors_list;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(EDMConverterApplication.class, "/edm_converter_preferences.properties");
        colors_list = prefs.get("colors_list");
    }

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

        // Get colors.list location from preferences
        if (colors_list.isBlank())
        {
            // Prompt if not set
            final File selected = new OpenFileDialog().promptForFile(window, "Select EDM colors.list", null, null);
            if (selected == null)
                return null;

            colors_list = selected.getAbsolutePath();
            final Preferences prefs = Preferences.userNodeForPackage(EDMConverterApplication.class);
            prefs.put("colors_list", colors_list);
        }

        // Perform actual conversion in background thread
        JobManager.schedule("Convert " + resource, monitor ->
        {
            try
            {
                EdmModel.reloadEdmColorFile(colors_list, new FileInputStream(colors_list));

                // Convert file
                final File input = ModelResourceUtil.getFile(resource);
                final File output = new File(input.getAbsolutePath().replace(".edl", ".bob"));
                new Converter(input, output);

                // On success, open in display editor, runtime, other editor
                Platform.runLater(() ->
                    ApplicationLauncherService.openFile(output, true, (Stage)window));
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(DISPLAY_NAME, "Failed to open " + resource, ex);
            }
        });
        return null;
    }
}
