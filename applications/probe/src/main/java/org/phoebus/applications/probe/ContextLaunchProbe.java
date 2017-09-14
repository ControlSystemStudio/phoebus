package org.phoebus.applications.probe;

import java.util.Arrays;
import java.util.List;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;

/**
 * A prototype action entry for starting the probe application
 * 
 * @author Kunal Shroff
 *
 */
public class ContextLaunchProbe implements ContextMenuEntry {

    private static final List<Class> supportedTypes = Arrays.asList(ProcessVariable.class);

    @Override
    public String getName() {
        return Probe.NAME;
    }

    @Override
    public Object callWithSelection(Selection selection) {
        List<ProcessVariable> pvs = selection.getSelections();
        LaunchProbe(pvs);
        return null;
    }

    private void LaunchProbe(List<ProcessVariable> pvs) {
        if (pvs.isEmpty()) {
            // Open an empty probe
            new Probe().create();
        } else {
            // Open a probe for each pv
            pvs.forEach(pv -> {
                ProbeInstance probe = new Probe().create();
                probe.setPV(pv.getName());
            });
        }
    }

    @Override
    public Object getIcon() {
        return null;
    }

    @Override
    public List<Class> getSupportedTypes() {
        return supportedTypes;
    }

}
