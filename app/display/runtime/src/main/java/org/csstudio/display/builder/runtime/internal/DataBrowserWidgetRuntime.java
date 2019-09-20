/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.plots.DataBrowserWidget;
import org.csstudio.display.builder.runtime.Messages;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/** Runtime for the {@link DataBrowserWidget}
 *
 *  <p>Adds 'configure' and toolbar entries to context menu.
 *
 *  <p>Updates 'selection_value' from 'selection_value_pv'.
 *  Selection PV must be set when runtime starts.
 *  Can not be changed while running.
 *
 *  @author Megan Grodowitz Initial version
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataBrowserWidgetRuntime  extends WidgetRuntime<DataBrowserWidget>
{
    private class OpenDataBrowserAction extends RuntimeAction
    {
        OpenDataBrowserAction()
        {
            super(Messages.OpenDataBrowser, "/icons/databrowser.png");
        }

        @Override
        public void run()
        {
            widget.runtimePropOpenFull().trigger();
        }
    }

    private final List<RuntimeAction> runtime_actions = new ArrayList<>(3);
    private volatile PV selection_pv = null;
    private volatile WidgetPropertyListener<VType> listener = null;

    @Override
    public void initialize(final DataBrowserWidget widget)
    {
        super.initialize(widget);
        runtime_actions.add(new ConfigureAction("Configure Plot", widget.runtimePropConfigure()));
        runtime_actions.add(new ToggleToolbarAction(widget));
        runtime_actions.add(new OpenDataBrowserAction());
    }

    @Override
    public Collection<RuntimeAction> getRuntimeActions()
    {
        return runtime_actions;
    }

    @Override
    public void start()
    {
        super.start();

        // Write selection (VTable) to PV?
        final String pv_name = widget.propSelectionValuePVName().getValue();
        if (!pv_name.isBlank())
        {
            try
            {
                selection_pv = PVPool.getPV(pv_name);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, widget + " cannot create selection PV '" + pv_name + "'");
            }
            listener = (p, o, value) ->
            {
                try
                {
                    selection_pv.write(value);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot update " + pv_name, ex);
                }
            };
            widget.propSelectionValue().addPropertyListener(listener);
        }
    }

    @Override
    public void stop()
    {
        if (selection_pv != null)
        {
            if (listener != null)
                widget.propSelectionValue().removePropertyListener(listener);
            PVPool.releasePV(selection_pv);
        }
        super.stop();
    }
}
