/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.List;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.XMLCommandReader;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.stage.FileChooser.ExtensionFilter;

/** Application instance for Scan Editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanEditorInstance  implements AppInstance
{
    private static final ExtensionFilter[] file_extensions = new ExtensionFilter[]
    {
        new ExtensionFilter("All", "*.*"),
        new ExtensionFilter("Scan", "*.scn"),
    };

    private final ScanEditorApplication app;
    private final DockItemWithInput tab;
    private final ScanEditor editor = new ScanEditor();

    ScanEditorInstance(final ScanEditorApplication app)
    {
        this.app = app;

        URI input = null;
        tab = new DockItemWithInput(this, editor, input , file_extensions, this::doSave);

        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    public void open(final File file)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        tab.setInput(ResourceParser.getURI(file));

        JobManager.schedule("Load Scan", monitor ->
        {
            monitor.beginTask("Read " + file);
            try
            {
                final List<ScanCommand> commands = XMLCommandReader.readXMLStream(new FileInputStream(file));
                editor.setCommands(commands);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(editor, "Error", "Cannot read scan from\n" + file, ex);
            }
        });
    }

    void doSave(final JobMonitor monitor) throws Exception
    {
        // TODO See DisplayEditorInstance
    }
}
