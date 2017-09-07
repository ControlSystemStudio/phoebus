package org.phoebus.applications.probe;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.phoebus.applications.probe.view.ProbeController;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TitledPane;
/**
 * A prototype action entry for starting the probe application
 * @author Kunal Shroff
 *
 */
public class ContextLaunchProbe implements ContextMenuEntry {

    private static final String NAME = "Probe";
    private static final List<Class> supportedTypes = Arrays.asList(ProcessVariable.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object callWithSelection(Selection selection) {
        List<ProcessVariable> pvs = selection.getSelections();
        LaunchProbe(pvs);
        return null;
    }

    private void LaunchProbe(List<ProcessVariable> pvs) {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("view/ProbeView.fxml"));
            TitledPane mainLayout = loader.load();

            if (pvs.isEmpty()) {
                // Open an empty probe
                DockPane.getActiveDockPane().addTab(new DockItem(NAME, mainLayout));
            } else {
                // Open a probe for each pv
                pvs.forEach(pv -> {
                	DockPane.getActiveDockPane().addTab(new DockItem(NAME, mainLayout));
                    ProbeController controller = (ProbeController) loader.getController();
                    controller.setPVName(pv.getName());
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
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
