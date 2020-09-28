package org.phoebus.ui.application;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.application.MenuEntryService.MenuTreeNode;
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
import org.phoebus.ui.javafx.FullScreenAction;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.monitoring.ResponsivenessMonitor;
import org.phoebus.ui.spi.MenuEntry;
import org.phoebus.ui.statusbar.StatusBar;
import org.phoebus.ui.welcome.Welcome;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
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
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class PhoebusApplication extends Application {
    /** Logger for all application messages */
    public static final Logger logger = Logger.getLogger(PhoebusApplication.class.getName());

    /** JavaFx {@link Application}
     *
     *  <p>Set on {@link #start()},
     *  may be used to for example get HostServices
     */
    public static Application INSTANCE;

    /** Memento keys */
    public static final String LAST_OPENED_FILE = "last_opened_file",
                               DEFAULT_APPLICATION = "default_application",
                               SHOW_TABS = "show_tabs",
                               SHOW_MENU = "show_menu",
                               SHOW_TOOLBAR = "show_toolbar",
                               SHOW_STATUSBAR = "show_statusbar";

    /** Menu item for top resources */
    private Menu top_resources_menu;

    /** Menu item to show/hide tabs */
    private CheckMenuItem show_tabs;

    /** Menu bar, may be hidden via memento */
    private MenuBar menuBar;

    /** Tool bar, may be hidden */
    private ToolBar toolbar;

    /** Menu item to show/hide toolbar */
    private CheckMenuItem show_toolbar;

    /** Menu item to show/hide status bar */
    private CheckMenuItem show_statusbar;

    /** Menu item to save layout */
    private SaveLayoutMenuItem save_layout;

    /** Menu item to delete layouts */
    private DeleteLayoutsMenuItem delete_layouts;

    /** Menu to load past layouts */
    private final Menu load_layout = new Menu(Messages.LoadLayout, ImageCache.getImageView(ImageCache.class, "/icons/layouts.png"));

    /** List of memento names
     *
     *  <p>This list contains the basic layout name,
     *  without the ".memento" suffix and without the 'user'
     *  location path.
     */
    private final List<String> memento_files = new CopyOnWriteArrayList<>();

    /** Toolbar button for top resources */
    private MenuButton top_resources_button;

    /** Toolbar button for home layout */
    private Button home_display_button;

    /** Toolbar button for past layouts */
    private MenuButton layout_menu_button;

    /** Last file used by 'File, Open' menu
     *  (the _directory_ is actually used by the file-open dialog)
     */
    private File last_opened_file = null;

    /** Application last picked when prompted for app to use */
    private String default_application;

    /** 'Main' stage which holds menu bar.
     *  <p>Closing this one exits the application.
     */
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
            {
                logger.log(Level.INFO, "Activated " + item);
                active_item_with_input = new WeakReference<>((DockItemWithInput) item);
            }
            else
            {
                logger.log(Level.INFO, "Activated " + item + ", no input");
                active_item_with_input = NO_ACTIVE_ITEM_WITH_INPUT;
            }
        }
    };

    private ResponsivenessMonitor freezeup_check;


    /** JavaFX entry point
     *  @param initial_stage Initial Stage created by JavaFX
     */
    @Override
    public void start(final Stage initial_stage) throws Exception {
        INSTANCE = this;

        // Show splash screen as soon as possible..
        final Splash splash = Preferences.splash ? new Splash(initial_stage) : null;

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
        monitor.beginTask(Messages.MonitorTaskApps, 100);

        // Locate registered applications and start them, allocating 30% to that
        startApplications(new SubJobMonitor(monitor, 30));

        // Load saved state (slow file access) off UI thread, allocating 30% to that
        monitor.beginTask(Messages.MonitorTaskSave);
        final MementoTree memento = loadDefaultMemento(new SubJobMonitor(monitor, 30));

        // Trigger initialization of authentication service
        AuthorizationService.init();

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
            if (splash != null)
                splash.close();
        });
    }

    private void startUI(final MementoTree memento, final JobMonitor monitor) throws Exception
    {
        monitor.beginTask(Messages.MonitorTaskUi, 4);

        main_stage = new Stage();
        menuBar = createMenu(main_stage);
        toolbar = createToolbar();
        createTopResourcesMenu();

        DockStage.configureStage(main_stage);
        // Patch ID of main window
        // (in case we ever need to identify the main window)
        main_stage.getProperties().put(DockStage.KEY_ID, DockStage.ID_MAIN);

        // isToolbarVisible() and showToolbar() depend on layout.top -> VBox -> menu, toolbar
        final BorderPane layout = DockStage.getLayout(main_stage);
        layout.setTop(new VBox(menuBar, toolbar));
        layout.setBottom(StatusBar.getInstance());

        // Update items now that methods like isToolbarVisible()
        // can function since the scene has been populated
        show_toolbar.setSelected(isToolbarVisible());
        show_statusbar.setSelected(isStatusbarVisible());

        // Main stage may still be moved, resized, and restored apps are added.
        // --> Would be nice to _not_ show it, yet.
        // But restoreState will only find ID_MAIN when the window is visible
        // --> Do show it.
        main_stage.show();
        monitor.worked(1);

        // If there's nothing to restore from a previous instance,
        // start with welcome
        monitor.updateTaskName(Messages.MonitorTaskTabs);
        if (! restoreState(memento))
            new Welcome().create();
        monitor.worked(1);

        // Check command line parameters
        monitor.updateTaskName(Messages.MonitorTaskCmdl);
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

        // DockStage.configureStage() installs an OnCloseRequest
        // handler that will close the associated stage/window.
        // For the primary window, replace with one that is like calling File/Exit,
        // i.e. it closes _all_ stages and ends the application.
        main_stage.setOnCloseRequest(event ->
        {
            // Prevent closing right now..
            event.consume();
            // .. but schedule preparation to close
            closeMainStage();
        });

        DockPane.addListener(dock_pane_listener);
        DockPane.setActiveDockPane(DockStage.getDockPanes(main_stage).get(0));
        monitor.done();

        // Now that UI has loaded,
        // start the responsiveness check.
        // During startup, it would trigger
        // as the UI loads style sheets etc.
        if (Preferences.ui_monitor_period > 0)
            freezeup_check = new ResponsivenessMonitor(3 * Preferences.ui_monitor_period,
                                                       Preferences.ui_monitor_period, TimeUnit.MILLISECONDS);
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
            else
                logger.log(Level.WARNING, "Ignoring launch parameter '" + cmd + "'");
        }

        // May have been invoked from background thread,
        // but application UIs need to open on UI thread
        Platform.runLater(() ->
        {
            // Handle requests to open resource from command line
            for (URI resource : launchResources)
                openResource(resource, false);

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
        open.setOnAction(event -> fileOpen(stage, false));

        final MenuItem open_with = new MenuItem(Messages.OpenWith, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        open_with.setOnAction(event -> fileOpen(stage, true));

        top_resources_menu = new Menu(Messages.TopResources, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        top_resources_menu.setDisable(true);

        final MenuItem file_save = new MenuItem(Messages.Save, ImageCache.getImageView(getClass(), "/icons/save_edit.png"));
        file_save.setOnAction(event -> JobManager.schedule(Messages.Save, monitor -> active_item_with_input.get().save(monitor)));

        final MenuItem file_save_as = new MenuItem(Messages.SaveAs, ImageCache.getImageView(getClass(), "/icons/saveas_edit.png"));
        file_save_as.setOnAction(event ->JobManager.schedule(Messages.SaveAs, monitor -> active_item_with_input.get().save_as(monitor)));

        final MenuItem exit = new MenuItem(Messages.Exit);
        exit.setOnAction(event -> closeMainStage());

        final Menu file = new Menu(Messages.File, null,
                                   open,
                                   open_with,
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
        final MenuTreeNode node = MenuEntryService.getInstance().getMenuEntriesTree();
        addMenuNode(applicationsMenu, node);
        menuBar.getMenus().add(applicationsMenu);

        // Window
        show_tabs = new CheckMenuItem(Messages.AlwaysShowTabs);
        show_tabs.setSelected(DockPane.isAlwaysShowingTabs());
        show_tabs.setOnAction(event ->  DockPane.alwaysShowTabs(show_tabs.isSelected()));

        show_toolbar = new CheckMenuItem(Messages.ShowToolbar);
        show_toolbar.setOnAction(event -> showToolbar(show_toolbar.isSelected()));

        show_statusbar = new CheckMenuItem(Messages.ShowStatusbar);
        show_statusbar.setOnAction(event -> showStatusbar(show_statusbar.isSelected()));

        save_layout = new SaveLayoutMenuItem(this, memento_files);
        delete_layouts = new DeleteLayoutsMenuItem(this, memento_files);

        final Menu menu = new Menu(Messages.Window, null,
                show_tabs,
                show_toolbar,
                show_statusbar,
                new SeparatorMenuItem(),
                save_layout,
                load_layout,
                delete_layouts,
                new SeparatorMenuItem(),
                /* Full Screen placeholder */
                new FullScreenAction(stage));
        // Update Full screen action when shown to get correct enter/exit FS mode
        menu.setOnShowing(event ->
        {   // Last menu item
            final int full_screen_index = menu.getItems().size()-1;
            final FullScreenAction full_screen = new FullScreenAction(stage);
            if (! AuthorizationService.hasAuthorization("full_screen"))
                full_screen.setDisable(true);
            menu.getItems().set(full_screen_index, full_screen);
        });
        menuBar.getMenus().add(menu);

        // Help
        final MenuItem content = createMenuItem(new OpenHelp());
        final MenuItem about = createMenuItem(new OpenAbout());
        menuBar.getMenus().add(new Menu(Messages.Help, null, about, content));

        return menuBar;
    }

    /** Create the load past layouts menu */
    void createLoadLayoutsMenu()
    {
        // Schedule on background thread. Looking for files so can't be on UI thread.
        JobManager.schedule("Create Load Layouts Menu", (monitor) ->
        {
            // Clear the list of memento files.
            memento_files.clear();

            final List<MenuItem> menuItemList = new ArrayList<>();
            final List<MenuItem> toolbarMenuItemList = new ArrayList<>();

            // Get every file in the default directory.
            final File dir = new File(Locations.user().getAbsolutePath());
            final File[] filesArray = dir.listFiles();
            // For every non default memento file create a menu item for the load layout menu.
            if (filesArray != null)
            {
                // Sort layout files alphabetically.
                Arrays.sort(filesArray, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                for (final File file : filesArray)
                {
                    String filename = file.getName();
                    // Skip "memento", the default. Only list SOME_NAME.memento
                    if (file.isFile() && filename.endsWith(".memento"))
                    {
                        // Remove ".memento"
                        filename = filename.substring(0, filename.length() - 8);

                        // Build the list of memento files.
                        memento_files.add(filename);
                        final MenuItem menuItem = new MenuItem(filename);
                        menuItem.setMnemonicParsing(false);
                        menuItem.setOnAction(event -> startLayoutReplacement(file));
                        // Add the item to the load layout menu.
                        menuItemList.add(menuItem);

                        // Repeat for the same menu in the toolbar. They can't share menu items.
                        final MenuItem toolbarMenuItem = new MenuItem(filename);
                        toolbarMenuItem.setMnemonicParsing(false);
                        toolbarMenuItem.setOnAction(event -> startLayoutReplacement(file));
                        toolbarMenuItemList.add(toolbarMenuItem);
                    }
                }
            }

            // Update the menu with the menu items on the UI thread.
            Platform.runLater(()->
            {
                load_layout.getItems().setAll(menuItemList);
                layout_menu_button.getItems().setAll(toolbarMenuItemList);
                delete_layouts.setDisable(memento_files.isEmpty());
            });
        });
    }

    /** @param entry {@link MenuEntry}
     *  @return {@link MenuItem}
     */
    private MenuItem createMenuItem(final MenuEntry entry)
    {
        final MenuItem item = new MenuItem(entry.getName());
        final Image icon = entry.getIcon();
        if (icon != null)
            item.setGraphic(new ImageView(icon));
        item.setOnAction(event ->
        {
            try
            {
                entry.call();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error invoking menu " + entry.getName(), ex);
            }
        });
        return item;
    }

    /** Open file dialog to open a resource
     *  @param stage Parent stage
     *  @param prompt Prompt for application (if there are multiple options), or use default app?
     */
    private void fileOpen(final Stage stage, final boolean prompt)
    {
        final File the_file = new OpenFileDialog().promptForFile(stage, Messages.Open, last_opened_file, null);
        if (the_file == null)
            return;
        last_opened_file = the_file;
        openResource(ResourceParser.getURI(the_file), prompt);
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
                menu_items[i].setOnAction(event -> openResource(resource, false));

                toolbar_items[i] = new MenuItem(description);
                toolbar_items[i].setOnAction(event -> openResource(resource, false));

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
            MenuItem m = createMenuItem(entry);
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

        ImageView homeIcon = ImageCache.getImageView(ImageCache.class, "/icons/home.png");
        homeIcon.setFitHeight(16.0);
        homeIcon.setFitWidth(16.0);
        home_display_button = new Button(null, homeIcon);
        home_display_button.setTooltip(new Tooltip(Messages.HomeTT));
        toolBar.getItems().add(home_display_button);

        final TopResources homeResource = TopResources.parse(Preferences.home_display);

        home_display_button.setOnAction(event -> openResource(homeResource.getResource(0), false));

        top_resources_button = new MenuButton(null, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        top_resources_button.setTooltip(new Tooltip(Messages.TopResources));
        top_resources_button.setDisable(true);
        toolBar.getItems().add(top_resources_button);

        layout_menu_button = new MenuButton(null, ImageCache.getImageView(getClass(), "/icons/layouts.png"));
        layout_menu_button.setTooltip(new Tooltip(Messages.LayoutTT));
        toolBar.getItems().add(layout_menu_button);

        // Contributed Entries
        ToolbarEntryService.getInstance().listToolbarEntries().forEach((entry) -> {
            final AtomicBoolean open_new = new AtomicBoolean();

            // If entry has icon, use that with name as tool tip.
            // Otherwise use the label as button text.
            final Button button = new Button();
            final Image icon = entry.getIcon();
            if (icon == null)
                button.setText(entry.getName());
            else
            {
                button.setGraphic(new ImageView(icon));
                button.setTooltip(new Tooltip(entry.getName()));
            }

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

    /** @return <code>true</code> if menu is visible */
    boolean isMenuVisible()
    {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        return top.getChildren().contains(menuBar);
    }

    private void showMenu(final boolean show)
    {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        if (show)
        {
            if (! top.getChildren().contains(menuBar))
                top.getChildren().add(0, menuBar);
        }
        else
            top.getChildren().remove(menuBar);
    }

    /** @return <code>true</code> if toolbar is visible */
    boolean isToolbarVisible()
    {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        return top.getChildren().contains(toolbar);
    }

    /** @return <code>true</code> if status bar is visible */
    boolean isStatusbarVisible()
    {
        final BorderPane layout = DockStage.getLayout(main_stage);
        return layout.getBottom() == StatusBar.getInstance();
    }

    private void showToolbar(final boolean show)
    {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        if (show)
        {
            if (! top.getChildren().contains(toolbar))
            {
                // Reload layouts menu on showing.
                createLoadLayoutsMenu();
                top.getChildren().add(toolbar);
            }
        }
        else
            top.getChildren().remove(toolbar);
    }

    private void showStatusbar(final boolean show)
    {
        final BorderPane layout = DockStage.getLayout(main_stage);
        if (show)
            layout.setBottom(StatusBar.getInstance());
        else
            layout.setBottom(null);
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
        final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
        if (applications.isEmpty())
        {
            logger.log(Level.WARNING, "No application found for opening " + resource);
            return null;
        }

        // Only one app?
        if (applications.size() == 1)
            return applications.get(0);

        // Pick default application based on preference setting?
        if (! prompt)
        {
            for (AppResourceDescriptor app : applications)
                for (String part : Preferences.default_apps)
                    if (app.getName().contains(part))
                        return app;
            // , not just the first one, which may be undefined
            logger.log(Level.WARNING, "No default application found for opening " + resource + ", using first one");
            return applications.get(0);
        }

        // Prompt user which application to use for this resource
        final List<String> options = applications.stream().map(app -> app.getDisplayName()).collect(Collectors.toList());
        final Dialog<String> which = new ListPickerDialog(main_stage.getScene().getRoot(), options, default_application);
        which.setTitle(Messages.OpenTitle);
        which.setHeaderText(Messages.OpenHdr + resource);
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
     *  @param prompt Prompt if there are multiple applications, or use first one?
     */
    private void openResource(final URI resource, final boolean prompt)
    {
        final AppResourceDescriptor application = findApplication(resource, prompt);
        if (application == null)
            return;

        final String query = resource.getQuery();

        if (query != null)
        {
            // Query could contain _anything_, to be used by the application.
            // Perform a simplistic search for "target=window" or "target=pane_name".
            final int i = query.indexOf("target=");
            if (i >= 0)
            {
                int end = query.indexOf('&', i+7);
                if (end < 0)
                    end = query.length();
                final String target = query.substring(i+7, end);
                if (target.equals("window"))
                {
                    // Open new Stage in which this app will be opened
                    final Stage new_stage = new Stage();
                    DockStage.configureStage(new_stage);
                    new_stage.show();
                }
                else
                {
                    // Should the new panel open in a specific, named pane?
                    final DockPane existing = DockStage.getDockPaneByName(target);
                    if (existing != null)
                        DockPane.setActiveDockPane(existing);
                    else
                    {
                        // Open new Stage with pane for that name
                        final Stage new_stage = new Stage();
                        DockStage.configureStage(new_stage);
                        new_stage.show();
                        DockPane.getActiveDockPane().setName(target);
                    }
                }
            }
        }

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
        ApplicationService.createInstance(appName);
    }

    /** Initiate replacing current layout with a different one
     *  @param memento_file Memento for the desired layout
     */
    private void startLayoutReplacement(final File memento_file)
    {
        JobManager.schedule(memento_file.getName(), monitor ->
        {
            // Load memento in background
            logger.log(Level.INFO, "Loading state from " + memento_file);
            final MementoTree memento = loadMemento(memento_file);

            // On success, switch layout on UI thread
            Platform.runLater(() -> replaceLayout(memento));
        });
    }

    /** @param memento Memento for new layout that should replace current one */
    private void replaceLayout(final MementoTree memento)
    {
        final List<Stage> stages = DockStage.getDockStages();

        // To switch layout, 'fixed' panes must be cleared
        for (Stage stage : stages)
            DockStage.clearFixedPanes(stage);

        JobManager.schedule("Close all stages", monitor->
        {
            for (Stage stage : stages)
                if (! DockStage.prepareToCloseItems(stage))
                    return;

            // All stages OK to close
            Platform.runLater(() ->
            {
                for (Stage stage : stages)
                {
                    DockStage.closeItems(stage);
                    // Don't wait for Platform.runLater-based tab handlers
                    // that will merge splits and eventually close the empty panes,
                    // but close all non-main stages right away
                    if (stage != main_stage)
                        stage.close();
                }

                // Go into the main stage and close all of the tabs. If any of them refuse, return.
                final Node node = DockStage.getPaneOrSplit(main_stage);
                if (! MementoHelper.closePaneOrSplit(node))
                    return;

                // Allow handlers for tab changes etc. to run as everything closed.
                // On next UI tick, load content from memento file.
                Platform.runLater(() -> restoreState(memento));
            });
        });
    }

    /** @param monitor {@link JobMonitor}
     *  @return Memento for previously persisted state or <code>null</code> if none found
     */
    private MementoTree loadDefaultMemento(final JobMonitor monitor)
    {
        monitor.beginTask(Messages.MonitorTaskPers, 1);
        final File memfile = XMLMementoTree.getDefaultFile();
        try
        {
            if (memfile.canRead())
            {
                logger.log(Level.INFO, "Loading state from " + memfile);
                return loadMemento(memfile);
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

    /** Load the contents of an XML memento file into a MementoTree.
     *  @param memfile Memento file.
     *  @return {@link MementoTree}
     *  @throws Exception on error
     */
    private MementoTree loadMemento(final File memfile) throws Exception
    {
        return XMLMementoTree.read(new FileInputStream(memfile));
    }

    /** Restore stages from memento
     *  @return <code>true</code> if any tab was restored
     */
    private boolean restoreState(final MementoTree memento)
    {
        boolean any = false;

        if (memento == null)
            return any;

        // There should be just one, empty stage
        final List<Stage> stages = DockStage.getDockStages();
        if (stages.size() > 1  ||
            stages.stream().map(DockStage::getDockPanes).count() > 1)
        {
            // More than one stage, or a stage with more than one pane
            logger.log(Level.WARNING, "Expected single, empty stage for restoring state", new Exception("Stack Trace"));
            final StringBuilder buf = new StringBuilder();
            buf.append("Found:\n");
            for (Stage stage : stages)
                DockStage.dump(buf, stage);
            logger.log(Level.WARNING, buf.toString());
        }

        createLoadLayoutsMenu();

        try
        {
            // Global settings
            memento.getString(LAST_OPENED_FILE).ifPresent(path -> last_opened_file = new File(path));
            memento.getString(DEFAULT_APPLICATION).ifPresent(app -> default_application = app);
            memento.getBoolean(SHOW_TABS).ifPresent(show ->
            {
                DockPane.alwaysShowTabs(show);
                show_tabs.setSelected(show);
            });
            memento.getBoolean(SHOW_MENU).ifPresent(this::showMenu);
            memento.getBoolean(SHOW_TOOLBAR).ifPresent(show ->
            {
                showToolbar(show);
                show_toolbar.setSelected(show);
            });
            memento.getBoolean(SHOW_STATUSBAR).ifPresent(show ->
            {
                showStatusbar(show);
                show_statusbar.setSelected(show);
            });

            // Settings for each stage
            for (MementoTree stage_memento : memento.getChildren())
            {
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
        }
        catch (Throwable ex) {
            logger.log(Level.WARNING, "Error restoring saved state", ex);
        }
        return any;
    }

    /** Close the main stage
     *
     *  <p>If there are more stages open, warn user that they will be closed.
     *
     *  <p>Then save memento, close _all_ stages and stop application.
     */
    private void closeMainStage()
    {
        final List<Stage> stages = DockStage.getDockStages();

        // If there are other stages still open,
        // closing them all might be unexpected to the user,
        // so prompt for confirmation.
        if (stages.size() > 1)
        {
            final Alert dialog = new Alert(AlertType.CONFIRMATION);
            dialog.setTitle(Messages.ExitTitle);
            dialog.setHeaderText(Messages.ExitHdr);
            dialog.setContentText(Messages.ExitContent);
            DialogHelper.positionDialog(dialog, stages.get(0).getScene().getRoot(), -200, -200);
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;
        }

        // Save current state, _before_ tabs are closed and thus
        // there's nothing left to save
        final File memfile = XMLMementoTree.getDefaultFile();
        MementoHelper.saveState(memfile, last_opened_file, default_application, isMenuVisible(), isToolbarVisible(), isStatusbarVisible());

        // TODO Necessary to close main_stage last?
        if (stages.contains(main_stage))
        {
            stages.remove(main_stage);
            stages.add(main_stage);
        }

        JobManager.schedule("Close all stages", monitor->
        {
            // closeStages()
            for (Stage stage : stages)
                if (! DockStage.prepareToCloseItems(stage))
                    return;

            // All stages OK to close
            Platform.runLater(() ->
            {
                for (Stage stage : stages)
                    DockStage.closeItems(stage);

                stop();
            });
        });
    }

    /**
     * Start all applications
     * @param monitor
     */
    private void startApplications(final JobMonitor monitor)
    {
        final Collection<AppDescriptor> apps = ApplicationService.getApplications();
        monitor.beginTask(Messages.MonitorTaskApps, apps.size());
        for (AppDescriptor app : apps)
        {
            monitor.updateTaskName(Messages.MonitorTaskStarting + app.getDisplayName());
            try
            {
                app.start();
            }
            catch (Throwable ex)
            {
                logger.log(Level.SEVERE, app.getDisplayName() + " startup failed", ex);
            }
            monitor.worked(1);
        }
    }

    /**
     * Stop all applications
     */
    private void stopApplications()
    {
        for (AppDescriptor app : ApplicationService.getApplications())
            app.stop();
    }

    @Override
    public void stop()
    {
        stopApplications();

        if (freezeup_check != null)
            freezeup_check.close();

        logger.log(Level.INFO, "Exiting");
        // Hard exit because otherwise background threads
        // might keep us from quitting the VM
        Platform.exit();
        System.exit(0);
    }
}
