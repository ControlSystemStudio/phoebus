package org.phoebus.applications.probe;

import java.io.IOException;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.spi.ToolbarEntry;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

@SuppressWarnings("rawtypes")
@ProviderFor(ToolbarEntry.class)
public class LaunchProbe implements ToolbarEntry {

    private static final String NAME = "Probe";
    private TitledPane mainLayout;

    public String getName() {
        return NAME;
    }

    @Override
    public Object call() throws Exception {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("view/ProbeView.fxml"));
            mainLayout = loader.load();
            Scene scene = new Scene(mainLayout);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
