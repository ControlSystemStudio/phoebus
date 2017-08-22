package org.phoebus.applications.greetings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.spi.ToolbarEntry;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

@SuppressWarnings("rawtypes")
// @ProviderFor(ToolbarEntry.class)
public class Greet implements ToolbarEntry {

    private static final String NAME = "Greetings";
    private TitledPane mainLayout;

    public String getName() {
        return GreetingsApp.Name;
    }

    public List<String> getActionNames() {
        return Arrays.asList(NAME);
    }

    @Override
    public Object call() throws Exception {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("ui/GreetingView.fxml"));
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
