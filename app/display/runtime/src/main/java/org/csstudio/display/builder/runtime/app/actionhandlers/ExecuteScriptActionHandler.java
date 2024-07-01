/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.app.actionhandlers;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.actions.ExecuteScriptAction;

import java.util.logging.Level;
import java.util.logging.Logger;


public class ExecuteScriptActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo actionInfo) {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(sourceWidget);
        final ExecuteScriptAction action = (ExecuteScriptAction)actionInfo;
        try
        {
            runtime.executeScriptAction(action);
        }
        catch (final Throwable ex)
        {
            Logger.getLogger(ExecuteScriptActionHandler.class.getName()).log(Level.WARNING, actionInfo + " failed", ex);
            ScriptUtil.showErrorDialog(sourceWidget, "Cannot execute " + action.getScriptInfo().getPath() + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(ActionInfo actionInfo) {
        return actionInfo.getType().equalsIgnoreCase(ExecuteScriptAction.EXECUTE_SCRIPT) ||
                actionInfo.getType().equalsIgnoreCase(ExecuteScriptAction.EXECUTE_JAVASCRIPT) ||
                actionInfo.getType().equalsIgnoreCase(ExecuteScriptAction.EXECUTE_PYTHONSCRIPT);
    }
}
