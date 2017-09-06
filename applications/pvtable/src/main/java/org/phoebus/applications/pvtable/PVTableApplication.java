/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.persistence.PVTableAutosavePersistence;
import org.phoebus.applications.pvtable.persistence.PVTablePersistence;
import org.phoebus.applications.pvtable.persistence.PVTableXMLPersistence;
import org.phoebus.applications.pvtable.ui.PVTable;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobMonitor;

import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser.ExtensionFilter;

/** PV Table Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableApplication
{
    public static final Logger logger = Logger.getLogger(PVTableApplication.class.getPackageName());

    private static final ExtensionFilter[] file_extensions = new ExtensionFilter[]
    {
        new ExtensionFilter("All", "*.*"),
        new ExtensionFilter("PV Table", "*." + PVTableXMLPersistence.FILE_EXTENSION),
        new ExtensionFilter("Autosave", "*." + PVTableAutosavePersistence.FILE_EXTENSION)
    };

    public static final String NAME = "PV Table";

    final PVTableModel model = new PVTableModel();

    private DockItemWithInput dock_item;

    public String getName()
    {
        return NAME;
    }

    public void start()
    {
        final List<ProcessVariable> pvs = new ArrayList<>();
        for (int i=1; i<=6; ++i)
        {
            pvs.add(new ProcessVariable("# Local"));
            pvs.add(new ProcessVariable("loc://x(42)"));
            pvs.add(new ProcessVariable("loc://pick<VEnum>(1, \"A\", \"B\", \"C\")"));
            pvs.add(new ProcessVariable("# Sim"));
            pvs.add(new ProcessVariable("sim://sine"));
            pvs.add(new ProcessVariable("sim://ramp"));
            pvs.add(new ProcessVariable("#"));
        }
        pvs.add(new ProcessVariable("DTL_LLRF:IOC1:Load"));

        start(pvs);
    }

    public void start(final List<ProcessVariable> pvs)
    {
        final PVTable table = new PVTable(model);

        // Start with list of PVs
        for (ProcessVariable pv : pvs)
            model.addItem(pv.getName());

        // Start with file
//        final File file = new File("/home/ky9/git/phoebus/phoebus-product/test.pvs");
//        try
//        {
//            PVTablePersistence.forFilename(file.toString()).read(model, new FileInputStream(file));
//        }
//        catch (Exception e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        final BorderPane layout = new BorderPane(table);
        dock_item = new DockItemWithInput(getName(), layout, null, this::doSave);
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

        dock_item.setOnClosed(event -> stop());
    }

    private void doSave(final JobMonitor monitor) throws Exception
    {
        File file = dock_item.getInputFile();
        if (file == null )
        {
            file = SaveAsDialog.promptForFile(dock_item.getTabPane().getScene().getWindow(), "Save PV Table", null, file_extensions);
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
        System.out.println("Remaining PVs " + PVPool.getPVReferences());
    }

    /** @param name Name of the icon
     *  @return Image for icon
     */
    public static Image getIcon(final String name)
    {
        return new Image(PVTableApplication.class.getResourceAsStream("/icons/" + name));
    }
}
