/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;

import org.csstudio.display.builder.editor.undo.SetWidgetEnumPropertyAction;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Bidirectional binding between a WidgetProperty and Java FX Node
 *  @author Kay Kasemir
 */
public class EnumWidgetPropertyBinding
       extends WidgetPropertyBinding<ComboBox<String>, EnumWidgetProperty<?>>
{
    // ComboBox allows selecting an enum label, but can also enter "$(SomeMacro)".

    // Would be nice to just listen to jfx_node.valueProperty(),
    // but want to support 'escape' and loss of focus to revert,
    // and only complete text confirmed with Enter is submitted as an undoable action,
    // not each key stroke.

    /** Update control if model changes */
    private final UntypedWidgetPropertyListener model_listener = (p, o, n) ->
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

    public EnumWidgetPropertyBinding(final UndoableActionManager undo,
                                     final ComboBox<String> field,
                                     final EnumWidgetProperty<?> widget_property,
                                     final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        restore();
        widget_property.addUntypedPropertyListener(model_listener);
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

    /** Submit combo value to model */
    private void submit()
    {
        final String entered = jfx_node.getValue();
        // Combo switched to editable, but user never entered anything?
        if (entered == null)
            return;

        updating = true;
        final int ordinal = jfx_node.getItems().indexOf(entered);

        final Object value = (ordinal >= 0) ? ordinal : entered;
        undo.execute(new SetWidgetEnumPropertyAction(widget_property, value));
        if (! other.isEmpty())
        {
            final String path = widget_property.getPath();
            for (Widget w : other)
            {
                final EnumWidgetProperty<?> other_prop = (EnumWidgetProperty<?>) w.getProperty(path);
                undo.execute(new SetWidgetEnumPropertyAction(other_prop, value));
            }
        }
        updating = false;
    }

    /** Restore combo from model */
    void restore()
    {
        final String spec = widget_property.getSpecification();
        // Try to resolve spec to enum ordinal
        try
        {
            final int ordinal = Integer.parseInt(spec);
            final String label = widget_property.getLabels()[ordinal];
            jfx_node.setValue(label);
            jfx_node.getEditor().setText(label);
            return;
        }
        catch (Throwable ex)
        {
            // Ignore
        }

        // Show spec, have no label/ordinal
        jfx_node.getEditor().setText(spec);
    }
}
