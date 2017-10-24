/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Information about an action that executes a script
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExecuteScriptActionInfo extends ActionInfo
{
    private final ScriptInfo info;

    /** @param description Action description
     *  @param pv PV name
     *  @param value Value to write
     */
    public ExecuteScriptActionInfo(final String description, final ScriptInfo info)
    {
        super(description);
        this.info = info;
    }

    @Override
    public ActionType getType()
    {
        return ActionType.EXECUTE_SCRIPT;
    }

    /** @return Script info */
    public ScriptInfo getInfo()
    {
        return info;
    }

    @Override
    public String toString()
    {
        if (getDescription().isEmpty())
            return "Execute " + info.getPath();
        else
            return getDescription();
    }
}
