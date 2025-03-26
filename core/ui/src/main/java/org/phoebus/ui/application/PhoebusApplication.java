package org.phoebus.ui.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.SubJobMonitor;
import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.framework.persistence.XMLMementoTree;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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
    /**
     * Logger for all application messages
     */
    public static final Logger logger = Logger.getLogger(PhoebusApplication.class.getName());

    /**
     * JavaFx {@link Application}
     *
     * <p>Set on {@link #start(Stage)},
     * may be used to for example get HostServices
     */
    public static PhoebusApplication INSTANCE;

    /**
     * Application parameters
     * <p>
     * Copy of original parameters that various code sections might digest.
     */
    private final CopyOnWriteArrayList<String> application_parameters = new CopyOnWriteArrayList<>();

    /**
     * Memento keys
     */
    public static final String LAST_OPENED_FILE = "last_opened_file",
            DEFAULT_APPLICATION = "default_application",
            SHOW_TABS = "show_tabs",
            SHOW_MENU = "show_menu",
            SHOW_TOOLBAR = "show_toolbar",
            SHOW_STATUSBAR = "show_statusbar";

    /**
     * Menu item for top resources
     */
    private Menu top_resources_menu;

    /**
     * Menu item to show/hide tabs
     */
    private CheckMenuItem show_tabs;

    /**
     * Menu bar, may be hidden via memento
     */
    private MenuBar menuBar;

    /**
     * Tool bar, may be hidden
     */
    private ToolBar toolbar;

    /**
     * Menu item to show/hide toolbar
     */
    private CheckMenuItem show_toolbar;

    /**
     * Menu item to show/hide status bar
     */
    private CheckMenuItem show_statusbar;

    /**
     * Menu item to select a tab
     */
    private Menu selectTabMenu = new Menu(Messages.SelectTab);

    /**
     * Menu item to close all tabs in all windows
     */
    private MenuItem closeAllTabsMenuItem = new MenuItem(Messages.CloseAllTabs);

    /**
     * Menu item to save layout
     */
    private MenuItem save_layout;

    /**
     * Menu item to delete layouts
     */
    private DeleteLayoutsMenuItem delete_layouts;

    /**
     * Menu to load past layouts
     */
    private final Menu load_layout = new Menu(Messages.LoadLayout, ImageCache.getImageView(ImageCache.class, "/icons/layouts.png"));

    /**
     * Menu to add a layout to the current layout
     */
    private final Menu add_layout = new Menu(Messages.AddLayout, ImageCache.getImageView(ImageCache.class, "/icons/add_layout.png"));
    
    /**
     * List of memento names
     *
     * <p>This list contains the basic layout name,
     * without the ".memento" suffix and without the 'user'
     * location path.
     */
    public final List<String> memento_files = new CopyOnWriteArrayList<>();

    /**
     * Toolbar button for top resources
     */
    private MenuButton top_resources_button;

    /**
     * Toolbar button for home layout
     */
    private Button home_display_button;

    /**
     * Toolbar button for past layouts
     */
    private MenuButton layout_menu_button;

    /**
     * Toolbar button for adding past layouts
     */
    private MenuButton add_layout_menu_button;

    /**
     * Last file used by 'File, Open' menu
     * (the _directory_ is actually used by the file-open dialog)
     */
    private File last_opened_file = null;

    /** Show the 'Welcome' tab?
     *  Suppressed on -clean, specific -layout,
     *  or when restored state has content.
     */
    private boolean show_welcome = true;

    /**
     * Application last picked when prompted for app to use
     */
    private String default_application;

    /**
     * 'Main' stage which holds menu bar.
     * <p>Closing this one exits the application.
     */
    private Stage main_stage;

    private static final WeakReference<DockItemWithInput> NO_ACTIVE_ITEM_WITH_INPUT = new WeakReference<>(null);

    public static KeyCombination closeAllTabsKeyCombination = PlatformInfo.is_mac_os_x ?
        new KeyCodeCombination(KeyCode.W, KeyCombination.SHIFT_DOWN, KeyCodeCombination.SHORTCUT_DOWN) :
        new KeyCodeCombination(KeyCode.F4, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN);


    /**
     * Active {@link DockItemWithInput}
     *
     * <p><code>null</code> when there's no active item with input.
     * Weak reference so that we don't hold on to a closed tab
     * (although closing a tab does tend to activate another tab,
     * so that is an unlikely scenario)
     */
    private WeakReference<DockItemWithInput> active_item_with_input = NO_ACTIVE_ITEM_WITH_INPUT;

    private final DockPaneListener dock_pane_listener = new DockPaneListener() {
        @Override
        public void activeDockItemChanged(final DockItem item) {
            if (item instanceof DockItemWithInput) {
                logger.log(Level.INFO, "Activated " + item);
                active_item_with_input = new WeakReference<>((DockItemWithInput) item);
            } else {
                logger.log(Level.INFO, "Activated " + item + ", no input");
                active_item_with_input = NO_ACTIVE_ITEM_WITH_INPUT;
            }
        }
    };

    private ResponsivenessMonitor freezeup_check;


    /**
     * JavaFX entry point
     *
     * @param initial_stage Initial Stage created by JavaFX
     */
    @Override
    public void start(final Stage initial_stage) throws Exception {
        INSTANCE = this;

        // Save original application parameters
        application_parameters.addAll(getParameters().getRaw());

        Platform.setImplicitExit(false); // Avoids shutdown of Phoebus when the '-select_settings' option is used after the dialog to select configuration file has been closed. Platform.setImplicitExit(true) is called below to restore the option again.
        possiblySelectIniFile(application_parameters); // possiblySelectIniFile() must be called before preferences are initialized, to ensure that the selected configuration options are applied before old configuration options are loaded.

        // Show splash screen as soon as possible..
        final Splash splash = Preferences.splash ? new Splash(initial_stage) : null;

        // .. then read saved state etc. in background job
        JobManager.schedule("Startup", monitor ->
        {
            final JobMonitor splash_monitor = new SplashJobMonitor(monitor, splash);
            backgroundStartup(splash_monitor, splash);
            Platform.setImplicitExit(true);
        });
    }

    private void possiblySelectIniFile(CopyOnWriteArrayList<String> application_parameters) {

        Consumer<Pair<String, String>> displayErrorMessageAndQuit = errorTitleAndErrorMessage -> {

            String errorTitle = errorTitleAndErrorMessage.getKey();
            String errorMessage = errorTitleAndErrorMessage.getValue();

            logger.log(Level.SEVERE, errorMessage);
            Alert errorDialog = new Alert(AlertType.ERROR);
            errorDialog.setTitle(errorTitle);
            errorDialog.setHeaderText(errorTitle);
            errorDialog.setContentText(errorMessage + "\n\n" + Messages.PhoebusWillQuit);
            errorDialog.showAndWait();

            stop();
        };

        if (application_parameters.contains("-select_settings")) {
            int indexOfFlag = application_parameters.indexOf("-select_settings", 0);
            if (indexOfFlag < 0) {
                throw new RuntimeException("Error, this should never happen!");
            }
            if (application_parameters.size() > indexOfFlag) {
                String iniFilesLocation_String = application_parameters.get(indexOfFlag + 1);
                File iniFilesLocation_File = new File(iniFilesLocation_String);
                if (iniFilesLocation_File.isDirectory()) {
                    List<File> iniFilesInDirectory_List = Arrays.stream(iniFilesLocation_File.listFiles()).filter(file -> file.getAbsolutePath().endsWith(".ini") || file.getAbsolutePath().endsWith(".xml")).collect(Collectors.toList());
                    ObservableList<File> iniFilesInDirectory_ObservableList = FXCollections.observableArrayList(iniFilesInDirectory_List);

                    if (iniFilesInDirectory_List.size() > 0) {
                        Dialog<File> iniFileSelectionDialog = new Dialog<>();
                        iniFileSelectionDialog.setTitle(Messages.SelectPhoebusConfiguration);
                        iniFileSelectionDialog.setHeaderText(Messages.SelectPhoebusConfiguration);
                        iniFileSelectionDialog.setGraphic(null);

                        iniFileSelectionDialog.setWidth(500);
                        iniFileSelectionDialog.setHeight(400);
                        iniFileSelectionDialog.setResizable(false);

                        ListView<File> listView = new ListView<>(iniFilesInDirectory_ObservableList);
                        listView.getSelectionModel().select(0);

                        Runnable setReturnValueAndCloseDialog = () -> {
                            File selectedFile = (File) listView.getSelectionModel().getSelectedItem();
                            if (selectedFile == null) {
                                selectedFile = (File) listView.getItems().get(0);
                            }
                            iniFileSelectionDialog.setResult(selectedFile);
                            iniFileSelectionDialog.close();
                        };
                        listView.setOnMouseClicked(mouseEvent -> {
                            if (mouseEvent.getClickCount() == 2) {
                                setReturnValueAndCloseDialog.run();
                            }
                        });
                        listView.setOnKeyPressed(keyEvent -> {
                            if (keyEvent.getCode() == KeyCode.ENTER) {
                                setReturnValueAndCloseDialog.run();
                            }
                        });

                        iniFileSelectionDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        Button closeButton = (Button) iniFileSelectionDialog.getDialogPane().lookupButton(ButtonType.CLOSE);
                        closeButton.setVisible(false); // In JavaFX, a button of type ButtonType.CLOSE must exist so that the "X"-button closes the window.

                        Button okButton = new Button(Messages.OK);
                        okButton.setOnAction(actionEvent -> setReturnValueAndCloseDialog.run());
                        okButton.setPrefWidth(500);

                        VBox vBox = new VBox(listView, okButton);
                        iniFileSelectionDialog.getDialogPane().setContent(vBox);
                        listView.requestFocus();

                        iniFileSelectionDialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                                iniFileSelectionDialog.close();
                                keyEvent.consume();
                            }
                        });

                        iniFileSelectionDialog.setOnCloseRequest(dialogEvent -> {
                            Object currentResult = iniFileSelectionDialog.getResult();
                            if (currentResult == null || !(currentResult instanceof File)) {
                                // Return null when closing the dialog by clicking the "X"-button or the ESC-key.
                                iniFileSelectionDialog.setResult(null);
                            }
                        });

                        Optional<File> maybeSelectedFile = iniFileSelectionDialog.showAndWait();
                        if (maybeSelectedFile.isPresent()) {
                            File selectedFile = maybeSelectedFile.get();
                            try {
                                FileInputStream selectedFile_FileInputStream = new FileInputStream(selectedFile);
                                try {
                                    if (selectedFile.getAbsolutePath().endsWith(".xml")) {
                                        java.util.prefs.Preferences.importPreferences(selectedFile_FileInputStream);
                                    }
                                    else {
                                        PropertyPreferenceLoader.load(selectedFile_FileInputStream);
                                    }
                                } catch (Exception exception) {
                                    displayErrorMessageAndQuit.accept(new Pair<>(Messages.ErrorLoadingPhoebusConfiguration, Messages.ErrorLoadingPhoebusConfiguration + " '" + selectedFile.getAbsolutePath() + "': " + exception.getMessage()));
                                }
                            } catch (FileNotFoundException e) {
                                displayErrorMessageAndQuit.accept(new Pair<>(Messages.ErrorLoadingPhoebusConfiguration, Messages.ErrorLoadingPhoebusConfiguration + " '" + selectedFile.getAbsolutePath() + "': " + Messages.FileDoesNotExist));
                            }
                        } else {
                            // Selecting a configuration was cancelled either by pressing the "X"-button or by pressing the ESC-key.
                            stop();
                        }
                    } else {
                        displayErrorMessageAndQuit.accept(new Pair<>(Messages.ErrorDuringEvalutationOfTheFlagSelectSettings, Messages.ErrorDuringEvalutationOfTheFlagSelectSettings + ": " + MessageFormat.format(Messages.TheDirectoryDoesNotContainConfigurationFiles, iniFilesLocation_String)));
                    }
                } else {
                    displayErrorMessageAndQuit.accept(new Pair<>(Messages.ErrorDuringEvalutationOfTheFlagSelectSettings, Messages.ErrorDuringEvalutationOfTheFlagSelectSettings + ": " + MessageFormat.format(Messages.TheArgumentIsNotADirectory, iniFilesLocation_String)));
                }
            }
        }
    }

    /**
     * Perform potentially slow startup task off the UI thread
     *
     * @param monitor
     * @param splash
     * @throws Exception
     */
    private void backgroundStartup(final JobMonitor monitor, final Splash splash) throws Exception {
        // Assume there's 100 percent of work do to,
        // not knowing, yet, how many applications to start etc.
        monitor.beginTask(Messages.MonitorTaskApps, 100);

        // Locate registered applications and start them, allocating 30% to that
        startApplications(new SubJobMonitor(monitor, 30));

        // Load saved state (slow file access) off UI thread, allocating 30% to that
        monitor.beginTask(Messages.MonitorTaskSave);
        final MementoTree memento = loadDefaultMemento(application_parameters, new SubJobMonitor(monitor, 30));

        // Trigger initialization of authentication service
        AuthorizationService.init();

        // Back to UI thread
        Platform.runLater(() ->
        {
            try {
                // Leaving remaining 40% to the UI startup
                startUI(memento, new SubJobMonitor(monitor, 40));
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Application cannot start up", ex);
            }
            monitor.done();
            if (splash != null)
                splash.close();
        });
    }

    public ToolBar getToolbar() {
        return toolbar;
    }

    private void startUI(final MementoTree memento, final JobMonitor monitor) throws Exception {
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
        if (application_parameters.contains("-clean") || restoreState(memento))
            show_welcome = false;
        if (show_welcome)
            new Welcome().create();
        monitor.worked(1);

        // Launch background job to list saved layouts
        createLoadLayoutsMenu();

        // Check command line parameters
        monitor.updateTaskName(Messages.MonitorTaskCmdl);
        handleParameters(application_parameters);
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

        closeAllTabsMenuItem.acceleratorProperty().setValue(closeAllTabsKeyCombination);

    }

    /**
     * Handle parameters from clients, logging errors
     *
     * @param parameters Command-line parameters from client
     */
    private void handleClientParameters(final List<String> parameters) {
        try {
            handleParameters(parameters);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Cannot handle client parameters " + parameters, ex);
        }
    }

    /**
     * Handle command line parameters
     *
     * @param parameters Command-line parameters
     * @throws Exception on error
     */
    private void handleParameters(final List<String> parameters) throws Exception {
        if (parameters.contains("-clean"))
        {   // Clean removes everything, including 'Welcome'
            return;
        }
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
            } else
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
        file_save.setOnAction(event -> JobManager.schedule(Messages.Save, monitor -> active_item_with_input.get().save(monitor, active_item_with_input.get().getTabPane().getScene().getWindow())));

        final MenuItem file_save_as = new MenuItem(Messages.SaveAs, ImageCache.getImageView(getClass(), "/icons/saveas_edit.png"));
        file_save_as.setOnAction(event -> JobManager.schedule(Messages.SaveAs, monitor -> active_item_with_input.get().save_as(monitor, active_item_with_input.get().getTabPane().getScene().getWindow())));

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
            if (input_item == null) {
                file_save.setDisable(true);
                file_save_as.setDisable(true);
            } else {
                file_save.setDisable(!input_item.isDirty());
                file_save_as.setDisable(!input_item.isSaveAsSupported());
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
        show_tabs.setOnAction(event -> DockPane.alwaysShowTabs(show_tabs.isSelected()));


        show_toolbar = new CheckMenuItem(Messages.ShowToolbar);
        show_toolbar.setOnAction(event -> showToolbar(show_toolbar.isSelected()));

        show_statusbar = new CheckMenuItem(Messages.ShowStatusbar);
        show_statusbar.setOnAction(event -> showStatusbar(show_statusbar.isSelected()));

        save_layout = new MenuItem(Messages.SaveLayoutAs, ImageCache.getImageView(getClass(), "/icons/new_layout.png"));
        save_layout.setOnAction(event -> SaveLayoutHelper.saveLayout(DockStage.getDockStages(), Messages.SaveLayoutAs));

        delete_layouts = new DeleteLayoutsMenuItem();

        final Menu menu = new Menu(Messages.Window, null,
                show_tabs,
                show_toolbar,
                show_statusbar,
                new SeparatorMenuItem(),
                selectTabMenu,
                closeAllTabsMenuItem,
                new SeparatorMenuItem(),
                save_layout,
                load_layout,
                add_layout,
                delete_layouts,
                new SeparatorMenuItem(),
                /* Full Screen placeholder */
                new FullScreenAction(stage));

        menuBar.getMenus().add(menu);

        // Help
        final MenuItem content = createMenuItem(new OpenHelp());
        final MenuItem about = createMenuItem(new OpenAbout());
        menuBar.getMenus().add(new Menu(Messages.Help, null, about, content));

        selectTabMenu.getParentMenu().setOnShowing(e -> {
            List<MenuItem> menuItems = new ArrayList<>();
            for (Stage s : DockStage.getDockStages()) {
                for (DockPane dockPane : DockStage.getDockPanes(s)) {
                    for (DockItem dockItem : dockPane.getDockItems()) {
                        CheckMenuItem menuItem = new CheckMenuItem(dockItem.getLabel());
                        menuItem.setSelected(dockItem.isSelected());
                        menuItem.setOnAction(ae -> dockItem.select());
                        menuItems.add(menuItem);
                    }
                }
            }
            menuItems.sort(Comparator.comparing(MenuItem::getText));
            selectTabMenu.getItems().clear();
            selectTabMenu.getItems().addAll(menuItems);

            // Update Full screen action when shown to get correct enter/exit FS mode
            final int full_screen_index = menu.getItems().size() - 1;
            final FullScreenAction full_screen = new FullScreenAction(stage);
            if (!AuthorizationService.hasAuthorization("full_screen"))
                full_screen.setDisable(true);
            menu.getItems().set(full_screen_index, full_screen);
        });

        closeAllTabsMenuItem.setOnAction(ae -> closeAllTabs());
        return menuBar;
    }

    private List<String> listOfLayouts = new LinkedList<>();
    protected List<String> getListOfLayouts() {
        return listOfLayouts;
    }

    /**
     * Create the load past layouts menu
     */
    void createLoadLayoutsMenu() {
        // Schedule on background thread. Looking for files so can't be on UI thread.
        JobManager.schedule("Create Load Layouts Menu", (monitor) ->
        {
            // Clear the list of memento files.
            memento_files.clear();

            final List<MenuItem> menuItemList = new ArrayList<>();
            final List<MenuItem> toolbarMenuItemList = new ArrayList<>();
            final List<MenuItem> addLayoutMenuItemList = new ArrayList<>();
            final List<MenuItem> toolbarAddLayoutMenuItemList = new ArrayList<>();

            final Map<String, File> layoutFiles = new HashMap<String, File>();

            // Get every file in the default directory.
            final File dir = new File(Locations.user().getAbsolutePath());
            final File[] userLayoutFiles = dir.listFiles();
            if (userLayoutFiles != null) {
                Arrays.stream(userLayoutFiles).forEach(file -> {
                    layoutFiles.put(file.getName(), file);
                });
            }

            // Get every momento file from the configured layout
            if (Preferences.layout_dir != null && !Preferences.layout_dir.isBlank()) {
                final File layoutDir = new File(Preferences.layout_dir);
                if (layoutDir.exists()) {
                    final File[] systemLayoutFiles = layoutDir.listFiles();
                    if (systemLayoutFiles != null) {
                        Arrays.stream(systemLayoutFiles).forEach(file -> {
                            if (!layoutFiles.containsKey(file.getName()) && file.getName().endsWith(".memento")) {
                                layoutFiles.put(file.getName(), file);
                            }
                        });
                    }
                }
            }


            listOfLayouts = new LinkedList<>();
            // For every non default memento file create a menu item for the load layout menu.
            if (!layoutFiles.keySet().isEmpty()) {
                // Sort layout files alphabetically.
                layoutFiles.keySet().stream().sorted((a, b) -> a.compareToIgnoreCase(b))
                        .forEach(key -> {

                            File file = layoutFiles.get(key);
                            String filename = file.getName();
                            // Skip "memento", the default. Only list SOME_NAME.memento
                            if (file.isFile() && filename.endsWith(".memento")) {
                                // Remove ".memento"
                                filename = filename.substring(0, filename.length() - 8);

                                listOfLayouts.add(filename);

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

                                // Create menu for adding a layout:
                                final MenuItem addLayoutMenuItem = new MenuItem(filename);
                                addLayoutMenuItem.setMnemonicParsing(false);
                                addLayoutMenuItem.setOnAction(event -> startAddingLayout(file));
                                addLayoutMenuItemList.add(addLayoutMenuItem);

                                // Repeat for the same menu in the toolbar. They can't share menu items.
                                final MenuItem toolbarAddLayoutMenuItem = new MenuItem(filename);
                                toolbarAddLayoutMenuItem.setMnemonicParsing(false);
                                toolbarAddLayoutMenuItem.setOnAction(event -> startAddingLayout(file));
                                toolbarAddLayoutMenuItemList.add(toolbarAddLayoutMenuItem);
                            }
                        });
            }

            // Update the menu with the menu items on the UI thread.
            Platform.runLater(() ->
            {
                load_layout.getItems().setAll(menuItemList);
                add_layout.getItems().setAll(addLayoutMenuItemList);
                layout_menu_button.getItems().setAll(toolbarMenuItemList);
                add_layout_menu_button.getItems().setAll(toolbarAddLayoutMenuItemList);
                delete_layouts.setDisable(memento_files.isEmpty());
            });
        });
    }

    /**
     * @param entry {@link MenuEntry}
     * @return {@link MenuItem}
     */
    private MenuItem createMenuItem(final MenuEntry entry) {
        final MenuItem item = new MenuItem(entry.getName());
        final Image icon = entry.getIcon();
        if (icon != null)
            item.setGraphic(new ImageView(icon));
        item.setOnAction(event ->
        {
            try {
                entry.call();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error invoking menu " + entry.getName(), ex);
            }
        });
        return item;
    }

    /**
     * Open file dialog to open a resource
     *
     * @param stage  Parent stage
     * @param prompt Prompt for application (if there are multiple options), or use default app?
     */
    private void fileOpen(final Stage stage, final boolean prompt) {
        final File the_file = new OpenFileDialog().promptForFile(stage, Messages.Open, last_opened_file, null);
        if (the_file == null)
            return;
        last_opened_file = the_file;
        openResource(ResourceParser.getURI(the_file), prompt);
    }

    /**
     * Fill the {@link #top_resources_menu} and {@link #top_resources_button}
     */
    private void createTopResourcesMenu() {
        // Create top resources menu items off UI thread
        JobManager.schedule("Get top resources", monitor ->
        {
            final TopResources tops = TopResources.parse(Preferences.top_resources);
            final int N = tops.size();
            if (N <= 0)
                return;
            final MenuItem[] menu_items = new MenuItem[N];
            final MenuItem[] toolbar_items = new MenuItem[N];
            for (int i = 0; i < N; ++i) {
                final String description = tops.getDescription(i);
                final URI resource = tops.getResource(i);

                menu_items[i] = new MenuItem(description);
                menu_items[i].setOnAction(event -> openResource(resource, false));

                toolbar_items[i] = new MenuItem(description);
                toolbar_items[i].setOnAction(event -> openResource(resource, false));

                // Lookup application icon
                final AppResourceDescriptor application = findApplication(resource, false);
                if (application != null) {
                    final Image icon = ImageCache.getImage(application.getIconURL());
                    if (icon != null) {
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
        if (! Preferences.toolbar_entries.contains("!Home"))
        {
            home_display_button.setTooltip(new Tooltip(Messages.HomeTT));
            toolBar.getItems().add(home_display_button);

            if (!Preferences.home_display.isEmpty()) {
                final TopResources homeResource = TopResources.parse(Preferences.home_display);
                home_display_button.setOnAction(event -> openResource(homeResource.getResource(0), false));
            }
            else {
                Welcome welcome = new Welcome();
                home_display_button.setOnAction(event -> welcome.create());
            }
        }

        top_resources_button = new MenuButton(null, ImageCache.getImageView(getClass(), "/icons/fldr_obj.png"));
        top_resources_button.setTooltip(new Tooltip(Messages.TopResources));
        top_resources_button.setDisable(true);
        if (! Preferences.toolbar_entries.contains("!Top Resources"))
            toolBar.getItems().add(top_resources_button);

        layout_menu_button = new MenuButton(null, ImageCache.getImageView(getClass(), "/icons/layouts.png"));
        layout_menu_button.setTooltip(new Tooltip(Messages.LayoutTT));
        if (! Preferences.toolbar_entries.contains("!Layouts"))
            toolBar.getItems().add(layout_menu_button);

        add_layout_menu_button = new MenuButton(null, ImageCache.getImageView(getClass(), "/icons/add_layout.png"));
        add_layout_menu_button.setTooltip(new Tooltip(Messages.AddLayout));
        if (Preferences.toolbar_entries.contains("Add Layouts") && !Preferences.toolbar_entries.contains("!Add Layouts")) {
            toolBar.getItems().add(add_layout_menu_button);
        }

        // Contributed Entries
        ToolbarEntryService.getInstance().listToolbarEntries().forEach((entry) -> {
            final AtomicBoolean open_new = new AtomicBoolean();

            // If entry has icon, use that with name as tool tip.
            // Otherwise use the label as button text.
            final Button button = new Button();
            final Image icon = entry.getIcon();
            if (icon == null)
                button.setText(entry.getName());
            else {
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

    /**
     * @return <code>true</code> if menu is visible
     */
    boolean isMenuVisible() {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        return top.getChildren().contains(menuBar);
    }

    private void showMenu(final boolean show) {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        if (show) {
            if (!top.getChildren().contains(menuBar))
                top.getChildren().add(0, menuBar);
        } else
            top.getChildren().remove(menuBar);
    }

    /**
     * @return <code>true</code> if toolbar is visible
     */
    boolean isToolbarVisible() {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        return top.getChildren().contains(toolbar);
    }

    /**
     * @return <code>true</code> if status bar is visible
     */
    boolean isStatusbarVisible() {
        final BorderPane layout = DockStage.getLayout(main_stage);
        return layout.getBottom() == StatusBar.getInstance();
    }

    private void showToolbar(final boolean show) {
        final BorderPane layout = DockStage.getLayout(main_stage);
        final VBox top = (VBox) layout.getTop();
        if (show) {
            if (!top.getChildren().contains(toolbar)) {
                // Reload layouts menu on showing.
                createLoadLayoutsMenu();
                top.getChildren().add(toolbar);
            }
        } else
            top.getChildren().remove(toolbar);
    }

    private void showStatusbar(final boolean show) {
        final BorderPane layout = DockStage.getLayout(main_stage);
        if (show)
            layout.setBottom(StatusBar.getInstance());
        else
            layout.setBottom(null);
    }

    /**
     * @param resource Resource
     * @param prompt   Prompt if there are multiple applications, or use first one?
     * @return Application for opening resource, or <code>null</code> if none found
     */
    private AppResourceDescriptor findApplication(final URI resource, final boolean prompt) {
        // Does resource request a specific application?
        final String app_name = ResourceParser.getAppName(resource);
        if (app_name != null) {
            final AppDescriptor app = ApplicationService.findApplication(app_name);
            if (app == null) {
                logger.log(Level.WARNING, "Unknown application '" + app_name + "'");
                return null;
            }
            if (app instanceof AppResourceDescriptor)
                return (AppResourceDescriptor) app;
            else {
                logger.log(Level.WARNING, "'" + app_name + "' application does not handle resources");
                return null;
            }
        }

        // Check all applications
        final List<AppResourceDescriptor> applications = ApplicationService.getApplications(resource);
        if (applications.isEmpty()) {
            logger.log(Level.WARNING, "No application found for opening " + resource);
            return null;
        }

        // Only one app?
        if (applications.size() == 1)
            return applications.get(0);

        // Pick default application based on preference setting?
        if (!prompt) {
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
        if (!result.isPresent())
            return null;
        default_application = result.get();
        return applications.get(options.indexOf(result.get()));
    }

    /**
     * @param resource Resource received as command line argument
     * @param prompt   Prompt if there are multiple applications, or use first one?
     */
    private void openResource(final URI resource, final boolean prompt) {
        final AppResourceDescriptor application = findApplication(resource, prompt);
        if (application == null)
            return;

        final String query = resource.getQuery();

        if (query != null) {
            // Query could contain _anything_, to be used by the application.
            // Perform a simplistic search for "target=window" or "target=pane_name".
            final int i = query.indexOf("target=");
            if (i >= 0) {
                int end = query.indexOf('&', i + 7);
                if (end < 0)
                    end = query.length();
                final String target = query.substring(i + 7, end);
                if (!target.startsWith("window")) {
                    // Should the new panel open in a specific, named pane?
                    final DockPane existing = DockStage.getDockPaneByName(target);
                    if (existing != null)
                        DockPane.setActiveDockPane(existing);
                    else {
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
     * @param appName Application name received as '-app ..' command line argument
     */
    private void launchApp(final String appName) {
        ApplicationService.createInstance(appName);
    }

    /**
     * Initiate replacing current layout with a different one
     *
     * @param memento_file Memento for the desired layout
     */
    private void startLayoutReplacement(final File memento_file) {
        JobManager.schedule(memento_file.getName(), monitor ->
        {
            // Load memento in background
            logger.log(Level.INFO, "Loading state from " + memento_file);
            final MementoTree memento = loadMemento(memento_file);

            // On success, switch layout on UI thread
            Platform.runLater(() -> replaceLayout(memento));
        });
    }

    /**
     * Initiate adding a layout to the current layout
     *
     * @param mementoFile Memento for the desired layout
     */
    private void startAddingLayout(File mementoFile) {
        JobManager.schedule(mementoFile.getName(), monitor ->
        {
            MementoTree mementoTree;
            try {
                mementoTree = loadMemento(mementoFile);
            } catch (FileNotFoundException fileNotFoundException) {
                logger.log(Level.SEVERE, "Unable to add a layout to the existing layout due to an error when opening the file '" + mementoFile.getAbsolutePath() + "'.");
                return;
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Unable to add a layout to the existing layout due to an error when parsing the file '" + mementoFile.getAbsolutePath() + "'.");
                return;
            }
            Platform.runLater(() -> addLayoutToCurrentLayout(mementoTree));
        });
    }

    /**
     * @param memento Memento for new layout that should replace current one
     */
    private void replaceLayout(final MementoTree memento) {
        final List<Stage> stages = DockStage.getDockStages();

        // To switch layout, 'fixed' panes must be cleared
        for (Stage stage : stages)
            DockStage.clearFixedPanes(stage);

        JobManager.schedule("Close all stages", monitor ->
        {
            boolean shouldReplaceLayout = confirmationDialogWhenUnsavedChangesExist(stages,
                                                                                    Messages.UnsavedChanges_wouldYouLikeToSaveAnyChangesBeforeReplacingTheLayout,
                                                                                    Messages.UnsavedChanges_replace,
                                                                                    main_stage,
                                                                                    monitor);

            if (shouldReplaceLayout) {
                for (Stage stage : stages) {
                    if (!DockStage.prepareToCloseItems(stage)) {
                        return;
                    }
                }

                // All stages OK to close
                Platform.runLater(() ->
                {
                    for (Stage stage : stages) {
                        DockStage.closeItems(stage);
                        // Don't wait for Platform.runLater-based tab handlers
                        // that will merge splits and eventually close the empty panes,
                        // but close all non-main stages right away
                        if (stage != main_stage)
                            stage.close();
                    }

                    // Go into the main stage and close all of the tabs. If any of them refuse, return.
                    final Node node = DockStage.getPaneOrSplit(main_stage);
                    if (!MementoHelper.closePaneOrSplit(node))
                        return;

                    // Allow handlers for tab changes etc. to run as everything closed.
                    // On next UI tick, load content from memento file.
                    Platform.runLater(() -> restoreState(memento));
                });
            }
        });
    }

    /**
     * @param parameters Command line parameters that may contain '-layout /path/to/Example.memento'
     * @param monitor    {@link JobMonitor}
     * @return Memento for previously persisted state or <code>null</code> if none found
     */
    private MementoTree loadDefaultMemento(final List<String> parameters, final JobMonitor monitor) {
        monitor.beginTask(Messages.MonitorTaskPers, 1);
        MementoTree memTree = null;
        File memfile = null;
        try {
            for (int i = 0; i < parameters.size(); ++i) {
                if ("-layout".equals(parameters.get(i))) {
                    if (i >= parameters.size() - 1)
                        throw new Exception("Missing /path/to/Example.memento for -layout option");
                    // Restoring a specific layout, even if empty, disables the 'Welcome' tab
                    show_welcome = false;
                    memfile = new File(parameters.get(i + 1));
                    // Remove -layout and path because they have been handled
                    parameters.remove(i + 1);
                    parameters.remove(i);
                    break;
                }
            }
            if(memfile == null) {//if no layout found in argument check preferences
                //Load a custom layout at start if layout_default is defined in preferences
                String layoutFileName = Preferences.layout_default;
                if(layoutFileName != null && !layoutFileName.isBlank()) {
                    //layout is in absolute path and not based on layout_dir
                    layoutFileName = !layoutFileName.endsWith(".memento")? layoutFileName + ".memento" :layoutFileName;
                    memfile = new File(layoutFileName);
                }
            }
            
            if(memfile == null) {// if still null get default one
                memfile = XMLMementoTree.getDefaultFile();
            }
            
            if (memfile.canRead()) {
                logger.log(Level.INFO, "Loading state from " + memfile);
                memTree = loadMemento(memfile);
            } else
                logger.log(Level.WARNING, "Cannot load state from " + memfile + ", no such file");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error restoring saved state from " + memfile, ex);
        } finally {
            monitor.done();
        }
        return memTree;
    }

    /**
     * Load the contents of an XML memento file into a MementoTree.
     *
     * @param memfile Memento file.
     * @return {@link MementoTree}
     * @throws Exception on error
     */
    private MementoTree loadMemento(final File memfile) throws Exception {
        return XMLMementoTree.read(new FileInputStream(memfile));
    }

    /**
     * Adds a layout from a MementoTree to the current layout.
     */
    private void addLayoutToCurrentLayout(MementoTree mementoTree) {

        List<Runnable> restoreSelectedTabFunctions = new LinkedList<>();
        for (Stage stage : DockStage.getDockStages()) {
            for (DockPane pane : DockStage.getDockPanes(stage)) {
                DockItem tab = (DockItem) pane.getSelectionModel().getSelectedItem();
                restoreSelectedTabFunctions.add(() -> tab.select());
            }
        }

        List<Runnable> focusNewlyCreatedStageFunctions = new LinkedList<>();
        for (MementoTree childMementoTree : mementoTree.getChildren()) {
            Stage stage = new Stage();
            DockStage.configureStage(stage);
            MementoHelper.restoreStage(childMementoTree, stage);

            DockStage.deferUntilAllPanesOfStageHaveScenes(stage, () ->
            {
                long numberOfRestoredTabsInStage = DockStage.getDockPanes(stage).stream()
                                                            .flatMap(pane -> pane.getTabs().stream())
                                                            .count();
                if (numberOfRestoredTabsInStage > 0) {
                    focusNewlyCreatedStageFunctions.add(() -> stage.requestFocus());
                } else {
                    stage.close();
                }
                restoreSelectedTabFunctions.forEach(f -> f.run());
                focusNewlyCreatedStageFunctions.forEach(f -> f.run());
            });
        }
    }

    /**
     * Restore stages from memento
     *
     * @return <code>true</code> if any tab was restored
     */
    private boolean restoreState(final MementoTree memento) {
        boolean any = false;

        if (memento == null)
            return any;

        // There should be just one, empty stage
        final List<Stage> stages = DockStage.getDockStages();
        if (stages.size() > 1 ||
                stages.stream().map(DockStage::getDockPanes).count() > 1) {
            // More than one stage, or a stage with more than one pane
            logger.log(Level.WARNING, "Expected single, empty stage for restoring state", new Exception("Stack Trace"));
            final StringBuilder buf = new StringBuilder();
            buf.append("Found:\n");
            for (Stage stage : stages)
                DockStage.dump(buf, stage);
            logger.log(Level.WARNING, buf.toString());
        }

        try {
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

    /**
     * Close the main stage
     *
     * <p>If there are more stages open, warn user that they will be closed.
     *
     * <p>Then save memento, close _all_ stages and stop application.
     */
    private void closeMainStage() {
        final List<Stage> stages = DockStage.getDockStages();

        // If there are other stages still open,
        // closing them all might be unexpected to the user,
        // so prompt for confirmation.
        if (stages.size() > 1) {
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
        MementoHelper.saveState(DockStage.getDockStages(), memfile, last_opened_file, default_application, isMenuVisible(), isToolbarVisible(), isStatusbarVisible());

        // TODO Necessary to close main_stage last?
        if (stages.contains(main_stage)) {
            stages.remove(main_stage);
            stages.add(main_stage);
        }

        JobManager.schedule("Close all stages", monitor ->
        {
            boolean shouldExit = confirmationDialogWhenUnsavedChangesExist(stages,
                                                                           Messages.UnsavedChanges_wouldYouLikeToSaveAnyChangesBeforeExiting,
                                                                           Messages.UnsavedChanges_exit,
                                                                           main_stage,
                                                                           monitor);

            if (shouldExit) {
                for (Stage stage : stages)
                    if (!DockStage.prepareToCloseItems(stage)) {
                        return;
                    }

                exitPhoebus();
            }
        });
    }

    private static SortedMap<String, List<DockItemWithInput>> listOfDockItems2ApplicationNameToDockItemsWithInput(List<DockItem> dockItems) {
        SortedMap<String, List<DockItemWithInput>> applicationNameToDockItemsWithInput = new TreeMap<>();
        for (DockItem dockItem : dockItems) {
            if (dockItem instanceof DockItemWithInput) {
                DockItemWithInput dockItemWithInput = (DockItemWithInput) dockItem;
                if (dockItemWithInput.isDirty()) {
                    String applicationName = dockItemWithInput.getApplication().getAppDescriptor().getDisplayName();
                    if (!applicationNameToDockItemsWithInput.containsKey(applicationName)) {
                        applicationNameToDockItemsWithInput.put(applicationName, new LinkedList<>());
                    }

                    applicationNameToDockItemsWithInput.get(applicationName).add(dockItemWithInput);
                }
            }
        }
        return applicationNameToDockItemsWithInput;
    }

    private static SortedMap<String, SortedMap<String, List<DockItemWithInput>>> stages2WindowNameToApplicationNameToDockItemsWithInput(List<Stage> stages) {
        SortedMap<String, SortedMap<String, List<DockItemWithInput>>> windowNameToApplicationNameToDockItemsWithInput = new TreeMap<>();
        {
            int currentWindowNr = 1;

            for (Stage stage : stages) {
                String currentWindowName;
                if (stage == DockStage.getDockStages().get(0)) {
                    currentWindowName = Messages.UnsavedChanges_mainWindow;
                }
                else {
                    currentWindowName = Messages.UnsavedChanges_secondaryWindow + " " + currentWindowNr;
                    currentWindowNr++;
                }

                List<DockItem> dockItems = DockStage.getDockPanes(stage).stream().flatMap(dockPane -> dockPane.getDockItems().stream()).collect(Collectors.toList());
                SortedMap<String, List<DockItemWithInput>> applicationNameToDockItemsWithInput = PhoebusApplication.listOfDockItems2ApplicationNameToDockItemsWithInput(dockItems);
                windowNameToApplicationNameToDockItemsWithInput.put(currentWindowName, applicationNameToDockItemsWithInput);
            }
        }
        return windowNameToApplicationNameToDockItemsWithInput;
    }

    public static boolean confirmationDialogWhenUnsavedChangesExist(Stage stage,
                                                                    String question,
                                                                    String closeActionName,
                                                                    JobMonitor monitor) throws ExecutionException, InterruptedException {
        List<DockItem> dockItems = DockStage.getDockPanes(stage).stream().flatMap(dockPane -> dockPane.getDockItems().stream()).collect(Collectors.toList());
        SortedMap<String, List<DockItemWithInput>> applicationNameToDockItemsWithInput = PhoebusApplication.listOfDockItems2ApplicationNameToDockItemsWithInput(dockItems);
        SortedMap<String, SortedMap<String, List<DockItemWithInput>>> windowNameToApplicationNameToDockItemsWithInput = new TreeMap<>();
        windowNameToApplicationNameToDockItemsWithInput.put("Window", applicationNameToDockItemsWithInput);

        return confirmationDialogWhenUnsavedChangesExist(windowNameToApplicationNameToDockItemsWithInput,
                                                         question,
                                                         closeActionName,
                                                         stage,
                                                         monitor);
    }

    public static boolean confirmationDialogWhenUnsavedChangesExist(ArrayList<DockItem> dockItems, // "ArrayList<DockItem>" is used instead of "List<DockItem>" to prevent a conflict with "List<DockItem>" after type erasure.
                                                                    String question,
                                                                    String closeActionName,
                                                                    Stage stage,
                                                                    JobMonitor monitor) throws ExecutionException, InterruptedException {
        SortedMap<String, List<DockItemWithInput>> applicationNameToDockItemsWithInput = PhoebusApplication.listOfDockItems2ApplicationNameToDockItemsWithInput(dockItems);
        SortedMap<String, SortedMap<String, List<DockItemWithInput>>> windowNameToApplicationNameToDockItemsWithInput = new TreeMap<>();
        windowNameToApplicationNameToDockItemsWithInput.put("Window", applicationNameToDockItemsWithInput);

        return confirmationDialogWhenUnsavedChangesExist(windowNameToApplicationNameToDockItemsWithInput,
                                                         question,
                                                         closeActionName,
                                                         stage,
                                                         monitor);
    }

    public static boolean confirmationDialogWhenUnsavedChangesExist(List<Stage> stages,
                                                                    String question,
                                                                    String closeActionName,
                                                                    Stage stage,
                                                                    JobMonitor monitor) throws ExecutionException, InterruptedException {
        return confirmationDialogWhenUnsavedChangesExist(stages2WindowNameToApplicationNameToDockItemsWithInput(stages),
                                                         question,
                                                         closeActionName,
                                                         stage,
                                                         monitor);
    }

    private enum SaveStatus {
        SUCCESS,
        FAILURE,
        NOTHING
    };

    public static boolean confirmationDialogWhenUnsavedChangesExist(SortedMap<String, SortedMap<String, List<DockItemWithInput>>> windowNrToApplicationNameToDockItemsWithInput,
                                                                    String question,
                                                                    String closeActionName,
                                                                    Stage stage,
                                                                    JobMonitor monitor) throws ExecutionException, InterruptedException {

            Stage stageToPositionTheConfirmationDialogOver;
            if (stage != null) {
                stageToPositionTheConfirmationDialogOver = stage;
            }
            else {
                stageToPositionTheConfirmationDialogOver = INSTANCE.main_stage;
            }

            ButtonType clearSelectionOfCheckboxes = new ButtonType(Messages.UnsavedChanges_clearButtonText);
            ButtonType selectAllCheckboxes = new ButtonType(Messages.UnsavedChanges_selectAllButtonText);
            ButtonType saveSelectedItems = new ButtonType(Messages.UnsavedChanges_saveButtonText);
            ButtonType exitPhoebusWithoutSavingUnsavedChanges = new ButtonType(Messages.UnsavedChanges_discardButtonText_discardAnd + " " + closeActionName);

            FutureTask displayConfirmationWindow = new FutureTask(() -> {
                Alert prompt = new Alert(AlertType.CONFIRMATION);

                prompt.getDialogPane().getButtonTypes().remove(ButtonType.OK);
                ((ButtonBar) prompt.getDialogPane().lookup(".button-bar")).setButtonOrder(ButtonBar.BUTTON_ORDER_NONE); // Set the button order manually (since they are non-standard)
                prompt.getDialogPane().getButtonTypes().add(clearSelectionOfCheckboxes);
                prompt.getDialogPane().getButtonTypes().add(selectAllCheckboxes);
                prompt.getDialogPane().getButtonTypes().add(saveSelectedItems);
                prompt.getDialogPane().getButtonTypes().add(exitPhoebusWithoutSavingUnsavedChanges);

                Button cancel_button = (Button) prompt.getDialogPane().lookupButton(ButtonType.CANCEL);
                cancel_button.setTooltip(new Tooltip(cancel_button.getText()));

                Button clearSelectionOfCheckboxes_button = (Button) prompt.getDialogPane().lookupButton(clearSelectionOfCheckboxes);
                clearSelectionOfCheckboxes_button.setTooltip(new Tooltip(clearSelectionOfCheckboxes_button.getText()));

                Button selectAllCheckboxes_button = (Button) prompt.getDialogPane().lookupButton(selectAllCheckboxes);
                selectAllCheckboxes_button.setTooltip(new Tooltip(selectAllCheckboxes_button.getText()));

                Button saveSelectedItems_button = (Button) prompt.getDialogPane().lookupButton(saveSelectedItems);
                saveSelectedItems_button.setTooltip(new Tooltip(saveSelectedItems_button.getText()));

                Button exitPhoebusWithoutSavingUnsavedChanges_button = (Button) prompt.getDialogPane().lookupButton(exitPhoebusWithoutSavingUnsavedChanges);
                exitPhoebusWithoutSavingUnsavedChanges_button.setTooltip(new Tooltip(exitPhoebusWithoutSavingUnsavedChanges_button.getText()));
                List<Consumer<Boolean>> setCheckBoxStatusActions = new LinkedList<>();
                List<Supplier<Boolean>> getCheckBoxStatusActions = new LinkedList<>();
                List<Supplier<SaveStatus>> saveActions = new LinkedList<>();

                Runnable enableAndDisableButtons = () -> {
                    if (getCheckBoxStatusActions.stream().anyMatch(getCheckBoxStatus -> getCheckBoxStatus.get())) {
                        clearSelectionOfCheckboxes_button.setDisable(false);
                        saveSelectedItems_button.setDisable(false);
                        exitPhoebusWithoutSavingUnsavedChanges_button.setDisable(true);
                    }
                    else {
                        clearSelectionOfCheckboxes_button.setDisable(true);
                        saveSelectedItems_button.setDisable(true);
                        exitPhoebusWithoutSavingUnsavedChanges_button.setDisable(false);
                    }

                    if (getCheckBoxStatusActions.stream().allMatch(getCheckBoxStatus -> getCheckBoxStatus.get())) {
                        selectAllCheckboxes_button.setDisable(true);
                        saveSelectedItems_button.setText(Messages.UnsavedChanges_saveButtonText_saveAnd + " " + closeActionName);
                        saveSelectedItems_button.setTooltip(new Tooltip(saveSelectedItems_button.getText()));
                    }
                    else {
                        selectAllCheckboxes_button.setDisable(false);
                        saveSelectedItems_button.setText(Messages.UnsavedChanges_saveButtonText);
                        saveSelectedItems_button.setTooltip(new Tooltip(saveSelectedItems_button.getText()));
                    }
                };

                GridPane gridPane = new GridPane();
                gridPane.setVgap(4);
                int currentRow = 0;
                for (String windowName : windowNrToApplicationNameToDockItemsWithInput.keySet()) {
                    var applicationNameToDockItemsWithInput = windowNrToApplicationNameToDockItemsWithInput.get(windowName);

                    if (applicationNameToDockItemsWithInput.size() > 0) {    // Only print unsaved changes for a window if it actually containts any unsaved changes.
                        if (windowNrToApplicationNameToDockItemsWithInput.size() >= 2) {    // Only print the window names if two or more windows are in the process of being closed.
                            Text windowTitle = new Text(windowName);
                            windowTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold");
                            gridPane.add(windowTitle, 0, currentRow);
                            currentRow++;
                        }

                        for (var applicationName : applicationNameToDockItemsWithInput.keySet()) {
                            for (var dockItemWithInput : applicationNameToDockItemsWithInput.get(applicationName)) {
                                CheckBox checkBox = new CheckBox();
                                checkBox.selectedProperty().addListener((observableValue, old_value, new_value) -> enableAndDisableButtons.run());

                                Text applicationName_text = new Text(applicationName + ":");
                                applicationName_text.setStyle("-fx-font-weight: bold");
                                Text instanceName_text = new Text(dockItemWithInput.getLabel());

                                HBox hBox = new HBox(checkBox, applicationName_text, instanceName_text);
                                hBox.setSpacing(4);
                                gridPane.add(hBox, 0, currentRow);

                                Consumer<Boolean> setCheckboxStatus = bool -> checkBox.setSelected(bool);
                                setCheckBoxStatusActions.add(setCheckboxStatus);

                                Supplier<Boolean> getCheckBoxStatus = () -> checkBox.isSelected();
                                getCheckBoxStatusActions.add(getCheckBoxStatus);

                                hBox.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> checkBox.setSelected(!checkBox.isSelected())); // Enable toggling checkbox by clicking on its label.

                                Supplier<SaveStatus> saveIfCheckboxEnabled = () -> {
                                    if (checkBox.isSelected()) {

                                        Text saving = new Text("[" + Messages.UnsavedChanges_saving + "]");
                                        saving.setFill(Color.ORANGE);
                                        saving.setStyle("-fx-font-weight: bold;");
                                        hBox.getChildren().set(0, saving);
                                        boolean saveSuccessful = dockItemWithInput.save(monitor, prompt.getDialogPane().getScene().getWindow());

                                        if (saveSuccessful) {
                                            // The functions setCheckboxStatus() and getCheckBoxStatus should not be available anymore:
                                            setCheckBoxStatusActions.remove(setCheckboxStatus);
                                            getCheckBoxStatusActions.remove(getCheckBoxStatus);
                                            setCheckboxStatus.accept(false);

                                            Text saved = new Text("[" + Messages.UnsavedChanges_saved + "]");
                                            saved.setFill(Color.GREEN);
                                            saved.setStyle("-fx-font-weight: bold;");
                                            hBox.getChildren().set(0, saved);
                                            return SaveStatus.SUCCESS;
                                        }
                                        else {
                                            Text savingFailed_text = new Text("[" + Messages.UnsavedChanges_savingFailed + "]");
                                            savingFailed_text.setFill(Color.RED);
                                            savingFailed_text.setStyle("-fx-font-weight: bold;");

                                            HBox savingFailed = new HBox(checkBox, savingFailed_text);
                                            savingFailed.setSpacing(6);
                                            hBox.getChildren().set(0, savingFailed);
                                            return SaveStatus.FAILURE;
                                        }
                                    }
                                    else {
                                        return SaveStatus.NOTHING;
                                    }
                                };
                                saveActions.add(saveIfCheckboxEnabled);

                                currentRow++;
                            }
                        }
                    }
                }

                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setContent(gridPane);

                prompt.getDialogPane().setContent(scrollPane);

                clearSelectionOfCheckboxes_button.addEventFilter(ActionEvent.ACTION, event -> {
                    event.consume();

                    setCheckBoxStatusActions.forEach(setCheckboxAction -> {
                        setCheckboxAction.accept(false);
                    });
                });

                selectAllCheckboxes_button.addEventFilter(ActionEvent.ACTION, event -> {
                    event.consume();

                    setCheckBoxStatusActions.forEach(setCheckboxAction -> {
                        setCheckboxAction.accept(true);
                    });
                });

                saveSelectedItems_button.addEventFilter(ActionEvent.ACTION, event -> {
                    event.consume();

                    List<Supplier<SaveStatus>> saveActionsThatHaveBeenCompleted = new LinkedList<>();
                    for (var saveAction : saveActions) {
                        SaveStatus result = saveAction.get();
                        if (result == SaveStatus.SUCCESS) {
                            saveActionsThatHaveBeenCompleted.add(saveAction);
                        }
                        else if (result == SaveStatus.FAILURE) {
                            break;
                        }
                        // If result == SaveStatus.NOTHING, continue.
                    }

                    for (var saveActionThatHasBeenCompleted : saveActionsThatHaveBeenCompleted) {
                        saveActions.remove(saveActionThatHasBeenCompleted);
                    }

                    if (saveActions.size() == 0) {
                        exitPhoebusWithoutSavingUnsavedChanges_button.fire();
                    }
                });

                // Initialize state of buttons:
                enableAndDisableButtons.run();

                prompt.setHeaderText(Messages.UnsavedChanges_theFollowingApplicationInstancesHaveUnsavedChanges + " " + question);
                prompt.setTitle(Messages.UnsavedChanges);

                int prefWidth = 750;
                int prefHeight = 400;
                prompt.getDialogPane().setPrefSize(prefWidth, prefHeight);
                prompt.getDialogPane().setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                prompt.setResizable(false);

                DialogHelper.positionDialog(prompt, stageToPositionTheConfirmationDialogOver.getScene().getRoot(), -prefWidth/2, -prefHeight/2);

                return prompt.showAndWait().orElse(ButtonType.CANCEL) == exitPhoebusWithoutSavingUnsavedChanges ? true : false;
            });

        if (windowNrToApplicationNameToDockItemsWithInput.isEmpty() || windowNrToApplicationNameToDockItemsWithInput.values().stream().allMatch(sortedMap -> sortedMap.values().stream().allMatch(Collection::isEmpty))) {
            // No unsaved changes.
            return true;
        }
        else {
            Platform.runLater(displayConfirmationWindow);
            boolean shouldClose = (boolean) displayConfirmationWindow.get();
            return shouldClose;
        }
    }

    private void exitPhoebus() {
        Platform.runLater(() ->
        {
            for (Stage stage : DockStage.getDockStages()) {
                DockStage.closeItems(stage);
            }
            stop();
        });
    };

    /**
     * Start all applications
     *
     * @param monitor
     */
    private void startApplications(final JobMonitor monitor) {
        final Collection<AppDescriptor> apps = ApplicationService.getApplications();
        monitor.beginTask(Messages.MonitorTaskApps, apps.size());
        for (AppDescriptor app : apps) {
            monitor.updateTaskName(Messages.MonitorTaskStarting + app.getDisplayName());
            try {
                app.start();
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, app.getDisplayName() + " startup failed", ex);
            }
            monitor.worked(1);
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

        if (freezeup_check != null)
            freezeup_check.close();

        logger.log(Level.INFO, "Exiting");
        // Hard exit because otherwise background threads
        // might keep us from quitting the VM
        Platform.exit();
        System.exit(0);
    }

    /**
     * Closes all tabs in all windows. Side effect is that all detached windows are also
     * closed. Main window is not closed.
     */
    public static void closeAllTabs(){
        final List<Stage> stages = DockStage.getDockStages();
        JobManager.schedule("Close All Tabs", monitor ->
        {
            boolean shouldCloseTabs = PhoebusApplication.confirmationDialogWhenUnsavedChangesExist(stages,
                                                                                                   Messages.UnsavedChanges_wouldYouLikeToSaveAnyChangesBeforeClosingAllTabs,
                                                                                                   Messages.UnsavedChanges_close,
                                                                                                   PhoebusApplication.INSTANCE.main_stage,
                                                                                                   monitor);

            if (shouldCloseTabs) {
                for (Stage stage : stages){
                    if (!DockStage.prepareToCloseItems(stage)){
                        return;
                    }
                }

                Platform.runLater(() -> stages.forEach(stage -> DockStage.closeItems(stage)));
            }
        });
    }
}
