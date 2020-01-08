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
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;

/** Bidirectional binding between a WidgetProperty and Java FX Node
 *  @author Kay Kasemir
 */
public class MacroizedWidgetPropertyBinding
       extends WidgetPropertyBinding<TextInputControl, MacroizedWidgetProperty<?>>
{
    private final UntypedWidgetPropertyListener model_listener = (p, o, n) ->
    {
        if (updating)
            return;
        updating = true;
        try
        {
            jfx_node.setText(widget_property.getSpecification().replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t"));
        }
        finally
        {
            updating = false;
        }
    };

    // Tried binding to JFX text property, but it changes
    // with every entered character.
    // An 'undoable' change for each char is too fine.
    // Desired are undo steps for new values that the user
    // confirms via 'enter'.
    private final ChangeListener<Boolean> focus_handler =
        (final ObservableValue<? extends Boolean> observable,
         final Boolean old_focus, final Boolean focus) ->
    {
        // Gain focus -> active. Loose focus -> submit 
        updating = true;
        // This will submit new value if looses focus.
        if (! focus)
        {
            submit(false);
            updating = false;
        }
    };

    private final EventHandler<KeyEvent> key_press_handler =
        (final KeyEvent event) ->
    {
        switch (event.getCode())
        {
        case SHIFT:
        case ALT:
        case CONTROL:
            // Ignore modifier keys
            break;
        case ESCAPE:
            // Revert original value, leave active state
            if (updating)
            {
                restore();
                updating = false;
            }
            break;
        case ENTER:
            // Submit value, leave active state
            submit(true);
            updating = false;
            break;
        default:
            // Any other key results in active state
            updating = true;
        }
    };

    private final EventHandler<ActionEvent> action_handler = event ->
    {
        submit(true);
        updating = false;
    };

    public MacroizedWidgetPropertyBinding(final UndoableActionManager undo,
                                          final TextInputControl field,
                                          final MacroizedWidgetProperty<?> widget_property,
                                          final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addUntypedPropertyListener(model_listener);
        jfx_node.setOnKeyPressed(key_press_handler);
        if (jfx_node instanceof TextField)
            ((TextField)jfx_node).setOnAction(action_handler);
        jfx_node.focusedProperty().addListener(focus_handler);
        restore();
    }

    @Override
    public void unbind()
    {
        jfx_node.focusedProperty().removeListener(focus_handler);
        if (jfx_node instanceof TextField)
            ((TextField)jfx_node).setOnAction(null);
        jfx_node.setOnKeyPressed(null);
        widget_property.removePropertyListener(model_listener);
        // Allow for submitting changes on picture background click
        submit(false);
    }

    private void submit(final boolean force)
    {
        final String text = jfx_node.getText().replaceAll("\\\\n", "\n").replaceAll("\\\\t", "\t");
        final boolean primary_changed = !widget_property.getSpecification().equals(text);
        if (primary_changed)
            undo.execute(new SetMacroizedWidgetPropertyAction(widget_property, text));
        // For multiple selection:
        // Block unintentional changes if field looses focus and there is no change in primary property
        // Allow for ENTER to copy through a value in primary property without its explicit change
        if ((! other.isEmpty()) && (force || primary_changed))
        {
            final String path = widget_property.getPath();
            for (Widget w : other)
            {
                final MacroizedWidgetProperty<?> other_prop = (MacroizedWidgetProperty<?>) w.getProperty(path);
                if (!text.equals(other_prop.getSpecification()))
                    undo.execute(new SetMacroizedWidgetPropertyAction(other_prop, text));
            }
        }
    }

    private void restore()
    {
        jfx_node.setText(widget_property.getSpecification().replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t"));
    }
}
