/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Information about an action that executes an external command
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExecuteCommandActionInfo extends ActionInfo
{
    private final String command;

    /** @param description Action description
     *  @param pv PV name
     *  @param value Value to write
     */
    public ExecuteCommandActionInfo(final String description, final String command)
    {
        super(description);
        this.command = command;
    }

    @Override
    public ActionType getType()
    {
        return ActionType.EXECUTE_SCRIPT;
    }

    /** @return Command */
    public String getCommand()
    {
        return command;
    }

    @Override
    public String toString()
    {
        if (getDescription().isEmpty())
            return "Execute " + command;
        else
            return getDescription();
    }
}
