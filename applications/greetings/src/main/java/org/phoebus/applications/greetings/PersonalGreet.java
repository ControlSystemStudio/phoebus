package org.phoebus.applications.greetings;

import java.util.Optional;
import java.util.concurrent.Callable;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;

import org.phoebus.framework.annotation.ProviderFor;
import org.phoebus.framework.spi.ToolbarEntry;

@ProviderFor(ToolbarEntry.class)
public class PersonalGreet implements ToolbarEntry {

    private static final String NAME = "Personal Greeting";

    public String getName() {
        return NAME;
    }

    @Override
    public Object call() throws Exception {

        TextInputDialog dialog = new TextInputDialog("Phoebus");
        dialog.setTitle("Personal Greeting Dialog");
        dialog.setHeaderText("Personal Greeting Dialog");
        dialog.setContentText("Please enter your name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Greeting Dialog");
            alert.setHeaderText("Greetings!" + name);
            alert.setContentText("Welcome to pheobus");
            alert.show();
        });
        return null;
    }

}
