package org.phoebus.applications.greetings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.spi.ToolbarEntry;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockStage;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

@SuppressWarnings("rawtypes")
// @ProviderFor(ToolbarEntry.class)
public class Greet implements ToolbarEntry {

    private static final String NAME = "Greetings";
    private TitledPane mainLayout;

    @Override
    public String getName() {
        return GreetingsApp.Name;
    }

    public List<String> getActionNames() {
        return Arrays.asList(NAME);
    }

    @Override
    public void call(final Stage stage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("ui/GreetingView.fxml"));
            mainLayout = loader.load();

            final DockItem tab = new DockItem("Greetings", mainLayout);
            DockStage.getDockPane(stage).addTab(tab);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
