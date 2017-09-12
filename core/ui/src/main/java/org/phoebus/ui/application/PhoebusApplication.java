package org.phoebus.ui.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.persistence.XMLMementoTree;
import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.MenuEntryService;
import org.phoebus.framework.workbench.MenuEntryService.MenuTreeNode;
import org.phoebus.framework.workbench.ToolbarEntryService;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
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
import javafx.stage.Window;

/** Primary UI for a phoebus application
 *
 *  <p>Menu bar, tool bar, ..
 *
 *  @author Kunal Shroff
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PhoebusApplication extends Application {
    /** Logger for all application messages */
    public static final Logger logger = Logger.getLogger(PhoebusApplication.class.getName());

    private List<org.phoebus.framework.spi.Application> applications;

    @Override
    public void start(Stage stage) throws Exception {

        final MenuBar menuBar = createMenu(stage);
        final ToolBar toolBar = createToolbar();

        final DockItem welcome = new DockItem("Welcome",
                new BorderPane(new Label("Welcome to Phoebus!\n\n" + "Try pushing the buttons in the toolbar")));

        DockStage.configureStage(stage, welcome);
        final BorderPane layout = DockStage.getLayout(stage);

        layout.setTop(new VBox(menuBar, toolBar));
        layout.setBottom(new Label("Status Bar..."));

        stage.show();

        applications = startApplications();

        loadState();

        // Handle requests to open resource from command line
        for (String resource : getParameters().getRaw())
            openResource(resource);

        // In 'server' mode, handle received requests to open resources
        ApplicationServer.setOnReceivedArgument(this::openResource);

        // If user closes all windows, do NOT persist that
        // because next time we'd then open with no windows...
        // --> Clear memento to restart as we did the very first time
        DockStage.setOnFinalCurtain(() ->
        {
            clearState();
            shutdown();
        });
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
        exit.setOnAction(event ->
        {
            saveState();
            shutdown();
        });
        final Menu file = new Menu("File", null, open, exit);
        menuBar.getMenus().add(file);

        // Contributions
        Menu applicationsMenu = new Menu("Applications");
        MenuTreeNode node = MenuEntryService.getInstance().getMenuEntriesTree();

        addMenuNode(applicationsMenu, node);

        menuBar.getMenus().add(applicationsMenu);
        // Help
        final Menu help = new Menu("Help");
        menuBar.getMenus().add(help);

        return menuBar;
    }

    private void addMenuNode(Menu parent, MenuTreeNode node) {

        for (MenuEntry entry : node.getMenuItems()) {
            MenuItem m = new MenuItem(entry.getName());
            m.setOnAction((event) -> {
                try {
                    entry.call();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error invoking menu " + entry.getName(), ex);
                }
            });
            parent.getItems().add(m);
        }

        for (MenuTreeNode child : node.getChildren()) {
            Menu childMenu = new Menu(child.getName());
            addMenuNode(childMenu, child);
            parent.getItems().add(childMenu);
        }
    }

    private ToolBar createToolbar() {
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
                        final Window existing = DockPane.getActiveDockPane().getScene().getWindow();

                        final Stage new_stage = new Stage();
                        DockStage.configureStage(new_stage);
                        entry.call();
                        // Position near but not exactly on top of existing stage
                        new_stage.setX(existing.getX() + 10.0);
                        new_stage.setY(existing.getY() + 10.0);
                        new_stage.show();
                    } else
                        entry.call();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error invoking toolbar " + entry.getName(), ex);
                }
            });

            toolBar.getItems().add(button);
        });
        toolBar.setPrefWidth(600);
        return toolBar;
    }

    /** Locate and start all applications
     *  @return Applications
     */
    private List<org.phoebus.framework.spi.Application> startApplications()
    {
        final List<org.phoebus.framework.spi.Application> apps = new ArrayList<>();
        for (org.phoebus.framework.spi.Application app : ServiceLoader.load(org.phoebus.framework.spi.Application.class))
        {
            app.start();
            apps.add(app);
        }
        return apps;
    }

    /** Find application for a resource
     *  @param resource Resource
     *  @return Application that can open the resource, or <code>null</code>
     */
    private org.phoebus.framework.spi.Application findApplicatation(final String resource)
    {
        for (org.phoebus.framework.spi.Application app : applications)
            if (app.canOpenResource(resource))
                return app;
        return null;
    }

    private void loadState()
    {
        final File memfile = XMLMementoTree.getDefaultFile();
        if (! memfile.canRead())
            return;

        try
        {
            XMLMementoTree memento = XMLMementoTree.read(new FileInputStream(memfile));

            // TODO restore state
            System.out.println("Should restore state from\n" + memento);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error restoring saved state from " + memfile, ex);
        }
    }

    /** @param resource Resource received as command line argument */
    private void openResource(final String resource)
    {
        org.phoebus.framework.spi.Application app = findApplicatation(resource);
        if (app != null)
        {
            logger.log(Level.INFO, "Opening " + resource + " with " + app.getName());
            app.open(resource);
        }
        else
            logger.log(Level.WARNING, "No application found for opening " + resource);
    }

    /** Delete any saved state */
    private void clearState()
    {
        final File memfile = XMLMementoTree.getDefaultFile();
        if (memfile.exists())
            memfile.delete();
    }

    /** Save state */
    private void saveState()
    {
        final File memfile = XMLMementoTree.getDefaultFile();
        try
        {
            final XMLMementoTree memento = XMLMementoTree.create();

            // TODO Persist all DockStages, their DockItems, their optional inputs, ..
            memento.getChild("StageAB312476CF").setNumber("width", 600);

            if (! memfile.getParentFile().exists())
                memfile.getParentFile().mkdirs();
            memento.write(new FileOutputStream(memfile));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error writing saved state to " + memfile, ex);
        }
    }

    /** Stop all applications */
    private void stopApplications()
    {
        for (org.phoebus.framework.spi.Application app : applications)
            app.stop();
    }

    /** Stop applications and exit */
    private void shutdown()
    {
        stopApplications();
        // Hard exit because otherwise background threads
        // might keep us from quitting the VM
        System.exit(0);
    }
}
