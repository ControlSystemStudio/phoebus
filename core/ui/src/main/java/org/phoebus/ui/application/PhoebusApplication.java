package org.phoebus.ui.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.SubJobMonitor;
import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.framework.persistence.XMLMementoTree;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.framework.workbench.MenuEntryService;
import org.phoebus.framework.workbench.MenuEntryService.MenuTreeNode;
import org.phoebus.framework.workbench.ResourceHandlerService;
import org.phoebus.framework.workbench.ToolbarEntryService;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ListPickerDialog;
import org.phoebus.ui.dialog.OpenFileDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockPaneListener;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.help.OpenAbout;
import org.phoebus.ui.help.OpenHelp;
import org.phoebus.ui.internal.MementoHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.monitoring.ResponsivenessMonitor;
import org.phoebus.ui.welcome.Welcome;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Primary UI for a phoebus application
 *
 * <p>
 * Menu bar, tool bar, ..
 *
 * @author Kunal Shroff
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PhoebusApplication extends Application {
    /** Logger for all application messages */
    public static final Logger logger = Logger.getLogger(PhoebusApplication.class.getName());

    /** Memento keys */
    private static final String LAST_OPENED_FILE = "last_opened_file",
                                DEFAULT_APPLICATION = "default_application",
                                SHOW_TABS = "show_tabs";

    /** Menu item for top resources */
    private Menu top_resources_menu;

    /** Menu item to show/hide tabs */
    private CheckMenuItem show_tabs;

    /** Toolbar button for top resources */
    private MenuButton top_resources_button;

    /** Last file used by 'File, Open' menu
     *  (the _directory_ is actually used by the file-open dialog)
     */
    private File last_opened_file = null;

    /** Application last picked when prompted for app to use */
    private String default_application;

    private Stage main_stage;

    private static final WeakReference<DockItemWithInput> NO_ACTIVE_ITEM_WITH_INPUT = new WeakReference<>(null);

    /** Active {@link DockItemWithInput}
     *
     *  <p><code>null</code> when there's no active item with input.
     *  Weak reference so that we don't hold on to a closed tab
     *  (although closing a tab does tend to activate another tab,
     *   so that is an unlikely scenario)
     */
    private WeakReference<DockItemWithInput> active_item_with_input = NO_ACTIVE_ITEM_WITH_INPUT;

    private final DockPaneListener dock_pane_listener = new DockPaneListener()
    {
        @Override
        public void activeDockItemChanged(final DockItem item)
        {
            if (item instanceof DockItemWithInput)
                active_item_with_input = new WeakReference<>((DockItemWithInput) item);
            else
                active_item_with_input = NO_ACTIVE_ITEM_WITH_INPUT;
        }
    };


    /** JavaFX entry point
     *  @param initial_stage Initial Stage created by JavaFX
     */
    @Override
    public void start(final Stage initial_stage) throws Exception {
        // Show splash screen as soon as possible..
        final Splash splash = new Splash(initial_stage);

        // .. then read saved state etc. in background job
        JobManager.schedule("Startup", monitor ->
        {
            final JobMonitor splash_monitor = new SplashJobMonitor(monitor, splash);
            backgroundStartup(splash_monitor, splash);
        });
    }

    /** Perform potentially slow startup task off the UI thread
     *  @param monitor
     *  @param splash
     *  @throws Exception
     */
    private void backgroundStartup(final JobMonitor monitor, final Splash splash) throws Exception
    {
        // Assume there's 100 percent of work do to,
        // not knowing, yet, how many applications to start etc.
        monitor.beginTask("Start Applications", 100);

        Locations.initialize();

        // Check for site-specific settings.ini bundled into distribution
        final File site_settings = new File(Locations.install(), "settings.ini");
        if (site_settings.canRead())
        {
            logger.log(Level.CONFIG, "Loading settings from " + site_settings);
            PropertyPreferenceLoader.load(new FileInputStream(site_settings));
        }
        // Locate registered applications and start them, allocating 30% to that
        startApplications(new SubJobMonitor(monitor, 30));

        // Load saved state (slow file access) off UI thread, allocating 30% to that
        monitor.beginTask("Load saved state");
        final MementoTree memento = loadMemento(new SubJobMonitor(monitor, 30));

        // Back to UI thread
        Platform.runLater(() ->
        {
            try
            {
                // Leaving remaining 40% to the UI startup
                startUI(memento, new SubJobMonitor(monitor, 40));
            }
            catch (Throwable ex)
            {
                logger.log(Level.SEVERE, "Application cannot start up", ex);
            }
            monitor.done();
            splash.close();
        });
    }

    private void startUI(final MementoTree memento, final JobMonitor monitor) throws Exception
    {
        monitor.beginTask("Start UI", 4);

        main_stage = new Stage();
        final MenuBar menuBar = createMenu(main_stage);
        final ToolBar toolBar = createToolbar();
        createTopResourcesMenu();

        DockStage.configureStage(main_stage);
        // Patch ID of main window
        // (in case we ever need to identify the main window)
        main_stage.getProperties().put(DockStage.KEY_ID, DockStage.ID_MAIN);

        final BorderPane layout = DockStage.getLayout(main_stage);
        layout.setTop(new VBox(menuBar, toolBar));
        layout.setBottom(new Label("Status Bar..."));

        // Main stage may still be moved, resized, and restored apps are added.
        // --> Would be nice to _not_ show it, yet.
        // But restoreState will only find ID_MAIN when the window is visible
        // --> Do show it.
        main_stage.show();
        monitor.worked(1);

        // If there's nothing to restore from a previous instance,
        // start with welcome
        monitor.updateTaskName("Restore tabs");
        if (! restoreState(memento))
            new Welcome().create();
        monitor.worked(1);

        // Check command line parameters
        monitor.updateTaskName("Handle command line parameters");
        handleParameters(getParameters().getRaw());
        monitor.worked(1);

        // In 'server' mode, handle parameters received from client instances
        ApplicationServer.setOnReceivedArgument(parameters ->
        {
            // Invoked by received arguments from the OS's file browser,
            // i.e. the file browser is currently in focus.
            // Assert that the phoebus window is visible
            Platform.runLater(() -> main_stage.toFront());
            handleClientParameters(parameters);
        });

        // Closing the primary window is like calling File/Exit.
        // When the primary window is the only open stage, that's OK.
        // If there are other stages still open,
        // closing them all might be unexpected to the user,
        // so prompt for confirmation.
        main_stage.setOnCloseRequest(event -> {
            if (closeMainStage(main_stage))
                stop();
            // Else: At least one tab in one stage didn't want to close
            event.consume();
        });

        DockPane.addListener(dock_pane_listener);
        DockPane.setActiveDockPane(DockStage.getDockPane(main_stage));
        monitor.done();

        // Now that UI has loaded,
        // start the responsiveness check.
        // During startup, it would trigger
        // as the UI loads style sheets etc.
        new ResponsivenessMonitor(2500, 500, TimeUnit.MILLISECONDS);
    }

    /** Handle parameters from clients, logging errors
     *  @param parameters Command-line parameters from client
     */
    private void handleClientParameters(final List<String> parameters)
    {
        try
        {
            handleParameters(parameters);
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot handle client parameters " + parameters, ex);
        }
    }

    /** Handle command line parameters
     *  @param parameters Command-line parameters
     *  @throws Exception on error
     */
    private void handleParameters(final List<String> parameters) throws Exception
    {
        // List of applications to launch as specified via cmd line args
        final List<String> launchApps = new ArrayList<>();

        // List of resources to launch as specified via cmd line args
        final List<URI> launchResources = new ArrayList<>();

        final Iterator<String> parametersIterator = parameters.iterator();
        while (parametersIterator.hasNext()) {
            final String cmd = parametersIterator.next();
            if (cmd.equals("-app")) {
                if (!parametersIterator.hasNext())
                    throw new Exception("Missing -app application name");
                final String app_name = parametersIterator.next();
                launchApps.add(app_name);
            } else if (cmd.equals("-resource")) {
                if (!parametersIterator.hasNext())
                    throw new Exception("Missing -resource resource file name");
                final URI resource = ResourceParser.createResourceURI(parametersIterator.next());
                launchResources.add(resource);
            }
        }

        // May have been invoked from background thread,
        // but application UIs need to open on UI thread
        Platform.runLater(() ->
        {
            // Handle requests to open resource from command line
            for (URI resource : launchResources)
                openResource(resource);

            // Handle requests to open application from command line
            for (String app_name : launchApps)
                launchApp(app_name);
        });
    }

    private MenuBar createMenu(final Stage stage) {
        final MenuBar menuBar = new MenuBar();
        // For Mac OS X, use it's menu bar on top of screen
        if (PlatformInfo.is_mac_os_x)
            menuBar.setUseSystemMenuBar(true);

        // File
        final MenuItem open = new MenuItem(Messages.Open, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        open.setOnAction(event ->
        {
            final File the_file = new OpenFileDialog().promptForFile(stage, Messages.Open, last_opened_file, null);
            if (the_file == null)
                return;
            last_opened_file = the_file;
            openResource(ResourceParser.getURI(the_file));
        });

        top_resources_menu = new Menu(Messages.TopResources, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        top_resources_menu.setDisable(true);

        final MenuItem file_save = new MenuItem(Messages.Save, ImageCache.getImageView(getClass(), "/icons/save_edit.png"));
        file_save.setOnAction(event -> JobManager.schedule(Messages.Save, monitor -> active_item_with_input.get().save(monitor)));

        final MenuItem file_save_as = new MenuItem(Messages.SaveAs, ImageCache.getImageView(getClass(), "/icons/saveas_edit.png"));
        file_save_as.setOnAction(event ->JobManager.schedule(Messages.SaveAs, monitor -> active_item_with_input.get().save_as(monitor)));

        final MenuItem exit = new MenuItem(Messages.Exit);
        exit.setOnAction(event ->
        {
            if (closeMainStage(null))
                stop();
        });

        final Menu file = new Menu(Messages.File, null,
                                   open,
                                   top_resources_menu,
                                   new SeparatorMenuItem(),
                                   file_save,
                                   file_save_as,
                                   new SeparatorMenuItem(),
                                   exit);
        file.setOnShowing(event ->
        {
            final DockItemWithInput input_item = active_item_with_input.get();
            if (input_item == null)
            {
                file_save.setDisable(true);
                file_save_as.setDisable(true);
            }
            else
            {
                file_save.setDisable(! input_item.isDirty());
                file_save_as.setDisable(! input_item.isSaveAsSupported());
            }
        });
        menuBar.getMenus().add(file);


        // Application Contributions
        final Menu applicationsMenu = new Menu(Messages.Applications);
        MenuTreeNode node = MenuEntryService.getInstance().getMenuEntriesTree();
        addMenuNode(applicationsMenu, node);
        menuBar.getMenus().add(applicationsMenu);


        show_tabs = new CheckMenuItem(Messages.AlwaysShowTabs);
        show_tabs.setSelected(DockPane.isAlwaysShowingTabs());
        show_tabs.setOnAction(event ->  DockPane.alwaysShowTabs(show_tabs.isSelected()));
        menuBar.getMenus().add(new Menu(Messages.Window, null, show_tabs));

        // Help
        final MenuEntry content_entry = new OpenHelp();
        final MenuItem content = new MenuItem(content_entry.getName());
        content.setOnAction(event ->
        {
            try
            {
                content_entry.call();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error invoking menu entry", ex);
            }
        });

        final MenuEntry about_entry = new OpenAbout();
        final MenuItem about = new MenuItem(about_entry.getName());
        about.setOnAction(event ->
        {
            try
            {
                about_entry.call();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error invoking menu entry", ex);
            }
        });
        menuBar.getMenus().add(new Menu(Messages.Help, null, about, content));

        return menuBar;
    }

    /** Fill the {@link #top_resources_menu} and {@link #top_resources_button} */
    private void createTopResourcesMenu()
    {
        // Create top resources menu items off UI thread
        JobManager.schedule("Get top resources", monitor->
        {
            final TopResources tops = TopResources.parse(Preferences.top_resources);
            final int N = tops.size();
            if (N <= 0)
                return;
            final MenuItem[] menu_items = new MenuItem[N];
            final MenuItem[] toolbar_items = new MenuItem[N];
            for (int i=0; i<N; ++i)
            {
                final String description = tops.getDescription(i);
                final URI resource = tops.getResource(i);

                menu_items[i] = new MenuItem(description);
                menu_items[i].setOnAction(event -> openResource(resource));

                toolbar_items[i] = new MenuItem(description);
                toolbar_items[i].setOnAction(event -> openResource(resource));

                // Lookup application icon
                final AppResourceDescriptor application = findApplication(resource, false);
                if (application != null)
                {
                    final Image icon = ImageCache.getImage(application.getIconURL());
                    if (icon != null)
                    {
                        menu_items[i].setGraphic(new ImageView(icon));
                        toolbar_items[i].setGraphic(new ImageView(icon));
                    }
                }
            }

            // Back to UI thread to hook into menu
            Platform.runLater(() ->
            {
                top_resources_menu.getItems().setAll(menu_items);
                top_resources_menu.setDisable(false);

                top_resources_button.getItems().setAll(toolbar_items);
                top_resources_button.setDisable(false);

            });
        });
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

        top_resources_button = new MenuButton(null, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        top_resources_button.setTooltip(new Tooltip(Messages.TopResources));
        top_resources_button.setDisable(true);
        toolBar.getItems().add(top_resources_button);

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

    /** @param resource Resource
     *  @param prompt Prompt if there are multiple applications, or use first one?
     *  @return Application for opening resource, or <code>null</code> if none found
     */
    private AppResourceDescriptor findApplication(final URI resource, final boolean prompt)
    {
        // Does resource request a specific application?
        final String app_name = ResourceParser.getAppName(resource);
        if (app_name != null)
        {
            final AppDescriptor app = ApplicationService.findApplication(app_name);
            if (app == null)
            {
                logger.log(Level.WARNING, "Unknown application '" + app_name + "'");
                return null;
            }
            if (app instanceof AppResourceDescriptor)
                return (AppResourceDescriptor) app;
            else
            {
                logger.log(Level.WARNING, "'" + app_name + "' application does not handle resources");
                return null;
            }
        }

        // Check all applications
        final List<AppResourceDescriptor> applications = ResourceHandlerService.getApplications(resource);
        if (applications.isEmpty())
        {
            logger.log(Level.WARNING, "No application found for opening " + resource);
            return null;
        }

        if (applications.size() == 1   ||   (applications.size() > 0  &&  !prompt))
            return applications.get(0);

        // Prompt user which application to use for this resource
        final List<String> options = applications.stream().map(app -> app.getDisplayName()).collect(Collectors.toList());
        final Dialog<String> which = new ListPickerDialog(main_stage.getScene().getRoot(), options, default_application);
        which.setTitle("Open");
        which.setHeaderText("Select application for opening\n" + resource);
        which.setWidth(300);
        which.setHeight(300);
        final Optional<String> result = which.showAndWait();
        if (! result.isPresent())
            return null;
        default_application = result.get();
        return applications.get(options.indexOf(result.get()));
    }

    /**
     * @param resource Resource received as command line argument
     */
    private void openResource(final URI resource)
    {
        final AppResourceDescriptor application = findApplication(resource, true);
        if (application == null)
            return;
        logger.log(Level.INFO, "Opening " + resource + " with " + application.getName());
        application.create(resource);
    }

    /**
     * Launch application
     *
     * @param appName
     *            Application name received as '-app ..' command line argument
     */
    private void launchApp(final String appName) {
        final AppDescriptor app = ApplicationService.findApplication(appName);
        if (app == null)
        {
            logger.log(Level.SEVERE, "Unknown application '" + appName + "'");
            return;
        }
        app.create();
    }

    /** @param monitor
     *  @return Memento for previously persisted state or <code>null</code> */
    private MementoTree loadMemento(final JobMonitor monitor)
    {
        monitor.beginTask("Load persisted state", 1);
        final File memfile = XMLMementoTree.getDefaultFile();
        try
        {
            if (memfile.canRead())
            {
                logger.log(Level.INFO, "Loading state from " + memfile);
                return XMLMementoTree.read(new FileInputStream(memfile));
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Error restoring saved state from " + memfile, ex);
        }
        finally
        {
            monitor.done();
        }
        return null;
    }

    /** Restore stages from memento
     *  @return <code>true</code> if any tab was restored
     */
    private boolean restoreState(final MementoTree memento) {
        boolean any = false;

        if (memento == null)
            return any;

        try {
            // Global settings
            memento.getString(LAST_OPENED_FILE).ifPresent(path -> last_opened_file = new File(path));
            memento.getString(DEFAULT_APPLICATION).ifPresent(app -> default_application = app);
            memento.getBoolean(SHOW_TABS).ifPresent(show ->
            {
                DockPane.alwaysShowTabs(show);
                show_tabs.setSelected(show);
            });

            // Settings for each stage
            for (MementoTree stage_memento : memento.getChildren()) {
                final String id = stage_memento.getName();
                Stage stage = DockStage.getDockStageByID(id);
                if (stage == null) {
                    // Create new Stage with that ID
                    stage = new Stage();
                    DockStage.configureStage(stage);
                    stage.getProperties().put(DockStage.KEY_ID, id);
                    stage.show();
                }

                any |= MementoHelper.restoreStage(stage_memento, stage);
            }
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Error restoring saved state", ex);
        }
        return any;
    }

    /** Save state of all stages to memento */
    private void saveState() {
        final File memfile = XMLMementoTree.getDefaultFile();
        logger.log(Level.INFO, "Persisting state to " + memfile);
        try {
            final XMLMementoTree memento = XMLMementoTree.create();

            // Persist global settings
            if (last_opened_file != null)
                memento.setString(LAST_OPENED_FILE, last_opened_file.toString());
            if (default_application != null)
                memento.setString(DEFAULT_APPLICATION, default_application);
            memento.setBoolean(SHOW_TABS, DockPane.isAlwaysShowingTabs());

            // Persist each stage (window) and its tabs
            for (Stage stage : DockStage.getDockStages())
                MementoHelper.saveStage(memento, stage);

            // Write the memento file
            if (!memfile.getParentFile().exists())
                memfile.getParentFile().mkdirs();
            memento.write(new FileOutputStream(memfile));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error writing saved state to " + memfile, ex);
        }
    }

    /**
     * Close the main stage
     *
     * <p>
     * If there are more stages open, warn user that they will be closed.
     *
     * <p>
     * When called from the onCloseRequested handler of the primary stage, we must
     * _not_ send another close request to it because that would create an infinite
     * loop.
     *
     * @param main_stage_already_closing
     *            Primary stage when called from its onCloseRequested handler, else
     *            <code>null</code>
     * @return
     */
    private boolean closeMainStage(final Stage main_stage_already_closing) {
        final List<Stage> stages = DockStage.getDockStages();

        if (stages.size() > 1) {
            final Alert dialog = new Alert(AlertType.CONFIRMATION);
            dialog.setTitle("Exit Phoebus");
            dialog.setHeaderText("Close main window");
            dialog.setContentText("Closing this window exits the application,\nclosing all other windows.\n");
            DialogHelper.positionDialog(dialog, stages.get(0).getScene().getRoot(), -200, -200);
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return false;
        }

        // If called from the main stage that's already about to close,
        // skip that one when closing all stages
        if (main_stage_already_closing != null)
            stages.remove(main_stage_already_closing);

        if (!closeStages(stages))
            return false;

        // Once all other stages are closed,
        // potentially check the main stage.
        if (main_stage_already_closing != null && !DockStage.isStageOkToClose(main_stage_already_closing))
            return false;
        return true;
    }

    /**
     * Close several stages
     *
     * @param stages_to_check
     *            Stages that will be asked to close
     * @return <code>true</code> if all stages closed, <code>false</code> if one
     *         stage didn't want to close.
     */
    private boolean closeStages(final List<Stage> stages_to_check) {
        // Save current state, _before_ tabs are closed and thus
        // there's nothing left to save
        saveState();

        for (Stage stage : stages_to_check) {
            // Could close via event, but then still need to check if the stage remained
            // open
            // stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            if (DockStage.isStageOkToClose(stage))
                stage.close();
            else
                return false;
        }
        return true;
    }

    /**
     * Start all applications
     * @param monitor
     */
    private void startApplications(final JobMonitor monitor)
    {
        final Collection<AppDescriptor> apps = ApplicationService.getApplications();
        monitor.beginTask("Start applications", apps.size());
        for (AppDescriptor app : apps)
        {
            monitor.updateTaskName("Starting " + app.getDisplayName());
            app.start();
            monitor.worked(1);

            // TODO Remove dummy delay
            try { Thread.sleep(100); } catch (InterruptedException ex) {}
        }
    }

    /**
     * Stop all applications
     */
    private void stopApplications() {
        for (AppDescriptor app : ApplicationService.getApplications())
            app.stop();
    }

    @Override
    public void stop() {
        stopApplications();

        // Hard exit because otherwise background threads
        // might keep us from quitting the VM
        logger.log(Level.INFO, "Exiting");
        System.exit(0);
    }
}
