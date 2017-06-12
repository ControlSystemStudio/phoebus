package org.phoebus.framework.workbench;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class PhoebusApplication extends Application {


    @Override
    public void start(Stage stage) throws Exception {

        ExecutorService executor = Executors.newScheduledThreadPool(8);
        final ToolBar toolBar = new ToolBar();

        ToolbarEntryService.getInstance().listToolbarEntries().forEach((entry) -> {

            Button button = new Button(entry.getName());
            button.setOnAction((event) -> {
                try {
//                    Future<?> future = executor.submit(entry.getActions());
                    entry.getActions().call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            toolBar.getItems().add(button);

        });
        toolBar.setPrefWidth(800);

        BorderPane layout = new BorderPane();
        layout.setTop(toolBar);

        stage.setScene(new Scene(layout));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
