/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.macros.DisplayMacroExpander;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/** PV Table Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayRuntimeInstance implements AppInstance
{
    // Compare to RCP RuntimeViewPart

    /** Memento tags */
    private static final String TAG_ZOOM = "ZOOM";
    private static final String TAG_TOOLBAR = "toolbar";

    /** Global tracker of last user's decision to show toolbar.
     *  Used when opening new display
     */
    private static boolean last_toolbar_visible = true;

    private final AppDescriptor app;
    private final BorderPane layout = new BorderPane();
    private final DockItemWithInput dock_item;
    private final DockItemRepresentation representation;
    private Node toolbar;

    /** Property on the 'model_parent' of the JFX scene that holds this DisplayRuntimeInstance */
    static final String MODEL_PARENT_DISPLAY_RUNTIME = "_runtime_view_part";

    /** Back/forward navigation */
    private final DisplayNavigation navigation = new DisplayNavigation();

    /** Display info for the currently shown model */
    private volatile Optional<DisplayInfo> display_info = Optional.empty();

    private DisplayModel active_model;

    /** Toolbar button for zoom */
    private ZoomAction zoom_action;

    /** Toolbar button for navigation */
    private ButtonBase navigate_backward, navigate_forward;

    /** Obtain the DisplayRuntimeInstance of a display
     *  @param model {@link DisplayModel}
     *  @return {@link DisplayRuntimeInstance}
     */
    public static DisplayRuntimeInstance ofDisplayModel(final DisplayModel model)
    {
        final Parent model_parent = Objects.requireNonNull(model.getUserData(Widget.USER_DATA_TOOLKIT_PARENT));
        return (DisplayRuntimeInstance) model_parent.getProperties().get(DisplayRuntimeInstance.MODEL_PARENT_DISPLAY_RUNTIME);
    }

    DisplayRuntimeInstance(final AppDescriptor app)
    {
        this(app, null);
    }
    
    DisplayRuntimeInstance(final AppDescriptor app, String prefTarget)
    {
        this.app = app;

        DockPane dock_pane = null;
        if (prefTarget != null)
        {
            if (prefTarget.equals("window"))
            {
                // Open new Stage in which this app will be opened, its DockPane is a new active one
                final Stage new_stage = new Stage();
                DockStage.configureStage(new_stage);
                new_stage.show();
            }
            else
                dock_pane = DockStage.getDockPaneByName(prefTarget);
        }
        if (dock_pane == null)
            dock_pane = DockPane.getActiveDockPane();
        dock_pane.deferUntilInScene(JFXRepresentation::setSceneStyle);

        representation = new DockItemRepresentation(this);
        RuntimeUtil.hookRepresentationListener(representation);

        toolbar = createToolbar();

        new ContextMenuSupport(this);

        if (last_toolbar_visible)
            layout.setTop(toolbar);

        layout.setCenter(representation.createModelRoot());
        dock_item = new DockItemWithInput(this, layout, null, null, null);
        dock_pane.addTab(dock_item);

        representation.getModelParent().getProperties().put(MODEL_PARENT_DISPLAY_RUNTIME, this);
        representation.getModelParent().setOnContextMenuRequested(event ->
        {
            final DisplayModel model = active_model;
            if (model != null)
            {
                event.consume();
                representation.fireContextMenu(model, (int)event.getScreenX(), (int)event.getScreenY());
            }
        });

        layout.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeys);

        dock_item.addClosedNotification(this::onClosed);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** @return {@link JFXRepresentation} */
    JFXRepresentation getRepresentation()
    {
        return representation;
    }

    /** @return DockItem in which this display is contained */
    public DockItem getDockItem()
    {
        return dock_item;
    }

    DisplayNavigation getNavigation()
    {
        return navigation;
    }

    private Node createToolbar()
    {
        zoom_action = new ZoomAction(this);
        navigate_backward = NavigationAction.createBackAction(this, navigation);
        navigate_forward = NavigationAction.createForewardAction(this, navigation);
        return new ToolBar(ToolbarHelper.createSpring(),
                           zoom_action,
                           navigate_backward,
                           navigate_forward
                           );
    }

    /** @return <code>true</code> if toolbar is visible */
    boolean isToolbarVisible()
    {
        return layout.getTop() == toolbar;
    }

    /** @param show Should the toolbar be shown? */
    void showToolbar(final boolean show)
    {
        if (show)
        {
            if (! isToolbarVisible())
                layout.setTop(toolbar);
        }
        else
            layout.setTop(null);
        last_toolbar_visible = show;
    }

    @Override
    public void restore(final Memento memento)
    {
        memento.getString(TAG_ZOOM).ifPresent(level ->
        {
            // Simulate user input: Set value, invoke handler
            zoom_action.setValue(level);
            zoom_action.getOnAction().handle(null);
        });
        memento.getBoolean(TAG_TOOLBAR).ifPresent(this::showToolbar);
    }

    @Override
    public void save(final Memento memento)
    {
        final String zoom = representation.getZoomLevelSpec();
        if (! JFXRepresentation.DEFAULT_ZOOM_LEVEL.equals(zoom))
            memento.setString(TAG_ZOOM, zoom);
        memento.setBoolean(TAG_TOOLBAR, isToolbarVisible());
    }

    /** Handle Alt-left & right as navigation keys */
    private void handleKeys(final KeyEvent event)
    {
        if (event.isAltDown())
        {
            if (event.getCode() == KeyCode.LEFT  &&  !navigate_backward.isDisabled())
                navigate_backward.getOnAction().handle(null);
            else if (event.getCode() == KeyCode.RIGHT  &&  !navigate_forward.isDisabled())
                navigate_forward.getOnAction().handle(null);
        }
    }

    /** Select dock item, make visible */
    public void raise()
    {
        dock_item.select();
    }

    /** Close the dock item */
    void close()
    {
        dock_item.close();
    }

    /** @return Current display info or <code>null</code> */
    DisplayInfo getDisplayInfo()
    {
        return display_info.orElse(null);
    }

    /** Load display file, represent it, start runtime
     *  @param info Display file to load & represent
     */
    public void loadDisplayFile(final DisplayInfo info)
    {
        // If already executing another display, shut it down
        disposeModel();

        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        dock_item.setInput(info.toURI());

        // Now that old model is no longer represented,
        // show info.
        // Showing this info before disposeModel()
        // would result in old representation not being able
        // to traverse its expected widget tree
        showMessage("Loading " + info);

        // Note the path & macros, then
        display_info = Optional.of(info);
        // load model off UI thread
        JobManager.schedule("Load Display", monitor ->
        {
            try
            {
                final DisplayModel model = loadModel(monitor, info);

                final Future<Void> represented = representation.submit(() -> representModel(model));
                represented.get();

                // Start runtime for the model
                RuntimeUtil.startRuntime(model);

                logger.log(Level.FINE, "Waiting for representation of model " + info.getPath());

                try
                {
                    representation.awaitRepresentation(30, TimeUnit.SECONDS);
                    logger.log(Level.FINE, "Done with representing model of " + info.getPath());
                }
                catch (TimeoutException | InterruptedException ex)
                {
                    logger.log(Level.SEVERE, "Cannot wait for representation of " + info.getPath(), ex);
                }

                // Check if there were widget errors
                if (model.isClean() == false)
                    showRepresentationError();
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot load model from " + info.getPath(), ex);

                final String exception_message;

                if (ex.getCause() != null)
                    exception_message = ":\n" + ex.getCause().getLocalizedMessage();
                else
                    exception_message = "";

                ExceptionDetailsErrorDialog.openError("Cannot load model",
                        "Cannot load model from\n" + info.getPath() + exception_message, ex);

                display_info = Optional.empty();
                Platform.runLater(() ->
                {
                    final Parent parent = representation.getModelParent();
                    JFXRepresentation.getChildren(parent).clear();
                    close();
                });
                return;
            }
        });
    }

    /** Re-load the current input */
    public void reload()
    {
        ModelResourceUtil.clearURLCache();
        loadDisplayFile(getDisplayInfo());
    }

    /** Load display model
     *  @param info Display to load
     *  @return Model that has been loaded
     */
    private DisplayModel loadModel(final JobMonitor monitor, final DisplayInfo info) throws Exception
    {
        monitor.beginTask(info.toString());
        final DisplayModel model = info.shouldResolve()
            ? ModelLoader.resolveAndLoadModel(null, info.getPath())
            : ModelLoader.loadModel(info.getPath());

        // This code is called
        // 1) When opening a new display
        //    No macros in info.
        // 2) On application restart with DisplayInfo from memento
        //    Info contains snapshot of macros from last run
        //    Could simply use info's macros if they are non-empty,
        //    but merging macros with those loaded from model file
        //    allows for newly added macros in the display file.
        final Macros macros = Macros.merge(model.propMacros().getValue(), info.getMacros());
        model.propMacros().setValue(macros);

        // For runtime, expand macros
        if (! representation.isEditMode())
            DisplayMacroExpander.expandDisplayMacros(model);
        return model;
    }

    /** Represent model
     *  @param model Model to represent
     *  @return {@link Void} to allow use in {@link Callable}
     */
    private Void representModel(final DisplayModel model) throws Exception
    {
        final Parent parent = representation.getModelParent();
        JFXRepresentation.getChildren(parent).clear();
        representation.representModel(parent, model);
        return null;
    }

    /** Take note of the currently displayed model
     *
     *  <p>Called by the {@link DockItemRepresentation},
     *  either when representing the initial model or when
     *  an action later changes the model.
     *
     *  @param model Model that's now represented
     */
    void trackCurrentModel(final DisplayModel model)
    {
        final DisplayInfo old_info = display_info.orElse(null);
        final DisplayInfo info = DisplayInfo.forModel(model);
        // A display might be loaded without macros,
        // but the DisplayModel may then have macros configured in the display itself.
        //
        // This can later result in not recognizing an existing display:
        // Display X is running, and it contained macros.
        // Now somehow we open X again, without macros, but
        // all the executing displays have X with macros,
        // so we open yet another one instead of showing the existing instance.
        //
        // To avoid this problem:
        //
        // When first loading a display, set display_info to the received info.
        //
        // When this is later updated, only replace the display_info
        // if there was none,
        // or the new one has a different path,
        // or different macros _and_ there were original macros.
        if ( old_info == null  ||
            !old_info.getPath().equals(info.getPath()) ||
          ( !old_info.getMacros().isEmpty()  &&  !old_info.getMacros().equals(info.getMacros())))
        {
            display_info = Optional.of(info);
            dock_item.setInput(info.toURI());
        }
        // setInput sets the tab name based on the input via runLater.
        // Update to the display name via another runLater.
        Platform.runLater(() -> dock_item.setLabel(info.getName()));

        navigation.setCurrentDisplay(info);
        active_model = model;
    }

    /** Stop runtime, dispose representation for current model */
    private void disposeModel()
    {
        final DisplayModel model = active_model;
        active_model = null;

        // Close handler disposes runtime and representation for model
        if (model != null)
            ActionUtil.handleClose(model);
    }

    /** Show error message and stack trace
     *  @param message Message
     *  @param error
     */
    private void showError(final String message, final Throwable error)
    {
        logger.log(Level.WARNING, message, error);

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(buf);
        out.append(message).append("\n\n");
        error.printStackTrace(out);
        showMessage(buf.toString());
    }

    /** Show message
     *  @param message Message
     */
    private void showMessage(final String message)
    {
        Platform.runLater(() ->
        {
            final Label text = new Label(message);
            final Parent parent = representation.getModelParent();
            text.setWrapText(true);
            text.setAlignment(Pos.TOP_LEFT);
            if (parent instanceof Pane)
            {
                text.prefWidthProperty().bind(((Pane)parent).widthProperty());
                text.prefHeightProperty().bind(((Pane)parent).heightProperty());
            }
            else
                text.setPrefSize(800, 600);
            JFXRepresentation.getChildren(parent).setAll(text);
        });
    }

    /** Inform user that there were representation errors
     *
     */
    private void showRepresentationError()
    {
        ExceptionDetailsErrorDialog.openError("Errors while loading model",
                "There were some errors while loading model from " + display_info.get().getPath() + "\nNot all widgets are displayed correctly. Please check the log for details.", null);
    }

    /** DockItem closed */
    public void onClosed()
    {
        // Stop runtime, dispose widgets for the model
        disposeModel();
        // Stop representation, so no more widgets can be created in this dock item
        representation.shutdown();

        navigation.dispose();
    }
}
