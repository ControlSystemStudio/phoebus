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
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ActionsWidgetProperty;
import org.csstudio.display.builder.representation.javafx.ActionsDialog;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between an actions property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionsPropertyBinding
       extends WidgetPropertyBinding<Button, ActionsWidgetProperty>
{
    /** Update property panel field as model changes */
    private final WidgetPropertyListener<ActionInfos> model_listener = (p, o, n) ->
    {
        jfx_node.setText(widget_property.toString());
    };

    /** Update model from user input */
    private EventHandler<ActionEvent> action_handler = event ->
    {
        final ActionsDialog dialog = new ActionsDialog(widget_property.getWidget(), widget_property.getValue(), jfx_node);

        if (! other.isEmpty())
        {   // Update dialog's header to include warning about affected widgets
            final StringBuilder buf = new StringBuilder();
            buf.append(dialog.getHeaderText()).append("\n\n");
            buf.append("ATTENTION, this will update the actions of ").append(other.size() + 1).append(" widgets:\n");
            buf.append(" - ").append(widget_property.getWidget());
            int count = 1;
            for (Widget w : other)
            {
                buf.append("\n - ").append(w);
                if (++count > 4)
                {
                    final int left = other.size() + 1 - count;
                    if (left > 0)
                        buf.append(" ... ").append(left).append(" more ...");
                    break;
                }
            }
            dialog.setHeaderText(buf.toString());
        }

        final Optional<ActionInfos> result = dialog.showAndWait();
        if (result.isPresent())
        {
            undo.execute(new SetWidgetPropertyAction<>(widget_property, result.get()));
            if (! other.isEmpty())
            {
                final String path = widget_property.getPath();
                for (Widget w : other)
                {
                    final ActionsWidgetProperty other_prop = (ActionsWidgetProperty) w.getProperty(path);
                    undo.execute(new SetWidgetPropertyAction<>(other_prop, result.get()));
                }
            }
        }
    };

    public ActionsPropertyBinding(final UndoableActionManager undo,
                                  final Button field,
                                  final ActionsWidgetProperty widget_property,
                                  final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(action_handler);
        jfx_node.setText(widget_property.toString());
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}
