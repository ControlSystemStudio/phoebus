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
import java.util.logging.Level;

import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.persistence.PVTablePersistence;
import org.phoebus.applications.pvtable.ui.PVTable;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobMonitor;

import javafx.scene.layout.BorderPane;

/** PV Table Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableInstance
{
    final PVTableModel model = new PVTableModel();

    private DockItemWithInput dock_item;

    public PVTableModel getModel()
    {
        return model;
    }

    public void start()
    {
        final PVTable table = new PVTable(model);

        final BorderPane layout = new BorderPane(table);
        dock_item = new DockItemWithInput(PVTableApplication.NAME, layout, null, this::doSave);
        DockPane.getActiveDockPane().addTab(dock_item);

        model.addListener(new PVTableModelListener()
        {
            @Override
            public void tableItemSelectionChanged(PVTableItem item)
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

    private void doSave(final JobMonitor monitor) throws Exception
    {
        File file = dock_item.getInputFile();
        if (file == null )
        {
            file = SaveAsDialog.promptForFile(dock_item.getTabPane().getScene().getWindow(), "Save PV Table", null, PVTableApplication.file_extensions);
            if (file == null)
                return;
        }
        dock_item.setInputFile(file);
        try
        (
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        )
        {
            PVTablePersistence.forFilename(file.toString()).write(model, out);
        }
    }

    public void stop()
    {
        logger.log(Level.INFO, "Stopping PV Table...");
        model.dispose();
    }
}
