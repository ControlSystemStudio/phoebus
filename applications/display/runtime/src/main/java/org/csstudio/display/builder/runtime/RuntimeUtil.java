/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.util.NamedDaemonPool;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.runtime.script.internal.ScriptSupport;

/** Runtime Helper
 *
 *  <p>Model is unaware of representation and runtime,
 *  but runtime needs to attach certain pieces of information
 *  to the model.
 *  This is done via the 'user data' support of the {@link Widget}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RuntimeUtil
{
    private static final ExecutorService executor = NamedDaemonPool.createThreadPool("DisplayRuntime");

    private static final ToolkitListener toolkit_listener = new ToolkitListener()
    {
        @Override
        public void handleAction(final Widget widget, final ActionInfo action)
        {
            ActionUtil.handleAction(widget, action);
        }

        @Override
        public void handleWrite(final Widget widget, final Object value)
        {
            final WidgetRuntime<Widget> runtime = getRuntime(widget);
            if (runtime == null)
                logger.log(Level.WARNING, "Widget " + widget + " has no runtime for writing " + value);
            else
                runtime.writePrimaryPV(value);
        }
    };

    private static final WidgetPropertyListener<List<Widget>> children_listener = (prop, removed, added) ->
    {
        if (removed != null)
            for (Widget child : removed)
                stopRuntime(child);

        if (added != null)
            for (Widget child : added)
                startRuntime(child);
    };

    /** Connect runtime listener to toolkit
     *  @param toolkit Toolkit that runtime needs to monitor
     */
    public static void hookRepresentationListener(final ToolkitRepresentation<?,?> toolkit)
    {
        // For representation in an RCP view or Phoebus dock item, a "new" display
        // may actually just bring an existing display back to the front.
        // In that case, prevent double-subscription by first trying to
        // remove the listener.
        toolkit.removeListener(toolkit_listener);
        toolkit.addListener(toolkit_listener);
    }

    /** @return {@link ExecutorService} that should be used for runtime-related background tasks
     */
    public static ExecutorService getExecutor()
    {
        return executor;
    }

    /** Obtain script support
     *
     *  <p>Script support is associated with the top-level display model
     *  and initialized on first access, i.e. each display has its own
     *  script support. Embedded displays use the script support of
     *  their parent display.
     *
     *  @param widget Widget
     *  @return {@link ScriptSupport} for the widget's top-level display model
     *  @throws Exception on error
     */
    public static ScriptSupport getScriptSupport(final Widget widget) throws Exception
    {
        final DisplayModel model = widget.getTopDisplayModel();
        // During display startup, several widgets will concurrently request script support.
        // Assert that only one ScriptSupport is created.
        // Synchronizing on the model seems straight forward because this is about script support
        // for this specific model, but don't want to conflict with other code that may eventually
        // need to lock the model for other reasons.
        // So sync'ing on the ScriptSupport class
        synchronized (ScriptSupport.class)
        {
            ScriptSupport scripting = model.getUserData(Widget.USER_DATA_SCRIPT_SUPPORT);
            if (scripting == null)
            {
                // This takes about 3 seconds
                final long start = System.currentTimeMillis();
                scripting = new ScriptSupport();
                final long elapsed = System.currentTimeMillis() - start;
                logger.log(Level.FINE, "ScriptSupport created for {0} by {1} in {2} ms", new Object[] { model, widget, elapsed });
                model.setUserData(Widget.USER_DATA_SCRIPT_SUPPORT, scripting);
            }
            return scripting;
        }
    }

    /** @param widget Widget
     *  @return {@link WidgetRuntime} of the widget or <code>null</code>
     */
    public static <MW extends Widget> WidgetRuntime<MW> getRuntime(final MW widget)
    {
        return widget.getUserData(Widget.USER_DATA_RUNTIME);
    }

    /** Create and start runtime for a widget
     *
     *  <p>Container widgets are responsible
     *  for starting their child widget runtimes,
     *  typically after handling their own startup.
     *
     *  @param widget {@link Widget}
     */
    public static void startRuntime(final Widget widget)
    {
        try
        {
            final WidgetRuntime<Widget> runtime = WidgetRuntimeFactory.INSTANCE.createRuntime(widget);
            runtime.start();
        }
        catch (final Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot start runtime for " + widget, ex);
        }
    }

    /** Stop runtime for a widget
     *
     *  <p>Container widgets are responsible
     *  for stopping their child widget runtimes,
     *  typically before handling their own shutdown.
     *
     *  @param widget {@link Widget}
     */
    public static void stopRuntime(final Widget widget)
    {
        final WidgetRuntime<?> runtime = getRuntime(widget);
        if (runtime != null)
            runtime.stop();
    }

    /** Start runtime of all child widgets
     *
     *  <p>Also starts/stops added/removed child widgets
     *
     * @param children
     */
    public static void startChildRuntimes(final ChildrenProperty children)
    {
        for (Widget child : children.getValue())
            RuntimeUtil.startRuntime(child);
        children.addPropertyListener(children_listener);
    }

    /** Stop runtime of all child widgets
     *
     *  <p>Also un-subscribes from child widget additions/removals.
     *
     * @param children
     */
    public static void stopChildRuntimes(final ChildrenProperty children)
    {
        children.removePropertyListener(children_listener);
        for (Widget child : children.getValue())
            RuntimeUtil.stopRuntime(child);
    }
}
