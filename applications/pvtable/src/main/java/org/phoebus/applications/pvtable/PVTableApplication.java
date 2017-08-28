package org.phoebus.applications.pvtable;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.control.Label;
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
        final Label dummy = new Label("PV Table");


        final BorderPane layout = new BorderPane(dummy);
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
