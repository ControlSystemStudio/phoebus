/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;

import org.csstudio.display.builder.editor.undo.SetWidgetClassAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.WidgetClassProperty;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Bidirectional binding between a WidgetProperty and Java FX Node
 *  @author Kay Kasemir
 */
public class WidgetClassBinding extends WidgetPropertyBinding<ComboBox<String>, WidgetClassProperty>
{
    /** Update control if model changes */
    private WidgetPropertyListener<String> model_listener = (p, o, n) ->
    {
        // Ignore model updates while we are updating the model,
        // or while user focused on combo box to use it
        if (updating  ||  jfx_node.isFocused())
            return;
        updating = true;
        restore();
        updating = false;
    };

    /** Submit new value, either selected from list or typed with 'Enter' */
    private final EventHandler<ActionEvent> combo_handler = (final ActionEvent event) ->
    {
        if (! updating  ||  jfx_node.isFocused())
            submit();
    };

    /** Revert on Escape, otherwise mark as active to prevent model updates */
    private final EventHandler<KeyEvent> key_filter = (final KeyEvent t) ->
    {
        if (t.getCode() == KeyCode.ESCAPE)
        {
            // Revert original value, leave active state
            if (updating)
            {
                restore();
                updating = false;
                t.consume();
            }
        }
        else // Any other key marks the control as being updated by user
            updating = true;
    };


    public WidgetClassBinding(final UndoableActionManager undo, final ComboBox<String> node,
                              final WidgetClassProperty widget_property, final List<Widget> other)
    {
        super(undo, node, widget_property, other);
    }

    @Override
    public void bind()
    {
        restore();
        widget_property.addPropertyListener(model_listener);
        jfx_node.addEventFilter(KeyEvent.KEY_PRESSED, key_filter);
        jfx_node.setOnAction(combo_handler);
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        jfx_node.removeEventFilter(KeyEvent.KEY_PRESSED, key_filter);
        widget_property.removePropertyListener(model_listener);
    }

    private void submit()
    {
        updating = true;
        final String value = jfx_node.getValue();
        undo.execute(new SetWidgetClassAction(widget_property, value));
        for (Widget w : other)
        {
            final WidgetClassProperty other_prop = (WidgetClassProperty) w.getProperty(widget_property.getName());
            undo.execute(new SetWidgetClassAction(other_prop, value));
        }
        updating = false;
    }

    /** Restore combo from model */
    private void restore()
    {
        // TODO: getEditor().setText()  instead of setValue??
        jfx_node.setValue(widget_property.getValue());
    }
}
