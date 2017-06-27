package org.phoebus.applications.probe;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.phoebus.applications.probe.view.ProbeController;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.spi.ContextMenuEntry;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

@ProviderFor(ContextMenuEntry.class)
public class ContextLaunchProbe implements ContextMenuEntry {

    private static final String NAME = "Probe";
    private static final List<Class> supportedTypes = Arrays.asList(ProcessVariable.class);

    public String getName() {
        return NAME;
    }

    @Override
    public Object callWithSelection(Selection selection) {
        List<ProcessVariable> pvs = (List<ProcessVariable>) selection.getSelections();
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
                Stage stage = new Stage();
                Scene scene = new Scene(mainLayout);
                stage.setScene(scene);
                stage.show();
            } else {
                // Open a probe for each pv
                pvs.forEach(pv -> {
                    Stage stage = new Stage();
                    Scene scene = new Scene(mainLayout);
                    ProbeController controller = (ProbeController) loader.getController();
                    controller.setPVName(pv.getName());
                    stage.setScene(scene);
                    stage.show();
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
