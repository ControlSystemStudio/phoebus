/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.actionhandlers;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.representation.javafx.actions.OpenFileAction;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.runtime.spi.ActionHandler;

import java.util.logging.Level;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

public class OpenFileActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, PluggableActionInfo pluggableActionInfo) {
        OpenFileAction action = (OpenFileAction) pluggableActionInfo;
        if (action.getFile().isEmpty())
        {
            logger.log(Level.WARNING, "Action without file: {0}", action);
            return;
        }
        try
        {
            final String resolved_name = ActionUtil.resolve(sourceWidget, action.getFile());
            final DisplayModel top_model = sourceWidget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(top_model);
            toolkit.execute(() ->
            {
                try
                {
                    toolkit.openFile(resolved_name);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot open " + action, ex);
                    toolkit.showErrorDialog(sourceWidget, "Cannot open " + resolved_name);
                }
            });
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Error handling " + action, ex);
            ScriptUtil.showErrorDialog(sourceWidget, "Cannot open " + action.getFile() + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(PluggableActionInfo pluggableActionInfo) {
        return pluggableActionInfo instanceof OpenFileAction;
    }
}
