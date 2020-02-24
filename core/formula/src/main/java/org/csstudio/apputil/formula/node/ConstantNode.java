/*******************************************************************************
 * Copyright (c) 2010-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** One computational node.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConstantNode implements Node
{
    final VType value;

    public ConstantNode(final double value)
    {
        this.value = VDouble.of(value, Alarm.none(), Time.now(), Display.none());
    }

    public ConstantNode(final String value)
    {
        this.value = VString.of(value, Alarm.none(), Time.now());
    }

    @Override
    public VType eval()
    {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final Node node)
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final String node)
    {
        return false;
    }

    @Override
    public String toString()
    {
        if (value instanceof VString)
            return "\"" + VTypeHelper.toString(value).replace("\"", "\\\"") + "\"";
        else
            return Double.toString(VTypeHelper.toDouble(value));
    }
}
