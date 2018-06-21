/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.util.Objects;

import org.csstudio.display.builder.model.Messages;
import org.phoebus.framework.macros.Macros;

/** Information about an action that opens a display
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenDisplayActionInfo extends ActionInfo
{
    public static enum Target
    {
        /** Replace current display */
        REPLACE(Messages.Target_Replace),

        /** Open a new tab in existing window */
        TAB(Messages.Target_Tab),

        /** Open a new window */
        WINDOW(Messages.Target_Window),

        /** Open standalone window
         *  @deprecated Was only used in RCP version.
         */
        @Deprecated
        STANDALONE(Messages.Target_Standalone);

        private final String name;

        private Target(final String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    private final String file;
    private final Macros macros;
    private final Target target;
    private final String pane;

    /** @param description Action description
     *  @param file Path to the display
     *  @param macros Macros
     *  @param target Where to show the display
     */
    public OpenDisplayActionInfo(final String description, final String file, final Macros macros, final Target target)
    {
        this(description, file, macros, target, "");
    }

    /** @param description Action description
     *  @param file Path to the display
     *  @param macros Macros
     *  @param target Where to show the display
     *  @param pane Pane in which to open (for target==TAB)
     */
    public OpenDisplayActionInfo(final String description, final String file, final Macros macros, final Target target, final String pane)
    {
        super(description);
        this.file = Objects.requireNonNull(file);
        this.macros = macros;
        this.target = target;
        this.pane = pane;
    }

    @Override
    public ActionType getType()
    {
        return ActionType.OPEN_DISPLAY;
    }

    /** @return Path to file (may also be URL, and contain macros) to the display */
    public String getFile()
    {
        return file;
    }

    /** @return Macros */
    public Macros getMacros()
    {
        return macros;
    }

    /** @return Where to show the display */
    public Target getTarget()
    {
        return target;
    }

    /** @return Pane in which to open (for target == TAB) */
    public String getPane()
    {
        return pane;
    }

    @Override
    public String toString()
    {
        if (getDescription().isEmpty())
            return "Open " + file;
        else
            return getDescription();
    }
}
