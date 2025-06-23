/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.application.Platform;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.workbench.ApplicationService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@link ActionHandler} implementation for opening a save-and-restore {@link org.phoebus.applications.saveandrestore.model.Node}
 * in the save-and-restore app.
 */
public class OpenNodeActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo actionInfo) {
        OpenNodeAction openNodeActionInfo = (OpenNodeAction) actionInfo;

        Platform.runLater(() -> {
            try {
                String nodeId = openNodeActionInfo.getNodeId();
                final String expanded = MacroHandler.replace(sourceWidget.getMacrosOrProperties(), nodeId);

                ApplicationService.createInstance(SaveAndRestoreApplication.NAME, URI.create("file:/" +
                        URLEncoder.encode(expanded, StandardCharsets.UTF_8) + "?app=saveandrestore&action=" + OpenNodeAction.OPEN_SAR_NODE));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean matches(ActionInfo actionInfo) {
        return actionInfo.getType().equals(OpenNodeAction.OPEN_SAR_NODE);
    }
}
