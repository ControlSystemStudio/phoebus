/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.actions;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.builder.model.spi.ActionInfo;

public class OpenFilterActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo actionInfo) {

    }

    @Override
    public boolean matches(ActionInfo actionInfo) {
        return actionInfo.getType().equals(OpenFilterAction.OPEN_SAR_FILTER);
    }
}
