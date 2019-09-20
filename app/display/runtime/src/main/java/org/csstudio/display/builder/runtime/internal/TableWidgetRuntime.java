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
import org.csstudio.display.builder.model.widgets.TableWidget;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.epics.vtype.VType;

/** Runtime for the TableWidget
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TableWidgetRuntime extends WidgetRuntime<TableWidget>
{
    private final List<RuntimeAction> runtime_actions = new ArrayList<>(1);

    private volatile RuntimePV selection_pv = null;

    private final WidgetPropertyListener<VType> selection_listener = (prop, old, value) ->
    {
        final RuntimePV pv = selection_pv;
        if (pv == null)
            return;
        try
        {
            pv.write(value);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error writing " + value + " to " + pv, ex);
        }
    };

    @Override
    public void initialize(final TableWidget widget)
    {
        super.initialize(widget);
        if (widget.propEditable().getValue())
            runtime_actions.add(new ToggleToolbarAction(widget));
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

        // Connect selection info PV
        final String selection_pv_name = widget.propSelectionPV().getValue();
        if (! selection_pv_name.isEmpty())
        {
            logger.log(Level.FINER, "Connecting {0} to {1}",  new Object[] { widget, selection_pv_name });
            try
            {
                final RuntimePV pv = PVFactory.getPV(selection_pv_name);
                addPV(pv);
                widget.runtimePropSelection().addPropertyListener(selection_listener);
                selection_pv = pv;
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error connecting PV " + selection_pv_name, ex);
            }
        }
    }

    @Override
    public void stop()
    {
        // Disconnect selection info PV
        final RuntimePV pv = selection_pv;
        selection_pv = null;
        if (pv != null)
        {
            widget.runtimePropSelection().removePropertyListener(selection_listener);
            removePV(pv);
            PVFactory.releasePV(pv);
        }

        super.stop();
    }
}
