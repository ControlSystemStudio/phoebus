/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ColorMap;
import org.csstudio.display.builder.model.properties.ColorMapWidgetProperty;
import org.csstudio.display.builder.model.properties.PredefinedColorMaps;
import org.csstudio.display.builder.representation.javafx.ColorMapDialog;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between a color map property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class ColorMapPropertyBinding
    extends WidgetPropertyBinding<Button, ColorMapWidgetProperty>
{
    private final WidgetPropertyListener<ColorMap> model_listener = (p, o, n) ->
    {
        final ColorMap map = widget_property.getValue();
        if (map instanceof PredefinedColorMaps.Predefined)
            jfx_node.setText(((PredefinedColorMaps.Predefined)map).getDescription());
        else
            jfx_node.setText(Messages.ColorMap_Custom);
    };

    private final EventHandler<ActionEvent> edit_colormap = event ->
    {
        final ColorMapDialog dialog = new ColorMapDialog(widget_property.getValue(), jfx_node);
        final Optional<ColorMap> result = dialog.showAndWait();
        if (result.isPresent())
        {
            undo.execute(new SetWidgetPropertyAction<>(widget_property, result.get()));
            if (! other.isEmpty())
            {
                final String path = widget_property.getPath();
                for (Widget w : other)
                {
                    final ColorMapWidgetProperty other_prop = (ColorMapWidgetProperty) w.getProperty(path);
                    undo.execute(new SetWidgetPropertyAction<>(other_prop, result.get()));
                }
            }
        }
    };

    public ColorMapPropertyBinding(final UndoableActionManager undo,
                                   final Button field,
                                   final ColorMapWidgetProperty widget_property,
                                   final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(edit_colormap);
        model_listener.propertyChanged(widget_property, null, null);
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}
