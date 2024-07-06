/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.model.spi;

import org.csstudio.display.builder.model.Widget;

/**
 * Interface to define action runtime behavior.
 */
public interface ActionHandler {

    /**
     * Implementation of the action.
     * @param sourceWidget Widget that triggered the action, e.g. action button.
     * @param actionInfo The {@link ActionInfo} object containing action specific information.
     */
    void handleAction(Widget sourceWidget, ActionInfo actionInfo);

    /**
     * Utility needed to determine if the implementation matches the {@link ActionInfo} object.
     * @param actionInfo An {@link ActionInfo} object
     * @return <code>true</code> if the implementation handles action defined in {@link ActionInfo}.
     */
    default boolean matches(ActionInfo actionInfo){
        return false;
    }
}
