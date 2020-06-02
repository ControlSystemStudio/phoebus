package org.phoebus.applications.probe;

import java.util.List;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/**
 * A prototype action entry for starting the probe application
 *
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("nls")
public class ContextLaunchProbe implements ContextMenuEntry {

    private static final Class<?> supportedTypes = ProcessVariable.class;

    @Override
    public String getName() {
        return Probe.DISPLAYNAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(Probe.class, "/icons/probe.png");
    }

    @Override
    public void call(Selection selection) {
        List<ProcessVariable> pvs = selection.getSelections();
        LaunchProbe(pvs);
    }

    private void LaunchProbe(List<ProcessVariable> pvs) {
        if (pvs.isEmpty()) {
            // Open an empty probe
            ApplicationService.createInstance(Probe.NAME);
        } else {
            // Open a probe for each pv
            pvs.forEach(pv -> {
                ProbeInstance probe = ApplicationService.createInstance(Probe.NAME);
                probe.setPV(pv.getName());
            });
        }
    }

    @Override
    public Class<?> getSupportedType() {
        return supportedTypes;
    }

}
