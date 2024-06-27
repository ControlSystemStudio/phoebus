/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.app.actionhandlers;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.actions.OpenFileAction;

import java.util.logging.Level;
import java.util.logging.Logger;


public class OpenFileActionHandler implements ActionHandler {

    private final Logger logger = Logger.getLogger(OpenFileActionHandler.class.getName());

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo pluggableActionInfo) {
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
    public boolean matches(ActionInfo pluggableActionInfo) {
        return pluggableActionInfo.getType().equalsIgnoreCase(OpenFileAction.OPEN_FILE);
    }
}
