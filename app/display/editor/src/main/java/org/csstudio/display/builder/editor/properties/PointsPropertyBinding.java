/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.SetWidgetPointsAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.properties.PointsWidgetProperty;
import org.csstudio.display.builder.representation.javafx.PointsDialog;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between a macro property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class PointsPropertyBinding
    extends WidgetPropertyBinding<Button, PointsWidgetProperty>

{
    private WidgetPropertyListener<Points> model_listener = (p, o, n) ->
    {
        jfx_node.setText(MessageFormat.format(Messages.PointCount_Fmt, widget_property.getValue().size()));
    };

    private EventHandler<ActionEvent> actionHandler = event ->
    {
        final PointsDialog dialog = new PointsDialog(widget_property.getValue(), jfx_node);
        final Optional<Points> result = dialog.showAndWait();
        if (result.isPresent())
        {
            undo.execute(new SetWidgetPointsAction(widget_property, result.get()));
            if (! other.isEmpty())
            {
                final String path = widget_property.getPath();
                for (Widget w : other)
                {
                    final PointsWidgetProperty other_prop = (PointsWidgetProperty) w.getProperty(path);
                    undo.execute(new SetWidgetPointsAction(other_prop, result.get()));
                }
            }
        }
    };

    public PointsPropertyBinding(final UndoableActionManager undo,
                                 final Button field,
                                 final PointsWidgetProperty widget_property,
                                 final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(actionHandler);
        model_listener.propertyChanged(widget_property, null,  null);
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}
