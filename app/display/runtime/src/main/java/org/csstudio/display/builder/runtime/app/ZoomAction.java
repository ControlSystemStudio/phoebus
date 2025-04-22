/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.runtime.Preferences;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;

/** Toolbar button for zooming the display
 *  @author Kay Kasemir
 */
public class ZoomAction extends ComboBox<String>
{
    private boolean updating = false;

    /** @param instance {@link DisplayRuntimeInstance} */
    public ZoomAction(final DisplayRuntimeInstance instance)
    {
        setEditable(true);
        setPrefWidth(100.0);
        getItems().addAll(JFXRepresentation.ZOOM_LEVELS);
        setValue(JFXRepresentation.DEFAULT_ZOOM_LEVEL);
        // For Ctrl-Wheel zoom gesture
        instance.getRepresentation().setZoomListener(txt ->
        {
            getSelectionModel().clearSelection();
            getEditor().setText(txt);
        });
        setOnAction(event -> zoom(instance.getRepresentation()));

        // Apply default zoom factor from settings.ini
        Platform.runLater(() ->
        {
            String zoom = String.format("%d %%", Preferences.default_zoom_factor);
            setValue(zoom);
            // Invoke zoom changed handler
            getOnAction().handle(null);
        });
    }

    private void zoom(final JFXRepresentation representation)
    {
        if (updating)
            return;
        // Request zoom, get actual zoom level.
        final String before = getValue();
        if (before == null)
            return;
        final String actual = representation.requestZoom(before);

        // For "100 %" request, actual is the same.
        // For "100" request, actual would be the correct "100 %" format.
        // For "All" request, actual is the actual level like "71 %".
        //
        // When updating the combo box to the actual zoom level,
        // the 'updating' flag should avoid recursion.
        // Before Java 9, was OK to do the following right now.
        // With Java 9 there can be multiple events
        // as the item list is checked for the value.
        // This results in IndexOutOfBoundException and calls with getValue() == null.
        // Delaying the update into another UI tick avoids the problem.
        Platform.runLater(() ->
        {
            updating = true;
            setValue(actual);
            updating = false;
        });
    }
}
