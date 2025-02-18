/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import javafx.application.Platform;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.applications.saveandrestore.FilterViewApplication;
import org.phoebus.framework.workbench.ApplicationService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@link ActionHandler} implementation for opening a save-and-restore {@link org.phoebus.applications.saveandrestore.model.search.Filter}
 * in the {@link FilterViewApplication} app.
 */
public class OpenFilterActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo actionInfo) {
        OpenFilterAction openFilterAction = (OpenFilterAction) actionInfo;

        Platform.runLater(() -> {
            try {
                ApplicationService.createInstance(FilterViewApplication.NAME, URI.create("file:/" +
                        URLEncoder.encode(openFilterAction.getFilterId(), StandardCharsets.UTF_8)));
            } catch (Exception e) {

                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean matches(ActionInfo actionInfo) {
        return actionInfo.getType().equals(OpenFilterAction.OPEN_SAR_FILTER);
    }
}
