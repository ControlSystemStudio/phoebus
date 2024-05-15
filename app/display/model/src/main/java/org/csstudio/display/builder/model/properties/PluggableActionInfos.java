/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * Information about {@link PluggableActionInfo}s
 *
 */
public class PluggableActionInfos
{
    /** Empty widget actions */
    public static final PluggableActionInfos EMPTY = new PluggableActionInfos(Collections.emptyList());

    final private List<PluggableActionInfo> actions;
    final private boolean executeAsOne;

    /** @param actions List of action infos */
    public PluggableActionInfos(final List<PluggableActionInfo> actions)
    {
        this(actions, false);
    }

    /** @param actions List of action infos
     *  @param executeAsOne Execute all in order?
     */
    public PluggableActionInfos(final List<PluggableActionInfo> actions, final boolean executeAsOne)
    {
        this.actions = Collections.unmodifiableList(actions);
        this.executeAsOne = executeAsOne;
    }

    /** @return List of actions */
    public List<PluggableActionInfo> getActions()
    {
        return actions;
    }

    /** @return Should all actions on list be executed as one, or are they separate actions? */
    public boolean isExecutedAsOne()
    {
        return executeAsOne;
    }

    /** @param actions Actions to represent
     *  @return String representation
     */
    public static String toString(final List<PluggableActionInfo> actions)
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
