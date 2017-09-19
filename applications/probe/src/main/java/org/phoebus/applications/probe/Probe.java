package org.phoebus.applications.probe;

import static org.phoebus.framework.util.ResourceParser.createResourceURL;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
/**
 * 
 * @author Kunal Shroff
 *
 */
public class Probe implements AppResourceDescriptor {

    public static final String NAME = "Probe";
    public static final String PVARG = "pv";

    public String getName() {
        return NAME;
    }

    public void start() {
    }

    public void stop() {
    }

    @Override
    public AppInstance create() {
        ProbeInstance probeInstance = new ProbeInstance(this);
        DockPane.getActiveDockPane().addTab(new DockItem(probeInstance, probeInstance.create()));
        return probeInstance;
    }

    @Override
    public AppInstance create(String resource) {
        final AppDescriptor app = ApplicationService.findApplication(Probe.NAME);

        Map<String, List<String>> args = parseQueryArgs(createResourceURL(resource));
        List<String> pvs = args.getOrDefault(PVARG, Collections.emptyList());
        if (pvs.isEmpty()) {
            // Open an empty probe
            app.create();
        } else {
            // Open a probe for each pv
            pvs.forEach(pv -> {
                ProbeInstance probe = (ProbeInstance) app.create();
                probe.setPV(pv);
            });
        }
        return null;
    }

}
