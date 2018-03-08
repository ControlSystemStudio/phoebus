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
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.XMLCommandReader;
import org.csstudio.scan.command.XMLCommandWriter;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.ui.ScanURI;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
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

    /** Scan that's monitored, -1 for none */
    private volatile long active_scan = -1;

    /** {@link ScanInfoModel}, set while monitoring scan */
    private final AtomicReference<ScanInfoModel> scan_info_model = new AtomicReference<>();

    /** Track update of {@link ScanEditorInstance#active_scan} */
    private final ScanInfoModelListener scan_info_listener = new ScanInfoModelListener()
    {
        @Override
        public void scanUpdate(List<ScanInfo> infos)
        {
            for (ScanInfo info : infos)
                if (info.getId() == active_scan)
                {
                    Platform.runLater(() -> editor.updateScanInfo(info));
                    if (info.getState().isDone())
                        detachFromScan();
                    return;
                }
            Platform.runLater(() -> editor.updateScanInfo(null));
        }

        @Override
        public void connectionError()
        {
            Platform.runLater(() -> editor.updateScanInfo(null));
        }
    };

    ScanEditorInstance(final ScanEditorApplication app)
    {
        this.app = app;

        URI input = null;
        tab = new DockItemWithInput(this, editor, input , file_extensions, this::doSave);
        tab.addClosedNotification(this::detachFromScan);

        DockPane.getActiveDockPane().addTab(tab);

        editor.getUndo().addListener((undo, redo) ->  tab.setDirty(undo != null));
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

        JobManager.schedule("Read Scan", monitor ->
        {
            monitor.beginTask("Read scan #" + id);
            try
            {
                // Get information about scans
                final ScanInfoModel infos = ScanInfoModel.getInstance();
                if (scan_info_model.getAndSet(infos) != null)
                    throw new IllegalStateException("Already attached to scan");

                // Read commands for scan
                final String xml = infos.getScanClient().getScanCommands(id);
                final List<ScanCommand> commands = XMLCommandReader.readXMLString(xml);
                editor.getModel().setCommands(commands);

                // Monitor scan progress until no longer active
                active_scan = id;
                editor.attachScan(id, infos.getScanClient());
                infos.addListener(scan_info_listener);
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
            XMLCommandWriter.write(out, editor.getModel().getCommands());
            editor.getUndo().clear();
        }
    }

    /** If currently monitoring a scan, detach */
    private void detachFromScan()
    {
        final ScanInfoModel infos = scan_info_model.getAndSet(null);
        if (infos != null)
        {
            infos.removeListener(scan_info_listener);
            Platform.runLater(editor::detachScan);
            infos.release();
        }
    }
}
