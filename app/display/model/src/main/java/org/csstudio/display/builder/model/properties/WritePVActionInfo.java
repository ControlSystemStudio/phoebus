/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Information about an action that writes a PV
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WritePVActionInfo extends ActionInfo
{
    private final String pv;
    private final String value;

    /** @param description Action description
     *  @param pv PV name
     *  @param value Value to write
     */
    public WritePVActionInfo(final String description, final String pv, final String value)
    {
        super(description);
        this.pv = pv;
        this.value = value;
    }

    @Override
    public ActionType getType()
    {
        return ActionType.WRITE_PV;
    }

    /** @return PV name */
    public String getPV()
    {
        return pv;
    }

    /** @return Value to write */
    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        if (getDescription().isEmpty())
            return "Write " + value + " to " + pv;
        else
            return getDescription();
    }
}
