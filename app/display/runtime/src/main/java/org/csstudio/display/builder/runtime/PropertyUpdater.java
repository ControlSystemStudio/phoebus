/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;

/** RuntimePVListener that updates a property with received VType
 *  @author Kay Kasemir
 */
public class PropertyUpdater implements RuntimePVListener
{
    private final WidgetProperty<VType> property;

    /** @param property Widget property to update with values received from PV */
    public PropertyUpdater(final WidgetProperty<VType> property)
    {
        this.property = property;
        // Send initial 'disconnected' update so widget shows
        // disconnected state until the first value arrives
        disconnected(null);
    }

    @Override
    public void valueChanged(final RuntimePV pv, final VType value)
    {
        property.setValue(value);
    }

    @Override
    public void disconnected(final RuntimePV pv)
    {
        property.setValue(null);
    }
}