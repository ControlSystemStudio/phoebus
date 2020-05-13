package org.csstudio.display.builder.representation.javafx;

import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class MacrosTableDemo extends ApplicationWrapper {

    private MacrosTable table;

    @Override
    public void start(Stage stage) throws Exception {
        // TODO Auto-generated method stub

        Macros initial_macros = new Macros();
        initial_macros.add("First", "firstValue");
        initial_macros.add("Second", "22222");
        table = new MacrosTable(initial_macros);
        final Scene scene = new Scene(table.getNode(), 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(MacrosTableDemo.class, args);
    }

}
