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
import org.csstudio.display.actions.OpenWebPageAction;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

import java.util.logging.Level;
import java.util.logging.Logger;


public class OpenWebPageActionHandler implements ActionHandler {

    private final Logger logger = Logger.getLogger(OpenWebPageActionHandler.class.getName());

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo pluggableActionInfo) {
        OpenWebPageAction action = (OpenWebPageAction)pluggableActionInfo;
        final String url = action.getURL();
        if (url.trim().isEmpty())
        {
            logger.log(Level.WARNING, "Open Web Page Action: No URL: {0}", action);
            return;
        }
        else if (!url.trim().equals(url)) {
            logger.log(Level.WARNING, "Open Web Page Action: URL has leading and/or trailing whitespace: \"{0}\"", url);
            return;
        }
        else if (!(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file:"))) {
            logger.log(Level.WARNING, "Open Web Page Action: URL does not start with one of the URL schemes \"http://\", \"https://\", or \"file:\": \"{0}\"", url);
            return;
        }
        try
        {
            final String escapedExpandedURL;
            {
                final MacroValueProvider macros = sourceWidget.getMacrosOrProperties();
                final String expandedURL = MacroHandler.replace(macros, url);
                escapedExpandedURL = expandedURL.replace(" ", "%20");
            }

            final ToolkitRepresentation<Object, Object> toolkit;
            {
                final DisplayModel top_model = sourceWidget.getTopDisplayModel();
                toolkit = ToolkitRepresentation.getToolkit(top_model);
            }
            toolkit.execute(() ->
            {
                try
                {
                    toolkit.openWebBrowser(escapedExpandedURL);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot open " + action, ex);
                    toolkit.showErrorDialog(sourceWidget, "Cannot open " + escapedExpandedURL);
                }
            });
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Error handling " + action, ex);
            ScriptUtil.showErrorDialog(sourceWidget, "Cannot open " + url + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(ActionInfo pluggableActionInfo) {
        return pluggableActionInfo.getType().equalsIgnoreCase(OpenWebPageAction.OPEN_WEBPAGE);
    }
}
