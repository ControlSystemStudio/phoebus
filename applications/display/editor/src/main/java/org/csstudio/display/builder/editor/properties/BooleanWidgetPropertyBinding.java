/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;

import org.csstudio.display.builder.editor.undo.SetMacroizedWidgetPropertyAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.BooleanWidgetProperty;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Bidirectional binding between a WidgetProperty and Java FX Node
 *  @author Kay Kasemir
 */
public class BooleanWidgetPropertyBinding
       extends WidgetPropertyBinding<ComboBox<String>, BooleanWidgetProperty>
{
    // Checkbox would be more natural for most boolean properties,
    // but also need to support macros.
    // ComboBox allows selecting true/false, but can also enter "$(SomeMacro)".

    // Would be nice to just listen to jfx_node.valueProperty(),
    // but want to support 'escape' and loss of focus to revert,
    // and only complete text confirmed with Enter is submitted as an undoable action,
    // not each key stroke.

    /** Update control if model changes */
    private final WidgetPropertyListener<Boolean> model_listener = (p, o, n) ->
    {
        if (updating)
            return;
        updating = true;
        try
        {
            jfx_node.setValue(widget_property.getSpecification());
        }
        finally
        {
            updating = false;
        }
    };

    /** When loosing focus, restore control to current value of property.
     *  (If user just submitted a new value, that's a NOP)
     */
    private final ChangeListener<Boolean> focus_handler =
        (final ObservableValue<? extends Boolean> observable,
         final Boolean old_focus, final Boolean focus) ->
    {
        if (! focus)
            restore();
        updating = focus;
    };

    /** Submit new value, either selected from list or typed with 'Enter' */
    private final EventHandler<ActionEvent> combo_handler = (final ActionEvent event) ->
    {
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

    public BooleanWidgetPropertyBinding(final UndoableActionManager undo,
                                        final ComboBox<String> field,
                                        final BooleanWidgetProperty widget_property,
                                        final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        restore();
        widget_property.addPropertyListener(model_listener);
        jfx_node.focusedProperty().addListener(focus_handler);
        jfx_node.addEventFilter(KeyEvent.KEY_PRESSED, key_filter);
        jfx_node.setOnAction(combo_handler);
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        jfx_node.removeEventFilter(KeyEvent.KEY_PRESSED, key_filter);
        jfx_node.focusedProperty().removeListener(focus_handler);
        widget_property.removePropertyListener(model_listener);
    }

    private void submit()
    {
        final String value = jfx_node.getValue();
        undo.execute(new SetMacroizedWidgetPropertyAction(widget_property, value));
        final String path = widget_property.getPath();
        for (Widget w : other)
        {
            final BooleanWidgetProperty other_prop = (BooleanWidgetProperty) w.getProperty(path);
            undo.execute(new SetMacroizedWidgetPropertyAction(other_prop, value));
        }
        updating = false;
    }

    private void restore()
    {
        final String orig = widget_property.getSpecification();
        // 'value' is the internal value of the combo box
        jfx_node.setValue(orig);
        // Also need to update the editor, which will otherwise
        // soon set the 'value'
        jfx_node.getEditor().setText(orig);
    }
}
