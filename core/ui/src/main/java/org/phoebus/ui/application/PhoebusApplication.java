package org.phoebus.ui.application;

import org.phoebus.framework.workbench.MenubarEntryService;
import org.phoebus.framework.workbench.ToolbarEntryService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockStage;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PhoebusApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        final ToolBar toolBar = new ToolBar();
        final MenuBar menuBar = new MenuBar();

        Menu file = new Menu("File");
        menuBar.getMenus().add(file);

        MenubarEntryService.getInstance().listToolbarEntries().forEach((entry) -> {
            Menu m = new Menu(entry.getName());
            m.setOnAction((event) -> {
                try {
                    entry.getActions().call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            menuBar.getMenus().add(m);
        });


        Menu help = new Menu("Help");
        menuBar.getMenus().add(help);

        ToolbarEntryService.getInstance().listToolbarEntries().forEach((entry) -> {

            Button button = new Button(entry.getName());
            button.setOnAction((event) -> {
                try {
                    // Future<?> future = executor.submit(entry.getActions());
                    entry.call(stage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            toolBar.getItems().add(button);

        });
        toolBar.setPrefWidth(600);

        final DockItem welcome = new DockItem("Welcome");

        DockStage.configureStage(stage, welcome);
        final BorderPane layout = DockStage.getLayout(stage);

        layout.setTop(new VBox(menuBar, toolBar));
        layout.setBottom(new Label("Status Bar..."));

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
