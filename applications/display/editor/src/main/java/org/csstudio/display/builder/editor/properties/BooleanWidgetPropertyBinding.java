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
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Bidirectional binding between a WidgetProperty and Java FX Node
 *  @author Kay Kasemir
 */
public class BooleanWidgetPropertyBinding
       extends WidgetPropertyBinding<ComboBox<String>, BooleanWidgetProperty>
{
    /** Checkbox used for plain boolean properties
     *
     *  <p>ComboBox is used for macro-based values, "$(SomeMacro)".
     */
    private CheckBox check;

    /** Enable macro editing? */
    private final ToggleButton macroButton;

    // Would be nice to just listen to jfx_node.valueProperty(),
    // but want to support 'escape' and loss of focus to revert,
    // and only complete text confirmed with Enter is submitted as an undoable action,
    // not each key stroke.

    /** Update controls if model changes */
    private final WidgetPropertyListener<Boolean> model_listener = (p, o, n) ->
    {
        if (updating)
            return;
        updating = true;
        try
        {
            restore();
        }
        finally
        {
            updating = false;
        }
    };

    private final EventHandler<ActionEvent> macro_button_handler = event -> restore();


    private final EventHandler<ActionEvent> check_handler = event ->
    {
        if (updating)
            return;

        // Update combo (see restore()), then update model
        final String spec = Boolean.toString(check.isSelected());
        jfx_node.setValue(spec);
        jfx_node.getEditor().setText(spec);
        submit(spec);
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
        // Get value from combo,
        // send to check box and model
        final String spec = jfx_node.getValue();
        check.setSelected(Boolean.parseBoolean(spec));
        submit(spec);
    };

    /** Revert on Escape, otherwise mark as active to prevent model updates */
    private final EventHandler<KeyEvent> key_filter = (final KeyEvent t) ->
    {
        if (t.getCode() == KeyCode.ESCAPE ||
            t.getCode() == KeyCode.TAB)
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
                                        final CheckBox check, final ComboBox<String> field,
                                        final ToggleButton macroButton,
                                        final BooleanWidgetProperty widget_property,
                                        final List<Widget> other)
    {
        super(undo, field, widget_property, other);
        this.check = check;
        this.macroButton = macroButton;
    }

    @Override
    public void bind()
    {
        restore();
        widget_property.addPropertyListener(model_listener);
        jfx_node.focusedProperty().addListener(focus_handler);
        jfx_node.addEventFilter(KeyEvent.KEY_PRESSED, key_filter);
        jfx_node.setOnAction(combo_handler);
        check.setOnAction(check_handler);
        macroButton.setOnAction(macro_button_handler);
    }

    @Override
    public void unbind()
    {
        macroButton.setOnAction(null);
        check.setOnAction(null);
        jfx_node.setOnAction(null);
        jfx_node.removeEventFilter(KeyEvent.KEY_PRESSED, key_filter);
        jfx_node.focusedProperty().removeListener(focus_handler);
        widget_property.removePropertyListener(model_listener);
    }

    /** Submit value to model
     *
     *  @param value Value to submit
     */
    private void submit(final String value)
    {
        updating = true;
        undo.execute(new SetMacroizedWidgetPropertyAction(widget_property, value));
        if (! other.isEmpty())
        {
            final String path = widget_property.getPath();
            for (Widget w : other)
            {
                final BooleanWidgetProperty other_prop = (BooleanWidgetProperty) w.getProperty(path);
                undo.execute(new SetMacroizedWidgetPropertyAction(other_prop, value));
            }
        }
        updating = false;
    }

    private void restore()
    {
        final String orig = widget_property.getSpecification();
        check.setSelected(Boolean.parseBoolean(orig));

        // 'value' is the internal value of the combo box
        jfx_node.setValue(orig);
        // Also need to update the editor, which will otherwise
        // soon set the 'value'
        jfx_node.getEditor().setText(orig);

        final boolean use_macro = MacroHandler.containsMacros(orig) || macroButton.isSelected();
        jfx_node.setVisible(use_macro);
        check.setVisible(! use_macro);
    }
}
