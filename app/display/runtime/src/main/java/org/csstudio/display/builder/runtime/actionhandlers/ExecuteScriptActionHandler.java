/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.actionhandlers;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.csstudio.display.builder.representation.javafx.actions.ExecuteScriptAction;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.runtime.spi.ActionHandler;

import java.util.logging.Level;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

public class ExecuteScriptActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, PluggableActionInfo pluggableActionInfo) {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(sourceWidget);
        final ExecuteScriptAction action = (ExecuteScriptAction)pluggableActionInfo;
        try
        {
            runtime.executeScriptAction(action);
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, pluggableActionInfo + " failed", ex);
            ScriptUtil.showErrorDialog(sourceWidget, "Cannot execute " + action.getScriptInfo().getPath() + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(PluggableActionInfo pluggableActionInfo) {
        return pluggableActionInfo instanceof ExecuteScriptAction;
    }
}
