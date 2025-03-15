/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

import static org.phoebus.applications.pvtable.PVTableApplication.logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Objects;
import java.util.logging.Level;

import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.persistence.PVTablePersistence;
import org.phoebus.applications.pvtable.ui.PVTable;
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

/** PV Table Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableInstance implements AppInstance
{
    final private AppDescriptor app;
    final private DockItemWithInput dock_item;

    private final PVTableModel model = new PVTableModel();
    private final PVTable table;

    PVTableInstance(final AppDescriptor app)
    {
        this.app = app;

        table = new PVTable(model);

        dock_item = new DockItemWithInput(this, table, null, PVTableApplication.file_extensions, this::doSave);
        DockPane.getActiveDockPane().addTab(dock_item);

        model.addListener(new PVTableModelListener()
        {
            @Override
            public void tableItemSelectionChanged(final PVTableItem item)
            {
                dock_item.setDirty(true);
            }

            @Override
            public void modelChanged()
            {
                dock_item.setDirty(true);
            }
        });

        dock_item.addClosedNotification(this::stop);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** @return {@link PVTableModel} */
    public PVTableModel getModel()
    {
        return model;
    }

    /** Raise instance in case another tab is currently visible */
    public void raise()
    {
        dock_item.select();
    }

    /** @param new_model Model from which items are transferred into this instance */
    public void transferModel(final PVTableModel new_model)
    {
        // This sends a model update
        model.transferItems(new_model);
        // Clear the 'dirty' indicator
        dock_item.setDirty(false);
    }

    void loadResource(final URI input)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        dock_item.setInput(input);

        // Load files in background job
        JobManager.schedule("Load PV Table", monitor ->
        {
            final PVTableModel load_model = new PVTableModel(false);
            try
            {
                monitor.updateTaskName("Load " + input);
                PVTablePersistence.forFilename(input.getPath())
                                  .read(load_model, ResourceParser.getContent(input));

                // On success, update on UI
                Platform.runLater(() ->
                {
                    transferModel(load_model);
                });
            }
            catch (Exception ex)
            {
                final String message = "Cannot open PV Table\n" + input;
                logger.log(Level.WARNING, message, ex);
                ExceptionDetailsErrorDialog.openError(app.getDisplayName(), message, ex);
            }
        });
    }

    private void doSave(final JobMonitor monitor) throws Exception
    {
        final File file = Objects.requireNonNull(ResourceParser.getFile(dock_item.getInput()));
        try
        (
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        )
        {
            PVTablePersistence.forFilename(file.toString()).write(model, out);
        }
    }

    /** Release resources */
    public void stop()
    {
        logger.log(Level.INFO, "Stopping PV Table...");
        model.dispose();
    }

    @Override
    public void restore(final Memento memento)
    {
        table.restore(memento);
    }

    @Override
    public void save(final Memento memento)
    {
        table.save(memento);
    }
}
