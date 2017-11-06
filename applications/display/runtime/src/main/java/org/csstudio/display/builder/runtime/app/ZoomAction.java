/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.representation.javafx.JFXRepresentation;

import javafx.scene.control.ComboBox;

/** Toolbar button for zooming the display
 *  @author Kay Kasemir
 */
public class ZoomAction extends ComboBox<String>
{
    private boolean updating = false;

    public ZoomAction(final DisplayRuntimeInstance instance)
    {
        setEditable(true);
        setPrefWidth(100.0);
        getItems().addAll(JFXRepresentation.ZOOM_LEVELS);
        setValue(JFXRepresentation.DEFAULT_ZOOM_LEVEL);
        setOnAction(event -> zoom(instance.getRepresentation()));
    }

    private void zoom(final JFXRepresentation representation)
    {
        if (updating)
            return;
        updating = true;
        try
        {
            setValue(representation.requestZoom(getValue()));
        }
        finally
        {
            updating = false;
        }
    }
}
