/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 * Copyright (C) 2025 Thales.
 */

package org.csstudio.display.builder.runtime.app.actionhandlers;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.actions.WritePVAction;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;


public class WritePVActionHandler implements ActionHandler {
    @Override
    public void handleAction(Widget sourceWidget, ActionInfo pluggableActionInfo) {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(sourceWidget);
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
            if (value.startsWith("="))
            {
                runtime.writePV(pvName, evaluateFormulaPV(value));
            }
            else
            {
                runtime.writePV(pvName,value);
            }
        }
        catch (final Exception ex)
        {
            final String message = "Cannot write '" + pvName + "' = " + value;
            Logger.getLogger(WritePVActionHandler.class.getName()).log(Level.WARNING, message, ex);
            ScriptUtil.showErrorDialog(sourceWidget, message + ".\n\nSee log for details.");
        }
    }

    /**
     * Evaluate a Formula PV expression
     * @param formulaExpression The complete formula expression including =` and `
     * @return The evaluated result as a string
     */
    private String evaluateFormulaPV(final String formulaExpression)
    {
        try
        {
            final PV formulaPV = PVPool.getPV(formulaExpression);

            final VType value = formulaPV.read();

            if (value != null)
            {
                return VTypeHelper.toString(value);
            }
            else
            {
                logger.log(Level.WARNING, "Formula PV '" + formulaExpression + "' returned null value");
                return "<null>";
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error evaluating Formula PV: " + formulaExpression, ex);
            return "<null>";
        }
    }

    @Override
    public boolean matches(ActionInfo pluggableActionInfo)
    {
        return pluggableActionInfo.getType().equalsIgnoreCase(WritePVAction.WRITE_PV);
    }
}
