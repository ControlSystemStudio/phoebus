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
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

/** PV Table Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayRuntimeInstance implements AppInstance
{
    // Compare to RCP RuntimeViewPart

    /** Memento tags */
    private static final String TAG_ZOOM = "ZOOM";

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

    DisplayRuntimeInstance(final AppDescriptor app)
    {
        this.app = app;

        final DockPane dock_pane = DockPane.getActiveDockPane();
        JFXRepresentation.setSceneStyle(dock_pane.getScene());

        representation = new DockItemRepresentation(this);
        RuntimeUtil.hookRepresentationListener(representation);

        toolbar = createToolbar();

        new ContextMenuSupport(this);

        BorderPane.setMargin(toolbar, new Insets(5, 5, 0, 5));
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

    private Node createToolbar()
    {
        zoom_action = new ZoomAction(this);
        return new ToolBar(ToolbarHelper.createSpring(),
                           zoom_action,
                           NavigationAction.createBackAction(this, navigation),
                           NavigationAction.createForewardAction(this, navigation)
                           );
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
    }

    @Override
    public void save(final Memento memento)
    {
        final String zoom = representation.getZoomLevelSpec();
        if (! JFXRepresentation.DEFAULT_ZOOM_LEVEL.equals(zoom))
            memento.setString(TAG_ZOOM, zoom);
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
     *  @param info Display file to load
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
        JobManager.schedule("Load Display", monitor -> loadModel(monitor, info));
    }

    /** Re-load the current input */
    public void reload()
    {
        loadDisplayFile(getDisplayInfo());
    }

    /** Load display model, schedule representation
     *  @param info Display to load
     */
    private void loadModel(final JobMonitor monitor, final DisplayInfo info)
    {
        monitor.beginTask(info.toString());
        try
        {
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

            // Schedule representation on UI thread
            representation.execute(() -> representModel(model));
        }
        catch (Exception ex)
        {
            showError("Error loading " + info, ex);
        }
    }

    /** Represent model, schedule start of runtime
     *  @param model Model to represent
     */
    private void representModel(final DisplayModel model)
    {
        try
        {
            final Parent parent = representation.getModelParent();
            JFXRepresentation.getChildren(parent).clear();
            representation.representModel(parent, model);
        }
        catch (Exception ex)
        {
            showError("Cannot represent model", ex);
        }

        // Start runtimes in background
        RuntimeUtil.getExecutor().execute(() -> RuntimeUtil.startRuntime(model));
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
        dock_item.setLabel(info.getName());

        navigation.setCurrentDisplay(info);
        active_model = model;
    }

    /** Stop runtime, dispose representation for current model */
    private void disposeModel()
    {
        final DisplayModel model = active_model;
        active_model = null;
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
            final TextArea text = new TextArea(message);
            text.setEditable(false);
            text.setPrefSize(800, 600);
            JFXRepresentation.getChildren(representation.getModelParent()).setAll(text);
        });
    }

    /** DockItem closed */
    public void onClosed()
    {
        // Stop runtime, dispose widgets for the model
        disposeModel();
        // Stop representation, so no more widgets can be created in this dock item
        representation.shutdown();
    }
}
