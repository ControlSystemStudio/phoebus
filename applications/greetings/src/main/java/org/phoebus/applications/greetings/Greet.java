package org.phoebus.applications.greetings;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.spi.ToolbarEntry;

@ProviderFor(ToolbarEntry.class)
public class Greet implements ToolbarEntry {

    private static final String NAME = "Greetings";

    public String getName() {
        return GreetingsApp.Name;
    }

    public <T> Callable<T> getActions() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Greeting Dialog");
        alert.setHeaderText("Greetings!");
        alert.setContentText("Welcome to pheobus");
        alert.show();
        return null;
    }

    public List<String> getActionNames() {
        return Arrays.asList(NAME);
    }

}
