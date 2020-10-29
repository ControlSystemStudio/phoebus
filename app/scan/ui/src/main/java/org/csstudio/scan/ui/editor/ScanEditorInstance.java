/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.XMLCommandReader;
import org.csstudio.scan.command.XMLCommandWriter;
import org.csstudio.scan.ui.ScanURI;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
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

        tab = new DockItemWithInput(this, editor, null, file_extensions, this::doSave);
        tab.addClosedNotification(this::onClose);

        DockPane.getActiveDockPane().addTab(tab);

        editor.getUndo().addListener((undo, redo, changeCount) ->  tab.setDirty(undo != null || changeCount > 0));
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** Reads commands for scan from file
     *
     *  @param file Scan file
     */
    void open(final File file)
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
                editor.setScanName(file);
                editor.getModel().setCommands(commands);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(editor, "Error", "Cannot read scan from\n" + file, ex);
            }
        });
    }

    /** Reads commands for scan from server, then monitors progress
     *
     *  @param id Scan ID
     */
    void open(final long id)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        tab.setInput(ScanURI.createURI(id));
        Platform.runLater(() -> tab.setLabel("Scan Editor #" + id));

        JobManager.schedule("Read Scan", monitor ->
        {
            monitor.beginTask("Read scan #" + id);
            try
            {
                // Read commands for scan
                final ScanClient client = new ScanClient(Preferences.host, Preferences.port);
                final String xml = client.getScanCommands(id);
                final List<ScanCommand> commands = XMLCommandReader.readXMLString(xml);
                editor.getModel().setCommands(commands);

                // Monitor scan progress until no longer active
                editor.attachScan(id);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(editor, "Error", "Cannot read scan #" + id, ex);
            }
        });
    }

    /** Save current scan to file, reset 'undo' */
    private void doSave(final JobMonitor monitor) throws Exception
    {
        final File file = Objects.requireNonNull(ResourceParser.getFile(tab.getInput()));
        try
        (
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        )
        {
            editor.setScanName(file);
            XMLCommandWriter.write(out, editor.getModel().getCommands());
            editor.getUndo().clear();
        }
    }

    @Override
    public void restore(final Memento memento)
    {
        editor.restore(memento);
    }

    @Override
    public void save(final Memento memento)
    {
        editor.save(memento);
    }

    private void onClose()
    {
        editor.detachFromScan();
    }
}
