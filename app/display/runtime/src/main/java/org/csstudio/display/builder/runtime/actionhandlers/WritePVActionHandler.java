/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.actionhandlers;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.csstudio.display.builder.representation.javafx.actions.WritePVAction;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.runtime.spi.ActionHandler;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

import java.util.logging.Level;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

public class WritePVActionHandler implements ActionHandler {
    @Override
    public void handleAction(Widget sourceWidget, PluggableActionInfo pluggableActionInfo) {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(sourceWidget);
        // System.out.println(action.getDescription() + ": Set " + action.getPV() + " = " + action.getValue());
        final MacroValueProvider macros = sourceWidget.getMacrosOrProperties();
        WritePVAction writePVAction = (WritePVAction)pluggableActionInfo;
        String pvName = writePVAction.getPV(), value = writePVAction.getValue();
        try
        {
            pvName = MacroHandler.replace(macros, pvName);
        }
        catch (Exception ignore)
        {
            // NOP
        }
        try
        {
            value = MacroHandler.replace(macros, value);
        }
        catch (Exception ignore)
        {
            // NOP
        }
        try
        {
            runtime.writePV(pvName,value);
        }
        catch (final Exception ex)
        {
            final String message = "Cannot write '" + pvName + "' = " + value;
            logger.log(Level.WARNING, message, ex);
            ScriptUtil.showErrorDialog(sourceWidget, message + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(PluggableActionInfo pluggableActionInfo) {
        return pluggableActionInfo instanceof WritePVAction;
    }
}
