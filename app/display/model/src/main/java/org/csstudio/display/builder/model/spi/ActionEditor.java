/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.model.spi;

import javafx.scene.Node;
import org.csstudio.display.builder.model.Widget;

/**
 * Interface for an {@link ActionInfo} subclass editor.
 * Implementations shall have only the default constructor as they are discovered over SPI.
 * To configure the underlying UI, clients <i>must</i> call {@link #configure(Widget, ActionInfo)} in order to
 * be able to make use of the {@link ActionEditor}.
 */
public interface ActionEditor {

    /**
     * @param type String uniquely identifying an action
     * @return <code>true</code> if the implementation supports the action type.
     */
    boolean matchesAction(String type);

    /**
     * @return An {@link ActionInfo} object holding data rendered in the editor UI and potentially
     * edited by user.
     */
    ActionInfo getActionInfo();

    /**
     *
     * @return Parent {@link Node} of the editor UI. Will be rendered by the actions dialog.
     */
    Node getEditorUi();

    /**
     * Must be called to configure the {@link ActionEditor}.
     * @param widget The {@link Widget} associated with the action property.
     * @param actionInfo {@link ActionInfo} describing the action.
     */
    void configure(Widget widget, ActionInfo actionInfo);
}
