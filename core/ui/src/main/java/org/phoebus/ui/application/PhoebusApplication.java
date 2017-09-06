package org.phoebus.ui.application;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.workbench.MenuEntryService;
import org.phoebus.framework.workbench.ToolbarEntryService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockStage;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PhoebusApplication extends Application {
    /** Logger for all application messages */
    public static final Logger logger = Logger.getLogger(PhoebusApplication.class.getName());

    @Override
    public void start(Stage stage) throws Exception {

        final MenuBar menuBar = createMenu(stage);
        final ToolBar toolBar = createToolbar(stage);

        final DockItem welcome = new DockItem("Welcome",
                new BorderPane(new Label("Welcome to Phoebus!\n\n" + "Try pushing the buttons in the toolbar")));

        DockStage.configureStage(stage, welcome);
        final BorderPane layout = DockStage.getLayout(stage);

        layout.setTop(new VBox(menuBar, toolBar));
        layout.setBottom(new Label("Status Bar..."));

        stage.show();
    }

    private MenuBar createMenu(final Stage stage) {
        final MenuBar menuBar = new MenuBar();

        // File
        final MenuItem open = new MenuItem("Open");
        open.setOnAction(event -> {
            final Alert todo = new Alert(AlertType.INFORMATION, "Will eventually open file browser etc.",
                    ButtonType.OK);
            todo.setHeaderText("File/Open");
            todo.showAndWait();
        });
        final MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(event -> {
            stage.close();
        });
        final Menu file = new Menu("File", null, open, exit);
        menuBar.getMenus().add(file);

        // Contributions
        Menu applicationsMenu = new Menu("Applications");
        Map<String, Object> menuMap = new HashMap<String, Object>();
        MenuEntryService.getInstance().listMenuEntries().forEach((entry) -> {

            MenuItem m = new MenuItem(entry.getName());
            m.setOnAction((event) -> {
                try {
                    entry.call();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error invoking menu " + entry.getName(), ex);
                }
            });
            applicationsMenu.getItems().add(m);
            menuBar.getMenus().add(applicationsMenu);
        });

        // Help
        final Menu help = new Menu("Help");
        menuBar.getMenus().add(help);

        return menuBar;
    }

    private ToolBar createToolbar(final Stage stage) {
        final ToolBar toolBar = new ToolBar();

        // Contributed Entries
        ToolbarEntryService.getInstance().listToolbarEntries().forEach((entry) -> {
            final AtomicBoolean open_new = new AtomicBoolean();

            final Button button = new Button(entry.getName());

            // Want to handle button presses with 'Control' in different way,
            // but action event does not carry key modifier information.
            // -> Add separate event filter to remember the 'Control' state.
            button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                open_new.set(event.isControlDown());
                // Still allow the button to react by 'arming' it
                button.arm();
            });

            button.setOnAction((event) -> {
                try {
                    // Future<?> future = executor.submit(entry.getActions());

                    if (open_new.get()) { // Invoke with new stage
                        final Stage new_stage = new Stage();
                        DockStage.configureStage(new_stage);
                        entry.call(new_stage);
                        // Position near but not exactly on top of existing stage
                        new_stage.setX(stage.getX() + 10.0);
                        new_stage.setY(stage.getY() + 10.0);
                        new_stage.show();
                    } else
                        entry.call(stage);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error invoking toolbar " + entry.getName(), ex);
                }
            });

            toolBar.getItems().add(button);
        });
        toolBar.setPrefWidth(600);
        return toolBar;
    }
}
