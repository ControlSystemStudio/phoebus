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
import org.csstudio.display.actions.WritePVAction;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

import java.util.logging.Level;
import java.util.logging.Logger;


public class WritePVActionHandler implements ActionHandler {
    @Override
    public void handleAction(Widget sourceWidget, ActionInfo pluggableActionInfo) {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(sourceWidget);
        // System.out.println(action.getDescription() + ": Set " + action.getPV() + " = " + action.getValue());
        final MacroValueProvider macros = sourceWidget.getMacrosOrProperties();
        WritePVAction writePVAction = (WritePVAction)pluggableActionInfo;
        String pvName = writePVAction.formatPv(sourceWidget);
        String value = writePVAction.formatValue(sourceWidget);
        try
        {
            runtime.writePV(pvName,value);
        }
        catch (final Exception ex)
        {
            final String message = "Cannot write '" + pvName + "' = " + value;
            Logger.getLogger(WritePVActionHandler.class.getName()).log(Level.WARNING, message, ex);
            ScriptUtil.showErrorDialog(sourceWidget, message + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(ActionInfo pluggableActionInfo) {
        return pluggableActionInfo.getType().equalsIgnoreCase(WritePVAction.WRITE_PV);
    }
}
