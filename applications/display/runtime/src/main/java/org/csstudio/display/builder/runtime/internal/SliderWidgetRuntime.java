/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.RuntimeEventProperty;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.builder.model.widgets.ScrollBarWidget;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;

/** Runtime for the {@link ScaledSliderWidget} and {@link ScrollBarWidget}
 *
 *  <p>Adds runtime action to 'configure'.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SliderWidgetRuntime extends WidgetRuntime<Widget>
{
    private final List<RuntimeAction> runtime_actions = new ArrayList<>(1);

    @Override
    public void initialize(final Widget widget)
    {
        super.initialize(widget);
        // Locate "configure" property by description
        // to support both ScaledSliderWidget and ScrollBarWidget
        final RuntimeEventProperty configure_prop = (RuntimeEventProperty)
                widget.getProperty(CommonWidgetProperties.runtimePropConfigure);
        runtime_actions.add(new ConfigureAction("Configure Slider", configure_prop));
    }

    @Override
    public Collection<RuntimeAction> getRuntimeActions()
    {
        return runtime_actions;
    }
}
