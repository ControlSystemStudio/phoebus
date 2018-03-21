package org.phoebus.applications.probe;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
/**
 *
 * @author Kunal Shroff
 *
 */
public class Probe implements AppResourceDescriptor {

    public static final String NAME = "probe";
    public static final String DISPLAYNAME = "Probe";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return DISPLAYNAME;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public AppInstance create() {
        ProbeInstance probeInstance = new ProbeInstance(this);
        final DockItem tab = new DockItem(probeInstance, probeInstance.create());
        DockPane.getActiveDockPane().addTab(tab);

        PVAutocompleteMenu.INSTANCE.attachField(probeInstance.getPVField());
        tab.addClosedNotification(() ->  PVAutocompleteMenu.INSTANCE.detachField(probeInstance.getPVField())   );
        return probeInstance;
    }

    @Override
    public AppInstance create(URI resource) {

        ProbeInstance probe = null;
        try
        {
            final List<String> pvs = ResourceParser.parsePVs(resource);
            if (pvs.isEmpty()) {
                // Open an empty probe
                probe = (ProbeInstance) create();
            } else {
                // Open a probe for each pv
                for (String pv : pvs)
                {
                    probe = (ProbeInstance) create();
                    probe.setPV(pv);
                }
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot create probe instance", ex);
        }
        return probe;
    }

}
