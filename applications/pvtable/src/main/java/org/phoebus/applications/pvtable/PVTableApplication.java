package org.phoebus.applications.pvtable;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.ui.PVTable;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.layout.BorderPane;

public class PVTableApplication
{
    public static final Logger logger = Logger.getLogger(PVTableApplication.class.getPackageName());

    public static final String NAME = "PV Table";

    public String getName()
    {
        return NAME;
    }

    public void start(final DockPane dock_pane)
    {
        final PVTableModel model = new PVTableModel();
        model.addItem("loc://x(42)");
        model.addItem("loc://x(42)");
        model.addItem("loc://x(42)");
        model.addItem("loc://x(42)");
        model.addItem("loc://x(42)");
        model.addItem("loc://x(42)");
        model.addItem("sim://sine");
        model.addItem("loc://x(42)");
        model.addItem("loc://x(42)");
        model.addItem("RFQ_LLRF:IOC1:Load");
        model.addItem("MEBT_LLRF:IOC1:Load");
        model.addItem("MEBT_LLRF:IOC3:Load");
        for (int i=1; i<=6; ++i)
            model.addItem(String.format("DTL_LLRF:IOC%d:Load", i));
        for (int i=1; i<=4; ++i)
            model.addItem(String.format("CCL_LLRF:IOC%d:Load", i));
        final PVTable table = new PVTable(model);


        final BorderPane layout = new BorderPane(table);
        final DockItem tab = new DockItem(getName(), layout);
        dock_pane.addTab(tab);

        tab.setOnClosed(event -> stop());
    }

    public void stop()
    {
        logger.log(Level.INFO, "Stopping PV Table...");
        // TODO model.shutdown();
        // System.out.println("Remaining PVs " + PVPool.getPVReferences());
    }
}
