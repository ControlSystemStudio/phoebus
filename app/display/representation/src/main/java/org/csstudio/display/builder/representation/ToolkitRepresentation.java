/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.PlaceholderWidget;
import org.csstudio.display.builder.representation.spi.WidgetRepresentationsService;

/** Representation for a toolkit.
 *
 *  <p>Each 'window' or related display part that represents
 *  a display model holds one instance of the toolkit.
 *
 *  <p>The toolkit maintains information about the part
 *  and creates toolkit items for model widgets within the part.
 *
 *  <p>Some toolkits (SWT) require a parent for each item,
 *  while others (JavaFX) can create items which are later
 *  assigned to a parent.
 *  This API requires a parent, and created items are
 *  right away assigned to that parent.
 *
 *  @author Kay Kasemir
 *  @param <TWP> Toolkit widget parent class
 *  @param <TW> Toolkit widget base class
 */
@SuppressWarnings("nls")
abstract public class ToolkitRepresentation<TWP extends Object, TW> implements Executor
{
    /** Logger suggested for all representation logging */
    public final static Logger logger = Logger.getLogger(ToolkitRepresentation.class.getPackageName());

    private final static AtomicBoolean initialized = new AtomicBoolean();

    /** Factories for representations based on widget type.
     *
     *  Holding WidgetRepresentationFactory<TWP, TW>,
     *  but static to prevent duplicates and thus loosing the type info
     */
    private final static Map<String, WidgetRepresentationFactory<?, ?>> factories = new ConcurrentHashMap<>();

    private final boolean edit_mode;

    private final RepresentationUpdateThrottle throttle = new RepresentationUpdateThrottle(this);

    /** Listener list */
    private final List<ToolkitListener> listeners = new CopyOnWriteArrayList<>();

    /** Add/remove representations for child elements as model changes */
    private final WidgetPropertyListener<List<Widget>> container_children_listener = (children, removed, added) ->
    {
        // Only the order of the widgets have changed
        if (removed != null && added != null && removed.size() == 1 && removed.equals(added))
        {
            final Widget widget = added.get(0);
            execute(() ->
            {
                final WidgetRepresentation<TWP, TW, Widget> representation = widget.getUserData(Widget.USER_DATA_REPRESENTATION);
                representation.updateOrder();
            });
            return;
        }

        // Move to toolkit thread.
        // May already be on toolkit, for example in drag/drop,
        // but updating the representation 'later' may reduce blocking.
        if (removed != null)
            for (Widget removed_widget : removed)
                execute(() -> disposeWidget(removed_widget));
        if (added != null)
            for (Widget added_widget : added)
            {
                final Optional<Widget> parent = added_widget.getParent();
                if (! parent.isPresent())
                    throw new IllegalStateException("Cannot locate parent widget for " + added_widget);
                final TWP parent_item = parent.get().getUserData(Widget.USER_DATA_TOOLKIT_PARENT);
                execute(() -> representWidget(parent_item, added_widget));
            }
    };

    protected DisplayModel model;
    private   Phaser       phaser;

    /** Constructor
     *  @param edit_mode Edit mode?
     */
    public ToolkitRepresentation(final boolean edit_mode)
    {
        this.edit_mode = edit_mode;
        if (! initialized.getAndSet(true))
            initialize();
    }

    /** 'Edit' mode is used by the editor,
     *  while non-edit mode is used by the runtime.
     *
     *  <p>In edit mode, the widget representation may
     *  appear 'passive', not reacting to user input,
     *  and instead showing additional information like
     *  file names used as input for an image
     *  or an outline for an otherwise shapeless widget.
     *
     *  <p>In runtime mode, the widget may react to user input
     *  or at times be invisible.
     *
     *  @return <code>true</code> for edit mode
     */
    public final boolean isEditMode()
    {
        return edit_mode;
    }

    /** Called once to initialize.
     *
     *  <p>Registers available representations.
     *
     *  <p>Derived class may override to
     *  add initialization steps, but must call
     *  base implementation.
     */
    protected void initialize()
    {
        // Load representations from service
        for (WidgetRepresentationsService service : ServiceLoader.load(WidgetRepresentationsService.class))
        {
            final Map<WidgetDescriptor, WidgetRepresentationFactory<TWP, TW>> map = service.getWidgetRepresentationFactories();
            map.forEach((desc, factory) ->
            {
                if (factories.putIfAbsent(desc.getType(), factory) != null)
                    throw new Error("Representation for " + desc + " already defined");


            });
        }
    }

    /** Obtain the toolkit used to represent widgets
     *
     *  @param model {@link DisplayModel}
     *  @return ToolkitRepresentation
     *  @throws NullPointerException if toolkit not set
     */
    public static <TWP, TW> ToolkitRepresentation<TWP, TW> getToolkit(final DisplayModel model) throws NullPointerException
    {
        final ToolkitRepresentation<TWP, TW> toolkit = model.getUserData(DisplayModel.USER_DATA_TOOLKIT);
        return Objects.requireNonNull(toolkit, "Toolkit not set");
    }

    /** Open display panel
    *
    *  <p>For RCP-based representation, this is a new workbench 'view'.
    *
    *  <p>Is invoked with the _initial_ model,
    *  calling <code>representModel</code> to create the
    *  individual widget representations.
    *
    *  <p>To later replace the model, call <code>disposeRepresentation</code>
    *  with the current model, and then <code>representModel</code> with the new model.
    *
    *  @param model {@link DisplayModel} that provides name and initial size
    *  @param name Optional name of the region where the new panel should be created.
    *              May be empty, may also be ignored.
    *  @param close_handler Will be invoked when user closes the window
    *                       with the then active model, i.e. the model
    *                       provided in last call to <code>representModel</code>.
    *                       Should stop runtime, dispose representation.
    *  @return The new ToolkitRepresentation of the new window
    *  @throws Exception on error
    */
    public ToolkitRepresentation<TWP, TW> openPanel(final DisplayModel model, final String name, final Consumer<DisplayModel> close_handler) throws Exception
    {   // Default: Same as openNewWindow
        return openNewWindow(model, close_handler);
    }

    /** Open new top-level window
     *
     *  <p>For RCP-based representation, this is a new workbench window.
     *
     *  <p>Is invoked with the _initial_ model,
     *  calling <code>representModel</code> to create the
     *  individual widget representations.
     *
     *  <p>To later replace the model, call <code>disposeRepresentation</code>
     *  with the current model, and then <code>representModel</code> with the new model.
     *
     *  @param model {@link DisplayModel} that provides name and initial size
     *  @param close_handler Will be invoked when user closes the window
     *                       with the then active model, i.e. the model
     *                       provided in last call to <code>representModel</code>.
     *                       Should stop runtime, dispose representation.
     *  @return The new ToolkitRepresentation of the new window
     *  @throws Exception on error
     */
    abstract public ToolkitRepresentation<TWP, TW> openNewWindow(DisplayModel model, Consumer<DisplayModel> close_handler) throws Exception;

    /** Open standalone window
     *
     *  <p>For RCP-based representation, this is a plain shell or stage,
     *  not associated with a workbench.
     *
     *  <p>Is invoked with the _initial_ model,
     *  calling <code>representModel</code> to create the
     *  individual widget representations.
     *
     *  <p>To later replace the model, call <code>disposeRepresentation</code>
     *  with the current model, and then <code>representModel</code> with the new model.
     *
     *  @param model {@link DisplayModel} that provides name and initial size
     *  @param close_handler Will be invoked when user closes the window
     *                       with the then active model, i.e. the model
     *                       provided in last call to <code>representModel</code>.
     *                       Should stop runtime, dispose representation.
     *  @return The new ToolkitRepresentation of the new window
     *  @throws Exception on error
     */
    public ToolkitRepresentation<TWP, TW> openStandaloneWindow(DisplayModel model, Consumer<DisplayModel> close_handler) throws Exception
    {   // Default: Same as openNewWindow
        return openNewWindow(model, close_handler);
    }

    /** Create toolkit widgets for a display model.
     *
     *  <p>The parent may be the top-level parent of a window,
     *  or the parent of an EmbeddedWidget representation.
     *
     *  @param parent Toolkit parent (Pane, Container, ..)
     *  @param model Display model
     *  @throws Exception on error
     *  @see #disposeRepresentation()
     */
    public void representModel(final TWP parent, final DisplayModel model) throws Exception
    {
        Objects.requireNonNull(parent, "Missing toolkit parent item");

        if (model.isTopDisplayModel())
        {
            this.model = model;
            // Register ourselves
            phaser = new Phaser(1);
        }

        // Attach toolkit
        model.setUserData(DisplayModel.USER_DATA_TOOLKIT, this);

        // DisplayModel itself is _not_ represented,
        // but all its children, recursively
        representChildren(parent, model, model.runtimeChildren());

        logger.log(Level.FINE, "Tracking changes to children of {0}", model);
        model.runtimeChildren().addPropertyListener(container_children_listener);
    }

    /** Create representation for each child of a ContainerWidget
     *  @param parent    Toolkit parent (Pane, Container, ..)
     *  @param container Widget that contains children (DisplayModel, GroupWidget, ..)
     *  @param children  The 'children' property of the container
     */
    private void representChildren(final TWP parent, final Widget container, final ChildrenProperty children)
    {
        container.setUserData(Widget.USER_DATA_TOOLKIT_PARENT, parent);

        for (Widget widget : children.getValue())
            representWidget(parent, widget);
    }

    /** Create a toolkit widget for a model widget.
     *
     *  <p>Will log errors, but not raise exception.
     *
     *  @param parent Toolkit parent (Group, Container, ..)
     *  @param widget Model widget to represent
     *  @return Toolkit item that represents the widget
     *  @see #disposeWidget(Object, Widget)
     */
    @SuppressWarnings("unchecked")
    public void representWidget(final TWP parent, final Widget widget)
    {
        try
        {
            // Signal the start of a representation
            onRepresentationStarted();

            WidgetRepresentationFactory<TWP, TW> factory = (WidgetRepresentationFactory<TWP, TW>) factories.get(widget.getType());
            if (factory == null)
            {
                if (! (widget instanceof PlaceholderWidget))
                    logger.log(Level.SEVERE, "Lacking representation for " + widget.getType());
                // Check for a generic "unknown" representation
                factory = (WidgetRepresentationFactory<TWP, TW>) factories.get(WidgetRepresentationFactory.UNKNOWN);
                if (factory == null)
                    return;
            }

            final TWP re_parent;
            try
            {
                final WidgetRepresentation<TWP, TW, Widget> representation = factory.create();
                representation.initialize(this, widget);
                re_parent = representation.createComponents(parent);
                widget.setUserData(Widget.USER_DATA_REPRESENTATION, representation);
                logger.log(Level.FINE, "Representing {0} as {1}", new Object[] { widget, representation });
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot represent " + widget, ex);
                return;
            }
            // Recurse into child widgets
            final ChildrenProperty children = ChildrenProperty.getChildren(widget);
            if (children != null)
            {
                representChildren(re_parent, widget, children);
                logger.log(Level.FINE, "Tracking changes to children of {0}", widget);
                children.addPropertyListener(container_children_listener);
            }
        }
        finally
        {
            // Signal the end of a representation
            onRepresentationFinished();
        }
    }

    /** Signal the start of the representation of a widget
     *
     *  <p> To be called by WidgetRepresentation classes before submitting a task to be run on another thread
     */
    public void onRepresentationStarted()
    {
        phaser.register();
    }

    /** Signal the end of the representation of a widget
     *
     *
     *  <p> To be called by WidgetRepresentation classes upon representation thread completion
     */
    public void onRepresentationFinished()
    {
        phaser.arriveAndDeregister();
    }

    /** Wait for the complete representation of a model
     *
     *  <p> This includes the representation of embedded displays and navigation tabs
     */
    public void awaitRepresentation(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException
    {
        phaser.arrive();
        phaser.awaitAdvanceInterruptibly(0, timeout, unit);
    }

    /** Remove all the toolkit items of the model
     *  @param model Display model
     *  @return Parent toolkit item (Group, Container, ..) that used to host the model items
     */
    public TWP disposeRepresentation(final DisplayModel model)
    {
        final TWP parent = disposeChildren(model, model.runtimeChildren());
        model.clearUserData(DisplayModel.USER_DATA_TOOLKIT);

        logger.log(Level.FINE, "No longer tracking changes to children of {0}", model);
        model.runtimeChildren().removePropertyListener(container_children_listener);

        return Objects.requireNonNull(parent);
    }

    /** Remove toolkit widgets for container
     *  @param container Container which should no longer be represented, recursing into children
     *  @return Parent toolkit item (Group, Container, ..) that used to host the container items
     */
    private TWP disposeChildren(final Widget container, final ChildrenProperty children)
    {
        for (Widget widget : children.getValue())
            disposeWidget(widget);

        final TWP parent = container.clearUserData(Widget.USER_DATA_TOOLKIT_PARENT);
        return parent;
    }

    /** Remove toolkit widget for model widget
     *  @param widget Model widget that should no longer be represented
     */
    public void disposeWidget(final Widget widget)
    {
        final ChildrenProperty children = ChildrenProperty.getChildren(widget);
        if (children != null)
        {
            logger.log(Level.FINE, "No longer tracking changes to children of {0}", widget);
            children.removePropertyListener(container_children_listener);

            for (Widget child : children.getValue())
                disposeWidget(child);
        }
        final WidgetRepresentation<TWP, TW, ? extends Widget> representation =
            widget.clearUserData(Widget.USER_DATA_REPRESENTATION);
        if (representation != null)
        {
            logger.log(Level.FINE, "Disposing {0} for {1}", new Object[] { representation, widget });
            representation.destroy();
        }
        // else: Widget has no representation because not implemented for this toolkit
    }

    /** Called by toolkit representation to request an update.
     *
     *  <p>That representation's <code>updateChanges()</code> will be called
     *
     *  @param representation Toolkit representation that requests update
     */
    public void scheduleUpdate(final WidgetRepresentation<TWP, TW, ? extends Widget> representation)
    {
        throttle.scheduleUpdate(representation);
    }

    /** @param enable Enable updates, or pause? */
    public void enable(final boolean enable)
    {
        throttle.enable(enable);
    }

    /** Execute command in toolkit's UI thread.
     *
     *  <p>If already on the UI thread, command
     *  may execute right away.
     *
     *  @param command Command to execute
     */
    @Override
    abstract public void execute(final Runnable command);

    /** Schedule a command to be executed on the UI thread
     *
     *  @param command Command to execute
     *  @param delay Delay from now until command will be executed
     *  @param unit {@link TimeUnit}
     */
    public void schedule(final Runnable command, final long delay, final TimeUnit unit)
    {
        // Use the model thread pool to schedule,
        // then perform the command on the UI thread.
        ModelThreadPool.getTimer().schedule(() -> execute(command), delay, unit);
    }

    /** Show message dialog.
     *
     *  <p>Calling thread is blocked until user closes the dialog
     *  by pressing "OK".
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param is_warning Whether to style dialog as warning or information
     *  @param message Message to display on dialog
     */
    abstract public void showMessageDialog(Widget widget, String message);

    /** Show error dialog.
     *
     *  <p>Calling thread is blocked until user closes the dialog
     *  by pressing "OK".
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param is_warning Whether to style dialog as warning or information
     *  @param message Message to display on dialog
     */
    abstract public void showErrorDialog(Widget widget, String error);

    /** Show confirmation dialog.
     *
     *  <p>Calling thread is blocked until user closes the dialog
     *  by selecting either "Yes" or "No"
     *  ("Confirm", "Cancel", depending on implementation).
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param mesquestionsage Message to display on dialog
     *  @return <code>true</code> if user selected "Yes" ("Confirm")
     */
    abstract public boolean showConfirmationDialog(Widget widget, String question);

    /** Show dialog for selecting one item from a list.
     *
     *  <p>Call blocks until the user closes the dialog
     *  by either selecting an item and pressing "OK",
     *  or by pressing "Cancel".
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param title Dialog title
     *  @param options Options to show in dialog
     *  @return Selected item or <code>null</code>
     */
    abstract public String showSelectionDialog(Widget widget, String title, List<String> options);

    /** Show dialog for entering a password.
     *
     *  <p>Call blocks until the user closes the dialog
     *  by either entering a password and pressing "OK",
     *  or by pressing "Cancel".
     *
     *  <p>When a <code>correct_password</code> is provided to the call,
     *  the password entered by the user is checked against it,
     *  prompting until the user enters the correct one.
     *
     *  <p>When no <code>correct_password</code> is provided to the call,
     *  any password entered by the user is returned.
     *  The calling script would then check the password and maybe open
     *  the dialog again.
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param title Dialog title
     *  @param correct_password Password to check
     *  @return Entered password or <code>null</code>
     */
    abstract public String showPasswordDialog(final Widget widget, final String title, final String correct_password);

    /** Show file "Save As" dialog for selecting/entering a new file name
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param initial_value Initial path and file name
     *  @return Path and file name or <code>null</code>
     */
    abstract public String showSaveAsDialog(Widget widget, final String initial_value);

    /** Play audio
     *  @param url URL for the audio. At least "file://.." and "http://.." should be supported.
     *  @return Future to await end of playback or cancel.
     *          Boolean value will indicate successful playback
     */
    abstract public Future<Boolean> playAudio(final String url);

    /** Open a file with the OS-assigned default tool
     *
     *  <p>RCP-based representation can override with
     *  RCP-based default editor for the file
     *
     *  @param path Path to file
     *  @throws Exception on error
     */
    abstract public void openFile(final String path) throws Exception;

    /** Open a URL with the OS-assigned default tool
     *
     *  <p>RCP-based representation can override with
     *  RCP-based web browser
     *
     *  @param path URL
     *  @throws Exception on error
     */
    abstract public void openWebBrowser(final String url) throws Exception;

    /** Execute callable in toolkit's UI thread.
     *  @param <T> Type to return
     *  @param callable Callable to execute
     *  @return {@link Future} to wait for completion,
     *          fetch result or learn about exception
     */
    public <T> Future<T> submit(final Callable<T> callable)
    {
        final FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    /** Add a toolkit listener
     *  @param listener Listener to add
     *  @throws IllegalArgumentException when adding the same listener more than once
     */
    public void addListener(final ToolkitListener listener)
    {
        if (listeners.contains(listener))
            throw new IllegalArgumentException("Duplicate listener");
        listeners.add(listener);
    }

    /** Remove a toolkit listener
     *  @param listener Listener to remove
     *  @return <code>true</code> if that listener was registered
     */
    public boolean removeListener(final ToolkitListener listener)
    {
        return listeners.remove(listener);
    }

    /** Notify listeners that action has been invoked
     *  @param widget Widget that invoked the action
     *  @param action Action to perform
     */
    public void fireAction(final Widget widget, final ActionInfo action)
    {
        for (final ToolkitListener listener : listeners)
        {
            try
            {
                listener.handleAction(widget, action);
            }
            catch (final Throwable ex)
            {
                logger.log(Level.WARNING, "Action failure when invoking " + action + " for " + widget, ex);
            }
        }
    }

    /** Notify listeners that a widget has been clicked
     *  @param widget Widget
     *  @param with_control Is 'control' key held?
     */
    public void fireClick(final Widget widget, final boolean with_control)
    {
        for (final ToolkitListener listener : listeners)
        {
            try
            {
                listener.handleClick(widget, with_control);
            }
            catch (final Throwable ex)
            {
                logger.log(Level.WARNING, "Click failure for " + widget, ex);
            }
        }
    }

    /** Notify listeners that context menu has been invoked
     *  @param widget Widget
     *  @param screen_x X coordinate of mouse when menu was invoked
     *  @param screen_y Y coordinate of mouse when menu was invoked
     */
    public void fireContextMenu(final Widget widget, final int screen_x, final int screen_y)
    {
        for (final ToolkitListener listener : listeners)
        {
            try
            {
                listener.handleContextMenu(widget, screen_x, screen_y);
            }
            catch (final Throwable ex)
            {
                logger.log(Level.WARNING, "Context menu failure for " + widget, ex);
            }
        }
    }

    /** Notify listeners that a widget requests writing a value
     *  @param widget Widget
     *  @param value Value
     */
    public void fireWrite(final Widget widget, final Object value)
    {
        for (final ToolkitListener listener : listeners)
        {
            try
            {
                listener.handleWrite(widget, value);
            }
            catch (final Throwable ex)
            {
                logger.log(Level.WARNING, "Failure when writing " + value + " for " + widget, ex);
            }
        }
    }

    /** Close the toolkit's "window" that displays a model
     *  @param model Model that has been represented in this toolkit
     *  @throws Exception on error
     */
    public void closeWindow(final DisplayModel model) throws Exception
    {
        throw new Exception("Not implemented");
    }

    /** Orderly shutdown */
    public void shutdown()
    {
        throttle.shutdown();
    }
}
