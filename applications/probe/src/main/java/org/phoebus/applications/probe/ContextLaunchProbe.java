package org.phoebus.applications.probe;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.phoebus.applications.probe.view.ProbeController;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockStage;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

//@ProviderFor(ContextMenuEntry.class)
public class ContextLaunchProbe implements ContextMenuEntry {

    private static final String NAME = "Probe";
    private static final List<Class> supportedTypes = Arrays.asList(ProcessVariable.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object callWithSelection(final Stage parent_stage, Selection selection) {
        List<ProcessVariable> pvs = selection.getSelections();
        LaunchProbe(parent_stage, pvs);
        return null;
    }

    private void LaunchProbe(final Stage stage, List<ProcessVariable> pvs) {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("view/ProbeView.fxml"));
            TitledPane mainLayout = loader.load();

            if (pvs.isEmpty()) {
                // Open an empty probe
                DockStage.getDockPane(stage).addTab(new DockItem(NAME, mainLayout));
            } else {
                // Open a probe for each pv
                pvs.forEach(pv -> {
                    DockStage.getDockPane(stage).addTab(new DockItem(NAME, mainLayout));
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
