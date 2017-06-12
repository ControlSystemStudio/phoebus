package org.phoebus.framework.workbench;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.effect.SepiaTone;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PhoebusApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        final ToolBar toolBar = new ToolBar();
        final MenuBar menuBar = new MenuBar();

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
                    entry.getActions().call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            toolBar.getItems().add(button);

        });
        toolBar.setPrefWidth(600);

        BorderPane layout = new BorderPane();
        layout.setTop(toolBar);

        Scene scene = new Scene(new VBox(), 600, 350);
        ((VBox) scene.getRoot()).getChildren().addAll(menuBar);
        ((VBox) scene.getRoot()).getChildren().addAll(toolBar);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
