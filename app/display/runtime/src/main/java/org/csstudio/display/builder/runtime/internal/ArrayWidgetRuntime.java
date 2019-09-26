/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.ArrayPVDispatcher;
import org.csstudio.display.builder.runtime.pv.RuntimePV;

/** Runtime for array widget
 *
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ArrayWidgetRuntime extends WidgetRuntime<ArrayWidget>
{
    private ArrayPVDispatcher dispatcher;
    private CopyOnWriteArrayList<String> pvnames = new CopyOnWriteArrayList<>();
    private String pvid;

    private final ArrayPVDispatcher.Listener assign_pv_names = new ArrayPVDispatcher.Listener()
    {
        @Override
        public void arrayChanged(List<RuntimePV> element_pvs)
        {
            pvnames.clear();
            for (RuntimePV pv : element_pvs)
                pvnames.add(pv.getName());
            setPVNames(0, new ArrayList<>(widget.runtimeChildren().getValue()));
        }
    };

    private final WidgetPropertyListener<List<Widget>> children_listener = (prop, removed, added) ->
    {
        if (removed != null)
            for (Widget child : removed)
            {
                RuntimeUtil.stopRuntime(child);

                final Optional<WidgetProperty<Object>> pvname = child.checkProperty("pv_name");
                if (!pvname.isPresent())
                    return;
                try
                {
                    pvname.get().setValueFromObject(pvname.get().getDefaultValue());
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Unable to clear pv name of " + child, ex);
                }
            }
        if (added != null)
        {
            setPVNames(this.widget.runtimeChildren().getValue().size() - added.size(), added);
            for (Widget child : added)
                RuntimeUtil.startRuntime(child);
        }
    };

    @Override
    public void initialize(final ArrayWidget widget)
    {
        super.initialize(widget);
        pvid = "elem" + widget.getID() + "_";
    }

    @Override
    public void start()
    {
        super.start();
        RuntimePV pv = getPrimaryPV().orElse(null);
        if (pv != null)
            dispatcher = new ArrayPVDispatcher(pv, pvid, assign_pv_names);
        for (final Widget child : widget.runtimeChildren().getValue())
            RuntimeUtil.startRuntime(child);
        widget.runtimeChildren().addPropertyListener(children_listener);
    }

    @Override
    public void stop()
    {
        widget.runtimeChildren().removePropertyListener(children_listener);
        for (final Widget child : widget.runtimeChildren().getValue())
            RuntimeUtil.stopRuntime(child);
        if (dispatcher != null)
            dispatcher.close();
        super.stop();
    }

    private void setPVNames(int i, List<Widget> added)
    {
        for (Widget widget : added)
            if (i < pvnames.size())
            {
                setPVName(widget, pvnames.get(i));
                i++;
            }
    }

    private void setPVName(final Widget widget, final String name)
    {
        final Optional<WidgetProperty<String>> pvname = widget.checkProperty("pv_name");
        if (pvname.isPresent())
            pvname.get().setValue(name);
        else if (widget instanceof GroupWidget)
        {   // For group widget, set PV name of every group member
            for (Widget child : ((GroupWidget) widget).runtimeChildren().getValue())
                setPVName(child, name);
        }
    }
}
