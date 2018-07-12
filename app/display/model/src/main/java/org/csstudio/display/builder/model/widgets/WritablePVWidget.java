/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVWritable;

import java.util.List;

import org.csstudio.display.builder.model.WidgetProperty;

/** Base class for all widgets that write to a primary PV.
 *
 *  <p>Default WidgetRuntime will connect PV to "pv_name",
 *  update "pv_value" with received updates,
 *  and update "pv_writable" to reflect write access.
 *
 *  @author Kay Kasemir
 */
public class WritablePVWidget extends PVWidget
{
    private volatile WidgetProperty<Boolean> pv_writable;

    /** @param type Widget type. */
    public WritablePVWidget(final String type)
    {
        super(type);
    }

    /** @param type Widget type.
     *  @param default_width Default widget width.
     *  @param default_height Default widget height.
     */
    public WritablePVWidget(final String type, final int default_width, final int default_height)
    {
        super(type, default_width, default_height);
    }

    @Override
    protected void defineProperties (final List<WidgetProperty<?>> properties )
    {
        super.defineProperties(properties);
        // Start 'true' for editor to enable widgets.
        // Runtime will set false when PVs are not writable.
        properties.add(pv_writable = runtimePropPVWritable.createProperty(this, true));
    }

    /** @return 'pv_writable' property */
    public final WidgetProperty<Boolean> runtimePropPVWritable()
    {
        return pv_writable;
    }
}
