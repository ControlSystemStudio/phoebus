package org.phoebus.applications.probe;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

/**
 * 
 * @author Kunal Shroff
 *
 */
public class Probe implements AppDescriptor {

    public static final String NAME = "Probe";

    public String getName() {
        return NAME;
    }

    public void start() {
    }

    public void stop() {
    }

    @Override
    public ProbeInstance create() {
        ProbeInstance probeInstance = new ProbeInstance(this);
        DockPane.getActiveDockPane().addTab(new DockItem(probeInstance, probeInstance.create()));
        return probeInstance;
    }

}
