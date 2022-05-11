/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.net.URL;
import java.util.Collections;
import java.util.Objects;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.phoebus.framework.macros.Macros;

/** Information about an action
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public abstract class ActionInfo
{
    /** Description of a type of action: Name, icon */
    public enum ActionType
    {
        /** Open a display */
        OPEN_DISPLAY(Messages.ActionOpenDisplay, "/icons/open_display.png"),
        /** Write fixed value to a PV */
        WRITE_PV(Messages.ActionWritePV, "/icons/write_pv.png"),
        /** Execute a script */
        EXECUTE_SCRIPT(Messages.ActionExecuteScript, "/icons/execute_script.png"),
        /** Execute external command */
        EXECUTE_COMMAND(Messages.ActionExecuteCommand, "/icons/execute_script.png"),
        /** Open a file with its associated tool */
        OPEN_FILE(Messages.ActionOpenFile, "/icons/open_file.png"),
        /** Open a wen page in system browser */
        OPEN_WEBPAGE(Messages.ActionOpenWebPage, "/icons/web_browser.png");

        private final String name, icon_path;

        private ActionType(final String name, final String icon_path)
        {
            this.name = name;
            this.icon_path = icon_path;
        }

        /** @return URL of icon */
        public URL getIconURL()
        {
            return ActionInfo.class.getResource(icon_path);
        }

        @Override
        public String toString()
        {
            return name;
        }
    };

    /** Create action with generic values
     *  @param type Action type
     *  @return Action of that type
     */
    public static ActionInfo createAction(final ActionType type)
    {
        switch (type)
        {
        case OPEN_DISPLAY:
            return new OpenDisplayActionInfo(type.toString(), "", new Macros(), Target.REPLACE);
        case WRITE_PV:
            return new WritePVActionInfo(type.toString(), "$(pv_name)", "0");
        case EXECUTE_SCRIPT:
            return new ExecuteScriptActionInfo(type.toString(),
                                               new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON,
                                                              ScriptInfo.EXAMPLE_PYTHON,
                                                              false,
                                                              Collections.emptyList()));
        case EXECUTE_COMMAND:
            return new ExecuteCommandActionInfo(type.toString(), "some_command");
        case OPEN_FILE:
            return new OpenFileActionInfo(type.toString(), "");
        case OPEN_WEBPAGE:
            return new OpenWebpageActionInfo(type.toString(), "");
        default:
            throw new IllegalStateException("Unknown type " + type);
        }
    }

    private final String description;

    /** @param description Action description */
    public ActionInfo(final String description)
    {
        this.description = Objects.requireNonNull(description);
    }

    /** @return Type info */
    abstract public ActionType getType();

    /** @return Action description */
    public String getDescription()
    {
        return description;
    }
}
