/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.csstudio.display.builder.model.Messages;

/** Information about actions
 *
 *  @author Kay Kasemir
 */
public class ActionInfos
{
    /** Empty widget actions */
    public static final ActionInfos EMPTY = new ActionInfos(Collections.emptyList());

    final private List<ActionInfo> actions;
    final private boolean execute_as_one;

    /** @param actions List of action infos */
    public ActionInfos(final List<ActionInfo> actions)
    {
        this(actions, false);
    }

    /** @param actions List of action infos
     *  @param execute_as_one Execute all in order?
     */
    public ActionInfos(final List<ActionInfo> actions, final boolean execute_as_one)
    {
        this.actions = Collections.unmodifiableList(actions);
        this.execute_as_one = execute_as_one;
    }

    /** @return List of actions */
    public List<ActionInfo> getActions()
    {
        return actions;
    }

    /** @return Should all actions on list be executed as one, or are they separate actions? */
    public boolean isExecutedAsOne()
    {
        return execute_as_one;
    }

    /** @param actions Actions to represent
     *  @return String representation
     */
    public static String toString(final List<ActionInfo> actions)
    {
        if (actions.isEmpty())
            return  MessageFormat.format(Messages.Actions_N_Fmt, 0);
        if (actions.size() == 1)
            return actions.get(0).getDescription().isEmpty() ?
                    MessageFormat.format(Messages.Actions_N_Fmt, 1) :
                    actions.get(0).getDescription();
        return MessageFormat.format(Messages.Actions_N_Fmt, actions.size());
    }

    @Override
    public String toString()
    {
        return toString(actions);
    }
}
