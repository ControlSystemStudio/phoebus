package org.phoebus.ui.application;

import java.util.concurrent.atomic.AtomicBoolean;

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
import javafx.scene.input.MouseEvent;
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
            final AtomicBoolean open_new = new AtomicBoolean();
            final Button button = new Button(entry.getName());
            // Want to handle button presses with 'Control' in different way,
            // but action event does not carry key modifier information.
            // -> Add separate event filter to remember the 'Control' state.
            button.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
            {
                open_new.set(event.isControlDown());
                // Still allow the button to react by 'arming' it
                button.arm();
            });
            button.setOnAction((event) -> {
                try {
                    // Future<?> future = executor.submit(entry.getActions());

                    if (open_new.get())
                    {   // Invoke with new stage
                        final Stage new_stage = new Stage();
                        DockStage.configureStage(new_stage);
                        entry.call(new_stage);
                    }
                    else
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
