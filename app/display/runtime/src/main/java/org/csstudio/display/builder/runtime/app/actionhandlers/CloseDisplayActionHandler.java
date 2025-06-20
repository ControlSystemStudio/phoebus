/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.app.actionhandlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.actions.CloseDisplayAction;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitRepresentation;

public class CloseDisplayActionHandler implements ActionHandler {

    private final Logger logger = Logger.getLogger(CloseDisplayActionHandler.class.getName());

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo pluggableActionInfo) {
        CloseDisplayAction action = (CloseDisplayAction) pluggableActionInfo;

        try
        {
            final DisplayModel model = sourceWidget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(model);
            toolkit.closeWindow(model);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, action+" failed. Cannot close display", ex);
        }
    }

    @Override
    public boolean matches(ActionInfo pluggableActionInfo) {
        return pluggableActionInfo.getType().equalsIgnoreCase(CloseDisplayAction.CLOSE_DISPLAY);
    }
}
