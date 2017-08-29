package org.phoebus.applications.pvtable;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.ui.PVTable;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.layout.BorderPane;

public class PVTableApplication
{
    public static final Logger logger = Logger.getLogger(PVTableApplication.class.getPackageName());

    public static final String NAME = "PV Table";

    final PVTableModel model = new PVTableModel();

    public String getName()
    {
        return NAME;
    }

    public void start(final DockPane dock_pane)
    {
        for (int i=1; i<=6; ++i)
        {
            model.addItem("# Local");
            model.addItem("loc://x(42)");
            model.addItem("loc://x(42)");
            model.addItem("# Sim");
            model.addItem("sim://sine");
            model.addItem("sim://ramp");
            model.addItem("#");
        }
        final PVTable table = new PVTable(model);


        final BorderPane layout = new BorderPane(table);
        final DockItem tab = new DockItem(getName(), layout);
        dock_pane.addTab(tab);

        tab.setOnClosed(event -> stop());
    }

    public void stop()
    {
        logger.log(Level.INFO, "Stopping PV Table...");
        model.dispose();
        System.out.println("Remaining PVs " + PVPool.getPVReferences());
    }
}
